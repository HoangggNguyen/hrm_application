package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.AttendanceForm;
import com.company.hrm.dto.AttendanceSummary;
import com.company.hrm.entity.Attendance;
import com.company.hrm.entity.AttendanceStatus;
import com.company.hrm.entity.Employee;
import com.company.hrm.entity.EmployeeStatus;
import com.company.hrm.entity.Role;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.repository.AttendanceRepository;
import com.company.hrm.repository.EmployeeRepository;
import com.company.hrm.repository.HolidayRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final HolidayRepository holidayRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public Attendance checkIn(HrmUserDetails user) {
        Long employeeId = requireOwnEmployee(user);
        LocalDateTime now = LocalDateTime.now(BUSINESS_ZONE);
        LocalDate workDate = now.toLocalDate();
        ensureWorkday(workDate);
        if (attendanceRepository.findByEmployee_IdAndWorkDate(employeeId, workDate).isPresent()) {
            throw new BusinessException("You have already checked in today.");
        }
        Attendance attendance = new Attendance();
        attendance.setEmployee(employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee record was not found.")));
        attendance.setWorkDate(workDate);
        attendance.setCheckIn(now);
        attendance.setStatus(isLate(now.toLocalTime()) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);
        try {
            return attendanceRepository.saveAndFlush(attendance);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException("You have already checked in today.");
        }
    }

    @Transactional
    public Attendance checkOut(HrmUserDetails user) {
        Long employeeId = requireOwnEmployee(user);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        ensureWorkday(today);
        Attendance attendance = attendanceRepository.findByEmployee_IdAndWorkDate(employeeId, today)
                .orElseThrow(() -> new BusinessException("Check in before checking out."));
        if (attendance.getCheckIn() == null) {
            throw new BusinessException("Check in before checking out.");
        }
        if (attendance.getCheckOut() != null) {
            throw new BusinessException("You have already checked out today.");
        }
        attendance.setCheckOut(LocalDateTime.now(BUSINESS_ZONE));
        return attendanceRepository.save(attendance);
    }

    public Page<Attendance> search(HrmUserDetails user, YearMonth month, Long employeeId, Long departmentId,
                                   Pageable pageable) {
        Scope scope = resolveScope(user);
        if (scope.employeeId() != null && employeeId != null && !scope.employeeId().equals(employeeId)) {
            boolean inScope = scope.includeReports() && employeeRepository.findById(employeeId)
                    .map(report -> report.getManager() != null
                            && report.getManager().getId().equals(scope.employeeId()))
                    .orElse(false);
            if (!inScope) {
                throw new AccessDeniedException("You cannot view attendance outside your scope.");
            }
        }
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        Specification<Attendance> specification = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.between(root.get("workDate"), from, to));
            if (employeeId != null) {
                predicates.add(builder.equal(root.get("employee").get("id"), employeeId));
            }
            if (departmentId != null) {
                predicates.add(builder.equal(root.get("employee").get("department").get("id"), departmentId));
            }
            if (scope.employeeId() != null) {
                Predicate own = builder.equal(root.get("employee").get("id"), scope.employeeId());
                if (scope.includeReports()) {
                    predicates.add(builder.or(own,
                            builder.equal(root.get("employee").get("manager").get("id"), scope.employeeId())));
                } else {
                    predicates.add(own);
                }
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return attendanceRepository.findAll(specification, pageable);
    }

    public List<AttendanceSummary> monthlySummary(HrmUserDetails user, YearMonth month,
                                                  Long employeeFilter, Long departmentFilter) {
        Scope scope = resolveScope(user);
        List<Employee> employees = employeeRepository.findAllByOrderByCode().stream()
                .filter(employee -> employee.getStatus() != EmployeeStatus.RESIGNED)
                .filter(employee -> scope.employeeId() == null || employee.getId().equals(scope.employeeId())
                        || scope.includeReports() && employee.getManager() != null
                        && employee.getManager().getId().equals(scope.employeeId()))
                .filter(employee -> employeeFilter == null || employee.getId().equals(employeeFilter))
                .filter(employee -> departmentFilter == null || employee.getDepartment().getId().equals(departmentFilter))
                .toList();
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        return employees.stream().map(employee -> {
            long onTime = attendanceRepository.countByEmployee_IdAndWorkDateBetweenAndStatus(
                    employee.getId(), from, to, AttendanceStatus.PRESENT);
            long late = attendanceRepository.countByEmployee_IdAndWorkDateBetweenAndStatus(
                    employee.getId(), from, to, AttendanceStatus.LATE);
            return new AttendanceSummary(employee.getCode(), employee.getFullName(), onTime + late, late);
        }).toList();
    }

    public Attendance today(HrmUserDetails user) {
        Long employeeId = user.employeeId();
        return employeeId == null ? null : attendanceRepository.findByEmployee_IdAndWorkDate(employeeId,
                LocalDate.now(BUSINESS_ZONE)).orElse(null);
    }

    @Transactional
    public void save(HrmUserDetails user, Long id, AttendanceForm form) {
        requireHr(user);
        Attendance attendance;
        if (id == null) {
            if (attendanceRepository.findByEmployee_IdAndWorkDate(form.getEmployeeId(), form.getWorkDate()).isPresent()) {
                throw new BusinessException("An attendance record already exists for this employee and date.");
            }
            attendance = new Attendance();
        } else {
            attendance = attendanceRepository.findById(id)
                    .orElseThrow(() -> new BusinessException("Attendance record was not found."));
            boolean duplicate = attendanceRepository.findByEmployee_IdAndWorkDate(form.getEmployeeId(), form.getWorkDate())
                    .filter(existing -> !existing.getId().equals(id)).isPresent();
            if (duplicate) {
                throw new BusinessException("An attendance record already exists for this employee and date.");
            }
        }
        attendance.setEmployee(employeeRepository.findById(form.getEmployeeId())
                .orElseThrow(() -> new BusinessException("Employee was not found.")));
        attendance.setWorkDate(form.getWorkDate());
        attendance.setCheckIn(form.getCheckIn());
        attendance.setCheckOut(form.getCheckOut());
        attendance.setStatus(form.getStatus());
        attendance.setNote(form.getNote() == null || form.getNote().isBlank() ? null : form.getNote().trim());
        attendanceRepository.save(attendance);
    }

    private void ensureWorkday(LocalDate date) {
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY
                || holidayRepository.existsByHolidayDate(date)) {
            throw new BusinessException("Attendance check-in is unavailable on weekends and holidays.");
        }
    }

    private boolean isLate(LocalTime checkIn) {
        String start = setting("work_start", "08:30");
        int graceMinutes = Integer.parseInt(setting("late_grace_minutes", "0"));
        return checkIn.isAfter(LocalTime.parse(start).plusMinutes(graceMinutes));
    }

    private String setting(String key, String fallback) {
        String value = jdbcTemplate.queryForObject(
            "SELECT setting_value FROM settings WHERE setting_key = ?", String.class, key);
        return value == null ? fallback : value;
    }

    private Long requireOwnEmployee(HrmUserDetails user) {
        if (user.employeeId() == null) {
            throw new AccessDeniedException("This account is not linked to an employee.");
        }
        return user.employeeId();
    }

    private Scope resolveScope(HrmUserDetails user) {
        if (user.role() == Role.ADMIN || user.role() == Role.HR) {
            return new Scope(null, false);
        }
        Long ownEmployeeId = requireOwnEmployee(user);
        return new Scope(ownEmployeeId, user.role() == Role.MANAGER);
    }

    private void requireHr(HrmUserDetails user) {
        if (user.role() != Role.ADMIN && user.role() != Role.HR) {
            throw new AccessDeniedException("You do not have permission to edit attendance.");
        }
    }

    private record Scope(Long employeeId, boolean includeReports) {
    }
}