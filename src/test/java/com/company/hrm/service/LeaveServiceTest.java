package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.LeaveRequestForm;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private LeaveTypeRepository leaveTypeRepository;
    @Mock private LeaveBalanceRepository leaveBalanceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private HolidayRepository holidayRepository;
    @InjectMocks private LeaveService leaveService;

    @Test
    void countsWeekdaysAndExcludesHolidays() {
        var holiday = new com.company.hrm.entity.Holiday();
        holiday.setHolidayDate(LocalDate.of(2026, 10, 21));
        when(holidayRepository.findByHolidayDateBetween(LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 23)))
                .thenReturn(List.of(holiday));

        assertEquals(new BigDecimal("4"), leaveService.countWeekdays(
                LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 23)));
    }

    @Test
    void createsRequestWhenBalanceAndDatesAreValid() {
        LeaveType type = paidAnnualType();
        LeaveBalance balance = new LeaveBalance();
        balance.setTotal(new BigDecimal("12.0"));
        balance.setUsed(new BigDecimal("10.0"));
        Employee employee = new Employee();
        employee.setId(3L);
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(type));
        when(holidayRepository.findByHolidayDateBetween(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
        when(leaveRequestRepository.existsByEmployee_IdAndStatusInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                eq(3L), anyCollection(), any(LocalDate.class), any(LocalDate.class))).thenReturn(false);
        when(leaveBalanceRepository.findByEmployee_IdAndLeaveType_IdAndYear(3L, 1L, 2026))
                .thenReturn(Optional.of(balance));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeaveRequest request = leaveService.create(employeeUser(), requestForm(26, 27));

        assertEquals(new BigDecimal("2"), request.getDays());
        assertEquals(LeaveStatus.PENDING, request.getStatus());
    }

    @Test
    void rejectsOverlappingPendingOrApprovedRequest() {
        when(leaveTypeRepository.findById(1L)).thenReturn(Optional.of(paidAnnualType()));
        when(holidayRepository.findByHolidayDateBetween(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of());
        when(leaveRequestRepository.existsByEmployee_IdAndStatusInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                eq(3L), anyCollection(), any(LocalDate.class), any(LocalDate.class))).thenReturn(true);

        assertThrows(BusinessException.class, () -> leaveService.create(employeeUser(), requestForm(26, 27)));
    }

    @Test
    void managerCannotDecideRequestOutsideTheirReports() {
        Employee report = new Employee();
        report.setId(3L);
        Employee otherManager = new Employee();
        otherManager.setId(4L);
        report.setManager(otherManager);
        LeaveRequest request = new LeaveRequest();
        request.setEmployee(report);
        request.setStatus(LeaveStatus.PENDING);
        when(leaveRequestRepository.findById(9L)).thenReturn(Optional.of(request));

        assertThrows(AccessDeniedException.class,
                () -> leaveService.approve(new HrmUserDetails(2L, "manager", "x", Role.MANAGER, true, 2L), 9L));
    }

    private HrmUserDetails employeeUser() {
        return new HrmUserDetails(4L, "employee", "x", Role.EMPLOYEE, true, 3L);
    }

    private LeaveType paidAnnualType() {
        LeaveType type = new LeaveType();
        type.setId(1L);
        type.setName("Annual");
        type.setDaysPerYear(12);
        type.setPaid(true);
        return type;
    }

    private LeaveRequestForm requestForm(int fromDay, int toDay) {
        LeaveRequestForm form = new LeaveRequestForm();
        form.setLeaveTypeId(1L);
        form.setFromDate(LocalDate.of(2026, 10, fromDay));
        form.setToDate(LocalDate.of(2026, 10, toDay));
        form.setReason("Personal leave");
        return form;
    }
}