# SPEC: HRM (Human Resource Management) Web App

> Audience: an autonomous coding agent. Follow this spec literally. If something is not specified, choose the simplest option, do not add features, and note the assumption in README.md.

## 0. Rules for the agent
1. Build ONLY what is in this spec. No extra modules, screens, or libraries.
2. Server-side rendered MVC (Thymeleaf). No REST API, no SPA, no JS framework. Vanilla JS only where stated.
3. Layered: `controller -> service -> repository`. No business logic in controllers or templates. Controllers use DTO/form objects, never expose entities to forms with sensitive fields.
4. Use constructor injection (Lombok `@RequiredArgsConstructor`). No field `@Autowired`.
5. POST-Redirect-GET for all form submits. Show success/error via flash messages (Bootstrap alerts).
6. Validate server-side with Bean Validation; show errors per field.
7. Build in the milestone order of section 11. After each milestone the app MUST compile and start (`mvn spring-boot:run`) before moving on.
8. UI language: English. Date format `dd/MM/yyyy`. Currency: VND, no decimals, thousands separator.
9. Timezone: `Asia/Ha_Noi`.

## 1. Tech stack (fixed)
| Item | Choice |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.x: Web, Data JPA, Validation, Security, Thymeleaf |
| Templates | Thymeleaf + `thymeleaf-layout-dialect` + `thymeleaf-extras-springsecurity6` |
| UI | Bootstrap 5 + Bootstrap Icons (via WebJars). Chart.js from jsDelivr CDN (pinned version) for Dashboard only |
| DB | MySQL 8, host `localhost`, **port `3306`**, schema `hrm_db`, charset `utf8mb4` |
| Migration | Flyway (`flyway-core`, `flyway-mysql`) |
| Build | Maven |
| Other | Lombok; Apache POI (Excel export of employee list only) |
| Tests | JUnit 5 + Mockito (service unit tests only) |

`application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hrm_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Ha_Noi&characterEncoding=UTF-8
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:root}
  jpa:
    hibernate.ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
  jackson.time-zone: Asia/Ha_Noi
server:
  port: 8080
```
Also provide `docker-compose.yml` with ONLY a MySQL 8 service (port `3306:3306`, db `hrm_db`, env-based password).

## 2. Project structure
```
com.company.hrm
  config/       SecurityConfig, JpaAuditingConfig, DataInitializer
  controller/   one controller per module
  service/      interfaces not required; use concrete @Service classes
  repository/   Spring Data JPA
  entity/       JPA entities + enums
  dto/          form/view objects
  exception/    BusinessException, GlobalExceptionHandler (@ControllerAdvice)
src/main/resources
  db/migration/ V1__schema.sql, V2__seed.sql
  templates/    layout/main.html, fragments/, one folder per module, error/403.html, error/404.html
  static/       css/app.css, js/app.js
```

## 3. Roles and access
Single role per user: `ADMIN`, `HR`, `MANAGER`, `EMPLOYEE`. ADMIN has every HR permission plus `/admin`.
"Direct reports" = employees whose `manager_id` = the manager's own employee id.

| Area | ADMIN / HR | MANAGER | EMPLOYEE |
|---|---|---|---|
| Dashboard | full | direct reports only | own data only |
| Employees list/detail | read + write | read-only, direct reports | no access |
| Contracts | read + write | no | no |
| Catalog (departments, positions) | read + write | no | no |
| Attendance | all employees, can edit | own + direct reports (read) | own only |
| Leave: create own request | yes | yes | yes |
| Leave: approve/reject | all (never own request) | direct reports only | no |
| Leave types CRUD | yes | no | no |
| Payroll manage | yes | no | no |
| Payslips view | all | own only | own only |
| `/admin` | ADMIN only | no | no |

Enforce at URL level (`SecurityFilterChain`) AND in services (check ownership/scope). Unauthorized -> 403 page. A user with `employee_id = NULL` (only seed admin) has no personal attendance/leave/payslips; hide those menu items.

