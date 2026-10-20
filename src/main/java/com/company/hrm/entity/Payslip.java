package com.company.hrm.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payslips", uniqueConstraints = @UniqueConstraint(
        name = "uk_payslips_period_employee", columnNames = {"period_id", "employee_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Payslip extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_id", nullable = false)
    private PayrollPeriod period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "base_salary", nullable = false, precision = 15, scale = 0)
    private BigDecimal baseSalary;

    @Column(name = "standard_days", nullable = false)
    private int standardDays;

    @Column(name = "actual_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal actualDays;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal allowance = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal bonus = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal deduction = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 0)
    private BigDecimal gross;

    @Column(name = "net_salary", nullable = false, precision = 15, scale = 0)
    private BigDecimal netSalary;
}