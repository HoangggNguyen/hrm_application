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
@Table(name = "leave_balances", uniqueConstraints = @UniqueConstraint(
        name = "uk_leave_balances_year", columnNames = {"employee_id", "leave_type_id", "year"}))
@Getter
@Setter
@NoArgsConstructor
public class LeaveBalance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_type_id", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal total;

    @Column(nullable = false, precision = 5, scale = 1)
    private BigDecimal used = BigDecimal.ZERO;
}