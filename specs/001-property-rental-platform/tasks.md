---
description: "Task list for Property Rental Platform implementation"
---

# Tasks: Property Rental Platform

**Input**: Design documents from `/specs/001-property-rental-platform/`

**Prerequisites**: plan.md ✅ | spec.md ✅ | research.md ✅ | data-model.md ✅ | contracts/ ✅

**Tests**: Service-layer JUnit 5 + Mockito tests are REQUIRED per the project constitution (70%+ coverage).

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Maps to user story from spec.md (US1–US5)
- Paths assume monorepo root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Initialize the monorepo structure and build tooling.

- [x] T001 Create monorepo directory structure: `apps/customer/`, `apps/admin/`, `services/api/`, `.gitignore`, `README.md`
- [x] T002 [P] Scaffold Spring Boot 4 Maven project in `services/api/` with Java 25 target — dependencies: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation, spring-boot-starter-mail, liquibase-core, postgresql, jjwt-api, jjwt-impl, jjwt-jackson, mapstruct, mapstruct-processor, lombok, springdoc-openapi-starter-webmvc-ui, razorpay-java
- [x] T003 [P] Scaffold React 19 customer app in `apps/customer/` using Vite — dependencies: react, react-dom, @tanstack/react-router, @tanstack/react-query, react-hook-form, zod, @hookform/resolvers, tailwindcss, leaflet, react-leaflet, axios
- [x] T004 [P] Scaffold React 19 admin app in `apps/admin/` using Vite — same React/TanStack/Tailwind dependencies as customer app
- [x] T005 Create `docker-compose.yml` in repo root with services: `postgres` (image: postgres:16), `api` (Spring Boot JAR)
- [x] T006 [P] Create `.gitignore` covering Java/Maven targets, `node_modules`, `.env` files, IDE folders
- [x] T007 Create `README.md` documenting project overview, env variable list, and local setup commands

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core backend infrastructure that ALL user stories depend on. No story work begins until this phase is complete.

**⚠️ CRITICAL**: No user story implementation can start until Phase 2 is complete.

