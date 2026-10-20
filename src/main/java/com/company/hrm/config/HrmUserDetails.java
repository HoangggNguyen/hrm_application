package com.company.hrm.config;

import com.company.hrm.entity.Role;
import com.company.hrm.entity.UserAccount;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public record HrmUserDetails(Long id, String username, String password, Role role,
                             boolean enabled, Long employeeId) implements UserDetails {

    public static HrmUserDetails from(UserAccount account) {
        Long employeeId = account.getEmployee() == null ? null : account.getEmployee().getId();
        return new HrmUserDetails(account.getId(), account.getUsername(), account.getPassword(),
                account.getRole(), account.isEnabled(), employeeId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
}