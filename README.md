E-Commerce Transactional Web Application

A full-stack academic e-commerce project developed for the Transactional Web Applications course.

**Project:** Um Hamza Food Stuff e-commerce project
**Repository:** E-Commerce-Transactional-Web-Application  
**Current milestone:** Quiz 3 — Frontend Development

---

## 1. Project Overview

This project aims to develop an e-commerce platform for Um Hamza Food Stuff Factory, presenting traditional food products through a user-friendly shopping experience.

The application is being developed incrementally, covering database design, REST API development, frontend implementation, testing, and eventual transactional integration.

## 2. Development Milestones

| Milestone | Scope | Progress |
|---|---|---|
| Quiz 1 | Database design, SQL Server implementation, relationships, constraints, sample data and testing | Completed |
| Quiz 2 | Spring Boot REST API, Product and Vendor CRUD, validation, security and Swagger documentation | Completed |
| Quiz 3 | React storefront, product catalogue, search, shopping cart, checkout, responsive design and dark mode | Implemented and tested |

## 3. Technology Stack

**Frontend**
- React 19, TypeScript and Vite
- React Bootstrap and Bootstrap 5
- CSS

**Backend**
- Java 21 and Spring Boot
- Spring Data JPA / Hibernate
- Spring Security
- Microsoft SQL Server
- Maven and Swagger / OpenAPI

**Database**
- Microsoft SQL Server
- Relational database design, constraints, triggers and sample data

## 4. Current Application Features

The React storefront currently includes:

- Um Hamza branding and product presentation.
- Product catalogue with product images and descriptions.
- Product search and category filtering.
- Shopping cart with quantity management.
- Automatic subtotal and order total calculations.
- Checkout form with required-field and email validation.
- Order confirmation demonstration.
- Responsive desktop and mobile layouts.
- Light and dark display modes.

## 5. Running the Application

### Frontend

**Requirements:** Node.js and npm.

From the project root:

```bash
cd frontend
npm install
npm run dev
```

Open the local development URL displayed in the terminal, usually:

http://localhost:5173/

To verify the production build, run from the project root:

```bash
npm --prefix frontend run build
```

### Backend

**Requirements:** Java 21 or later and an accessible SQL Server instance.

Configure the following environment variables locally:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `APP_DEV_PASSWORD`

Refer to the backend configuration and project setup instructions before starting the service.

From the project root:

```bash
./backend/mvnw -f backend/pom.xml spring-boot:run
```

The backend uses port `8080` by default.

Swagger documentation:

http://localhost:8080/swagger-ui/index.html

Run backend tests:

```bash
./backend/mvnw -f backend/pom.xml test
```

### Database

The `database/` directory contains the SQL scripts for database creation, table structures, sample data, constraints, triggers and verification.

Follow the documented script execution order when setting up a fresh database.

## 6. Testing and Verification

The current frontend has undergone functional checks covering:

- Product category filtering and search.
- Shopping cart additions, quantity changes, removal and totals.
- Empty-cart behavior.
- Required-field and email validation.
- Checkout confirmation and return-to-storefront behavior.
- Dark mode and responsive layouts.

The TypeScript production build was also verified successfully.

The Quiz 2 report documents the backend milestone's API testing, validation, authentication and Swagger verification.

## 7. Current Scope and Next Development Phase

The current storefront uses sample product data and local frontend cart state. Checkout is a demonstration flow; it does not create or persist real orders.

The React frontend is not yet integrated with the Spring Boot API.

Future development will focus on frontend/backend integration, database-backed customer and order operations, authentication, payment workflows and complete transactional processing.

## 8. Repository Structure

```text
E-Commerce-Transactional-Web-Application/
├── backend/       # Spring Boot REST API
├── database/      # SQL Server scripts and database design
├── deployment/    # Deployment preparation
├── docs/          # Project reports and documentation
├── frontend/      # React e-commerce storefront
├── testing/       # Testing and verification resources
├── screenshots/   
└── README.md
```

## 9. Security and Repository Practices

- Database credentials and development passwords are configured through local environment variables.
- Actual credentials must never be committed to version control.
- The development authentication configuration is not intended for production use.
- GitHub is used for source control and incremental project milestones.

---

**Academic Project — Transactional Web Applications**  
Um Hamza Food Stuff Factory | E-Commerce Transactional Web Application
