package com.company.hrm.repository;

import com.company.hrm.entity.Contract;
import com.company.hrm.entity.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface ContractRepository extends JpaRepository<Contract, Long> {
    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.position"})
    List<Contract> findByStatus(ContractStatus status);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = "employee")
    List<Contract> findByEmployee_IdOrderByStartDateDesc(Long employeeId);
    List<Contract> findByEmployee_IdAndStatus(Long employeeId, com.company.hrm.entity.ContractStatus status);
}