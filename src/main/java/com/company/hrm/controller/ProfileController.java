package com.company.hrm.controller;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.PasswordChangeForm;
import com.company.hrm.dto.ProfileForm;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public String profile(@AuthenticationPrincipal HrmUserDetails user, Model model) {
        var employee = profileService.employee(user);
        model.addAttribute("employee", employee.orElse(null));
        ProfileForm form = new ProfileForm();
        employee.ifPresent(item -> {
            form.setPhone(item.getPhone());
            form.setAddress(item.getAddress());
        });
        model.addAttribute("profileForm", form);
        model.addAttribute("passwordForm", new PasswordChangeForm());
        return "profile/index";
    }

    @PostMapping
    public String update(@AuthenticationPrincipal HrmUserDetails user,
                         @Valid @ModelAttribute("profileForm") ProfileForm form,
                         BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter a valid phone number and address.");
            return "redirect:/profile";
        }
        try {
            profileService.update(user, form);
            redirect.addFlashAttribute("successMessage", "Profile updated.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal HrmUserDetails user,
                                 @Valid @ModelAttribute("passwordForm") PasswordChangeForm form,
                                 BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "New password must contain at least 8 characters.");
            return "redirect:/profile";
        }
        try {
            profileService.changePassword(user, form);
            redirect.addFlashAttribute("successMessage", "Password changed.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/profile";
    }
}