package com.company.hrm.dto;

import com.company.hrm.entity.EmployeeStatus;
import com.company.hrm.entity.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class EmployeeForm {

    @NotBlank
    @Size(max = 100)
    private String fullName;

    @NotNull
    private Gender gender;

    @NotNull
    @Past
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dob;

    @Pattern(regexp = "^[0-9+\\-\\s]{8,20}$")
    private String phone;

    @NotBlank
    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 255)
    private String address;

    @NotBlank
    @Size(max = 20)
    private String idNumber;

    @Size(max = 20)
    private String taxCode;

    @Size(max = 100)
    private String bankName;

    @Size(max = 30)
    private String bankAccount;

    @NotNull
    private Long departmentId;

    @NotNull
    private Long positionId;

    private Long managerId;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    @NotNull
    private EmployeeStatus status;
}