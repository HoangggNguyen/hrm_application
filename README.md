# HRM

Server-rendered Human Resource Management app built with Spring Boot, Thymeleaf, and MySQL.

## Prerequisites

- JDK 17 or newer. The project compiles with Java 17 language compatibility; Java 19 is also supported for local development.
- Maven 3.8+.
- MySQL 8 on `localhost:3306`.

## Start MySQL

Start the MySQL service on port 3306, or use the included MySQL-only Compose service:

```powershell
$env:DB_PASSWORD = 'root'
docker compose up -d
```

Set `DB_USER` and `DB_PASSWORD` when your local MySQL credentials differ from the defaults. Flyway creates the `hrm_db` schema when MySQL permits `createDatabaseIfNotExist`.

## Run

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-19.0.2'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn spring-boot:run
```

Open <http://localhost:8080/login>.

## Seed accounts

| Username | Password | Role |
|---|---|---|
| `admin` | `Admin@123` | ADMIN |
| `hr` | `Hr@12345` | HR |
| `manager` | `Manager@123` | MANAGER |
| `employee` | `Employee@123` | EMPLOYEE |

Passwords are BCrypt-encoded when the initializer creates the sample accounts.

## Assumptions

- Source and target compatibility remain at Java 17; the available JDK 19 is used locally without downloading another JDK.
- The existing local `hrm_db` already had the schema and V2 reference rows but no Flyway history. It was inspected read-only, then baselined at version 2 without changing or deleting its existing tables or data. New empty databases run V1 and V2 normally.
- The initial sample dataset uses fictional employee names, IDs, and contact details.
- MySQL credentials default to `root`/`root` to match the specification; override them with `DB_USER` and `DB_PASSWORD` for local installations.