- [x] T008 Create `services/api/src/main/java/com/propertyrental/api/entity/BaseEntity.java` — abstract `@MappedSuperclass` with UUID id, `createdBy`, `createdDate`, `lastModifiedBy`, `lastModifiedDate` using Spring Data JPA auditing annotations
- [x] T009 Create `services/api/src/main/java/com/propertyrental/api/config/JpaAuditingConfig.java` — enable JPA auditing via `@EnableJpaAuditing`, provide `AuditorAware<String>` bean reading from `SecurityContext`
- [x] T010 [P] Create `services/api/src/main/resources/application.yml` — configure datasource (PostgreSQL), JPA (show-sql: false, ddl-auto: validate), Liquibase (enabled: true), JWT secret/expiry via env vars, Razorpay keys via env vars, CORS origins via env var
- [x] T011 [P] Create `services/api/src/main/resources/db/changelog/db.changelog-master.xml` — Liquibase root changelog that includes all child changelogs in order
- [x] T012 Create `services/api/src/main/resources/db/changelog/001-create-users.sql` — Liquibase changeset: `users` table with all columns from data-model.md
- [x] T013 Create `services/api/src/main/java/com/propertyrental/api/entity/User.java` — JPA entity (no `@Data`), `@Builder`, `@Getter`, `@Setter`, implements `UserDetails`, fields matching data-model.md, role as `@Enumerated(EnumType.STRING)`, `@Table(name="users")`
- [x] T014 Create `services/api/src/main/java/com/propertyrental/api/entity/enums/` — enum classes: `Role`, `PropertyStatus`, `BookingStatus`, `PaymentStatus`, `CancellationPolicy`, `PropertyType`
- [x] T015 [P] Create `services/api/src/main/java/com/propertyrental/api/repository/UserRepository.java` — extends `JpaRepository<User, UUID>` + `JpaSpecificationExecutor<User>`, dynamic query methods: `findByEmail`, `findByForgotPasswordToken`
- [x] T016 Create `services/api/src/main/java/com/propertyrental/api/security/JwtService.java` — service for generating access tokens (15 min), parsing claims, validating signatures using env-configured secret (JJWT library, HS256)
- [x] T017 Create `services/api/src/main/java/com/propertyrental/api/entity/RefreshToken.java` — JPA entity: id, token (unique), user FK, expiresAt, revoked boolean
- [x] T018 Create `services/api/src/main/resources/db/changelog/002-create-refresh-tokens.sql` — Liquibase changeset: `refresh_tokens` table
- [x] T019 Create `services/api/src/main/java/com/propertyrental/api/repository/RefreshTokenRepository.java` — dynamic query methods: `findByToken`, `deleteByUser`
- [x] T020 Create `services/api/src/main/java/com/propertyrental/api/security/JwtAuthFilter.java` — `OncePerRequestFilter`: extract Bearer token from header, validate via `JwtService`, set `SecurityContextHolder`
- [x] T021 Create `services/api/src/main/java/com/propertyrental/api/config/SecurityConfig.java` — `@Configuration`, `@EnableMethodSecurity`, configure `SecurityFilterChain`: stateless sessions, permit `/api/v1/auth/**` and `/api/v1/docs/**`, require authentication elsewhere, add `JwtAuthFilter` before `UsernamePasswordAuthenticationFilter`, BCrypt password encoder bean (strength 12)
- [x] T022 Create `services/api/src/main/java/com/propertyrental/api/config/CorsConfig.java` — configure CORS to allow only origins defined in `ALLOWED_ORIGINS` env var
- [x] T023 Create `services/api/src/main/java/com/propertyrental/api/config/OpenApiConfig.java` — configure SpringDoc OpenAPI: title, version, JWT `SecurityScheme`, servers URL from env, mounted at `/api/v1/docs`
- [x] T024 Create `services/api/src/main/java/com/propertyrental/api/dto/response/ApiResponse.java` — generic wrapper record `ApiResponse<T>` with success, message, data, timestamp fields; builder pattern
- [x] T025 Create `services/api/src/main/java/com/propertyrental/api/exception/` — custom exceptions: `ResourceNotFoundException`, `DuplicateEmailException`, `BusinessRuleException`, `UnauthorizedException`
- [x] T026 Create `services/api/src/main/java/com/propertyrental/api/exception/GlobalExceptionHandler.java` — `@RestControllerAdvice` handling all custom exceptions, `MethodArgumentNotValidException`, `AccessDeniedException`, returning `ApiResponse` with appropriate HTTP status codes
- [x] T027 Create `services/api/src/main/java/com/propertyrental/api/security/RateLimitFilter.java` — in-memory (ConcurrentHashMap) per-IP login attempt tracker; 5 failures → 15-min lockout returning 423

**Checkpoint**: Foundation ready — user story implementation can now proceed.

---

## Phase 3: User Story 1 — Auth, Registration & Roles (Priority: P1) 🎯 MVP

**Goal**: Any user can register, verify email, log in, and receive JWT tokens. Authenticated requests respect role-based access control.

**Independent Test**: Register a guest user, verify email, log in, receive access+refresh tokens, call a protected endpoint with Bearer token, and confirm role-based rejection for unauthorized endpoints.

### Implementation for User Story 1

