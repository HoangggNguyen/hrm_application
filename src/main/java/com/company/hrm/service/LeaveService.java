package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.LeaveRequestForm;
import com.company.hrm.dto.LeaveTypeForm;
import com.company.hrm.entity.Employee;
import com.company.hrm.entity.LeaveBalance;
import com.company.hrm.entity.LeaveRequest;
import com.company.hrm.entity.LeaveStatus;
import com.company.hrm.entity.LeaveType;
import com.company.hrm.entity.Role;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.repository.EmployeeRepository;
import com.company.hrm.repository.HolidayRepository;
import com.company.hrm.repository.LeaveBalanceRepository;
import com.company.hrm.repository.LeaveRequestRepository;
import com.company.hrm.repository.LeaveTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LeaveService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<LeaveStatus> OVERLAPPING_STATUSES = EnumSet.of(LeaveStatus.PENDING, LeaveStatus.APPROVED);
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;
    private final HolidayRepository holidayRepository;

    public List<LeaveBalance> myBalances(HrmUserDetails user, int year) {
        Long employeeId = requireOwnEmployee(user);
        return leaveBalanceRepository.findByEmployee_IdAndYearOrderByLeaveType_Name(employeeId, year);
    }

    public List<LeaveRequest> myRequests(HrmUserDetails user) {
        Long employeeId = requireOwnEmployee(user);
        return leaveRequestRepository.findByEmployee_IdOrderByFromDateDesc(employeeId);
    }

    public List<LeaveRequest> pendingApprovals(HrmUserDetails user) {
        if (user.role() == Role.ADMIN || user.role() == Role.HR) {
            return leaveRequestRepository.findByStatusInOrderByFromDateAsc(List.of(LeaveStatus.PENDING)).stream()
                    .filter(request -> user.employeeId() == null
                            || !request.getEmployee().getId().equals(user.employeeId())).toList();
        }
        if (user.role() != Role.MANAGER || user.employeeId() == null) {
            throw new AccessDeniedException("You cannot approve leave requests.");
        }
        List<Long> reportIds = employeeRepository.findByManager_IdOrderByCode(user.employeeId()).stream()
                .map(Employee::getId).toList();
        if (reportIds.isEmpty()) {
            return List.of();
        }
        return leaveRequestRepository.findByEmployee_IdInAndStatusOrderByFromDateAsc(reportIds, LeaveStatus.PENDING);
    }

    public List<LeaveType> leaveTypes(HrmUserDetails user) {
        requireHr(user);
        return leaveTypeRepository.findAllByOrderById();
    }

    public List<LeaveType> requestTypes(HrmUserDetails user) {
        requireOwnEmployee(user);
        return leaveTypeRepository.findAllByOrderById();
    }

    @Transactional
    public LeaveRequest create(HrmUserDetails user, LeaveRequestForm form) {
        Long employeeId = requireOwnEmployee(user);
        if (form.getFromDate().isAfter(form.getToDate())) {
            throw new BusinessException("The start date must be on or before the end date.");
        }
        if (form.getFromDate().getYear() != form.getToDate().getYear()) {
            throw new BusinessException("A leave request cannot span two calendar years.");
        }
        LeaveType leaveType = leaveTypeRepository.findById(form.getLeaveTypeId())
                .orElseThrow(() -> new BusinessException("Leave type was not found."));
        BigDecimal days = countWeekdays(form.getFromDate(), form.getToDate());
        if (days.signum() <= 0) {
            throw new BusinessException("The selected dates contain no working days.");
        }
        boolean overlap = leaveRequestRepository.existsByEmployee_IdAndStatusInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                employeeId, OVERLAPPING_STATUSES, form.getToDate(), form.getFromDate());
        if (overlap) {
            throw new BusinessException("This request overlaps an existing pending or approved request.");
        }
        if (leaveType.isPaid() && leaveType.getDaysPerYear() > 0) {
            LeaveBalance balance = leaveBalanceRepository.findByEmployee_IdAndLeaveType_IdAndYear(
                            employeeId, leaveType.getId(), form.getFromDate().getYear())
                    .orElseThrow(() -> new BusinessException("No balance is available for this leave type and year."));
            if (days.compareTo(balance.getTotal().subtract(balance.getUsed())) > 0) {
                throw new BusinessException("The requested days exceed the remaining leave balance.");
            }
        }
        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee record was not found.")));
        request.setLeaveType(leaveType);
        request.setFromDate(form.getFromDate());
        request.setToDate(form.getToDate());
        request.setDays(days);
        request.setReason(form.getReason().trim());
        request.setStatus(LeaveStatus.PENDING);
        return leaveRequestRepository.save(request);
    }

    @Transactional
    public void cancel(HrmUserDetails user, Long requestId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("Leave request was not found."));
        if (user.employeeId() == null || !request.getEmployee().getId().equals(user.employeeId())) {
            throw new AccessDeniedException("You can only cancel your own leave request.");
        }
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Only pending requests can be cancelled.");
        }
        request.setStatus(LeaveStatus.CANCELLED);
        leaveRequestRepository.save(request);
    }

    @Transactional
    public void approve(HrmUserDetails user, Long requestId) {
        LeaveRequest request = getPendingRequest(requestId);
        requireApprover(user, request);
        request.setStatus(LeaveStatus.APPROVED);
        request.setApprover(user.employeeId() == null ? null : employeeRepository.findById(user.employeeId()).orElse(null));
        request.setDecidedAt(LocalDateTime.now(BUSINESS_ZONE));
        LeaveType leaveType = request.getLeaveType();
        if (leaveType.isPaid() && leaveType.getDaysPerYear() > 0) {
            LeaveBalance balance = leaveBalanceRepository.findByEmployee_IdAndLeaveType_IdAndYear(
                            request.getEmployee().getId(), leaveType.getId(), request.getFromDate().getYear())
                    .orElseThrow(() -> new BusinessException("No balance is available for this leave type and year."));
            if (request.getDays().compareTo(balance.getTotal().subtract(balance.getUsed())) > 0) {
                throw new BusinessException("The remaining leave balance is no longer sufficient.");
            }
            balance.setUsed(balance.getUsed().add(request.getDays()));
            leaveBalanceRepository.save(balance);
        }
        leaveRequestRepository.save(request);
    }

    @Transactional
    public void reject(HrmUserDetails user, Long requestId, String reason) {
        LeaveRequest request = getPendingRequest(requestId);
        requireApprover(user, request);
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A rejection reason is required.");
        }
        request.setStatus(LeaveStatus.REJECTED);
        request.setRejectReason(reason.trim());
        request.setApprover(user.employeeId() == null ? null : employeeRepository.findById(user.employeeId()).orElse(null));
        request.setDecidedAt(LocalDateTime.now(BUSINESS_ZONE));
        leaveRequestRepository.save(request);
    }

    @Transactional
    public void saveLeaveType(HrmUserDetails user, Long id, LeaveTypeForm form) {
        requireHr(user);
        LeaveType type = id == null ? new LeaveType() : leaveTypeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Leave type was not found."));
        String name = form.getName().trim();
        if (leaveTypeRepository.existsByNameIgnoreCase(name) && !name.equalsIgnoreCase(type.getName())) {
            throw new BusinessException("Leave type name is already in use.");
        }
        type.setName(name);
        type.setDaysPerYear(form.getDaysPerYear());
        type.setPaid(form.isPaid());
        leaveTypeRepository.save(type);
    }

    @Transactional
    public void deleteLeaveType(HrmUserDetails user, Long id) {
        requireHr(user);
        if (leaveRequestRepository.existsByLeaveType_Id(id) || leaveBalanceRepository.existsByLeaveType_Id(id)) {
            throw new BusinessException("This leave type is referenced and cannot be deleted.");
        }
        leaveTypeRepository.deleteById(id);
    }

    public BigDecimal countWeekdays(LocalDate from, LocalDate to) {
        Set<LocalDate> holidays = holidayRepository.findByHolidayDateBetween(from, to).stream()
                .map(holiday -> holiday.getHolidayDate()).collect(java.util.stream.Collectors.toSet());
        long days = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY
                    && !holidays.contains(date)) {
                days++;
            }
        }
        return BigDecimal.valueOf(days);
    }

    private LeaveRequest getPendingRequest(Long requestId) {
        LeaveRequest request = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("Leave request was not found."));
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BusinessException("Only pending requests can be decided.");
        }
        return request;
    }

    private void requireApprover(HrmUserDetails user, LeaveRequest request) {
        if (user.employeeId() != null && request.getEmployee().getId().equals(user.employeeId())) {
            throw new AccessDeniedException("You cannot decide your own leave request.");
        }
        if (user.role() == Role.ADMIN || user.role() == Role.HR) {
            return;
        }
        if (user.role() == Role.MANAGER && user.employeeId() != null
                && request.getEmployee().getManager() != null
                && request.getEmployee().getManager().getId().equals(user.employeeId())) {
            return;
        }
        throw new AccessDeniedException("You cannot decide this leave request.");
    }

    private Long requireOwnEmployee(HrmUserDetails user) {
        if (user.employeeId() == null) {
            throw new AccessDeniedException("This account is not linked to an employee.");
        }
        return user.employeeId();
    }

    private void requireHr(HrmUserDetails user) {
        if (user.role() != Role.ADMIN && user.role() != Role.HR) {
            throw new AccessDeniedException("You do not have permission to manage leave types.");
        }
    }
}