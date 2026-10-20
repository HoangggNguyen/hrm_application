package com.company.hrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
public class HolidayForm {

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate holidayDate;

    @NotBlank
    @Size(max = 100)
    private String name;
}