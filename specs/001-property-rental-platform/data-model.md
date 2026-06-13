# Data Model Design: Property Rental Platform

This document describes the database schema design and entity relationships. All entities inherit audit fields from a common `BaseEntity` configuration.

## Base Entity (MappedSuperclass)
Contains audit fields populated automatically via Spring Data JPA `@CreatedDate`, `@LastModifiedDate`, etc.
- `id`: UUID (Primary Key)
- `created_by`: String (Auditor)
- `created_date`: Instant
- `last_modified_by`: String (Auditor)
- `last_modified_date`: Instant

---

## 1. User
Represents all system accounts and user roles.
- `id`: UUID (PK)
- `email`: String (Unique, Not Null, JSR-380 Email format)
- `password_hash`: String (Not Null, BCrypt hashed)
- `first_name`: String (Not Null)
- `last_name`: String (Not Null)
- `role`: Enum (SUPER_ADMIN, PROPERTY_MANAGER, HOST, GUEST, SUPPORT_AGENT)
- `is_active`: Boolean (Default: true)
- `is_email_verified`: Boolean (Default: false)
- `forgot_password_token`: String (Nullable)
- `forgot_password_expiry`: Instant (Nullable)

---

## 2. Property
Represents listing details created by hosts.
- `id`: UUID (PK)
- `host_id`: UUID (FK to User, Not Null)
- `title`: String (Not Null, max 150 chars)
- `description`: String (Not Null, max 2000 chars)
- `address`: String (Not Null)
- `latitude`: Double (Not Null)
- `longitude`: Double (Not Null)
- `price_per_night`: BigDecimal (Not Null)
- `guest_capacity`: Integer (Not Null, min 1)
- `bedroom_count`: Integer (Not Null, min 0)
- `bathroom_count`: Integer (Not Null, min 0)
- `property_type`: Enum (APARTMENT, HOUSE, VILLA, CABIN)
- `status`: Enum (DRAFT, PENDING_REVIEW, ACTIVE, SUSPENDED)
- `amenities`: List<String> (String array or join table)
- `photo_urls`: List<String> (String array or join table)

### Property State Transitions:
- `DRAFT` -> `PENDING_REVIEW` (Host submits listing)
- `PENDING_REVIEW` -> `ACTIVE` (Admin/Manager approves)
- `PENDING_REVIEW` -> `DRAFT` (Admin/Manager rejects with reason)
- `ACTIVE` -> `SUSPENDED` (Admin/Manager suspends active listing)
- `SUSPENDED` -> `ACTIVE` (Admin/Manager lifts suspension)

---

## 3. Booking
Represents guest reservations.
- `id`: UUID (PK)
- `property_id`: UUID (FK to Property, Not Null)
- `guest_id`: UUID (FK to User, Not Null)
- `start_date`: LocalDate (Not Null)
- `end_date`: LocalDate (Not Null)
- `base_price`: BigDecimal (Not Null, start_date to end_date night calculation)
- `service_fee`: BigDecimal (Not Null, platform fee %)
- `tax_fee`: BigDecimal (Not Null, regional tax %)
- `total_price`: BigDecimal (Not Null, base_price + service_fee + tax_fee)
- `status`: Enum (PENDING, CONFIRMED, COMPLETED, CANCELLED)

### Booking State Transitions:
- `PENDING` -> `CONFIRMED` (Instant Book payment success OR Host approval)
- `CONFIRMED` -> `COMPLETED` (Checkout date passed)
- `PENDING`/`CONFIRMED` -> `CANCELLED` (Guest or Host cancels stay)

---

## 4. Payment
Tracks Razorpay orders and verification details.
- `id`: UUID (PK)
- `booking_id`: UUID (FK to Booking, Unique, Not Null)
- `razorpay_order_id`: String (Unique, Not Null)
- `razorpay_payment_id`: String (Unique, Nullable)
- `razorpay_signature`: String (Nullable)
- `amount`: BigDecimal (Not Null)
- `currency`: String (Default: "INR")
- `status`: Enum (CREATED, CAPTURED, REFUNDED, FAILED)

---

## 5. Review
Tracks double-blind stay reviews.
- `id`: UUID (PK)
- `booking_id`: UUID (FK to Booking, Not Null)
- `author_id`: UUID (FK to User, Not Null)
- `recipient_id`: UUID (FK to User, Not Null)
- `rating_cleanliness`: Integer (1 to 5)
- `rating_accuracy`: Integer (1 to 5)
- `rating_check_in`: Integer (1 to 5)
- `rating_communication`: Integer (1 to 5)
- `rating_location`: Integer (1 to 5)
- `rating_value`: Integer (1 to 5)
- `comment`: String (max 1000 chars)
- `host_response`: String (max 1000 chars, Nullable)
- `is_public`: Boolean (Default: false)

---

## 6. PlatformConfig
Stores global configurations (Singleton instance).
- `id`: Long (PK, constant 1)
- `service_fee_percent`: BigDecimal (Default: 10.00)
- `tax_rate_percent`: BigDecimal (Default: 18.00)
- `payout_delay_days`: Integer (Default: 3)
- `cancellation_policy`: Enum (FLEXIBLE, MODERATE, STRICT)
