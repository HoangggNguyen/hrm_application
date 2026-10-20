package com.company.hrm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class PayslipEditForm {

    @NotNull
    @DecimalMin("0")
    private BigDecimal allowance;

    @NotNull
    @DecimalMin("0")
    private BigDecimal bonus;

    @NotNull
    @DecimalMin("0")
    private BigDecimal deduction;
}