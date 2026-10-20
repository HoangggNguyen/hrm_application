package com.company.hrm.repository;

import com.company.hrm.entity.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveTypeRepository extends JpaRepository<LeaveType, Long> {
    List<LeaveType> findAllByOrderById();
    boolean existsByNameIgnoreCase(String name);
}