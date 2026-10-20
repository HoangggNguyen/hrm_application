package com.company.hrm.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeaveTypeForm {

    @NotBlank
    @Size(max = 50)
    private String name;

    @Min(0)
    private int daysPerYear;

    private boolean paid;
}