- [x] T028 [P] [US1] Create `services/api/src/main/java/com/propertyrental/api/dto/request/RegisterRequest.java` — `@Builder`, Bean Validation (`@Email`, `@NotBlank`, `@Size`), role field restricted to HOST or GUEST
- [x] T029 [P] [US1] Create `services/api/src/main/java/com/propertyrental/api/dto/request/LoginRequest.java` — `@Builder`, `@Email`, `@NotBlank`
- [x] T030 [P] [US1] Create `services/api/src/main/java/com/propertyrental/api/dto/request/RefreshTokenRequest.java`, `ForgotPasswordRequest.java`, `ResetPasswordRequest.java` — all with `@Builder` and Bean Validation
- [x] T031 [P] [US1] Create `services/api/src/main/java/com/propertyrental/api/dto/response/AuthResponse.java` — `@Builder`, fields: accessToken, refreshToken, tokenType, expiresIn
- [x] T032 [US1] Create `services/api/src/main/java/com/propertyrental/api/mapper/UserMapper.java` — MapStruct interface: `toDto(User)`, `toEntity(RegisterRequest)`
- [x] T033 [US1] Create `services/api/src/main/java/com/propertyrental/api/service/AuthService.java` interface — methods: `register`, `login`, `refreshToken`, `verifyEmail`, `forgotPassword`, `resetPassword`
- [x] T034 [US1] Create `services/api/src/main/java/com/propertyrental/api/service/impl/AuthServiceImpl.java` — constructor injection, implements `AuthService`: register (check duplicate, encode password, generate verification token, save user), login (authenticate, generate JWT+refresh, rate-limit check), refresh (validate refresh token, issue new access token), email verification, forgot/reset password flows
- [x] T035 [US1] Create `services/api/src/main/java/com/propertyrental/api/service/RefreshTokenService.java` interface + `RefreshTokenServiceImpl.java` — create, validate (check not revoked/expired), revoke by user
- [x] T036 [US1] Create `services/api/src/main/java/com/propertyrental/api/service/EmailService.java` interface + `EmailServiceImpl.java` — send verification email, forgot-password email using Spring Mail; templates use env vars for base URL
- [x] T037 [US1] Create `services/api/src/main/java/com/propertyrental/api/controller/AuthController.java` — `@RestController`, `@RequestMapping("/api/v1/auth")`, no try-catch, all methods return `ResponseEntity<ApiResponse<?>>`, Swagger `@Operation` + `@ApiResponse` annotations on all endpoints per auth.json contract
- [x] T038 [US1] Write `services/api/src/test/java/com/propertyrental/api/service/AuthServiceImplTest.java` — Mockito unit tests: register success, register duplicate email, login success, login wrong password, token refresh, email verification

**Checkpoint**: User Story 1 independently testable — full auth flow works end-to-end.

---

## Phase 4: User Story 2 — Guest Search & Discovery (Priority: P1) 🎯 MVP

**Goal**: Guests can search, filter, and browse active property listings with pagination and map coordinates.

**Independent Test**: Call `GET /api/v1/properties?location=Mumbai&startDate=2026-07-01&endDate=2026-07-05&guests=2&page=0&size=12` and verify only ACTIVE properties matching all criteria are returned with correct pagination metadata.

### Implementation for User Story 2

- [x] T039 [P] [US2] Create `services/api/src/main/resources/db/changelog/003-create-properties.sql` — Liquibase changeset: `properties` table with all columns from data-model.md
- [x] T040 [P] [US2] Create `services/api/src/main/java/com/propertyrental/api/entity/Property.java` — JPA entity, `@Builder`, `@Getter`, `@Setter`, `@ManyToOne` host (User), `@ElementCollection` for amenities/photoUrls, `@Enumerated` for status and propertyType, no `@Data`
- [x] T041 [US2] Create `services/api/src/main/java/com/propertyrental/api/repository/PropertyRepository.java` — extends `JpaRepository<Property, UUID>` + `JpaSpecificationExecutor<Property>`, dynamic query method: `findByHostId`
- [x] T042 [US2] Create `services/api/src/main/java/com/propertyrental/api/repository/specification/PropertySpecification.java` — static `Specification<Property>` factories: `hasLocation`, `hasAvailability`, `hasGuestCapacity`, `hasPriceRange`, `hasPropertyType`, `hasAmenity`, `isActive` — used only in Search API per constitution
- [x] T043 [P] [US2] Create `services/api/src/main/java/com/propertyrental/api/dto/request/PropertySearchRequest.java` — `@Builder`, all filter fields with Bean Validation, page/size/sort
- [x] T044 [P] [US2] Create `services/api/src/main/java/com/propertyrental/api/dto/response/PropertySummaryResponse.java` and `PropertyDetailResponse.java` — `@Builder`, fields per properties.json contract
- [x] T045 [US2] Create `services/api/src/main/java/com/propertyrental/api/mapper/PropertyMapper.java` — MapStruct: `toSummaryDto(Property)`, `toDetailDto(Property)`, `toEntity(CreatePropertyRequest)`
- [x] T046 [US2] Create `services/api/src/main/java/com/propertyrental/api/service/PropertyService.java` interface — methods: `searchProperties`, `getPropertyById`, `createProperty`, `updateProperty`, `submitForReview`, `getHostListings`
- [x] T047 [US2] Create `services/api/src/main/java/com/propertyrental/api/service/impl/PropertyServiceImpl.java` — constructor injection, implements `PropertyService`: searchProperties uses `JpaSpecificationExecutor` with assembled `Specification` chain, returns `Page<PropertySummaryResponse>`
- [x] T048 [US2] Create `services/api/src/main/java/com/propertyrental/api/controller/PropertyController.java` — `@RestController`, `@RequestMapping("/api/v1/properties")`, search endpoint (public), detail endpoint (public), Swagger annotations per properties.json contract, no try-catch
- [x] T049 [US2] Write `services/api/src/test/java/com/propertyrental/api/service/PropertyServiceImplTest.java` — Mockito tests: search with filters, search returns empty when no match, getById success, getById not found

