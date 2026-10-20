package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.entity.Attendance;
import com.company.hrm.entity.AttendanceStatus;
import com.company.hrm.entity.Employee;
import com.company.hrm.entity.Role;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.repository.AttendanceRepository;
import com.company.hrm.repository.EmployeeRepository;
import com.company.hrm.repository.HolidayRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private HolidayRepository holidayRepository;
    @Mock private JdbcTemplate jdbcTemplate;
    @InjectMocks private AttendanceService attendanceService;

    @Test
    void checkInIsLateAfterConfiguredWorkStart() {
        Employee employee = new Employee();
        employee.setId(3L);
        when(attendanceRepository.findByEmployee_IdAndWorkDate(eq(3L), any(LocalDate.class))).thenReturn(Optional.empty());
        when(holidayRepository.existsByHolidayDate(any(LocalDate.class))).thenReturn(false);
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(employee));
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq("work_start"))).thenReturn("00:00");
        when(jdbcTemplate.queryForObject(anyString(), eq(String.class), eq("late_grace_minutes"))).thenReturn("0");
        when(attendanceRepository.saveAndFlush(any(Attendance.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Attendance attendance = attendanceService.checkIn(employeeUser());

        assertEquals(AttendanceStatus.LATE, attendance.getStatus());
    }

    @Test
    void rejectsDoubleCheckIn() {
        when(attendanceRepository.findByEmployee_IdAndWorkDate(eq(3L), any(LocalDate.class)))
                .thenReturn(Optional.of(new Attendance()));

        assertThrows(BusinessException.class, () -> attendanceService.checkIn(employeeUser()));
    }

    private HrmUserDetails employeeUser() {
        return new HrmUserDetails(4L, "employee", "x", Role.EMPLOYEE, true, 3L);
    }
}