package com.company.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminPasswordForm {

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;
}