**Checkpoint**: Search & discovery independently functional — guests can browse and filter properties.

---

## Phase 5: User Story 3 — Host Listing Creation & Management (Priority: P2)

**Goal**: Approved hosts can create listings (multi-step form data), manage photos/amenities, and submit for review. Property status transitions from DRAFT → PENDING_REVIEW.

**Independent Test**: Log in as a HOST role user, POST a full `CreatePropertyRequest`, verify the property is created in `DRAFT` status. Then call the submit endpoint, verify status changes to `PENDING_REVIEW`.

### Implementation for User Story 3

- [x] T050 [P] [US3] Create `services/api/src/main/java/com/propertyrental/api/dto/request/CreatePropertyRequest.java` and `UpdatePropertyRequest.java` — `@Builder`, full Bean Validation per properties.json contract
- [x] T051 [US3] Create `services/api/src/main/java/com/propertyrental/api/entity/HostApplication.java` — JPA entity: id, user FK, status (PENDING/APPROVED/REJECTED), reason, `@Builder`, `@Getter`, `@Setter`
- [x] T052 Create `services/api/src/main/resources/db/changelog/004-create-host-applications.sql` — Liquibase changeset: `host_applications` table
- [x] T053 Create `services/api/src/main/java/com/propertyrental/api/repository/HostApplicationRepository.java` — dynamic query method: `findByUserId`, `findByStatus`
- [x] T054 [US3] Extend `PropertyServiceImpl` with `createProperty` (validates user is HOST, creates in DRAFT), `updateProperty` (only owner or admin, validates state), `submitForReview` (DRAFT → PENDING_REVIEW transition), `getHostListings` with pagination
- [x] T055 [US3] Extend `PropertyController` with POST (create), PUT (update), POST /{id}/submit, GET /host/my-listings endpoints — all `@PreAuthorize("hasRole('HOST')")`, Swagger annotations, `@Valid` on request bodies
- [x] T056 [US3] Write `services/api/src/test/java/com/propertyrental/api/service/PropertyServiceImplTest.java` — add test cases: createProperty success, createProperty by non-host throws, submitForReview success, submitForReview from wrong state throws

**Checkpoint**: Hosts can create and submit listings — listing moderation queue can now be populated.

---

## Phase 6: User Story 4 — Booking Flow & Payments (Priority: P1) 🎯 MVP

**Goal**: Guests can book a property (Instant Book or Request-to-Book). Itemized price breakdown is generated. Razorpay order is created for Instant Book. Payment verification confirms the booking.

**Independent Test**: As a guest, POST a `CreateBookingRequest` for an ACTIVE Instant-Book property. Verify Razorpay order ID is returned in the response. Call `POST /api/v1/payments/verify` with mock valid signature. Verify booking status is now `CONFIRMED`.

### Implementation for User Story 4

