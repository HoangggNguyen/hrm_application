package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.PasswordChangeForm;
import com.company.hrm.dto.ProfileForm;
import com.company.hrm.entity.Employee;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.repository.EmployeeRepository;
import com.company.hrm.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final EmployeeRepository employeeRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public Optional<Employee> employee(HrmUserDetails user) {
        return user.employeeId() == null ? Optional.empty() : employeeRepository.findWithReferencesById(user.employeeId());
    }

    @Transactional
    public void update(HrmUserDetails user, ProfileForm form) {
        Long employeeId = user.employeeId();
        if (employeeId == null) {
            throw new BusinessException("This account has no linked employee profile.");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException("Employee profile was not found."));
        employee.setPhone(form.getPhone() == null || form.getPhone().isBlank() ? null : form.getPhone().trim());
        employee.setAddress(form.getAddress() == null || form.getAddress().isBlank() ? null : form.getAddress().trim());
        employeeRepository.save(employee);
    }

    @Transactional
    public void changePassword(HrmUserDetails user, PasswordChangeForm form) {
        var account = userAccountRepository.findById(user.id())
                .orElseThrow(() -> new AccessDeniedException("Account was not found."));
        if (!passwordEncoder.matches(form.getCurrentPassword(), account.getPassword())) {
            throw new BusinessException("Current password is incorrect.");
        }
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            throw new BusinessException("New password and confirmation do not match.");
        }
        account.setPassword(passwordEncoder.encode(form.getNewPassword()));
        userAccountRepository.save(account);
    }
}