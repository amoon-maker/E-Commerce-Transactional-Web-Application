# E-Commerce Transactional Web Application

Semester project for developing a transactional e-commerce web application.

## Project Overview

This project is developed as part of my studies in Transactional Web Applications. It follows a phased development approach, beginning with database design and continuing through backend development, frontend development, testing, and deployment.

## Project Structure

- `database/` — conceptual schema, logical schema, SQL Server implementation, documentation, diagrams, screenshots, and database testing
- `backend/` — backend application development
- `frontend/` — frontend application development
- `testing/` — testing and verification
- `deployment/` — deployment and delivery work

## Database

The Database Completion phase includes:

- Conceptual schema
- Logical schema
- Microsoft SQL Server implementation
- Database constraints and relationships
- Sample data
- Inventory/status trigger
- Database verification and testing

## Development Approach

The project is developed incrementally, with each phase building on the previous work while maintaining organized documentation and version control.

## Academic Project

This repository contains academic project work developed for educational purposes.

## Fresh Database Setup and Verification

### Prerequisites

- Microsoft SQL Server must be running and accessible.
- Connect to the correct SQL Server instance using VS Code or another SQL client that supports T-SQL.
- Run the scripts in the order listed below.

### Setup Order

1. `01_create_database.sql` — creates `UmHamzaECommerce` if it does not already exist.
2. `02_create_tables.sql` — creates the tables, primary keys, foreign keys, constraints, and indexes.
3. `03_insert_sample_data.sql` — inserts illustrative roles, accounts, categories, products, and related sample records.
4. `04_create_triggers.sql` — creates the inventory/status synchronization trigger.
5. `05_insert_test_order.sql` — inserts a demonstration order, item, and status-history record for testing.
6. `06_test_database.sql` — displays database structure, sample data, inventory behavior, and relationship information.
7. `07_test_address_ownership.sql` — verifies that an order cannot reference an address belonging to another customer.
8. `08_enforce_user_role_consistency.sql` — creates the user/profile consistency triggers.
9. `09_test_user_role_consistency.sql` — tests invalid user/vendor associations, role changes, role names, and customer-profile deletion.
10. `10_enforce_monetary_consistency.sql` — applies order and invoice monetary consistency constraints.
11. `11_test_monetary_consistency.sql` — verifies that invalid order and invoice totals and excessive discounts are rejected.
12. `12_test_inventory_trigger.sql` — tests inserts, stock updates, status-only updates, and multi-row updates.

### Important Notes

- For a complete rebuild, use a fresh database or a separate test database. The table-creation and sample-data scripts are not intended to be repeatedly executed against an already populated database.
- The database creation script does not drop an existing database.
- Do not run the complete setup sequence repeatedly on a populated database; duplicate records or existing objects may cause errors.
- The negative-test scripts attempt invalid operations and use transaction rollback to avoid leaving their test changes behind.
- Monetary values use SQL Server `DECIMAL` types. The order and invoice constraints validate component totals and prevent discounts from exceeding subtotals.
- The inventory trigger synchronizes product status with stock quantity, including multi-row operations.
- A copy of the setup scripts was verified against a separate fresh test database. The original working database was preserved during verification.

## Backend — Spring Boot REST API

### Prerequisites

- Java 21 or above
- Microsoft SQL Server running and accessible
- Maven Wrapper included in `backend/`

### Configure Environment Variables

The backend reads database credentials and the temporary development login from environment variables. Do not commit actual credentials to GitHub.

The example below assumes a local SQL Server instance exposed on port `14330` and a database named `UmHamzaECommerce`. Adjust the server address, port, database name, and SQL Server username to match your own setup.

From the project root, configure the variables:

```bash
export DB_URL='jdbc:sqlserver://localhost:14330;databaseName=UmHamzaECommerce;encrypt=true;trustServerCertificate=true'
export DB_USERNAME='YOUR_SQL_SERVER_USERNAME'

printf "SQL Server password: "
read -s DB_PASSWORD
printf '\n'
export DB_PASSWORD

printf "Temporary development password: "
read -s APP_DEV_PASSWORD
printf '\n'
export APP_DEV_PASSWORD
```

Replace `YOUR_SQL_SERVER_USERNAME` with a SQL Server login that has the required permissions for the database. Type each password when prompted; password characters will not appear on screen.

`APP_DEV_PASSWORD` is a locally chosen password for the temporary development login named `user`. Do not include actual passwords in this README or commit them to version control.

### Run the Backend

From the project root, after configuring the environment variables:

```bash
./backend/mvnw -f backend/pom.xml spring-boot:run
```

The API uses port `8080` by default when startup succeeds.

### Swagger Documentation

After starting the backend, open:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

Swagger documentation is permitted without authentication. API endpoints require authentication under the current development security configuration.

### Run Tests

From the project root:

```bash
./backend/mvnw -f backend/pom.xml test
```

Review the test results and confirm `BUILD SUCCESS`.

### Current Security Scope

The current implementation uses Spring Security with a temporary in-memory development user. Database-backed user authentication and role-based authorization remain planned for a subsequent development phase. This configuration is intended for development and is not a production-ready authentication design.
