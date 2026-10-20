package com.company.hrm.controller;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.AttendanceForm;
import com.company.hrm.entity.Attendance;
import com.company.hrm.entity.AttendanceStatus;
import com.company.hrm.entity.EmployeeStatus;
import com.company.hrm.entity.Role;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.repository.AttendanceRepository;
import com.company.hrm.repository.EmployeeRepository;
import com.company.hrm.service.AttendanceService;
import com.company.hrm.service.CatalogService;
import com.company.hrm.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@Controller
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final EmployeeService employeeService;
    private final CatalogService catalogService;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    @GetMapping
    public String index(@AuthenticationPrincipal HrmUserDetails user,
                        @RequestParam(required = false) String month,
                        @RequestParam(required = false) Long employee,
                        @RequestParam(required = false) Long department,
                        @RequestParam(defaultValue = "0") int page, Model model) {
        YearMonth selectedMonth = parseMonth(month);
        model.addAttribute("month", selectedMonth.toString());
        model.addAttribute("attendancePage", attendanceService.search(user, selectedMonth, employee, department,
                PageRequest.of(Math.max(0, page), 10, Sort.by(Sort.Direction.DESC, "workDate"))));
        model.addAttribute("summaries", attendanceService.monthlySummary(user, selectedMonth, employee, department));
        model.addAttribute("today", attendanceService.today(user));
        model.addAttribute("employeeId", user.employeeId());
        model.addAttribute("isAdminOrHr", user.role() == Role.ADMIN || user.role() == Role.HR);
        model.addAttribute("canFilterEmployees", user.role() != Role.EMPLOYEE);
        model.addAttribute("attendanceStatuses", AttendanceStatus.values());
        model.addAttribute("employeeFilter", employee);
        model.addAttribute("departmentFilter", department);
        if (user.role() != Role.EMPLOYEE) {
            model.addAttribute("employees", employeeService.searchAll(user, null, null, null, null));
            model.addAttribute("departments", catalogService.departments(user));
        }
        model.addAttribute("newAttendance", new AttendanceForm());
        return "attendance/index";
    }

    @PostMapping("/check-in")
    public String checkIn(@AuthenticationPrincipal HrmUserDetails user, RedirectAttributes redirect) {
        try {
            attendanceService.checkIn(user);
            redirect.addFlashAttribute("successMessage", "Check-in recorded.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/attendance";
    }

    @PostMapping("/check-out")
    public String checkOut(@AuthenticationPrincipal HrmUserDetails user, RedirectAttributes redirect) {
        try {
            attendanceService.checkOut(user);
            redirect.addFlashAttribute("successMessage", "Check-out recorded.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/attendance";
    }

    @PostMapping
    public String create(@AuthenticationPrincipal HrmUserDetails user,
                         @Valid @ModelAttribute("newAttendance") AttendanceForm form,
                         BindingResult result, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter valid attendance details.");
            return "redirect:/attendance";
        }
        try {
            attendanceService.save(user, null, form);
            redirect.addFlashAttribute("successMessage", "Attendance record added.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return redirectToMonth(form.getWorkDate());
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                         @Valid @ModelAttribute AttendanceForm form, BindingResult result,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Enter valid attendance details.");
        } else {
            try {
                attendanceService.save(user, id, form);
                redirect.addFlashAttribute("successMessage", "Attendance record updated.");
            } catch (BusinessException exception) {
                redirect.addFlashAttribute("errorMessage", exception.getMessage());
            }
        }
        return redirectToMonth(form.getWorkDate());
    }

    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException exception) {
            return YearMonth.now();
        }
    }

    private String redirectToMonth(LocalDate date) {
        return "redirect:/attendance?month=" + (date == null ? YearMonth.now() : YearMonth.from(date));
    }
}