Auth: form login at `/login`, BCrypt, session, CSRF enabled, logout via POST `/logout`. Disabled user cannot log in. Default landing: `/dashboard`.

## 4. Database (Flyway `V1__schema.sql`)
Conventions: snake_case; `id BIGINT AUTO_INCREMENT PK`; all tables have `created_at`, `updated_at` (DATETIME, set via JPA auditing); money `DECIMAL(15,0)`; enums stored as `VARCHAR` (`@Enumerated(STRING)`); engine InnoDB. Add FKs and unique constraints as listed. No hard-delete for employees.

| Table | Columns (type notes) | Constraints |
|---|---|---|
| `departments` | code VARCHAR(20), name VARCHAR(100), manager_id (FK employees, nullable) | UNIQUE code |
| `positions` | name VARCHAR(100) | UNIQUE name |
| `employees` | code VARCHAR(20), full_name VARCHAR(100), gender (MALE/FEMALE/OTHER), dob DATE, phone VARCHAR(20), email VARCHAR(100), address VARCHAR(255), id_number VARCHAR(20), tax_code VARCHAR(20) null, bank_name VARCHAR(100) null, bank_account VARCHAR(30) null, department_id FK, position_id FK, manager_id FK employees null, hire_date DATE, status (PROBATION/ACTIVE/RESIGNED), resign_date DATE null | UNIQUE code, email, id_number |
| `users` | username VARCHAR(50), password VARCHAR(100), role (ADMIN/HR/MANAGER/EMPLOYEE), enabled BOOLEAN default true, employee_id FK null | UNIQUE username; UNIQUE employee_id |
| `contracts` | employee_id FK, type (PROBATION/FIXED_TERM/INDEFINITE), start_date DATE, end_date DATE null, base_salary DECIMAL(15,0), status (ACTIVE/EXPIRED) | |
| `attendances` | employee_id FK, work_date DATE, check_in DATETIME null, check_out DATETIME null, status (PRESENT/LATE), note VARCHAR(255) null | UNIQUE (employee_id, work_date) |
| `leave_types` | name VARCHAR(50), days_per_year INT, is_paid BOOLEAN | UNIQUE name |
| `leave_balances` | employee_id FK, leave_type_id FK, year INT, total DECIMAL(5,1), used DECIMAL(5,1) default 0 | UNIQUE (employee_id, leave_type_id, year) |
| `leave_requests` | employee_id FK, leave_type_id FK, from_date DATE, to_date DATE, days DECIMAL(5,1), reason VARCHAR(500), status (PENDING/APPROVED/REJECTED/CANCELLED), approver_id FK employees null, decided_at DATETIME null, reject_reason VARCHAR(500) null | |
| `payroll_periods` | month INT, year INT, status (DRAFT/CONFIRMED/PAID) | UNIQUE (month, year) |
| `payslips` | period_id FK, employee_id FK, base_salary, standard_days INT, actual_days DECIMAL(5,1), allowance, bonus, deduction, gross, net_salary (all DECIMAL(15,0)) | UNIQUE (period_id, employee_id) |
| `holidays` | holiday_date DATE, name VARCHAR(100) | UNIQUE holiday_date |
| `settings` | setting_key VARCHAR(50), setting_value VARCHAR(100) | UNIQUE setting_key |

Indexes: `employees(department_id)`, `employees(manager_id)`, `employees(status)`, `leave_requests(employee_id, status)`, `attendances(work_date)`.

