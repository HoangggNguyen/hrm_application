package com.company.hrm.dto;

import com.company.hrm.entity.PayrollPeriod;

import java.math.BigDecimal;

public record PayrollPeriodSummary(PayrollPeriod period, long payslipCount, BigDecimal totalNet) {
}