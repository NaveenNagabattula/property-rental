# Quickstart Validation Guide: Property Rental Platform

This guide details the steps to start and validate the Property Rental Platform locally.

## Prerequisites
- **Java**: JDK 25 installed
- **Node.js**: v18+ and npm installed
- **Docker**: Docker Desktop installed and running
- **Maven**: (Utilizes the bundled `./mvnw` wrapper)

---

## Setup & Running

### Step 1: Start the Infrastructure & Backend API
Spin up PostgreSQL and run database migrations automatically via Liquibase:
```bash
# From the repository root
docker-compose up -d --build
```
Ensure the backend API is running and available at `http://localhost:8081` (or the configured port).

### Step 2: Start the Customer Web Application
```bash
# Navigate to customer frontend
cd apps/customer
npm install
npm run dev
```
Access the Customer App at `http://localhost:5173`.

### Step 3: Start the Admin / Host Portal
```bash
# Navigate to admin/host frontend
cd apps/admin
npm install
npm run dev
```
Access the Admin/Host Portal at `http://localhost:5174`.

---

## E2E Validation Scenario (Happy Path)

### Scenario 1: Host Listing Creation & Approval
1. Open the Customer App at `http://localhost:5173` and register a new user as a "Host".
2. Fill out the Host Application form and submit it.
3. Open the Admin Portal at `http://localhost:5174`, log in as a Super Admin, and navigate to **Host Applications**. Approve the user.
4. Return to the Host dashboard in the Customer App (or Admin/Host portal) and create a new listing. Complete the multi-step form and submit it.
5. In the Admin Portal, go to **Listing Moderation**, locate the pending listing, and click **Approve**.
6. Verify that the listing is now set to `ACTIVE`.

### Scenario 2: Guest Search & Booking
1. In the Customer App, register a new user as a "Guest" or log in with a guest account.
2. Search for the property by location and date range.
3. Select the listing, review the itemized price breakdown, and click **Book Now**.
4. Complete the simulated Razorpay payment checkout.
5. Verify the booking status updates to `Confirmed`.
