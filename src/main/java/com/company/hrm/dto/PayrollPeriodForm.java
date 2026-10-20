package com.company.hrm.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PayrollPeriodForm {

    @NotNull
    @Min(1)
    @Max(12)
    private Integer month;

    @NotNull
    @Min(2000)
    private Integer year;
}