### Seed
`V2__seed.sql` inserts: settings (`work_start=08:30`, `work_end=17:30`, `late_grace_minutes=0`); leave types (`Annual` 12 paid, `Sick` 5 paid, `Unpaid` 0 unpaid); holidays (Jan 1, Apr 30, May 1, Sep 2 for the current year); 3 departments (HR, ENG, SALES); 5 positions.
`DataInitializer` (`CommandLineRunner`, runs only if `users` is empty) creates: 10 sample employees (with ACTIVE contracts, leave balances for current year, manager links) and users: `admin/Admin@123` (ADMIN, no employee), `hr/Hr@12345` (HR), `manager/Manager@123` (MANAGER), `employee/Employee@123` (EMPLOYEE), each linked to a sample employee except admin. Passwords are BCrypt-encoded in code (never hardcode hashes).

## 5. Business rules
**Employee**
- `code` auto-generated: `EMP` + 4-digit zero-padded sequence (EMP0001...). Read-only in forms.
- `manager_id` must differ from the employee itself.
- Resign: set `status=RESIGNED`, `resign_date` required, set linked user `enabled=false`, set ACTIVE contract to EXPIRED. Resigned employees are hidden from lists by default (filter can show them). Never delete.
- On create: create `leave_balances` rows for the current year for every leave type (`total = days_per_year`, `used = 0`).

**Contract**
- Only one ACTIVE contract per employee. Creating a new one sets the previous ACTIVE to EXPIRED.
- `end_date` required for PROBATION and FIXED_TERM, must be null for INDEFINITE. `base_salary > 0`.

**Attendance**
- Check-in: allowed once per day; creates a row with `check_in = now`. `LATE` if `check_in time > work_start + late_grace_minutes`, else `PRESENT`.
- Check-out: requires existing check-in today; sets `check_out = now`; only once.
- Check-in/out is blocked on weekends (Sat/Sun) and holidays.
- HR/ADMIN can edit any row (check_in, check_out, status, note) or create a missing one.

**Leave**
- `days` = number of weekdays (Mon-Fri) in `[from_date, to_date]` excluding holidays. No half days. Must be > 0.
- Validations: `from_date <= to_date`; no overlap with the same employee's PENDING/APPROVED requests; for paid leave types, `days <= total - used` of the balance for `from_date`'s year; request spanning two years is rejected.
- `Unpaid` (and any type with `days_per_year = 0`) skips the balance check.
- Approver: direct manager (MANAGER role) or any HR/ADMIN. A user can never decide their own request. If the employee has no manager, only HR/ADMIN can decide.
- Approve: status APPROVED, `leave_balances.used += days` (only if the type has a balance), set approver and `decided_at`. Reject: `reject_reason` required.
- Cancel: owner only, only while PENDING.

**Payroll**
- Create period (month, year) -> status DRAFT.
- `Generate` (DRAFT only; re-running overwrites payslips of that period): for each employee with `status IN (PROBATION, ACTIVE)` and an ACTIVE contract:
  - `standard_days` = weekdays in the month minus holidays.
  - `actual_days` = count of attendance rows in the month (PRESENT or LATE) + days of APPROVED **paid** leave falling in the month (weekdays, excluding holidays). Cap at `standard_days`.
  - `base_salary` = contract base salary; `allowance`, `bonus`, `deduction` = 0 initially.
  - `gross = ROUND(base_salary / standard_days * actual_days) + allowance + bonus`
  - `net_salary = gross - deduction` (not below 0).
- HR can edit `allowance`, `bonus`, `deduction` per payslip while the period is DRAFT; `gross`/`net` recalculated server-side.
- `Confirm` (DRAFT -> CONFIRMED): locks edits. `Mark paid` (CONFIRMED -> PAID).
- Employees can only see their own payslips in CONFIRMED or PAID periods.
- Use `BigDecimal`, `RoundingMode.HALF_UP`, scale 0. No tax/insurance calculation.

## 6. Screens
Layout (`layout/main.html`): left sidebar (menu items shown by role) + top bar (user name, Profile link, Logout) + content area + flash alerts. Responsive Bootstrap grid. Delete/destructive actions use a Bootstrap confirm modal. All lists paginate server-side (page size 10).

### 6.1 Login `/login`
Fields: username, password. Show error on bad credentials/disabled account. Public.

