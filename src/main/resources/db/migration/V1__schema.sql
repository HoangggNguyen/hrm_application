CREATE TABLE departments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    manager_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_departments_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE positions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_positions_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE employees (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(20) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    gender VARCHAR(10) NOT NULL,
    dob DATE NOT NULL,
    phone VARCHAR(20) NULL,
    email VARCHAR(100) NOT NULL,
    address VARCHAR(255) NULL,
    id_number VARCHAR(20) NOT NULL,
    tax_code VARCHAR(20) NULL,
    bank_name VARCHAR(100) NULL,
    bank_account VARCHAR(30) NULL,
    department_id BIGINT NOT NULL,
    position_id BIGINT NOT NULL,
    manager_id BIGINT NULL,
    hire_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    resign_date DATE NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_employees_code UNIQUE (code),
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT uk_employees_id_number UNIQUE (id_number),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_employees_position FOREIGN KEY (position_id) REFERENCES positions (id),
    CONSTRAINT fk_employees_manager FOREIGN KEY (manager_id) REFERENCES employees (id),
    INDEX idx_employees_department (department_id),
    INDEX idx_employees_manager (manager_id),
    INDEX idx_employees_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_manager FOREIGN KEY (manager_id) REFERENCES employees (id);

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    employee_id BIGINT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_employee UNIQUE (employee_id),
    CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE contracts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    base_salary DECIMAL(15,0) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_contracts_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE attendances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    work_date DATE NOT NULL,
    check_in DATETIME NULL,
    check_out DATETIME NULL,
    status VARCHAR(20) NOT NULL,
    note VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendances_employee_date UNIQUE (employee_id, work_date),
    CONSTRAINT fk_attendances_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    INDEX idx_attendances_work_date (work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE leave_types (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    days_per_year INT NOT NULL,
    is_paid BOOLEAN NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_leave_types_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE leave_balances (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    year INT NOT NULL,
    total DECIMAL(5,1) NOT NULL,
    used DECIMAL(5,1) NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_leave_balances_year UNIQUE (employee_id, leave_type_id, year),
    CONSTRAINT fk_leave_balances_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_leave_balances_type FOREIGN KEY (leave_type_id) REFERENCES leave_types (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE leave_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    employee_id BIGINT NOT NULL,
    leave_type_id BIGINT NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    days DECIMAL(5,1) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    approver_id BIGINT NULL,
    decided_at DATETIME NULL,
    reject_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_leave_requests_employee FOREIGN KEY (employee_id) REFERENCES employees (id),
    CONSTRAINT fk_leave_requests_type FOREIGN KEY (leave_type_id) REFERENCES leave_types (id),
    CONSTRAINT fk_leave_requests_approver FOREIGN KEY (approver_id) REFERENCES employees (id),
    INDEX idx_leave_requests_employee_status (employee_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payroll_periods (
    id BIGINT NOT NULL AUTO_INCREMENT,
    month INT NOT NULL,
    year INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payroll_period_month_year UNIQUE (month, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE payslips (
    id BIGINT NOT NULL AUTO_INCREMENT,
    period_id BIGINT NOT NULL,
    employee_id BIGINT NOT NULL,
    base_salary DECIMAL(15,0) NOT NULL,
    standard_days INT NOT NULL,
    actual_days DECIMAL(5,1) NOT NULL,
    allowance DECIMAL(15,0) NOT NULL,
    bonus DECIMAL(15,0) NOT NULL,
    deduction DECIMAL(15,0) NOT NULL,
    gross DECIMAL(15,0) NOT NULL,
    net_salary DECIMAL(15,0) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_payslips_period_employee UNIQUE (period_id, employee_id),
    CONSTRAINT fk_payslips_period FOREIGN KEY (period_id) REFERENCES payroll_periods (id),
    CONSTRAINT fk_payslips_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE holidays (
    id BIGINT NOT NULL AUTO_INCREMENT,
    holiday_date DATE NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_holidays_date UNIQUE (holiday_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE settings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    setting_key VARCHAR(50) NOT NULL,
    setting_value VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_settings_key UNIQUE (setting_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;