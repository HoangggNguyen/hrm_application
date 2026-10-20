package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.PayslipEditForm;
import com.company.hrm.entity.AttendanceStatus;
import com.company.hrm.entity.Contract;
import com.company.hrm.entity.ContractStatus;
import com.company.hrm.entity.ContractType;
import com.company.hrm.entity.Employee;
import com.company.hrm.entity.EmployeeStatus;
import com.company.hrm.entity.PayrollPeriod;
import com.company.hrm.entity.PayrollStatus;
import com.company.hrm.entity.Payslip;
import com.company.hrm.entity.Role;
import com.company.hrm.repository.AttendanceRepository;
import com.company.hrm.repository.ContractRepository;
import com.company.hrm.repository.EmployeeRepository;
import com.company.hrm.repository.LeaveRequestRepository;
import com.company.hrm.repository.PayrollPeriodRepository;
import com.company.hrm.repository.PayslipRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock private PayrollPeriodRepository periodRepository;
    @Mock private PayslipRepository payslipRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveService leaveService;
    @InjectMocks private PayrollService payrollService;

    @Test
    void recalculatesGrossWithHalfUpAndFloorsNetAtZero() {
        PayrollPeriod period = new PayrollPeriod();
        period.setStatus(PayrollStatus.DRAFT);
        Payslip slip = new Payslip();
        slip.setPeriod(period);
        slip.setBaseSalary(new BigDecimal("1001"));
        slip.setStandardDays(3);
        slip.setActualDays(BigDecimal.ONE);
        when(payslipRepository.findById(7L)).thenReturn(Optional.of(slip));
        when(payslipRepository.save(any(Payslip.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PayslipEditForm form = new PayslipEditForm();
        form.setAllowance(new BigDecimal("100"));
        form.setBonus(BigDecimal.ZERO);
        form.setDeduction(new BigDecimal("500"));

        payrollService.editPayslip(hrUser(), 7L, form);

        assertEquals(new BigDecimal("434"), slip.getGross());
        assertEquals(BigDecimal.ZERO, slip.getNetSalary());
    }

    @Test
    void generateUsesWeekdaysAndAttendanceToSetStandardAndActualDays() {
        PayrollPeriod period = new PayrollPeriod();
        period.setId(1L);
        period.setMonth(10);
        period.setYear(2026);
        period.setStatus(PayrollStatus.DRAFT);
        Employee employee = new Employee();
        employee.setId(3L);
        employee.setCode("EMP0003");
        employee.setStatus(EmployeeStatus.ACTIVE);
        Contract contract = new Contract();
        contract.setEmployee(employee);
        contract.setStatus(ContractStatus.ACTIVE);
        contract.setType(ContractType.INDEFINITE);
        contract.setBaseSalary(new BigDecimal("20000"));
        when(periodRepository.findById(1L)).thenReturn(Optional.of(period));
        when(leaveService.countWeekdays(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(new BigDecimal("22"));
        when(leaveRequestRepository.findByStatusInOrderByFromDateAsc(List.of(com.company.hrm.entity.LeaveStatus.APPROVED)))
                .thenReturn(List.of());
        when(contractRepository.findByStatus(ContractStatus.ACTIVE)).thenReturn(List.of(contract));
        when(employeeRepository.findAllByOrderByCode()).thenReturn(List.of(employee));
        when(attendanceRepository.countByEmployee_IdAndWorkDateBetweenAndStatusIn(eq(3L), any(LocalDate.class),
                any(LocalDate.class), anyCollection())).thenReturn(4L);

        payrollService.generate(hrUser(), 1L);

        ArgumentCaptor<Payslip> captor = ArgumentCaptor.forClass(Payslip.class);
        verify(payslipRepository).save(captor.capture());
        assertEquals(22, captor.getValue().getStandardDays());
        assertEquals(new BigDecimal("4"), captor.getValue().getActualDays());
        assertEquals(new BigDecimal("3636"), captor.getValue().getGross());
    }

    private HrmUserDetails hrUser() {
        return new HrmUserDetails(2L, "hr", "x", Role.HR, true, 1L);
    }
}