### 6.2 Dashboard `/dashboard`
- Cards (ADMIN/HR: company-wide; MANAGER: direct reports): total employees (not RESIGNED), on probation, on leave today (APPROVED leave covering today), late today.
- Chart (ADMIN/HR): employees by department (Chart.js bar).
- Table (ADMIN/HR): contracts expiring within 30 days.
- Table (MANAGER/HR/ADMIN): pending leave requests they can decide (max 5, link to `/leaves`).
- EMPLOYEE: own leave balances + own today's attendance status.

### 6.3 Employees `/employees`
- **List** `GET /employees`: search (`q`: name/code/email), filters (department, position, status), pagination, sort by code. Button "Export Excel" exports the filtered list (POI). Button "New" (HR/ADMIN).
- **Create** `GET/POST /employees/new`, **Edit** `GET/POST /employees/{id}/edit`: one form, sections Personal (full_name*, gender*, dob*, phone, email*, address, id_number*) and Job (department*, position*, manager, hire_date*, status*, tax_code, bank_name, bank_account). `*` = required. Phone regex `^[0-9+\-\s]{8,20}$`; dob must be in the past; email valid and unique; id_number unique.
- **Detail** `GET /employees/{id}`: tabs: *Info* (read-only + Edit and Resign buttons), *Contracts* (table + "Add contract" modal form per section 5), *Leave* (balances for current year + request history, read-only), *Payslips* (list of CONFIRMED/PAID payslips, read-only, HR/ADMIN only).
- **Resign** `POST /employees/{id}/resign` (param `resignDate`).
- **Contracts** `POST /employees/{id}/contracts`.

### 6.4 Catalog `/catalog`
Two tabs: *Departments* (code*, name*, manager) and *Positions* (name*). Inline list + add/edit modal + delete. Delete is blocked with an error message if referenced by any employee.
Routes: `GET /catalog`, `POST /catalog/departments[/{id}]`, `POST /catalog/departments/{id}/delete`, same for `/catalog/positions`.

### 6.5 Attendance `/attendance`
- Top card (users with an employee): today's status, buttons **Check in** / **Check out** (disabled when not applicable). `POST /attendance/check-in`, `POST /attendance/check-out`.
- Filter: month (default current); for MANAGER/HR/ADMIN also employee and department filters (MANAGER limited to direct reports).
- Table: date, employee, check-in, check-out, status. Below it a per-employee monthly summary: days present, late count.
- HR/ADMIN: "Edit" per row (modal) `POST /attendance/{id}` and "Add record" `POST /attendance`.

### 6.6 Leaves `/leaves`
Tabs:
1. *My requests*: balances table (type, total, used, remaining) + requests table (type, dates, days, status, reason) + "New request" modal (leave type*, from*, to*, reason*). `POST /leaves`. "Cancel" button on PENDING rows `POST /leaves/{id}/cancel`.
2. *Pending approvals* (MANAGER/HR/ADMIN): PENDING requests in scope; Approve `POST /leaves/{id}/approve`; Reject modal with reason `POST /leaves/{id}/reject`.
3. *Leave types* (HR/ADMIN): CRUD (name*, days_per_year*, is_paid). Delete blocked if referenced.

