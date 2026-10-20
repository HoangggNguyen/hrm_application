package com.company.hrm.dto;

public record AttendanceSummary(String employeeCode, String employeeName, long presentDays, long lateDays) {
}