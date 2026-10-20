# HRM Task Status

## Summary

- Implemented the Foundation through the Dashboard/Profile/Admin milestones, plus service unit tests and setup documentation.
- The application builds and runs with JDK 19 while targeting Java 17 compatibility.
- Last verification: `mvn clean verify` passed. Manual smoke tests covered the main role-specific pages and selected workflows.
- This is not yet a complete spec sign-off; remaining items are listed below.

## Completed

### Milestone 1: Foundation

- Maven/Spring Boot 3.3, Thymeleaf MVC, Security, JPA, Validation, Flyway, Bootstrap/WebJars, POI, MySQL configuration, and MySQL-only Compose service.
- Flyway schema and seed migrations for the specified tables, constraints, indexes, settings, leave types, departments, positions, and holidays.
- JPA auditing, BCrypt form login, CSRF, logout, role-aware navigation, 403/404 pages, and business-error handling.
- Initializer creates 10 sample employees, active contracts, leave balances, and the four specified accounts.

### Milestone 2: Catalog, Employees, Contracts

- Department/position create, edit, and delete with reference checks.
- Employee search, department/position/status filters, code ordering, page size 10, and filtered Excel export.
- Employee create/edit/detail, unique-field validation, generated employee codes, leave balances on create, resign workflow, and contract creation/expiry rules.
- Manager employee access is read-only and limited to direct reports; service and URL authorization are applied.
- Employee detail includes personal data, contracts, leave balances/history, and HR/Admin confirmed or paid payslips.

### Milestone 3: Attendance

- Check-in/out with Asia/Ho_Chi_Minh time, configured start/grace time, late detection, duplicate protection, and weekend/holiday blocking.
- Monthly filters, employee/department scope, monthly summaries, and HR/Admin create/edit forms.

### Milestone 4: Leave

- Weekday counting excluding holidays; same-year, overlap, paid-balance, and positive-days validation.
- Own request history and cancellation; manager direct-report and HR/Admin approval/rejection; balances update on approval.
- Leave-type CRUD with reference checks.

### Milestone 5: Payroll

- Period creation, draft payslip generation/re-generation, attendance and approved paid-leave calculation, and HALF_UP salary rounding.
- Draft amount edits, confirm/pay transitions, employee-owned confirmed/paid payslips, and browser-printable payslip view.

### Milestone 6: Dashboard, Profile, Admin

- Role-scoped dashboard metrics, HR/Admin department chart and expiring contracts, pending approvals, and employee own balances/attendance.
- Profile contact edits and password change with current-password verification.
- Admin account create/edit/reset, self-demotion/disable guard, work settings, and holiday CRUD.

### Milestone 7: Tests and README

- Mockito/JUnit service tests cover leave weekday counting/create/overlap/scope, attendance late/double check-in, and payroll rounding/standard-day calculations.
- README documents prerequisites, database startup, run command, seed accounts, and assumptions.

## Incomplete or Needs More Coverage

- Server-side pagination with page size 10 is implemented for employees and attendance, but not consistently for every list (catalog, leave requests/approvals/types, payroll periods/payslips, accounts, and holidays).
- Validation errors are field-specific on employee forms; several other forms currently show a general flash error instead of errors beside each invalid field.
- Destructive actions use `window.confirm()` rather than the Bootstrap confirmation modal required by the spec.
- Several tab-like screens use section anchors, and some catalog/leave edit controls are inline or collapsible rather than modal dialogs.
- Unit tests do not yet cover leave balance exhaustion, approval balance updates/reject-reason validation, weekend/holiday attendance blocking, checkout cases, or controller/URL permission matrices. Route checks performed so far were manual, not automated end-to-end tests.
- The local database had already-existing tables and V2 seed data without Flyway history. It was inspected and baselined at version 2 without dropping or recreating those tables. A verification employee `EMP0011` was retained as `RESIGNED` instead of being hard-deleted, per the no-hard-delete rule.

## Verification

- `mvn clean verify`: passed using installed JDK 19 with Java 17 release target.
- Manual smoke checks: login and primary pages for all four roles, representative 403 cases, employee creation/export, leave request and approval, attendance check-in/out, payroll generate/edit/confirm/pay, and payslip ownership.
- Local app URL: <http://localhost:8080/login>