package com.company.hrm.controller;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.PayrollPeriodForm;
import com.company.hrm.dto.PayslipEditForm;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.service.PayrollService;
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
@RequestMapping("/payroll")
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping
    public String periods(@AuthenticationPrincipal HrmUserDetails user, Model model) {
        model.addAttribute("periods", payrollService.periods(user));
        model.addAttribute("periodForm", new PayrollPeriodForm());
        return "payroll/index";
    }

    @PostMapping
    public String createPeriod(@AuthenticationPrincipal HrmUserDetails user,
                               @Valid @ModelAttribute("periodForm") PayrollPeriodForm form,
                               BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter a valid month and year.");
            return "redirect:/payroll";
        }
        try {
            var period = payrollService.createPeriod(user, form);
            redirect.addFlashAttribute("successMessage", "Payroll period created.");
            return "redirect:/payroll/" + period.getId();
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/payroll";
        }
    }

    @GetMapping("/{id}")
    public String period(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id, Model model) {
        model.addAttribute("period", payrollService.period(user, id));
        model.addAttribute("payslips", payrollService.payslips(user, id));
        return "payroll/detail";
    }

    @PostMapping("/{id}/generate")
    public String generate(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                           RedirectAttributes redirect) {
        try {
            payrollService.generate(user, id);
            redirect.addFlashAttribute("successMessage", "Payslips generated.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/payroll/" + id;
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                          RedirectAttributes redirect) {
        try {
            payrollService.confirm(user, id);
            redirect.addFlashAttribute("successMessage", "Payroll confirmed and locked.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/payroll/" + id;
    }

    @PostMapping("/{id}/pay")
    public String pay(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                      RedirectAttributes redirect) {
        try {
            payrollService.markPaid(user, id);
            redirect.addFlashAttribute("successMessage", "Payroll marked as paid.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/payroll/" + id;
    }

    @PostMapping("/payslips/{id}")
    public String editPayslip(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                              @Valid @ModelAttribute PayslipEditForm form, BindingResult result,
                              RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Amounts must be zero or greater.");
            return "redirect:/payroll";
        }
        try {
            payrollService.editPayslip(user, id, form);
            redirect.addFlashAttribute("successMessage", "Payslip updated.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/payroll";
    }

    @GetMapping("/my")
    public String myPayslips(@AuthenticationPrincipal HrmUserDetails user, Model model) {
        model.addAttribute("payslips", payrollService.myPayslips(user));
        return "payroll/my";
    }

    @GetMapping("/payslips/{id}")
    public String payslip(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id, Model model) {
        model.addAttribute("payslip", payrollService.payslip(user, id));
        return "payroll/payslip";
    }
}