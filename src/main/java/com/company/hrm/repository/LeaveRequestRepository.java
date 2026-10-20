package com.company.hrm.repository;

import com.company.hrm.entity.LeaveRequest;
import com.company.hrm.entity.LeaveStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    @EntityGraph(attributePaths = {"leaveType", "approver"})
    List<LeaveRequest> findByEmployee_IdOrderByFromDateDesc(Long employeeId);

    @EntityGraph(attributePaths = {"employee", "employee.department", "leaveType"})
    List<LeaveRequest> findByStatusInOrderByFromDateAsc(Collection<LeaveStatus> statuses);

    @EntityGraph(attributePaths = {"employee", "employee.manager", "leaveType"})
    List<LeaveRequest> findByEmployee_IdInAndStatusOrderByFromDateAsc(Collection<Long> employeeIds, LeaveStatus status);

        @EntityGraph(attributePaths = {"employee", "employee.manager", "leaveType"})
        List<LeaveRequest> findByStatusAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            LeaveStatus status, LocalDate toDate, LocalDate fromDate);

    boolean existsByEmployee_IdAndStatusInAndFromDateLessThanEqualAndToDateGreaterThanEqual(
            Long employeeId, Collection<LeaveStatus> statuses, LocalDate toDate, LocalDate fromDate);

    boolean existsByLeaveType_Id(Long leaveTypeId);
}