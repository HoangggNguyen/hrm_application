package com.company.hrm.controller;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.entity.Role;
import com.company.hrm.service.AttendanceService;
import com.company.hrm.service.DashboardService;
import com.company.hrm.service.LeaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import java.time.LocalDate;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final LeaveService leaveService;
    private final AttendanceService attendanceService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal HrmUserDetails user, Model model) {
        model.addAttribute("username", user.username());
        model.addAttribute("role", user.role().name());
        model.addAttribute("metrics", dashboardService.metrics(user));
        model.addAttribute("isHrOrAdmin", user.role() == Role.ADMIN || user.role() == Role.HR);
        model.addAttribute("isManager", user.role() == Role.MANAGER);
        model.addAttribute("hasEmployee", user.employeeId() != null);
        model.addAttribute("pendingApprovals", user.role() == Role.ADMIN || user.role() == Role.HR || user.role() == Role.MANAGER
                ? leaveService.pendingApprovals(user).stream().limit(5).toList() : java.util.List.of());
        if (user.role() == Role.ADMIN || user.role() == Role.HR) {
            var departmentData = dashboardService.employeesByDepartment(user);
            model.addAttribute("departmentLabels", departmentData.keySet());
            model.addAttribute("departmentValues", departmentData.values());
            model.addAttribute("expiringContracts", dashboardService.expiringContracts(user));
        }
        if (user.role() == Role.EMPLOYEE && user.employeeId() != null) {
            model.addAttribute("balances", leaveService.myBalances(user, LocalDate.now().getYear()));
            model.addAttribute("todayAttendance", attendanceService.today(user));
        }
        return "dashboard/index";
    }
}