- [x] T057 [P] [US4] Create `services/api/src/main/resources/db/changelog/005-create-bookings.sql` — Liquibase changeset: `bookings` table
- [x] T058 [P] [US4] Create `services/api/src/main/resources/db/changelog/006-create-payments.sql` — Liquibase changeset: `payments` table
- [x] T059 [P] [US4] Create `services/api/src/main/java/com/propertyrental/api/entity/Booking.java` — JPA entity, `@Builder`, `@Getter`, `@Setter`, ManyToOne property + guest, date range, price breakdown fields, `BookingStatus` enum
- [x] T060 [P] [US4] Create `services/api/src/main/java/com/propertyrental/api/entity/Payment.java` — JPA entity, `@Builder`, `@Getter`, `@Setter`, OneToOne booking, Razorpay fields, `PaymentStatus` enum
- [x] T061 [US4] Create `services/api/src/main/java/com/propertyrental/api/repository/BookingRepository.java` — dynamic query methods: `findByGuestId`, `findByPropertyId`, `existsByPropertyIdAndDateOverlap` (custom JPQL for date conflict check), `findByPropertyIdAndStatus`
- [x] T062 [US4] Create `services/api/src/main/java/com/propertyrental/api/repository/PaymentRepository.java` — dynamic query methods: `findByBookingId`, `findByRazorpayOrderId`
- [x] T063 [US4] Create `services/api/src/main/java/com/propertyrental/api/entity/PlatformConfig.java` — JPA entity with singleton id=1L, serviceFeePercent, taxRatePercent, payoutDelayDays, cancellationPolicy enum
- [x] T064 Create `services/api/src/main/resources/db/changelog/007-create-platform-config.sql` — Liquibase changeset: `platform_config` table with seed row (id=1)
- [x] T065 Create `services/api/src/main/java/com/propertyrental/api/repository/PlatformConfigRepository.java` — `JpaRepository<PlatformConfig, Long>`
- [x] T066 [P] [US4] Create `services/api/src/main/java/com/propertyrental/api/dto/request/CreateBookingRequest.java` and `PaymentVerifyRequest.java` — `@Builder`, Bean Validation per bookings.json contract
- [x] T067 [P] [US4] Create `services/api/src/main/java/com/propertyrental/api/dto/response/BookingResponse.java` — `@Builder`, all fields per bookings.json including itemized breakdown
- [x] T068 [US4] Create `services/api/src/main/java/com/propertyrental/api/mapper/BookingMapper.java` — MapStruct: `toDto(Booking)`
- [x] T069 [US4] Create `services/api/src/main/java/com/propertyrental/api/service/BookingService.java` interface + `BookingServiceImpl.java` — createBooking: validate property is ACTIVE, check date overlap (throw `BusinessRuleException` on conflict), calculate itemized price using PlatformConfig, create Razorpay order for Instant Book, persist Booking + Payment, return `BookingResponse`
- [x] T070 [US4] Create `services/api/src/main/java/com/propertyrental/api/service/RazorpayService.java` interface + `RazorpayServiceImpl.java` — createOrder, verifySignature (HMAC-SHA256 on `razorpayOrderId + "|" + razorpayPaymentId` using webhook secret), initiateRefund
- [x] T071 [US4] Create `services/api/src/main/java/com/propertyrental/api/service/PaymentService.java` interface + `PaymentServiceImpl.java` — verifyAndConfirm: call `RazorpayService.verifySignature`, update Payment to CAPTURED, update Booking to CONFIRMED; handleWebhook: verify Razorpay webhook signature from `X-Razorpay-Signature` header
- [x] T072 [US4] Create `services/api/src/main/java/com/propertyrental/api/controller/BookingController.java` — `@RequestMapping("/api/v1/bookings")`, createBooking `@PreAuthorize("hasRole('GUEST')")`, listMyBookings (guest), getBookingById (guest/host/admin), cancelBooking, Swagger annotations, no try-catch
- [x] T073 [US4] Create `services/api/src/main/java/com/propertyrental/api/controller/PaymentController.java` — POST `/verify` (authenticated), POST `/webhook` (public, verifies header signature), Swagger annotations
- [x] T074 [US4] Write `services/api/src/test/java/com/propertyrental/api/service/BookingServiceImplTest.java` — tests: createBooking success (Instant Book), createBooking date conflict throws, createBooking for inactive property throws, cancelBooking success

**Checkpoint**: Full booking and payment flow independently functional end-to-end.

---

## Phase 7: User Story 5 — Admin & Moderation (Priority: P2)

