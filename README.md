# E-Commerce Transactional Web Application

A full-stack academic e-commerce application built to demonstrate transactional purchasing, role-based access control, product management, inventory handling, and order fulfillment workflows.

The application uses a sample food catalog inspired by Um Hamza Foodstuff Factory.

## Technology Stack

- **Frontend:** React 19, TypeScript, Vite, React Bootstrap
- **Backend:** Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA
- **Database:** Microsoft SQL Server
- **API Documentation:** Springdoc OpenAPI / Swagger UI
- **Build Tools:** Maven, npm

## Features

- Session-based authentication and customer registration with CSRF protection.
- Role-specific navigation for customers, vendors, and administrators.
- Product browsing, search, category filtering, favorites, and shopping cart.
- Single-vendor checkout.
- Simulated credit and debit card payments.
- Transactional order creation, inventory reservation, payment recording, and invoice generation.
- Vendor approval, rejection, and order fulfillment workflows.
- Customer order history and invoice details.
- Vendor product management with account-based ownership restrictions.
- Administrator vendor and product management.
- Backend validation, authorization, and centralized error handling.


## Prerequisites

- Java 21 or later
- Node.js and npm
- Microsoft SQL Server
- SQL Server database named `UmHamzaECommerce`

## Database Setup

For a fresh database, execute the core SQL scripts in the following order using SQL Server Management Studio or another SQL Server client:

1. `database/01_create_database.sql`
2. `database/02_create_tables.sql`
3. `database/03_insert_sample_data.sql`
4. `database/04_create_triggers.sql`
5. `database/08_enforce_user_role_consistency.sql`
6. `database/10_enforce_monetary_consistency.sql`
7. `database/11_product_variants.sql`
8. `database/13_add_sample_vendor_catalog.sql`

The final script adds the second sample vendor and its products.

### Optional scripts

- `database/05_insert_test_order.sql` — creates a demonstration order.
- `database/06_test_database.sql` — read-only database verification queries.
- Scripts beginning with `07_test`, `09_test`, `11_test`, and `12_test` — database verification and negative tests. Review their comments before execution because some tests deliberately attempt invalid operations inside transactions that are rolled back.

**Important:** Scripts `01_create_database.sql` and `02_create_tables.sql` are intended for fresh database setup. Do not rerun them against an existing populated database.

The sample vendor catalog script is additive and checks for conflicting data before inserting records.

For additional database setup information, see [`database/README.md`](database/README.md).

## Configuration

The backend reads the following environment variables:

| Variable | Description |
|---|---|
| `DB_URL` | SQL Server JDBC connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `APP_DEV_PASSWORD` | Development application password configuration, if applicable |

Configure these variables locally through your shell or IDE run configuration.

Database credentials and other secrets should not be committed to the repository.

Hibernate is configured to validate the existing database schema rather than automatically create, update, or drop database tables.

## Running the Application

### Start the Backend

From the repository root, configure the required database environment variables, then run:

```bash
./backend/mvnw -f backend/pom.xml spring-boot:run
```

The backend runs by default at:

```text
http://localhost:8080
```

### API Documentation

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

### Start the Frontend

Open a second terminal:

```bash
cd frontend
npm install
npm run dev
```

Open the Vite URL displayed in the terminal, usually:

```text
http://localhost:5173
```

The frontend communicates with the backend running at `http://localhost:8080`.

## Academic Demonstration Accounts

The project database includes sample accounts for demonstrating the different application roles.

| Role | Email |
|---|---|
| Customer | `customer1@example.com` |
| Customer | `customer2@example.com` |
| Vendor 1 — Um Hamza | `vendor@umhamza.local` |
| Vendor 1 — Staff | `staff@umhamza.local` |
| Vendor 2 — Levant Pantry | `vendor2@umhamza.local` |
| Administrator | `admin@umhamza.local` |


Registration creates customer accounts only. Vendor and administrator accounts are provisioned separately.

Creating a vendor record through Admin Management does not automatically create a vendor login account.

## Demonstrating Application Roles

### Customer

1. Log in or register.
2. Browse and search products.
3. Add or remove favorites.
4. Add products to the shopping cart.
5. Complete checkout using a simulated payment method.
6. View orders and invoices in **My Orders**.

### Vendor — Um Hamza

Log in using the Um Hamza vendor or staff account.

- **Vendor Workspace:** Manage products belonging to the vendor.
- **Vendor Orders:** Approve, reject, and update order fulfillment status.

### Vendor — Levant Pantry

Log in using the Levant Pantry vendor account.

- Vendor Workspace displays products associated with that vendor.
- Backend ownership checks restrict product and order operations to authorized resources.

### Administrator

Log in using the administrator account.

- **Admin Management:** Manage vendors and products.
- **Vendor Orders:** Access orders across vendors.

Log out before switching accounts.

A page refresh restores the server session while it remains valid. Session expiration clears role-specific screens and customer-owned frontend state.

## Purchase Transaction

Checkout is implemented as a single service-level database transaction.

The checkout process:

1. Validates the customer, cart, products, inventory availability, and quantities.
2. Calculates the order total using `BigDecimal`.
3. Reserves inventory.
4. Creates the delivery address, order, and order items.
5. Updates the cart and order status history.
6. Processes the simulated payment.
7. Records the payment and creates the invoice.

These database operations are performed within the checkout transaction.

If payment processing or persistence fails, the transaction rolls back the database changes, preventing partial order creation and restoring the previously persisted inventory and cart state.

Payment methods are simulated locally and do not connect to an external payment provider.

Available inventory is reserved during checkout. On-hand inventory is deducted when delivery is completed. If a vendor rejects or an order is cancelled, the existing order workflow releases the reservation and handles the payment refund process.

## Testing

### Backend Tests

Run the complete backend test suite:

```bash
./backend/mvnw -f backend/pom.xml test
```

### Transaction Tests

Run the dedicated purchase transaction tests:

```bash
./backend/mvnw -f backend/pom.xml -Dtest=OrderPurchaseTransactionTests test
```

These tests use an isolated in-memory H2 database to verify transaction behavior without modifying the project SQL Server database.

### Frontend Lint

```bash
npm --prefix frontend run lint
```

### Frontend Production Build

```bash
npm --prefix frontend run build
```

There is currently no frontend test script configured.

## Repository Structure

```text
backend/
    Spring Boot API
    Controllers
    Services
    Repositories
    Entities
    DTOs
    Backend tests

database/
    SQL Server database scripts
    Schema
    Sample data
    Triggers
    Verification scripts

frontend/
    React storefront
    Authentication
    Customer workflows
    Vendor management
    Admin management

docs/
    Project documentation

screenshots/
    Application demonstration screenshots
```

## Academic Project Disclaimer

I developed this application for academic purposes.

The product catalog, inventory, vendors, accounts, and payment processing are demonstration data and functionality. Payment methods are simulated and no real financial transactions are processed for now.

