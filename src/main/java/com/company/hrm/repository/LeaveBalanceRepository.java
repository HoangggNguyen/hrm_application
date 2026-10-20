package com.company.hrm.repository;

import com.company.hrm.entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByEmployee_IdAndLeaveType_IdAndYear(Long employeeId, Long leaveTypeId, int year);
    boolean existsByLeaveType_Id(Long leaveTypeId);

    @EntityGraph(attributePaths = "leaveType")
    List<LeaveBalance> findByEmployee_IdAndYearOrderByLeaveType_Name(Long employeeId, int year);
}