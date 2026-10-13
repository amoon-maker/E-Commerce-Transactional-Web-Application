# Database Setup — E-Commerce Transactional Web Application

## 1. Overview

Database engine: Microsoft SQL Server
Database name: UmHamzaECommerce
Development environment: Docker and Visual Studio Code SQL Server extension
Connection endpoint used during development: localhost,14330

The sample product data is illustrative academic data, not a claim
about the factory's current inventory.

## 2. Prerequisites

- Docker Desktop installed and running.
- SQL Server container running and accessible.
- VS Code with the SQL Server extension.
- Access to the project SQL scripts in this folder.

Database credentials must be configured locally and must not be
committed to GitHub.

## 3. Fresh Database Installation

IMPORTANT: The table-creation script is intended for a fresh database.
Do not run the complete schema script against an existing populated
database because its CREATE TABLE statements are not intended to
recreate existing tables.

Run the scripts in this order:

1. 01_create_database.sql
   Create the UmHamzaECommerce database. Start from the master
   database connection if required by the script.

2. 02_create_tables.sql
   Create the tables, primary keys, foreign keys, unique constraints,
   check constraints, indexes, and relationships.

3. 03_insert_sample_data.sql
   Insert illustrative roles, accounts, vendors, customers, products,
   and related sample data.

4. 04_create_triggers.sql
   Create or update the product inventory/status synchronization trigger.

5. 05_insert_test_order.sql
   Insert the optional demonstration order used by the database tests.
   Run once per fresh installation.

6. 06_test_database.sql
   Run the database verification queries.

7. 08_enforce_user_role_consistency.sql
   Create or update the user-role/profile consistency triggers.

8. 10_enforce_monetary_consistency.sql
   Apply the monetary constraints. The script checks whether the
   constraints already exist, so it can also be used to update an
   existing database.

After creating the database, refresh the SQL Server connection tree
and select UmHamzaECommerce before running database-specific scripts.

## 4. Verification and Negative Tests

Run the following tests after the schema and sample data are installed:

- 07_test_address_ownership.sql
  Verifies that an order cannot use another customer's delivery address.

- 09_test_user_role_consistency.sql
  Tests invalid user/vendor associations, incompatible customer
  profiles and roles, and invalid role names.

- 11_test_monetary_consistency.sql
  Tests invalid order and invoice totals and excessive discounts.

- 12_test_inventory_trigger.sql
  Tests inserts, stock updates, status-only updates, and multi-row
  inventory changes.

The negative tests deliberately attempt invalid data operations.
Where documented, changes are rolled back so the permanent sample
data remains unchanged.

## 5. Important Notes

- Run 01_create_database.sql and 02_create_tables.sql only when
  creating a fresh database.
- Do not rerun sample-data or demonstration-order inserts on an
  existing database unless the scripts are explicitly designed to
  handle duplicates.
- The trigger scripts use CREATE OR ALTER where applicable.
- Keep schema creation, incremental corrections, and test scripts
  separate.
- Never commit passwords, access tokens, private keys, or other
  credentials.
- Product sample quantities and prices are illustrative only.

## 6. Current Validation

The following checks have passed during development:

- Delivery-address ownership negative test.
- Three user-role consistency negative tests.
- Order and invoice monetary negative tests.
- Five inventory trigger tests covering insert, update, status-only
  update, and multiple affected rows.

Re-run the relevant test scripts when the database schema or trigger
logic changes.