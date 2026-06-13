# Research & Technical Decisions: Property Rental Platform

This document consolidates research and technical design decisions for the Property Rental Platform.

## 1. Resolution of Clarifications

### Payout Automation
- **Decision**: Automated Scheduler (Daily Job).
- **Rationale**: Reduces administrative overhead by automatically calculating host payouts. A Spring `@Scheduled` background job runs daily at midnight to find bookings completed at least `PlatformConfig.payoutDelayDays` ago, aggregates host balances, and logs payouts.
- **Alternatives Considered**: Manual admin approval (rejected due to scalability issues).

### Double-blind Review Expiry
- **Decision**: 14-Day Limit post-checkout.
- **Rationale**: Encourages timely feedback. If both guest and host submit within 14 days, reviews are made public immediately. If only one party submits, their review is revealed after the 14-day window expires.
- **Alternatives Considered**: Indefinite hiding until both submit (rejected because it discourages completion).

### Automatic Refunds on Cancellation
- **Decision**: Queue for Human Approval.
- **Rationale**: Prevents financial exposure from automated Razorpay refunds. While the system automatically calculates the correct refund amount according to the policy (Flexible, Moderate, Strict), the actual fund transfer is queued in the Admin dashboard for approval and verification.
- **Alternatives Considered**: Full instant automation (rejected to minimize chargeback/fraud risks).

## 2. Technical Stack Decisions

### Java 25 & Spring Boot 4
- **Decision**: Compiler target set to Java 25.
- **Rationale**: Spring Boot 4 baseline aligns with the latest LTS Java releases.
- **Implementation**: Set compiler source and target to `25` in `pom.xml`.

### Security and Rate Limiting
- **Decision**: Spring Security with JWT + Refresh Tokens and custom Filter-based Rate Limiter.
- **Rationale**: Standard stateless security for REST APIs. Access tokens will have a short lifetime (15 mins), while Refresh Tokens will be stored in PostgreSQL/Redis with a longer lifetime (7 days). Rate limiting will track login attempts using an in-memory bucket (or Redis) and lock accounts/IPs for 15 minutes after 5 consecutive failures.

### Database Schema Management (Liquibase)
- **Decision**: Liquibase migrations with plain SQL format (.sql files).
- **Rationale**: Provides readable, standard, and versioned SQL schema changes that are easy to review and write manually.
- **Implementation**: Liquibase changelog files organized under `services/api/src/main/resources/db/changelog/` as formatted SQL files, registered via the master changelog XML.

### Frontend Stack (React 19 + TanStack)
- **Decision**: React 19 + TanStack Router (type-safe routing) + TanStack Query (server state caching) + Tailwind CSS.
- **Rationale**: High performance, modern react hook-only design, and clean decoupled state.
- **Implementation**: Customer app and Admin app will run as independent React applications.
