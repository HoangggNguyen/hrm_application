package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.AccountCreateForm;
import com.company.hrm.dto.AccountUpdateForm;
import com.company.hrm.dto.AdminPasswordForm;
import com.company.hrm.dto.HolidayForm;
import com.company.hrm.dto.SettingsForm;
import com.company.hrm.entity.Employee;
import com.company.hrm.entity.Holiday;
import com.company.hrm.entity.Role;
import com.company.hrm.entity.Setting;
import com.company.hrm.entity.UserAccount;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.repository.EmployeeRepository;
import com.company.hrm.repository.HolidayRepository;
import com.company.hrm.repository.SettingRepository;
import com.company.hrm.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserAccountRepository userAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final SettingRepository settingRepository;
    private final HolidayRepository holidayRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserAccount> accounts(HrmUserDetails user) {
        requireAdmin(user);
        return userAccountRepository.findAllByOrderByUsername();
    }

    public List<Employee> availableEmployees(HrmUserDetails user) {
        requireAdmin(user);
        return employeeRepository.findAllByOrderByCode().stream()
                .filter(employee -> !userAccountRepository.existsByEmployee_Id(employee.getId())).toList();
    }

    public SettingsForm settings(HrmUserDetails user) {
        requireAdmin(user);
        SettingsForm form = new SettingsForm();
        form.setWorkStart(setting("work_start", "08:30"));
        form.setWorkEnd(setting("work_end", "17:30"));
        form.setLateGraceMinutes(Integer.parseInt(setting("late_grace_minutes", "0")));
        return form;
    }

    public List<Holiday> holidays(HrmUserDetails user) {
        requireAdmin(user);
        return holidayRepository.findAllByOrderByHolidayDate();
    }

    @Transactional
    public void createAccount(HrmUserDetails user, AccountCreateForm form) {
        requireAdmin(user);
        if (userAccountRepository.existsByUsernameIgnoreCase(form.getUsername().trim())) {
            throw new BusinessException("Username is already in use.");
        }
        Employee employee = null;
        if (form.getEmployeeId() != null) {
            employee = employeeRepository.findById(form.getEmployeeId())
                    .orElseThrow(() -> new BusinessException("Employee was not found."));
            if (userAccountRepository.existsByEmployee_Id(employee.getId())) {
                throw new BusinessException("This employee already has an account.");
            }
        }
        UserAccount account = new UserAccount();
        account.setUsername(form.getUsername().trim());
        account.setPassword(passwordEncoder.encode(form.getPassword()));
        account.setRole(form.getRole());
        account.setEnabled(true);
        account.setEmployee(employee);
        userAccountRepository.save(account);
    }

    @Transactional
    public void updateAccount(HrmUserDetails user, Long accountId, AccountUpdateForm form) {
        requireAdmin(user);
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Account was not found."));
        if (account.getId().equals(user.id()) && (!form.isEnabled() || form.getRole() != Role.ADMIN)) {
            throw new BusinessException("You cannot disable or demote your own account.");
        }
        account.setRole(form.getRole());
        account.setEnabled(form.isEnabled());
        userAccountRepository.save(account);
    }

    @Transactional
    public void resetPassword(HrmUserDetails user, Long accountId, AdminPasswordForm form) {
        requireAdmin(user);
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException("Account was not found."));
        account.setPassword(passwordEncoder.encode(form.getPassword()));
        userAccountRepository.save(account);
    }

    @Transactional
    public void saveSettings(HrmUserDetails user, SettingsForm form) {
        requireAdmin(user);
        if (!LocalTime.parse(form.getWorkEnd()).isAfter(LocalTime.parse(form.getWorkStart()))) {
            throw new BusinessException("Work end must be after work start.");
        }
        saveSetting("work_start", form.getWorkStart());
        saveSetting("work_end", form.getWorkEnd());
        saveSetting("late_grace_minutes", Integer.toString(form.getLateGraceMinutes()));
    }

    @Transactional
    public void addHoliday(HrmUserDetails user, HolidayForm form) {
        requireAdmin(user);
        if (holidayRepository.existsByHolidayDate(form.getHolidayDate())) {
            throw new BusinessException("A holiday is already configured for this date.");
        }
        Holiday holiday = new Holiday();
        holiday.setHolidayDate(form.getHolidayDate());
        holiday.setName(form.getName().trim());
        holidayRepository.save(holiday);
    }

    @Transactional
    public void deleteHoliday(HrmUserDetails user, Long id) {
        requireAdmin(user);
        holidayRepository.deleteById(id);
    }

    private String setting(String key, String fallback) {
        return settingRepository.findBySettingKey(key).map(Setting::getSettingValue).orElse(fallback);
    }

    private void saveSetting(String key, String value) {
        Setting setting = settingRepository.findBySettingKey(key).orElseGet(() -> {
            Setting created = new Setting();
            created.setSettingKey(key);
            return created;
        });
        setting.setSettingValue(value);
        settingRepository.save(setting);
    }

    private void requireAdmin(HrmUserDetails user) {
        if (user.role() != Role.ADMIN) {
            throw new AccessDeniedException("Administrator access is required.");
        }
    }
}