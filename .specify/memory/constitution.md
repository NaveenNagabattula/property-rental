<!--
Sync Impact Report:
- Version change: 1.0.0 -> 1.0.1
- List of modified principles:
  - Updated "IV. Data Access & Schema Governance" (enforced .sql-only Liquibase changesets)
- Added sections: none
- Removed sections: none
- Templates requiring updates: none (project templates do not specify migration format, but research.md and tasks.md updated to align)
- Follow-up TODOs: none
-->

# Property Rental Constitution

## Core Principles

### I. Technology Stack & Coding Standards
- The project MUST use Java 25 and Spring Boot 4.
- The project MUST use Lombok annotations to reduce boilerplate, but MUST NOT use `@Data` annotation on JPA Entities to avoid performance issues and infinite loops in `equals`/`hashCode`/`toString`.
- The project MUST strictly adhere to SOLID principles.
- Code generated MUST be production-grade, scalable, maintainable, enterprise-ready, and interview-quality.

### II. Architectural Layers & Dependency Injection
- The project MUST use Service Interfaces and corresponding `ServiceImpl` implementation classes.
- The project MUST use constructor injection only; field injection (e.g., `@Autowired` on fields) is strictly forbidden.
- The project MUST use the Factory Pattern for creating domain objects.
- The project MUST use the Builder Pattern for DTOs and Entity classes.

### III. API Design & Security Guidelines
- The project MUST NOT expose JPA Entity classes from Controllers. All controller requests and responses MUST use DTOs.
- The project MUST use MapStruct for mapping between entities and DTOs.
- The project MUST follow REST naming conventions for endpoints.
- The project MUST maintain standard API Response structures.
- The project MUST use Spring Security with JWT (JSON Web Token) and Refresh Tokens.
- The project MUST enforce Role-Based Access Control (RBAC).
- The project MUST implement pagination on all list APIs.

### IV. Data Access & Schema Governance
- The project MUST use Liquibase for all database schema management. Manual schema changes are forbidden. All Liquibase changesets MUST be written as plain .sql files (using the Liquibase formatted SQL changelog format); XML, YAML, and JSON changelog formats are NOT permitted for changesets. The master changelog file (e.g., db.changelog-master.xml or db.changelog-master.yaml) MUST only serve as a registry to include these .sql files.
- The project MUST implement audit fields (e.g., created_by, created_date, last_modified_by, last_modified_date) using a `BaseEntity`.
- The project MUST use enums instead of raw string values for status representations.
- The project MUST use Dynamic Query Methods in repositories for simple queries.
- The project MUST use `JpaSpecificationExecutor` only for advanced Search APIs.

### V. Quality, Testing & Error Handling
- The project MUST write JUnit and Mockito tests for the service layer.
- The project MUST use Bean Validation (`@Valid`, `@NotNull`, etc.) for all input models.
- The project MUST handle exceptions globally using `@RestControllerAdvice`. Controllers MUST NOT contain try-catch blocks.
- The project MUST add Swagger annotations (OpenAPI documentation) to all API controllers and DTOs.
- The project MUST maintain a clean package structure (e.g., package-by-feature).

## VI. Security & Configuration Constraints
- Hardcoded secrets, API keys, or private keys MUST NOT be committed to the repository. Use environment configuration or secure vaults.

## VII. Development Quality Gates
- All PRs must satisfy the following checks:
  - 100% compilation under Java 25.
  - Service layer unit tests coverage using JUnit and Mockito.
  - Verification of database migrations via Liquibase changelogs.

## Governance
- The constitution is the ultimate source of truth for repository structure and quality gates.
- Amendments to these principles require a minor or major version bump, documented migration plans, and validation of all templates.
- Enforcement is performed at pull request code review times.

**Version**: 1.0.1 | **Ratified**: 2026-06-12 | **Last Amended**: 2026-06-12
