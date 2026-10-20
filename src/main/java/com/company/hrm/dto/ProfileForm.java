package com.company.hrm.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileForm {

    @Pattern(regexp = "^$|^[0-9+\\-\\s]{8,20}$")
    private String phone;

    @Size(max = 255)
    private String address;
}