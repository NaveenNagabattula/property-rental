# Feature Specification: Property Rental Platform

**Feature Branch**: `001-property-rental-platform`

**Created**: 2026-06-12

**Status**: Draft

**Input**: User description: "Assignment #2: Build a Property Rental Platform..."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Guest Search and Discovery (Priority: P1)

Guests want to search, filter, and discover available properties on the platform.

**Why this priority**: Search and discovery is the primary entry point for guests to find listings and eventually book stays, which drives platform utilization.

**Independent Test**: Can be tested independently by loading the customer-facing web app and executing queries with various filters (location, dates, guests) to verify only matching active properties are displayed.

**Acceptance Scenarios**:

1. **Given** there are active property listings in "Mumbai", **When** a guest searches for "Mumbai" with dates 2026-07-01 to 2026-07-05 and 2 guests, **Then** only active listings in Mumbai that are fully available for those dates and accommodate at least 2 guests are returned in the search results.
2. **Given** a guest is viewing the search results, **When** they toggle the "Map View", **Then** the matching listings are displayed as pins on an interactive map.

---

### User Story 2 - Guest Booking Flow and Checkout (Priority: P1)

Guests want to book stays either via Instant Book or by submitting a booking request, with itemized price breakdowns, secure payments, and status transitions.

**Why this priority**: Core transactional module that generates revenue and completes the core user journey.

**Independent Test**: Can be tested by selecting an active property, proceeding to the checkout screen, viewing the itemized breakdown, and completing a simulated test-mode payment to confirm the stay.

**Acceptance Scenarios**:

1. **Given** an active listing with "Instant Book" enabled, **When** a guest submits a booking for valid available dates, **Then** an itemized price breakdown (base rate + service fee + tax) is generated, a Razorpay order is created, and upon successful payment completion, the booking status transitions to "Confirmed".
2. **Given** an active listing with "Request to Book" enabled, **When** a guest submits a request, **Then** a booking is created in "Pending" status, and the Host is notified to approve or reject the request.

---

### User Story 3 - Host Listing Creation and Management (Priority: P2)

Hosts want to register, apply for host status, and create and manage their property listings (details, photos, pricing, calendar) via a multi-step form.

**Why this priority**: Required for hosts to supply listings to the marketplace, though search and basic static discovery can exist first.

**Independent Test**: Can be tested by logging in as a host, submitting a host application, filling out the multi-step listing form, and verifying the listing enters the pending review queue.

**Acceptance Scenarios**:

1. **Given** a registered user, **When** they apply to become a host, **Then** their application enters a pending review status for Admin approval, and they receive an email notification upon decision.
2. **Given** an approved Host, **When** they complete the multi-step property creation form (info, location, amenities, photos, pricing, calendar), **Then** the property listing status is set to "Pending Review" and sent to the Listing Moderation queue.

---

### User Story 4 - Admin Listing Moderation and Host Review (Priority: P2)

Platform managers want to review listings and host applications to ensure quality and compliance.

**Why this priority**: Crucial for platform safety, content moderation, and preventing fraudulent listings before they go live.

**Independent Test**: Can be tested by logging in as a Property Manager, reviewing the listing queue, and executing an approve/reject action.

**Acceptance Scenarios**:

1. **Given** a listing in "Pending Review" status, **When** a Property Manager approves the listing, **Then** the status changes to "Active" and it immediately becomes searchable.
2. **Given** a listing in "Pending Review" status, **When** a Property Manager rejects the listing with a reason, **Then** the status changes to "Draft" and the Host is notified of the requested changes.

---

### User Story 5 - User Management and Platform Configuration (Priority: P3)

Super Admins want to moderate user accounts and configure global settings such as platform fees, tax rates, and cancellation policies.

**Why this priority**: Supports long-term platform tuning, security management, and financial configuration.

**Independent Test**: Can be tested by changing global settings in the Super Admin dashboard and verifying that new bookings calculate fees using the updated values.

**Acceptance Scenarios**:

