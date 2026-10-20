package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.DepartmentForm;
import com.company.hrm.dto.PositionForm;
import com.company.hrm.entity.Department;
import com.company.hrm.entity.Employee;
import com.company.hrm.entity.Position;
import com.company.hrm.entity.Role;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.repository.DepartmentRepository;
import com.company.hrm.repository.EmployeeRepository;
import com.company.hrm.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final EmployeeRepository employeeRepository;

    public List<Department> departments(HrmUserDetails user) {
        requireHrOrManager(user);
        return departmentRepository.findAllByOrderByName();
    }

    public List<Position> positions(HrmUserDetails user) {
        requireHrOrManager(user);
        return positionRepository.findAllByOrderByName();
    }

    public List<Employee> managers(HrmUserDetails user) {
        requireHr(user);
        return employeeRepository.findAllByOrderByCode().stream()
                .filter(employee -> employee.getStatus() != com.company.hrm.entity.EmployeeStatus.RESIGNED)
                .toList();
    }

    @Transactional
    public void saveDepartment(HrmUserDetails user, Long id, DepartmentForm form) {
        requireHr(user);
        Department department = id == null ? new Department() : departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Department was not found."));
        String code = form.getCode().trim();
        if (departmentRepository.existsByCodeIgnoreCase(code) && !code.equalsIgnoreCase(department.getCode())) {
            throw new BusinessException("Department code is already in use.");
        }
        department.setCode(code);
        department.setName(form.getName().trim());
        department.setManager(form.getManagerId() == null ? null : employeeRepository.findById(form.getManagerId())
                .orElseThrow(() -> new BusinessException("Manager was not found.")));
        departmentRepository.save(department);
    }

    @Transactional
    public void deleteDepartment(HrmUserDetails user, Long id) {
        requireHr(user);
        if (employeeRepository.existsByDepartment_Id(id)) {
            throw new BusinessException("This department is assigned to employees and cannot be deleted.");
        }
        departmentRepository.deleteById(id);
    }

    @Transactional
    public void savePosition(HrmUserDetails user, Long id, PositionForm form) {
        requireHr(user);
        Position position = id == null ? new Position() : positionRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Position was not found."));
        String name = form.getName().trim();
        if (positionRepository.existsByNameIgnoreCase(name) && !name.equalsIgnoreCase(position.getName())) {
            throw new BusinessException("Position name is already in use.");
        }
        position.setName(name);
        positionRepository.save(position);
    }

    @Transactional
    public void deletePosition(HrmUserDetails user, Long id) {
        requireHr(user);
        if (employeeRepository.existsByPosition_Id(id)) {
            throw new BusinessException("This position is assigned to employees and cannot be deleted.");
        }
        positionRepository.deleteById(id);
    }

    private void requireHr(HrmUserDetails user) {
        if (user.role() != Role.ADMIN && user.role() != Role.HR) {
            throw new AccessDeniedException("You do not have permission to manage the catalog.");
        }
    }

    private void requireHrOrManager(HrmUserDetails user) {
        if (user.role() != Role.ADMIN && user.role() != Role.HR && user.role() != Role.MANAGER) {
            throw new AccessDeniedException("You do not have permission to view catalog values.");
        }
        if (user.role() == Role.MANAGER && user.employeeId() == null) {
            throw new AccessDeniedException("This account is not linked to an employee.");
        }
    }
}