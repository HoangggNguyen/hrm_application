package com.company.hrm.controller;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.AccountCreateForm;
import com.company.hrm.dto.AccountUpdateForm;
import com.company.hrm.dto.AdminPasswordForm;
import com.company.hrm.dto.HolidayForm;
import com.company.hrm.dto.SettingsForm;
import com.company.hrm.entity.Role;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public String index(@AuthenticationPrincipal HrmUserDetails user, Model model) {
        model.addAttribute("accounts", adminService.accounts(user));
        model.addAttribute("availableEmployees", adminService.availableEmployees(user));
        model.addAttribute("settingsForm", adminService.settings(user));
        model.addAttribute("holidays", adminService.holidays(user));
        model.addAttribute("accountForm", new AccountCreateForm());
        model.addAttribute("holidayForm", new HolidayForm());
        model.addAttribute("roles", Role.values());
        model.addAttribute("currentUserId", user.id());
        return "admin/index";
    }

    @PostMapping("/users")
    public String createAccount(@AuthenticationPrincipal HrmUserDetails user,
                                @Valid @ModelAttribute AccountCreateForm accountForm,
                                BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter a unique username, password, and role.");
            return "redirect:/admin#accounts";
        }
        try {
            adminService.createAccount(user, accountForm);
            redirect.addFlashAttribute("successMessage", "Account created.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#accounts";
    }

    @PostMapping("/users/{id}")
    public String updateAccount(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                                @Valid @ModelAttribute AccountUpdateForm form, BindingResult result,
                                RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Select a valid role.");
            return "redirect:/admin#accounts";
        }
        try {
            adminService.updateAccount(user, id, form);
            redirect.addFlashAttribute("successMessage", "Account updated.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#accounts";
    }

    @PostMapping("/users/{id}/password")
    public String resetPassword(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                                @Valid @ModelAttribute AdminPasswordForm form, BindingResult result,
                                RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "New password must contain at least 8 characters.");
            return "redirect:/admin#accounts";
        }
        try {
            adminService.resetPassword(user, id, form);
            redirect.addFlashAttribute("successMessage", "Password reset.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#accounts";
    }

    @PostMapping("/settings")
    public String saveSettings(@AuthenticationPrincipal HrmUserDetails user,
                               @Valid @ModelAttribute SettingsForm settingsForm,
                               BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter valid work times and grace minutes.");
            return "redirect:/admin#settings";
        }
        try {
            adminService.saveSettings(user, settingsForm);
            redirect.addFlashAttribute("successMessage", "Settings saved.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#settings";
    }

    @PostMapping("/holidays")
    public String addHoliday(@AuthenticationPrincipal HrmUserDetails user,
                             @Valid @ModelAttribute HolidayForm holidayForm,
                             BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter a holiday date and name.");
            return "redirect:/admin#holidays";
        }
        try {
            adminService.addHoliday(user, holidayForm);
            redirect.addFlashAttribute("successMessage", "Holiday added.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#holidays";
    }

    @PostMapping("/holidays/{id}/delete")
    public String deleteHoliday(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                                RedirectAttributes redirect) {
        try {
            adminService.deleteHoliday(user, id);
            redirect.addFlashAttribute("successMessage", "Holiday deleted.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/admin#holidays";
    }
}