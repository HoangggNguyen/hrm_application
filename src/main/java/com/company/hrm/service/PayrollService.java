package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.PayrollPeriodForm;
import com.company.hrm.dto.PayrollPeriodSummary;
import com.company.hrm.dto.PayslipEditForm;
import com.company.hrm.entity.AttendanceStatus;
import com.company.hrm.entity.Contract;
import com.company.hrm.entity.ContractStatus;
import com.company.hrm.entity.Employee;
import com.company.hrm.entity.EmployeeStatus;
import com.company.hrm.entity.LeaveRequest;
import com.company.hrm.entity.LeaveStatus;
import com.company.hrm.entity.PayrollPeriod;
import com.company.hrm.entity.PayrollStatus;
import com.company.hrm.entity.Payslip;
import com.company.hrm.entity.Role;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.repository.AttendanceRepository;
import com.company.hrm.repository.ContractRepository;
import com.company.hrm.repository.EmployeeRepository;
import com.company.hrm.repository.LeaveRequestRepository;
import com.company.hrm.repository.PayrollPeriodRepository;
import com.company.hrm.repository.PayslipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private static final Set<AttendanceStatus> COUNTED_ATTENDANCE = EnumSet.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE);
    private static final Set<PayrollStatus> VISIBLE_STATUSES = EnumSet.of(PayrollStatus.CONFIRMED, PayrollStatus.PAID);
    private final PayrollPeriodRepository periodRepository;
    private final PayslipRepository payslipRepository;
    private final EmployeeRepository employeeRepository;
    private final ContractRepository contractRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveService leaveService;

    public List<PayrollPeriodSummary> periods(HrmUserDetails user) {
        requireHr(user);
        return periodRepository.findAllByOrderByYearDescMonthDesc().stream().map(period -> {
            List<Payslip> slips = payslipRepository.findByPeriod_IdOrderByEmployee_Code(period.getId());
            BigDecimal total = slips.stream().map(Payslip::getNetSalary).reduce(BigDecimal.ZERO, BigDecimal::add);
            return new PayrollPeriodSummary(period, slips.size(), total);
        }).toList();
    }

    public PayrollPeriod period(HrmUserDetails user, Long id) {
        requireHr(user);
        return periodRepository.findById(id).orElseThrow(() -> new BusinessException("Payroll period was not found."));
    }

    public List<Payslip> payslips(HrmUserDetails user, Long periodId) {
        requireHr(user);
        period(user, periodId);
        return payslipRepository.findByPeriod_IdOrderByEmployee_Code(periodId);
    }

    public List<Payslip> myPayslips(HrmUserDetails user) {
        Long employeeId = requireOwnEmployee(user);
        return payslipRepository.findByEmployee_IdOrderByPeriod_YearDescPeriod_MonthDesc(employeeId).stream()
                .filter(slip -> VISIBLE_STATUSES.contains(slip.getPeriod().getStatus())).toList();
    }

    @Transactional
    public PayrollPeriod createPeriod(HrmUserDetails user, PayrollPeriodForm form) {
        requireHr(user);
        if (periodRepository.existsByMonthAndYear(form.getMonth(), form.getYear())) {
            throw new BusinessException("A payroll period already exists for this month and year.");
        }
        PayrollPeriod period = new PayrollPeriod();
        period.setMonth(form.getMonth());
        period.setYear(form.getYear());
        period.setStatus(PayrollStatus.DRAFT);
        return periodRepository.save(period);
    }

    @Transactional
    public void generate(HrmUserDetails user, Long periodId) {
        requireHr(user);
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessException("Payroll period was not found."));
        requireDraft(period);
        payslipRepository.deleteByPeriod_Id(periodId);
        YearMonth yearMonth = YearMonth.of(period.getYear(), period.getMonth());
        LocalDate from = yearMonth.atDay(1);
        LocalDate to = yearMonth.atEndOfMonth();
        int standardDays = leaveService.countWeekdays(from, to).intValueExact();
        if (standardDays == 0) {
            throw new BusinessException("This payroll period has no standard working days.");
        }
        List<LeaveRequest> approvedRequests = leaveRequestRepository
                .findByStatusInOrderByFromDateAsc(List.of(LeaveStatus.APPROVED));
        Map<Long, List<LeaveRequest>> paidLeaveByEmployee = approvedRequests.stream()
                .filter(request -> request.getLeaveType().isPaid() && request.getLeaveType().getDaysPerYear() > 0)
                .filter(request -> !request.getFromDate().isAfter(to) && !request.getToDate().isBefore(from))
                .collect(Collectors.groupingBy(request -> request.getEmployee().getId()));
        Map<Long, Contract> activeContracts = contractRepository.findByStatus(ContractStatus.ACTIVE).stream()
                .collect(Collectors.toMap(contract -> contract.getEmployee().getId(), Function.identity(),
                        (first, second) -> first));

        for (Employee employee : employeeRepository.findAllByOrderByCode()) {
            if (employee.getStatus() != EmployeeStatus.ACTIVE && employee.getStatus() != EmployeeStatus.PROBATION) {
                continue;
            }
            Contract contract = activeContracts.get(employee.getId());
            if (contract == null) {
                continue;
            }
            long attendanceDays = attendanceRepository.countByEmployee_IdAndWorkDateBetweenAndStatusIn(
                    employee.getId(), from, to, COUNTED_ATTENDANCE);
            BigDecimal paidLeaveDays = paidLeaveByEmployee.getOrDefault(employee.getId(), List.of()).stream()
                    .map(request -> {
                        LocalDate leaveFrom = request.getFromDate().isBefore(from) ? from : request.getFromDate();
                        LocalDate leaveTo = request.getToDate().isAfter(to) ? to : request.getToDate();
                        return leaveService.countWeekdays(leaveFrom, leaveTo);
                    }).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal actualDays = BigDecimal.valueOf(attendanceDays).add(paidLeaveDays)
                    .min(BigDecimal.valueOf(standardDays));
            Payslip slip = new Payslip();
            slip.setPeriod(period);
            slip.setEmployee(employee);
            slip.setBaseSalary(contract.getBaseSalary());
            slip.setStandardDays(standardDays);
            slip.setActualDays(actualDays);
            slip.setAllowance(BigDecimal.ZERO);
            slip.setBonus(BigDecimal.ZERO);
            slip.setDeduction(BigDecimal.ZERO);
            recalculate(slip);
            payslipRepository.save(slip);
        }
    }

    @Transactional
    public void editPayslip(HrmUserDetails user, Long payslipId, PayslipEditForm form) {
        requireHr(user);
        Payslip slip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new BusinessException("Payslip was not found."));
        requireDraft(slip.getPeriod());
        slip.setAllowance(form.getAllowance());
        slip.setBonus(form.getBonus());
        slip.setDeduction(form.getDeduction());
        recalculate(slip);
        payslipRepository.save(slip);
    }

    @Transactional
    public void confirm(HrmUserDetails user, Long periodId) {
        requireHr(user);
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessException("Payroll period was not found."));
        requireDraft(period);
        period.setStatus(PayrollStatus.CONFIRMED);
        periodRepository.save(period);
    }

    @Transactional
    public void markPaid(HrmUserDetails user, Long periodId) {
        requireHr(user);
        PayrollPeriod period = periodRepository.findById(periodId)
                .orElseThrow(() -> new BusinessException("Payroll period was not found."));
        if (period.getStatus() != PayrollStatus.CONFIRMED) {
            throw new BusinessException("Only confirmed payroll periods can be marked paid.");
        }
        period.setStatus(PayrollStatus.PAID);
        periodRepository.save(period);
    }

    public Payslip payslip(HrmUserDetails user, Long payslipId) {
        Payslip slip = payslipRepository.findById(payslipId)
                .orElseThrow(() -> new BusinessException("Payslip was not found."));
        if (user.role() == Role.ADMIN || user.role() == Role.HR) {
            return slip;
        }
        if (user.employeeId() == null || !slip.getEmployee().getId().equals(user.employeeId())) {
            throw new AccessDeniedException("You can only view your own payslip.");
        }
        if (!VISIBLE_STATUSES.contains(slip.getPeriod().getStatus())) {
            throw new AccessDeniedException("This payslip is not available yet.");
        }
        return slip;
    }

    private void recalculate(Payslip slip) {
        BigDecimal salaryForDays = slip.getBaseSalary()
                .divide(BigDecimal.valueOf(slip.getStandardDays()), 10, RoundingMode.HALF_UP)
                .multiply(slip.getActualDays())
                .setScale(0, RoundingMode.HALF_UP);
        BigDecimal gross = salaryForDays.add(slip.getAllowance()).add(slip.getBonus());
        slip.setGross(gross);
        slip.setNetSalary(gross.subtract(slip.getDeduction()).max(BigDecimal.ZERO));
    }

    private void requireDraft(PayrollPeriod period) {
        if (period.getStatus() != PayrollStatus.DRAFT) {
            throw new BusinessException("This payroll period is locked.");
        }
    }

    private Long requireOwnEmployee(HrmUserDetails user) {
        if (user.employeeId() == null) {
            throw new AccessDeniedException("This account is not linked to an employee.");
        }
        return user.employeeId();
    }

    private void requireHr(HrmUserDetails user) {
        if (user.role() != Role.ADMIN && user.role() != Role.HR) {
            throw new AccessDeniedException("You do not have permission to manage payroll.");
        }
    }
}