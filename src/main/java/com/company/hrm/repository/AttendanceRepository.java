package com.company.hrm.repository;

import com.company.hrm.entity.Attendance;
import com.company.hrm.entity.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface AttendanceRepository extends JpaRepository<Attendance, Long>, JpaSpecificationExecutor<Attendance> {
    Optional<Attendance> findByEmployee_IdAndWorkDate(Long employeeId, LocalDate workDate);
    List<Attendance> findByEmployee_IdAndWorkDateBetweenOrderByWorkDate(Long employeeId, LocalDate from, LocalDate to);
    long countByEmployee_IdAndWorkDateBetweenAndStatus(Long employeeId, LocalDate from, LocalDate to, AttendanceStatus status);
    long countByWorkDateAndStatus(LocalDate workDate, AttendanceStatus status);
    long countByEmployee_IdAndWorkDateBetweenAndStatusIn(Long employeeId, LocalDate from, LocalDate to,
                                                          Collection<AttendanceStatus> statuses);

    @Override
    @EntityGraph(attributePaths = {"employee", "employee.department"})
    Page<Attendance> findAll(Specification<Attendance> specification, Pageable pageable);
}