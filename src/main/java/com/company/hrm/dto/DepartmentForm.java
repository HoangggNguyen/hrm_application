package com.company.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DepartmentForm {

    @NotBlank
    @Size(max = 20)
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;

    private Long managerId;
}