package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.ContractForm;
import com.company.hrm.dto.EmployeeForm;
import com.company.hrm.entity.Contract;
import com.company.hrm.entity.ContractStatus;
import com.company.hrm.entity.Employee;
import com.company.hrm.entity.EmployeeStatus;
import com.company.hrm.entity.LeaveBalance;
import com.company.hrm.entity.Role;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.repository.ContractRepository;
import com.company.hrm.repository.DepartmentRepository;
import com.company.hrm.repository.EmployeeRepository;
import com.company.hrm.repository.LeaveBalanceRepository;
import com.company.hrm.repository.LeaveTypeRepository;
import com.company.hrm.repository.PositionRepository;
import com.company.hrm.repository.UserAccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final ContractRepository contractRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveTypeRepository leaveTypeRepository;
    private final UserAccountRepository userAccountRepository;

    public Page<Employee> search(HrmUserDetails user, String query, Long departmentId, Long positionId,
                                 EmployeeStatus status, Pageable pageable) {
        return employeeRepository.findAll(buildSpecification(user, query, departmentId, positionId, status), pageable);
    }

    public java.util.List<Employee> searchAll(HrmUserDetails user, String query, Long departmentId,
                                              Long positionId, EmployeeStatus status) {
        return employeeRepository.findAll(buildSpecification(user, query, departmentId, positionId, status),
                org.springframework.data.domain.Sort.by("code").ascending());
    }

    private org.springframework.data.jpa.domain.Specification<Employee> buildSpecification(
            HrmUserDetails user, String query, Long departmentId, Long positionId, EmployeeStatus status) {
        Long managerId = null;
        if (user.role() == Role.MANAGER) {
            requireLinkedEmployee(user);
            managerId = user.employeeId();
        } else {
            requireHrOrManager(user);
        }
        return EmployeeSpecification.matching(query, departmentId, positionId, status, managerId);
    }

    public Employee getForView(HrmUserDetails user, Long id) {
        requireHrOrManager(user);
        Employee employee = employeeRepository.findWithReferencesById(id)
                .orElseThrow(() -> new BusinessException("Employee was not found."));
        if (user.role() == Role.MANAGER && !isDirectReport(user.employeeId(), employee)) {
            throw new AccessDeniedException("You can only view direct reports.");
        }
        return employee;
    }

    public Employee getForEdit(HrmUserDetails user, Long id) {
        requireHr(user);
        return employeeRepository.findWithReferencesById(id)
                .orElseThrow(() -> new BusinessException("Employee was not found."));
    }

    @Transactional
    public Employee create(HrmUserDetails user, EmployeeForm form) {
        requireHr(user);
        validateUniqueFields(form, null);
        Employee employee = new Employee();
        applyForm(employee, form);
        employee.setCode(String.format("EMP%04d", employeeRepository.countEmployees() + 1));
        Employee savedEmployee = employeeRepository.saveAndFlush(employee);
        int year = LocalDate.now().getYear();
        leaveTypeRepository.findAllByOrderById().forEach(type -> {
            LeaveBalance balance = new LeaveBalance();
            balance.setEmployee(savedEmployee);
            balance.setLeaveType(type);
            balance.setYear(year);
            balance.setTotal(BigDecimal.valueOf(type.getDaysPerYear()));
            balance.setUsed(BigDecimal.ZERO);
            leaveBalanceRepository.save(balance);
        });
        return savedEmployee;
    }

    @Transactional
    public Employee update(HrmUserDetails user, Long id, EmployeeForm form) {
        requireHr(user);
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Employee was not found."));
        validateUniqueFields(form, employee);
        applyForm(employee, form);
        return employeeRepository.save(employee);
    }

    @Transactional
    public void resign(HrmUserDetails user, Long id, LocalDate resignDate) {
        requireHr(user);
        if (resignDate == null) {
            throw new BusinessException("Resignation date is required.");
        }
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Employee was not found."));
        employee.setStatus(EmployeeStatus.RESIGNED);
        employee.setResignDate(resignDate);
        employeeRepository.save(employee);
        userAccountRepository.findByEmployee_Id(id).ifPresent(account -> {
            account.setEnabled(false);
            userAccountRepository.save(account);
        });
        contractRepository.findByEmployee_IdAndStatus(id, ContractStatus.ACTIVE).forEach(contract -> {
            contract.setStatus(ContractStatus.EXPIRED);
            contractRepository.save(contract);
        });
    }

    @Transactional
    public void addContract(HrmUserDetails user, Long employeeId, ContractForm form) {
        requireHr(user);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee was not found."));
        validateContract(form);
        contractRepository.findByEmployee_IdAndStatus(employeeId, ContractStatus.ACTIVE).forEach(active -> {
            active.setStatus(ContractStatus.EXPIRED);
            contractRepository.save(active);
        });
        Contract contract = new Contract();
        contract.setEmployee(employee);
        contract.setType(form.getType());
        contract.setStartDate(form.getStartDate());
        contract.setEndDate(form.getEndDate());
        contract.setBaseSalary(form.getBaseSalary());
        contract.setStatus(ContractStatus.ACTIVE);
        contractRepository.save(contract);
    }

    private void applyForm(Employee employee, EmployeeForm form) {
        employee.setFullName(form.getFullName().trim());
        employee.setGender(form.getGender());
        employee.setDob(form.getDob());
        employee.setPhone(blankToNull(form.getPhone()));
        employee.setEmail(form.getEmail().trim());
        employee.setAddress(blankToNull(form.getAddress()));
        employee.setIdNumber(form.getIdNumber().trim());
        employee.setTaxCode(blankToNull(form.getTaxCode()));
        employee.setBankName(blankToNull(form.getBankName()));
        employee.setBankAccount(blankToNull(form.getBankAccount()));
        employee.setDepartment(departmentRepository.findById(form.getDepartmentId())
                .orElseThrow(() -> new BusinessException("Department was not found.")));
        employee.setPosition(positionRepository.findById(form.getPositionId())
                .orElseThrow(() -> new BusinessException("Position was not found.")));
        if (form.getManagerId() != null) {
            if (employee.getId() != null && employee.getId().equals(form.getManagerId())) {
                throw new BusinessException("An employee cannot manage themselves.");
            }
            employee.setManager(employeeRepository.findById(form.getManagerId())
                    .orElseThrow(() -> new BusinessException("Manager was not found.")));
        } else {
            employee.setManager(null);
        }
        employee.setHireDate(form.getHireDate());
        employee.setStatus(form.getStatus());
        if (form.getStatus() != EmployeeStatus.RESIGNED) {
            employee.setResignDate(null);
        }
    }

    private void validateUniqueFields(EmployeeForm form, Employee current) {
        boolean duplicateEmail = employeeRepository.existsByEmail(form.getEmail().trim())
                && (current == null || !current.getEmail().equalsIgnoreCase(form.getEmail().trim()));
        boolean duplicateId = employeeRepository.existsByIdNumber(form.getIdNumber().trim())
                && (current == null || !current.getIdNumber().equals(form.getIdNumber().trim()));
        if (duplicateEmail) {
            throw new BusinessException("Email is already in use.");
        }
        if (duplicateId) {
            throw new BusinessException("ID number is already in use.");
        }
        if (form.getStatus() == EmployeeStatus.RESIGNED && current != null
                && current.getStatus() != EmployeeStatus.RESIGNED) {
            throw new BusinessException("Use the resignation action to resign an employee.");
        }
        if (current != null && current.getStatus() == EmployeeStatus.RESIGNED
                && form.getStatus() != EmployeeStatus.RESIGNED) {
            throw new BusinessException("A resigned employee cannot be reactivated through this form.");
        }
        if (form.getStatus() == EmployeeStatus.RESIGNED && current == null) {
            throw new BusinessException("New employees cannot be created as resigned.");
        }
    }

    private void validateContract(ContractForm form) {
        boolean needsEndDate = form.getType() != com.company.hrm.entity.ContractType.INDEFINITE;
        if (needsEndDate && form.getEndDate() == null) {
            throw new BusinessException("An end date is required for this contract type.");
        }
        if (!needsEndDate && form.getEndDate() != null) {
            throw new BusinessException("Indefinite contracts cannot have an end date.");
        }
        if (form.getEndDate() != null && form.getEndDate().isBefore(form.getStartDate())) {
            throw new BusinessException("Contract end date cannot be before its start date.");
        }
    }

    private boolean isDirectReport(Long managerId, Employee employee) {
        return managerId != null && employee.getManager() != null
                && managerId.equals(employee.getManager().getId());
    }

    private void requireLinkedEmployee(HrmUserDetails user) {
        if (user.employeeId() == null) {
            throw new AccessDeniedException("This account is not linked to an employee.");
        }
    }

    private void requireHrOrManager(HrmUserDetails user) {
        if (user.role() != Role.ADMIN && user.role() != Role.HR && user.role() != Role.MANAGER) {
            throw new AccessDeniedException("You do not have permission to view employees.");
        }
        if (user.role() == Role.MANAGER) {
            requireLinkedEmployee(user);
        }
    }

    private void requireHr(HrmUserDetails user) {
        if (user.role() != Role.ADMIN && user.role() != Role.HR) {
            throw new AccessDeniedException("You do not have permission to change employee data.");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}