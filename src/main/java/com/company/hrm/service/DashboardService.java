package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.AttendanceSummary;
import com.company.hrm.dto.DashboardMetrics;
import com.company.hrm.entity.AttendanceStatus;
import com.company.hrm.entity.Contract;
import com.company.hrm.entity.ContractStatus;
import com.company.hrm.entity.Employee;
import com.company.hrm.entity.EmployeeStatus;
import com.company.hrm.entity.LeaveRequest;
import com.company.hrm.entity.LeaveStatus;
import com.company.hrm.entity.Role;
import com.company.hrm.repository.AttendanceRepository;
import com.company.hrm.repository.ContractRepository;
import com.company.hrm.repository.EmployeeRepository;
import com.company.hrm.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final ContractRepository contractRepository;

    public DashboardMetrics metrics(HrmUserDetails user) {
        List<Employee> inScope = scopedEmployees(user).stream()
                .filter(employee -> employee.getStatus() != EmployeeStatus.RESIGNED).toList();
        var employeeIds = inScope.stream().map(Employee::getId).collect(Collectors.toSet());
        LocalDate today = LocalDate.now();
        long onLeave = leaveRequestRepository.findByStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
                        LeaveStatus.APPROVED, today, today).stream()
                .filter(request -> employeeIds.contains(request.getEmployee().getId()))
                .map(request -> request.getEmployee().getId()).distinct().count();
        long late = inScope.stream().mapToLong(employee -> attendanceRepository
                .countByEmployee_IdAndWorkDateBetweenAndStatus(employee.getId(), today, today, AttendanceStatus.LATE)).sum();
        long probation = inScope.stream().filter(employee -> employee.getStatus() == EmployeeStatus.PROBATION).count();
        return new DashboardMetrics(inScope.size(), probation, onLeave, late);
    }

    public Map<String, Long> employeesByDepartment(HrmUserDetails user) {
        return scopedEmployees(user).stream().filter(employee -> employee.getStatus() != EmployeeStatus.RESIGNED)
                .collect(Collectors.groupingBy(employee -> employee.getDepartment().getName(),
                        Collectors.counting()));
    }

    public List<Contract> expiringContracts(HrmUserDetails user) {
        if (user.role() != Role.ADMIN && user.role() != Role.HR) {
            return List.of();
        }
        LocalDate today = LocalDate.now();
        LocalDate cutoff = today.plusDays(30);
        return contractRepository.findByStatus(ContractStatus.ACTIVE).stream()
                .filter(contract -> contract.getEndDate() != null && !contract.getEndDate().isBefore(today)
                        && !contract.getEndDate().isAfter(cutoff))
                .toList();
    }

    public List<Employee> scopedEmployees(HrmUserDetails user) {
        List<Employee> all = employeeRepository.findAllByOrderByCode();
        if (user.role() == Role.ADMIN || user.role() == Role.HR) {
            return all;
        }
        if (user.employeeId() == null) {
            return List.of();
        }
        if (user.role() == Role.MANAGER) {
            return all.stream().filter(employee -> employee.getManager() != null
                    && employee.getManager().getId().equals(user.employeeId())).toList();
        }
        return all.stream().filter(employee -> employee.getId().equals(user.employeeId())).toList();
    }
}