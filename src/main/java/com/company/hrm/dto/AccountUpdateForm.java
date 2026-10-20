package com.company.hrm.dto;

import com.company.hrm.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountUpdateForm {

    @NotNull
    private Role role;

    private boolean enabled;
}