1. **Given** a Super Admin changes the global service fee from 10% to 12%, **When** a guest initiates a new booking, **Then** the itemized price breakdown displays a 12% service fee.
2. **Given** a disruptive user, **When** an Admin deactivates their account, **Then** their active sessions are invalidated, and they are prevented from logging in.

---

### Edge Cases

- **Double Booking Avoidance**: What happens when two guests attempt to book the exact same listing for overlapping dates simultaneously?
- **Payment Verification Failure**: How does the system handle booking creation if a user completes the Razorpay checkout modal but the webhook payload is delayed, lost, or returns a verification error?
- **Suspension Action on Active Bookings**: What happens to confirmed future bookings when an Admin suspends a listing due to a violation?
- **Review Collusion**: How does the system ensure double-blind reviews remain hidden until both reviews are submitted or the review window expires?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001 (Auth)**: Users MUST be able to register, log in, verify their email, and retrieve or refresh JWT tokens securely.
- **FR-002 (RBAC)**: Enforce role-based access control for Super Admin, Property Manager, Host, Guest, and Support Agent.
- **FR-003 (Property State)**: Property listings MUST transition through the states: Draft → Pending Review → Active → Suspended.
- **FR-004 (Calendar)**: Hosts MUST be able to mark specific dates as blocked on their property calendars, preventing guests from booking them.
- **FR-005 (Search Filters)**: Search API MUST support pagination and filtering by location, dates, guest count, price range, amenities, and property type.
- **FR-006 (Itemized Checkout)**: Bookings MUST calculate an itemized breakdown including base price, platform service fee, and regional taxes.
- **FR-007 (OpenAPI Docs)**: Swagger documentation MUST be auto-generated and exposed at `/api/v1/docs`.
- **FR-008 (User Status)**: Admins MUST be able to soft-delete, deactivate, or activate user accounts, instantly revoking active JWTs.

### Key Entities

- **User**: Represents registered accounts with credentials, email verification status, and one or more roles.
- **Property**: Represents listings with detailed info, address/location coordinates, price per night, amenities list, host reference, and current status.
- **Booking**: Links a Guest and Property for a date range, tracking transaction total, breakdown details, and lifecycle status.
- **Payment**: Tracks Razorpay transaction IDs, payment status (Created, Authorized, Captured, Refunded, Failed), and timestamps.
- **Review**: Double-blind review submitted by Guests and Hosts after completed stays.
- **PlatformConfig**: Singleton entity storing global fees, taxes, cancellation policies, and feature flags.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Search results MUST be paginated and load matching properties in under 500 milliseconds for standard queries.
- **SC-002**: 100% of successful payments verified via webhooks MUST result in booking status changing to "Confirmed" within 5 seconds of payment completion.
- **SC-003**: 100% of user account deactivations MUST result in immediate token validation failure on subsequent API requests.
- **SC-004**: System MUST maintain database integrity by throwing overlapping-date conflicts for overlapping concurrent booking requests.

## Assumptions

- **A-001**: Guests and Hosts communicate in a single timezone context or dates are handled in UTC to avoid overlapping check-in calculations.
- **A-002**: Razorpay payment integration is restricted to test mode for demo purposes, utilizing the mock checkout wrapper.
- **A-003**: Images for properties are stored in a simple directory structure or object storage, and URLs are saved in the database.
- **A-004**: No physical emails are sent in test environments; email verification and notifications are logged or sent to a local SMTP mock (e.g., MailHog) or mock email service implementation.

## [NEEDS CLARIFICATION]

- **[NEEDS CLARIFICATION: Payout Automation]**: Should host payouts be automatically calculated and processed via a background job after a delay, or manually processed/initiated by the Super Admin?
- **[NEEDS CLARIFICATION: Double-blind Review Expiry]**: How long is the window for double-blind reviews to be submitted (e.g., 14 days post-checkout) before any single submitted review is automatically made public?
- **[NEEDS CLARIFICATION: Automatic Refunds]**: Does cancellation of a confirmed booking automatically trigger a Razorpay refund based on the cancellation policy rules, or does it queue a manual review task for an admin?
