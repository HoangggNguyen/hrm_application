package com.company.hrm.repository;

import com.company.hrm.entity.Payslip;
import com.company.hrm.entity.PayrollStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PayslipRepository extends JpaRepository<Payslip, Long> {
    @EntityGraph(attributePaths = {"employee", "period"})
    List<Payslip> findByPeriod_IdOrderByEmployee_Code(Long periodId);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.position", "period"})
    List<Payslip> findByEmployee_IdOrderByPeriod_YearDescPeriod_MonthDesc(Long employeeId);

    @EntityGraph(attributePaths = {"employee", "employee.department", "employee.position", "period"})
    @Override
    Optional<Payslip> findById(Long id);

    long countByPeriod_Id(Long periodId);
    void deleteByPeriod_Id(Long periodId);
    boolean existsByEmployee_IdAndPeriod_StatusIn(Long employeeId, Collection<PayrollStatus> statuses);
}