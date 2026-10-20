package com.company.hrm.controller;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.dto.ContractForm;
import com.company.hrm.dto.EmployeeForm;
import com.company.hrm.entity.Employee;
import com.company.hrm.entity.EmployeeStatus;
import com.company.hrm.entity.LeaveStatus;
import com.company.hrm.entity.PayrollStatus;
import com.company.hrm.entity.Role;
import com.company.hrm.exception.BusinessException;
import com.company.hrm.repository.ContractRepository;
import com.company.hrm.repository.LeaveBalanceRepository;
import com.company.hrm.repository.LeaveRequestRepository;
import com.company.hrm.repository.PayslipRepository;
import com.company.hrm.service.CatalogService;
import com.company.hrm.service.EmployeeExcelService;
import com.company.hrm.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeExcelService employeeExcelService;
    private final CatalogService catalogService;
    private final ContractRepository contractRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayslipRepository payslipRepository;

    @GetMapping
    public String list(@AuthenticationPrincipal HrmUserDetails user,
                       @RequestParam(required = false) String q,
                       @RequestParam(required = false) Long department,
                       @RequestParam(required = false) Long position,
                       @RequestParam(required = false) EmployeeStatus status,
                       @RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("employees", employeeService.search(user, q, department, position, status,
                PageRequest.of(Math.max(0, page), 10, Sort.by("code").ascending())));
        model.addAttribute("q", q);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("selectedPosition", position);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("departments", catalogService.departments(user));
        model.addAttribute("positions", catalogService.positions(user));
        model.addAttribute("statuses", EmployeeStatus.values());
        model.addAttribute("canEdit", user.role() == Role.ADMIN || user.role() == Role.HR);
        return "employees/list";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@AuthenticationPrincipal HrmUserDetails user,
                                         @RequestParam(required = false) String q,
                                         @RequestParam(required = false) Long department,
                                         @RequestParam(required = false) Long position,
                                         @RequestParam(required = false) EmployeeStatus status) {
        byte[] file = employeeExcelService.export(user, q, department, position, status);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=employees.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @GetMapping("/new")
    public String createForm(@AuthenticationPrincipal HrmUserDetails user, Model model) {
        EmployeeForm form = new EmployeeForm();
        form.setStatus(EmployeeStatus.ACTIVE);
        form.setHireDate(LocalDate.now());
        model.addAttribute("employeeForm", form);
        model.addAttribute("formTitle", "New employee");
        addFormOptions(user, model);
        return "employees/form";
    }

    @PostMapping("/new")
    public String create(@AuthenticationPrincipal HrmUserDetails user, @Valid @ModelAttribute("employeeForm") EmployeeForm form,
                         BindingResult result, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("formTitle", "New employee");
            addFormOptions(user, model);
            return "employees/form";
        }
        try {
            Employee employee = employeeService.create(user, form);
            redirect.addFlashAttribute("successMessage", "Employee created.");
            return "redirect:/employees/" + employee.getId();
        } catch (BusinessException exception) {
            model.addAttribute("formTitle", "New employee");
            model.addAttribute("errorMessage", exception.getMessage());
            addFormOptions(user, model);
            return "employees/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id, Model model) {
        Employee employee = employeeService.getForEdit(user, id);
        model.addAttribute("employeeForm", toForm(employee));
        model.addAttribute("employeeCode", employee.getCode());
        model.addAttribute("formTitle", "Edit employee");
        model.addAttribute("employeeId", id);
        model.addAttribute("allowResignedStatus", employee.getStatus() == EmployeeStatus.RESIGNED);
        addFormOptions(user, model);
        return "employees/form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                       @Valid @ModelAttribute("employeeForm") EmployeeForm form, BindingResult result,
                       Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) {
            Employee employee = employeeService.getForView(user, id);
            model.addAttribute("employeeCode", employee.getCode());
            model.addAttribute("formTitle", "Edit employee");
            model.addAttribute("employeeId", id);
            model.addAttribute("allowResignedStatus", employee.getStatus() == EmployeeStatus.RESIGNED);
            addFormOptions(user, model);
            return "employees/form";
        }
        try {
            employeeService.update(user, id, form);
            redirect.addFlashAttribute("successMessage", "Employee updated.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/employees/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id, Model model) {
        Employee employee = employeeService.getForView(user, id);
        model.addAttribute("employee", employee);
        model.addAttribute("contracts", contractRepository.findByEmployee_IdOrderByStartDateDesc(id));
        model.addAttribute("balances", leaveBalanceRepository.findByEmployee_IdAndYearOrderByLeaveType_Name(
                id, LocalDate.now().getYear()));
        model.addAttribute("leaveRequests", leaveRequestRepository.findByEmployee_IdOrderByFromDateDesc(id));
        model.addAttribute("payslips", payslipRepository.findByEmployee_IdOrderByPeriod_YearDescPeriod_MonthDesc(id)
            .stream().filter(slip -> slip.getPeriod().getStatus() == PayrollStatus.CONFIRMED
                || slip.getPeriod().getStatus() == PayrollStatus.PAID).toList());
        model.addAttribute("canEdit", user.role() == Role.ADMIN || user.role() == Role.HR);
        model.addAttribute("contractForm", new ContractForm());
        return "employees/detail";
    }

    @PostMapping("/{id}/resign")
    public String resign(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                         @RequestParam LocalDate resignDate, RedirectAttributes redirect) {
        try {
            employeeService.resign(user, id, resignDate);
            redirect.addFlashAttribute("successMessage", "Employee resignation recorded.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/employees/" + id;
    }

    @PostMapping("/{id}/contracts")
    public String addContract(@AuthenticationPrincipal HrmUserDetails user, @PathVariable Long id,
                              @Valid @ModelAttribute ContractForm contractForm, BindingResult result,
                              RedirectAttributes redirect) {
        if (result.hasErrors()) {
            redirect.addFlashAttribute("errorMessage", "Please enter valid contract details.");
            return "redirect:/employees/" + id + "#contracts";
        }
        try {
            employeeService.addContract(user, id, contractForm);
            redirect.addFlashAttribute("successMessage", "Contract added.");
        } catch (BusinessException exception) {
            redirect.addFlashAttribute("errorMessage", exception.getMessage());
        }
        return "redirect:/employees/" + id + "#contracts";
    }

    private void addFormOptions(HrmUserDetails user, Model model) {
        model.addAttribute("departments", catalogService.departments(user));
        model.addAttribute("positions", catalogService.positions(user));
        model.addAttribute("managers", catalogService.managers(user));
        model.addAttribute("statuses", EmployeeStatus.values());
        model.addAttribute("genders", com.company.hrm.entity.Gender.values());
        model.addAttribute("allowResignedStatus", false);
    }

    private EmployeeForm toForm(Employee employee) {
        EmployeeForm form = new EmployeeForm();
        form.setFullName(employee.getFullName());
        form.setGender(employee.getGender());
        form.setDob(employee.getDob());
        form.setPhone(employee.getPhone());
        form.setEmail(employee.getEmail());
        form.setAddress(employee.getAddress());
        form.setIdNumber(employee.getIdNumber());
        form.setTaxCode(employee.getTaxCode());
        form.setBankName(employee.getBankName());
        form.setBankAccount(employee.getBankAccount());
        form.setDepartmentId(employee.getDepartment().getId());
        form.setPositionId(employee.getPosition().getId());
        form.setManagerId(employee.getManager() == null ? null : employee.getManager().getId());
        form.setHireDate(employee.getHireDate());
        form.setStatus(employee.getStatus());
        return form;
    }
}