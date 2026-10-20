package com.company.hrm.repository;

import com.company.hrm.entity.PayrollPeriod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {
    boolean existsByMonthAndYear(int month, int year);
    List<PayrollPeriod> findAllByOrderByYearDescMonthDesc();
}