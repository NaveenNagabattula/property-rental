# Implementation Plan: Property Rental Platform

**Branch**: `001-property-rental-platform` | **Date**: 2026-06-12 | **Spec**: [spec.md](file:///c:/Users/nagab/OneDrive/Desktop/Java/property%20rental/specs/001-property-rental-platform/spec.md)

**Input**: Feature specification from `/specs/001-property-rental-platform/spec.md`

**Note**: This template is filled in by the `/speckit-plan` command. See `.specify/templates/plan-template.md` for the execution workflow.

## Summary

The Property Rental Platform is a full-stack, Airbnb-style monorepo. It features a single Java 25 + Spring Boot 4 REST API, and two React 19 frontends (Customer web app and Admin/Host management portal). Database storage is powered by PostgreSQL with Liquibase migration management. JWT token-based authentication (with refresh tokens) secures roles, and Razorpay handles payments in test mode.

## Technical Context

**Language/Version**: Java 25 (Backend), TypeScript/JavaScript (Frontend)

**Primary Dependencies**: Spring Boot 4, Spring Security, JWT (jjwt), MapStruct, Liquibase, React 19, TanStack Router, TanStack Query, Tailwind CSS, Razorpay Java SDK

**Storage**: PostgreSQL (development/production), H2/PostgreSQL (testing)

**Testing**: JUnit 5 + Mockito (backend service layer), React Testing Library (frontend components)

**Target Platform**: Docker Compose (Backend API + PostgreSQL), Local development node server (frontends)

**Project Type**: Monorepo with Maven backend service and npm workspaces / individual React projects

**Performance Goals**: Search results returned in under 500ms; token verification in under 50ms

**Constraints**: BCrypt cost 12; CORS restricted to trusted frontend origins; rate limiting (5 failed login attempts -> 15 min lockout); strict RBAC

**Scale/Scope**: Supports 5 user roles (Super Admin, Property Manager, Host, Guest, Support Agent) and full booking/listing lifecycle operations

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Java 25 & Spring Boot 4**: Plan uses Java 25 and Spring Boot 4. (✅ Pass)
- **Lombok Usage**: Lombok is used, and `@Data` is explicitly excluded from JPA entities. (✅ Pass)
- **SOLID & Design Patterns**: Standard Service-ServiceImpl structure, Factory Pattern for domain object creation, Builder Pattern for DTOs and Entities, and constructor-only injection are enforced. (✅ Pass)
- **No Entities in Controllers**: Entities will be converted to/from DTOs using MapStruct before entering or leaving the REST controller layer. (✅ Pass)
- **Liquibase & Auditing**: Schema changes will be managed via Liquibase. All entities will inherit from a `BaseEntity` that defines auditing fields. (✅ Pass)
- **Exception Handling**: A global `@RestControllerAdvice` will capture exceptions; try-catch blocks will be avoided in controllers. (✅ Pass)
- **Security & RBAC**: Enforces JWT access & refresh tokens, role-based access control, and BCrypt password hashing. (✅ Pass)
- **Pagination & Validation**: Enforces pagination on all list APIs and standard JSR-380 Bean Validation on DTO inputs. (✅ Pass)

## Project Structure

### Documentation (this feature)

```text
specs/001-property-rental-platform/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── checklists/
│   └── requirements.md  # Quality Checklist
└── contracts/           # Phase 1 output (OpenAPI spec/API contracts)
    ├── auth.json
    ├── properties.json
    ├── bookings.json
    └── admin.json
```

### Source Code (repository root)

```text
apps/
├── customer/             # Customer React 19 Frontend
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   └── services/
│   └── package.json
└── admin/                # Admin/Host React 19 Frontend
    ├── src/
    │   ├── components/
    │   ├── pages/
    │   └── services/
    └── package.json

services/
└── api/                  # Spring Boot 4 Backend API
    ├── src/
    │   ├── main/
    │   │   ├── java/com/propertyrental/api/
    │   │   │   ├── config/      # Spring Security, CORS, Swagger
    │   │   │   ├── controller/  # REST Controllers
    │   │   │   ├── dto/         # Request/Response DTOs
    │   │   │   ├── entity/      # JPA Entities
    │   │   │   ├── exception/   # Global Exception Handling
    │   │   │   ├── mapper/      # MapStruct Mappers
    │   │   │   ├── repository/  # Spring Data JPA Repositories
    │   │   │   └── service/     # Service interfaces & ServiceImpl
    │   │   └── resources/
    │   │       ├── db/changelog/ # Liquibase migrations
    │   │       └── application.yml
    │   └── test/                # JUnit 5 & Mockito service-layer tests
    └── pom.xml

docker-compose.yml        # PostgreSQL + Spring Boot API
.gitignore
README.md
```

**Structure Decision**: Monorepo layout selected using separate backend and frontend folders. Enforces clean boundaries between the single-API backend and separate client applications.

## Complexity Tracking

*No violations of the project constitution detected.*
