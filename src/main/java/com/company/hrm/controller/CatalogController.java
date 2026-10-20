package com.company.hrm.controller;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.DepartmentForm;
import com.company.hrm.dto.PositionForm;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.service.CatalogService;
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
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public String index(@AuthenticationPrincipal HrmUserDetails user, Model model) {
        model.addAttribute("departments", catalogService.departments(user));
        model.addAttribute("positions", catalogService.positions(user));
        model.addAttribute("managers", catalogService.managers(user));
        model.addAttribute("departmentForm", new DepartmentForm());
        model.addAttribute("positionForm", new PositionForm());
        return "catalog/index";
    }

    @PostMapping("/departments")
    public String createDepartment(@AuthenticationPrincipal HrmUserDetails user,
                                   @Valid @ModelAttribute DepartmentForm departmentForm,
                                   BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter a department code and name.");
            return "redirect:/catalog#departments";
        }
        return saveDepartment(user, null, departmentForm, redirect);
    }

    @PostMapping("/departments/{id}")
    public String updateDepartment(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                                   @Valid @ModelAttribute DepartmentForm departmentForm,
                                   BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter a department code and name.");
            return "redirect:/catalog#departments";
        }
        return saveDepartment(user, id, departmentForm, redirect);
    }

    @PostMapping("/departments/{id}/delete")
    public String deleteDepartment(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                                   RedirectAttributes redirect) {
        try {
            catalogService.deleteDepartment(user, id);
            redirect.addFlashAttribute("successMessage", "Department deleted.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/catalog#departments";
    }

    @PostMapping("/positions")
    public String createPosition(@AuthenticationPrincipal HrmUserDetails user,
                                @Valid @ModelAttribute PositionForm positionForm,
                                BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter a position name.");
            return "redirect:/catalog#positions";
        }
        return savePosition(user, null, positionForm, redirect);
    }

    @PostMapping("/positions/{id}")
    public String updatePosition(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                                 @Valid @ModelAttribute PositionForm positionForm,
                                 BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter a position name.");
            return "redirect:/catalog#positions";
        }
        return savePosition(user, id, positionForm, redirect);
    }

    @PostMapping("/positions/{id}/delete")
    public String deletePosition(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                                 RedirectAttributes redirect) {
        try {
            catalogService.deletePosition(user, id);
            redirect.addFlashAttribute("successMessage", "Position deleted.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/catalog#positions";
    }

    private String saveDepartment(HrmUserDetails user, Long id, DepartmentForm form, RedirectAttributes redirect) {
        try {
            catalogService.saveDepartment(user, id, form);
            redirect.addFlashAttribute("successMessage", "Department saved.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/catalog#departments";
    }

    private String savePosition(HrmUserDetails user, Long id, PositionForm form, RedirectAttributes redirect) {
        try {
            catalogService.savePosition(user, id, form);
            redirect.addFlashAttribute("successMessage", "Position saved.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/catalog#positions";
    }
}