### 6.7 Payroll `/payroll`
- HR/ADMIN `GET /payroll`: list of periods (month/year, status, #payslips, total net) + "Create period" form `POST /payroll`.
- HR/ADMIN `GET /payroll/{periodId}`: payslip table (employee, base, standard days, actual days, allowance, bonus, deduction, gross, net). Buttons by status: **Generate** `POST /payroll/{id}/generate` (DRAFT), **Confirm** `POST /payroll/{id}/confirm` (DRAFT), **Mark paid** `POST /payroll/{id}/pay` (CONFIRMED). In DRAFT each row has an edit modal for allowance/bonus/deduction `POST /payroll/payslips/{id}`.
- All roles with an employee: `GET /payroll/my` list of own payslips; `GET /payroll/payslips/{id}` printable payslip page (clean layout + "Print" button using `window.print()`; no PDF library). Access check: owner or HR/ADMIN.
- For HR/ADMIN, menu item "Payroll" goes to `/payroll`; for others it goes to `/payroll/my`.

### 6.8 Profile `/profile`
Shows own employee info (read-only) with editable `phone` and `address`. Change-password form (current password*, new password* min 8, confirm*). `POST /profile`, `POST /profile/password`.

### 6.9 Admin `/admin` (ADMIN only)
Tabs:
1. *Accounts*: list (username, role, enabled, linked employee). Create (username* unique, password* min 8, role*, employee [only employees without an account]). Edit role/enabled. Reset password (sets a new password entered by admin). Routes: `GET /admin`, `POST /admin/users`, `POST /admin/users/{id}`, `POST /admin/users/{id}/password`. Admin cannot disable or demote their own account.
2. *Settings*: `work_start`, `work_end` (HH:mm), `late_grace_minutes` (int >= 0). `POST /admin/settings`.
3. *Holidays*: list + add (date*, name*) + delete. `POST /admin/holidays`, `POST /admin/holidays/{id}/delete`.

### 6.10 Error pages
`error/403.html`, `error/404.html`, and a generic error view for `BusinessException` (shown as flash error on the previous page when possible).

## 7. Non-functional
- Security: BCrypt, CSRF tokens in all forms, Thymeleaf escaping (`th:text`, never `th:utext` with user data), service-level authorization checks.
- Performance: paginate all lists; use fetch joins or `@EntityGraph` to avoid N+1 on lists.
- Logging: SLF4J, log business exceptions at WARN.
- Concurrency: guard against double submit for check-in and approve via the unique constraints / status checks in service.

## 8. Tests (JUnit 5 + Mockito, no DB)
Minimum unit tests: `LeaveService` (day counting, overlap, balance, approve/reject permissions), `AttendanceService` (late detection, double check-in), `PayrollService` (gross/net formula, rounding, standard_days with holidays).

## 9. Deliverables
- Source code, `pom.xml`, `application.yml`, Flyway migrations, `docker-compose.yml` (MySQL only).
- `README.md`: prerequisites, how to start MySQL on port 3306, run command, seed accounts, assumptions made.

## 10. Out of scope (do NOT build)
Recruitment, OT, tax/social insurance calculation, email sending/password reset by email, notifications, audit log, avatar/file uploads, org chart/hierarchical departments, multi-company, i18n, REST API, PDF generation, Excel import, Docker image for the app.

## 11. Milestones (build in order)
1. **Foundation**: project, deps, `application.yml`, Flyway V1/V2, entities, repositories, Security + login/logout, main layout, error pages, `DataInitializer`. *Accept:* app starts, can log in with all 4 seed accounts, menu differs by role.
2. **Catalog + Employees + Contracts**: sections 6.3, 6.4, rules for employee/contract. *Accept:* create, edit, resign an employee; add contract; Excel export works.
3. **Attendance**: 6.5. *Accept:* check-in/out with late detection; HR edits a record; manager sees only direct reports.
4. **Leaves**: 6.6. *Accept:* request -> approve/reject -> balance updated; cancel; all validations from section 5.
5. **Payroll**: 6.7. *Accept:* generate, edit, confirm, pay; employee sees only own confirmed payslip; printable page.
6. **Dashboard + Profile + Admin**: 6.2, 6.8, 6.9. *Accept:* all cards/tables per role; accounts, settings, holidays work.
7. **Tests + README**: section 8 and 9. *Accept:* `mvn test` passes.

## 12. Definition of done
- `mvn clean verify` passes.
- `mvn spring-boot:run` starts against MySQL `localhost:3306` and every route in section 6 works for the allowed roles and returns 403 for others.
- No feature from section 10 exists.
