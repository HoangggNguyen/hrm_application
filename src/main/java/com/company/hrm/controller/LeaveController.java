package com.company.hrm.controller;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.LeaveRequestForm;
import com.company.hrm.dto.LeaveTypeForm;
import com.company.hrm.entity.Role;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.service.LeaveService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @GetMapping
    public String index(@AuthenticationPrincipal HrmUserDetails user, Model model) {
        boolean hasEmployee = user.employeeId() != null;
        boolean canApprove = user.role() == Role.ADMIN || user.role() == Role.HR || user.role() == Role.MANAGER;
        boolean canManageTypes = user.role() == Role.ADMIN || user.role() == Role.HR;
        model.addAttribute("hasEmployee", hasEmployee);
        model.addAttribute("canApprove", canApprove);
        model.addAttribute("canManageTypes", canManageTypes);
        model.addAttribute("requestForm", new LeaveRequestForm());
        model.addAttribute("leaveTypeForm", new LeaveTypeForm());
        if (hasEmployee) {
            model.addAttribute("balances", leaveService.myBalances(user, LocalDate.now().getYear()));
            model.addAttribute("requests", leaveService.myRequests(user));
            model.addAttribute("requestTypes", leaveService.requestTypes(user));
        }
        if (canApprove) {
            model.addAttribute("approvals", leaveService.pendingApprovals(user));
        }
        if (canManageTypes) {
            model.addAttribute("leaveTypes", leaveService.leaveTypes(user));
        }
        return "leaves/index";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal HrmUserDetails user,
                         @Valid @ModelAttribute("requestForm") LeaveRequestForm form,
                         BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter a leave type, valid dates, and a reason.");
            return "redirect:/leaves";
        }
        try {
            leaveService.create(user, form);
            redirect.addFlashAttribute("successMessage", "Leave request submitted.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/leaves";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                         RedirectAttributes redirect) {
        try {
            leaveService.cancel(user, id);
            redirect.addFlashAttribute("successMessage", "Leave request cancelled.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/leaves";
    }

    @PostMapping("/{id}/approve")
    public String approve(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                          RedirectAttributes redirect) {
        try {
            leaveService.approve(user, id);
            redirect.addFlashAttribute("successMessage", "Leave request approved.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/leaves";
    }

    @PostMapping("/{id}/reject")
    public String reject(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                         @RequestParam String reason, RedirectAttributes redirect) {
        try {
            leaveService.reject(user, id, reason);
            redirect.addFlashAttribute("successMessage", "Leave request rejected.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/leaves";
    }

    @PostMapping("/types")
    public String createType(@AuthenticationPrincipal HrmUserDetails user,
                             @Valid @ModelAttribute LeaveTypeForm leaveTypeForm,
                             BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter a valid leave type.");
            return "redirect:/leaves#types";
        }
        return saveType(user, null, leaveTypeForm, redirect);
    }

    @PostMapping("/types/{id}")
    public String updateType(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                             @Valid @ModelAttribute LeaveTypeForm leaveTypeForm,
                             BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter a valid leave type.");
            return "redirect:/leaves#types";
        }
        return saveType(user, id, leaveTypeForm, redirect);
    }

    @PostMapping("/types/{id}/delete")
    public String deleteType(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                             RedirectAttributes redirect) {
        try {
            leaveService.deleteLeaveType(user, id);
            redirect.addFlashAttribute("successMessage", "Leave type deleted.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/leaves#types";
    }

    private String saveType(HrmUserDetails user, Long id, LeaveTypeForm form, RedirectAttributes redirect) {
        try {
            leaveService.saveLeaveType(user, id, form);
            redirect.addFlashAttribute("successMessage", "Leave type saved.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/leaves#types";
    }
}