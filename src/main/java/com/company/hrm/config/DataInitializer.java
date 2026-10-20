package com.company.hrm.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        if (userCount != null && userCount > 0) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        List<Long> employeeIds = new java.util.ArrayList<>();
        List<Long> departments = jdbcTemplate.queryForList("SELECT id FROM departments ORDER BY id", Long.class);
        List<Long> positions = jdbcTemplate.queryForList("SELECT id FROM positions ORDER BY id", Long.class);
        List<Object[]> leaves = jdbcTemplate.query("SELECT id, days_per_year FROM leave_types ORDER BY id",
                (result, row) -> new Object[]{result.getLong(1), result.getInt(2)});

        for (int index = 1; index <= 10; index++) {
            long departmentId = departments.get(index == 1 ? 0 : index <= 8 ? 1 : 2);
            long positionId = positions.get(index == 1 ? 0 : index == 2 ? 1 : index <= 8 ? 2 : index == 9 ? 3 : 4);
            Long managerId = index > 2 ? employeeIds.get(1) : null;
            jdbcTemplate.update("""
                    INSERT INTO employees (code, full_name, gender, dob, phone, email, address, id_number,
                                           department_id, position_id, manager_id, hire_date, status,
                                           created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?)
                    """, String.format("EMP%04d", index), "Sample Employee " + index,
                    index % 2 == 0 ? "FEMALE" : "MALE", today.minusYears(25L + index),
                    "+84 900 000 " + String.format("%04d", index), "employee" + index + "@example.com",
                    "Ho Chi Minh City", String.format("ID%08d", index), departmentId, positionId,
                    managerId, today.minusMonths(12), now, now);
            Long employeeId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            employeeIds.add(employeeId);

            jdbcTemplate.update("""
                    INSERT INTO contracts (employee_id, type, start_date, base_salary, status, created_at, updated_at)
                    VALUES (?, 'INDEFINITE', ?, ?, 'ACTIVE', ?, ?)
                    """, employeeId, today.minusMonths(12), new BigDecimal("20000000"), now, now);

            for (Object[] leave : leaves) {
                jdbcTemplate.update("""
                        INSERT INTO leave_balances (employee_id, leave_type_id, year, total, used, created_at, updated_at)
                        VALUES (?, ?, ?, ?, 0, ?, ?)
                        """, employeeId, leave[0], today.getYear(), leave[1], now, now);
            }
        }

        jdbcTemplate.update("UPDATE departments SET manager_id = ?, updated_at = ? WHERE code = 'ENG'",
                employeeIds.get(1), now);
        jdbcTemplate.update("UPDATE departments SET manager_id = ?, updated_at = ? WHERE code = 'SALES'",
                employeeIds.get(8), now);

        insertUser("admin", "Admin@123", "ADMIN", null, now);
        insertUser("hr", "Hr@12345", "HR", employeeIds.get(0), now);
        insertUser("manager", "Manager@123", "MANAGER", employeeIds.get(1), now);
        insertUser("employee", "Employee@123", "EMPLOYEE", employeeIds.get(2), now);
    }

    private void insertUser(String username, String rawPassword, String role, Long employeeId, LocalDateTime now) {
        jdbcTemplate.update("""
                INSERT INTO users (username, password, role, enabled, employee_id, created_at, updated_at)
                VALUES (?, ?, ?, TRUE, ?, ?, ?)
                """, username, passwordEncoder.encode(rawPassword), role, employeeId, now, now);
    }
}