**Goal**: Admins and Property Managers can moderate listings, manage host applications, manage user accounts, and configure platform settings.

**Independent Test**: Log in as PROPERTY_MANAGER, call `GET /api/v1/admin/listings/pending`, approve a listing, verify its status changes to ACTIVE. Log in as SUPER_ADMIN, update service fee, verify new bookings use the updated fee.

### Implementation for User Story 5

- [x] T075 [P] [US5] Create `services/api/src/main/java/com/propertyrental/api/dto/request/ModerationActionRequest.java`, `ChangeRoleRequest.java`, `UpdatePlatformConfigRequest.java` — `@Builder`, Bean Validation per admin.json contract
- [x] T076 [P] [US5] Create `services/api/src/main/java/com/propertyrental/api/dto/response/UserResponse.java`, `HostApplicationResponse.java`, `PlatformConfigResponse.java` — `@Builder`
- [x] T077 [US5] Create `services/api/src/main/java/com/propertyrental/api/service/AdminPropertyService.java` interface + `AdminPropertyServiceImpl.java` — constructor injection, `@PreAuthorize` at service level: getPendingListings (PROPERTY_MANAGER+), approveListing (state: PENDING_REVIEW → ACTIVE, notify host), rejectListing (PENDING_REVIEW → DRAFT, notify host with reason), suspendListing (ACTIVE → SUSPENDED)
- [x] T078 [US5] Create `services/api/src/main/java/com/propertyrental/api/service/UserManagementService.java` interface + `UserManagementServiceImpl.java` — searchUsers (uses `JpaSpecificationExecutor<User>` with dynamic spec for query/role/isActive), deactivateUser (set isActive=false, revoke all refresh tokens), activateUser, changeRole (SUPER_ADMIN only), softDelete
- [x] T079 [US5] Create `services/api/src/main/java/com/propertyrental/api/repository/specification/UserSpecification.java` — static Specification factories: `hasEmailContaining`, `hasRole`, `isActive`
- [x] T080 [US5] Create `services/api/src/main/java/com/propertyrental/api/service/HostApplicationService.java` interface + `HostApplicationServiceImpl.java` — applyForHost (creates PENDING application), getPendingApplications, approveApplication (change user role to HOST, notify), rejectApplication (notify with reason)
- [x] T081 [US5] Create `services/api/src/main/java/com/propertyrental/api/service/PlatformConfigService.java` interface + `PlatformConfigServiceImpl.java` — getConfig, updateConfig (SUPER_ADMIN only)
- [x] T082 [US5] Create `services/api/src/main/java/com/propertyrental/api/controller/AdminController.java` — `@RequestMapping("/api/v1/admin")`, all endpoints with `@PreAuthorize` role checks per admin.json contract, full Swagger annotations, no try-catch, returns `ResponseEntity<ApiResponse<?>>`
- [x] T083 [US5] Write `services/api/src/test/java/com/propertyrental/api/service/AdminPropertyServiceImplTest.java` — tests: approveListing success, approveListing from wrong state throws, rejectListing success, suspendListing success
- [x] T084 [US5] Write `services/api/src/test/java/com/propertyrental/api/service/UserManagementServiceImplTest.java` — tests: deactivateUser success (tokens revoked), changeRole by non-super-admin throws, searchUsers filters correctly

**Checkpoint**: All 5 user stories independently functional.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Integration, final wiring, and production readiness across all stories.

