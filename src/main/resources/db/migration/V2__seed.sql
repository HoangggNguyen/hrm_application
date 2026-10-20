INSERT INTO settings (setting_key, setting_value, created_at, updated_at) VALUES
    ('work_start', '08:30', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('work_end', '17:30', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('late_grace_minutes', '0', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO leave_types (name, days_per_year, is_paid, created_at, updated_at) VALUES
    ('Annual', 12, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Sick', 5, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Unpaid', 0, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO holidays (holiday_date, name, created_at, updated_at)
SELECT STR_TO_DATE(CONCAT(YEAR(CURRENT_DATE), '-01-01'), '%Y-%m-%d'), 'New Year''s Day', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
UNION ALL
SELECT STR_TO_DATE(CONCAT(YEAR(CURRENT_DATE), '-04-30'), '%Y-%m-%d'), 'Reunification Day', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
UNION ALL
SELECT STR_TO_DATE(CONCAT(YEAR(CURRENT_DATE), '-05-01'), '%Y-%m-%d'), 'International Workers'' Day', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
UNION ALL
SELECT STR_TO_DATE(CONCAT(YEAR(CURRENT_DATE), '-09-02'), '%Y-%m-%d'), 'National Day', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP;

INSERT INTO departments (code, name, created_at, updated_at) VALUES
    ('HR', 'Human Resources', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ENG', 'Engineering', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('SALES', 'Sales', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO positions (name, created_at, updated_at) VALUES
    ('HR Specialist', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Engineering Manager', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Software Engineer', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Sales Manager', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Sales Executive', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);