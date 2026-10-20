package com.company.hrm.service;

import com.company.hrm.config.HrmUserDetails;
import com.company.hrm.entity.Employee;
import com.company.hrm.entity.EmployeeStatus;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class EmployeeExcelService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final EmployeeService employeeService;

    public byte[] export(HrmUserDetails user, String query, Long departmentId, Long positionId,
                         EmployeeStatus status) {
        var employees = employeeService.searchAll(user, query, departmentId, positionId, status);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Employees");
            Row header = sheet.createRow(0);
            String[] headings = {"Code", "Full name", "Email", "Phone", "Department", "Position", "Manager",
                    "Hire date", "Status"};
            for (int column = 0; column < headings.length; column++) {
                header.createCell(column).setCellValue(headings[column]);
            }
            int rowIndex = 1;
            for (Employee employee : employees) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(employee.getCode());
                row.createCell(1).setCellValue(employee.getFullName());
                row.createCell(2).setCellValue(employee.getEmail());
                row.createCell(3).setCellValue(employee.getPhone() == null ? "" : employee.getPhone());
                row.createCell(4).setCellValue(employee.getDepartment().getName());
                row.createCell(5).setCellValue(employee.getPosition().getName());
                row.createCell(6).setCellValue(employee.getManager() == null ? "" : employee.getManager().getFullName());
                row.createCell(7).setCellValue(employee.getHireDate().format(DATE_FORMAT));
                row.createCell(8).setCellValue(employee.getStatus().name());
            }
            for (int column = 0; column < headings.length; column++) {
                sheet.autoSizeColumn(column);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create the employee workbook.", exception);
        }
    }
}