- [x] T085 [P] Implement Reviews module: create `services/api/src/main/resources/db/changelog/008-create-reviews.sql`, `Review.java` entity, `ReviewRepository.java`, `ReviewService.java` interface + `ReviewServiceImpl.java` (double-blind logic: `isPublic=false` until both submitted OR 14-day window expires), `ReviewController.java` at `/api/v1/reviews`, Swagger annotations
- [x] T086 [P] Implement scheduled payout job: create `services/api/src/main/java/com/propertyrental/api/scheduler/PayoutScheduler.java` — `@Scheduled(cron = "0 0 0 * * *")`, finds COMPLETED bookings older than `payoutDelayDays` and logs payout records
- [x] T087 [P] Add booking cancellation & refund queueing: extend `BookingServiceImpl.cancelBooking` to calculate refund per cancellation policy (FLEXIBLE=full, MODERATE=partial, STRICT=no-refund), persist a `RefundRequest` entity queued for Admin approval
- [x] T088 Create `services/api/src/main/resources/db/changelog/009-create-refund-requests.sql` — Liquibase changeset: `refund_requests` table
- [x] T089 [P] Customer frontend — implement core pages in `apps/customer/src/pages/`: `SearchPage.tsx` (filter form + map via Leaflet + property grid), `ListingDetailPage.tsx`, `BookingCheckoutPage.tsx` (itemized breakdown + Razorpay checkout JS), `MyTripsPage.tsx`, `AuthPages.tsx` (register/login/verify)
- [x] T090 [P] Admin frontend — implement core pages in `apps/admin/src/pages/`: `DashboardPage.tsx` (KPI cards), `ListingModerationPage.tsx` (pending queue + approve/reject actions), `UserManagementPage.tsx` (table + role/status controls), `HostApplicationsPage.tsx`, `PlatformConfigPage.tsx`
- [x] T091 [P] Implement JWT interceptor in both frontends: `apps/customer/src/services/apiClient.ts` and `apps/admin/src/services/apiClient.ts` — native Fetch wrapper that injects `Authorization: Bearer <token>` header from localStorage, auto-refreshes on 401
- [x] T092 Add rate limiting integration test: `services/api/src/test/` — verify 5 failed logins return 423 on the 6th attempt within the window
- [x] T093 Verify Liquibase changelogs apply cleanly: run `docker-compose up` and confirm `api` service starts without schema errors
- [x] T094 [P] Security hardening audit: verify no secrets in code, CORS origins locked to env var, BCrypt strength=12, no entity exposed from any controller, all list endpoints paginated
- [x] T095 [P] Run full Swagger validation: start API locally, open `/api/v1/docs`, verify all endpoints from auth.json, properties.json, bookings.json, admin.json contracts are documented with sample requests
- [x] T096 Update `README.md` with final env variable list, `docker-compose up` instructions, and Swagger URL


---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — **BLOCKS all user stories**
- **User Stories (Phases 3–7)**: All depend on Phase 2 completion
  - US1 (Auth) — must complete first as it provides security context for all other stories
  - US2 (Search) and US3 (Host Listing) — can start in parallel after US1 JWT works
  - US4 (Booking) — depends on US2 (active listings) and US3 (property entity)
  - US5 (Admin) — depends on US1 (roles), US2 (property state), US3 (host application)
- **Polish (Phase 8)**: Depends on all core stories complete

### Within Each User Story

- Entities → Repositories → Services → Controllers → Tests
- DTOs and Mappers can be created in parallel with entity work

### Parallel Opportunities

- All `[P]`-marked tasks can run simultaneously within their phase
- Phase 1 setup tasks (T002–T004): frontend and backend scaffolding fully parallel
- Phase 2 Liquibase changelogs (T012, T018) and security config (T016, T020, T021, T022, T023) fully parallel
- Within each user story phase: DTO tasks and entity tasks can run in parallel

---

## Implementation Strategy

### MVP First

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: US1 — Auth
4. Complete Phase 6: US4 — Booking (requires US2 search for property selection)
5. Complete Phase 4: US2 — Search (needed by booking)
6. **STOP and VALIDATE**: Guest can search → view listing → book → pay
7. Demo the MVP

### Full Delivery Order

Phase 1 → Phase 2 → Phase 3 (US1) → Phase 4 (US2) → Phase 5 (US3) → Phase 6 (US4) → Phase 7 (US5) → Phase 8 (Polish)

---

## Notes

- `[P]` tasks = different files, no shared state, safe to parallelize
- `[USn]` label = maps to user story for implementation traceability
- Each user story phase MUST be independently testable before moving on
- Never expose JPA entities from controllers — always map through `ApiResponse<DTO>`
- All list endpoints MUST be paginated (Spring `Pageable`)
- No try-catch in controllers — all exceptions handled by `GlobalExceptionHandler`
- No field injection — constructor injection only, enforced by constitution
