# Property Rental Platform

A full-stack, Airbnb-style property rental platform monorepo.

## Project Structure
- `apps/customer/`: React 19 frontend for guest search, listing views, booking, and reviews.
- `apps/admin/`: React 19 frontend for hosts and administrators (listing moderation, host applications, platform configuration).
- `services/api/`: Java 17 + Spring Boot 3 REST API backend.

## Tech Stack
- **Backend**: Java 17, Spring Boot 3.3.0, Spring Security, JWT, JPA, PostgreSQL, Liquibase, MapStruct, Lombok, OpenAPI/Swagger.
- **Frontends**: React 19, Vite, TanStack Router, TanStack Query, Tailwind CSS, Leaflet Maps.
- **Payments**: Razorpay (Test Mode).

## Prerequisites
- Java 17 JDK
- Node.js v20+
- Maven 3.9+
- Docker & Docker Compose (optional, for local PostgreSQL)

## Setup and Running

### 1. Database and Environment Configuration
Configure the following environment variables:
- `DB_URL`: JDBC URL for PostgreSQL (default: `jdbc:postgresql://localhost:5432/property_rental`)
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `JWT_SECRET`: Base64 encoded secret key for JWT signing
- `JWT_EXPIRATION`: Access token lifetime in milliseconds (e.g., `900000` for 15 mins)
- `JWT_REFRESH_EXPIRATION`: Refresh token lifetime in milliseconds (e.g., `604800000` for 7 days)
- `RAZORPAY_KEY_ID`: Razorpay API Key ID (test mode)
- `RAZORPAY_KEY_SECRET`: Razorpay API Key Secret (test mode)
- `ALLOWED_ORIGINS`: CORS allowed origins (e.g. `http://localhost:3000,http://localhost:3001`)

### 2. Running the Backend
```bash
cd services/api
mvn spring-boot:run
```

### 3. Running the Frontends
```bash
# Customer app
cd apps/customer
npm install
npm run dev

# Admin app
cd apps/admin
npm install
npm run dev
```

### 4. Running with Docker Compose
To start PostgreSQL and the API service:
```bash
docker-compose up --build
```
The Swagger UI will be available at `http://localhost:8081/api/v1/docs`.
