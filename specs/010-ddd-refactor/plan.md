# Implementation Plan: Pure DDD Boundary Refactor

**Status**: Implemented (2026-09-24)
**Scope**: `backend/` only  
**Architectural target**: Package-level ports-and-adapters in the existing Maven modular monolith

## Decision

The backend is **not yet Pure DDD end-to-end**. Its Financial Domain Engine is framework-free and
identity-agnostic, but the application layer is coupled directly to Spring and the JPA persistence
implementation. Refactor toward the dependency direction specified by the constitution:

```text
REST / Security adapter -> application use case -> domain model / ports
                                      ^
                                      |
                     infrastructure adapters implement ports
```

This is a package-level refactor. Do not split the project into Maven modules: the constitution
explicitly defers that change. Preserve the public REST contract and PostgreSQL schema in this
initiative; it changes ownership of code dependencies, not product behavior.

## Audit evidence (2026-09-24)

### Already aligned

| Concern | Evidence | Assessment |
|---|---|---|
| Financial domain purity | `com.financialgps.domain` imports no Spring, JPA, HTTP, persistence, identity, clock, randomness, or external service types | Aligned |
| Deterministic engine boundary | `FinancialEngine.calculate(input, assumptions, asOf, policy)` receives explicit inputs and date | Aligned |
| API controllers | Controllers delegate to application services; no financial formula was found in controllers | Mostly aligned |
| Automated guard | `DomainBoundaryGuardTest` rejects framework/platform references under `domain` | Aligned, but narrow |
| Infrastructure direction | Persistence package does not import application/domain classes | Aligned |

### Gaps preventing a Pure DDD assessment

| Priority | Finding | Evidence | Why it matters |
|---|---|---|---|
| P0 | Application services depend on JPA entities and Spring Data repositories | `ProfileService` imports `ProfileEntity`, `IncomeEntity`, `ExpenseEntity` and their repositories; the five account use cases do likewise | Domain-facing use cases know database representation and cannot be tested or reused without persistence-shaped collaborators |
| P0 | Application layer is a Spring component layer | 11 application classes use `@Service`/`@Component` and/or `@Transactional` | Framework configuration and transaction mechanics leak into the use-case boundary |
| P1 | Persistence-to-domain mapping lives in the application service | `ProfileService.assemble` reads persistence entities, constructs `Income`/`Expense`, calculates, and builds response-shaped maps | A use case owns both adapter mapping and financial input assembly, obscuring the domain boundary |
| P1 | API layer supplies technical time and primitive parsing | Profile controllers construct `BigDecimal` and call `LocalDate.now()` | HTTP adapter owns a domain-relevant date; the application use case cannot receive a deterministic explicit clock/date dependency |
| P1 | Identity/application types are mixed across contexts | `OwnerId` lives in `application.account`, while profile use cases import it; password policy and hasher reference `platform.security.AuthProperties` | Identity & Access concepts have no explicit context-facing port/model boundary |
| P2 | Guard verifies only domain purity | Current test cannot fail when application imports infrastructure or when controllers call repositories | Regressions against the intended dependency direction remain possible |

## Target package structure

```text
com.financialgps
  domain
    finance | engine | model | policy                  # remains pure
  application
    account
      port.in | port.out | usecase | model
    profile
      port.in | port.out | usecase | model
  infrastructure
    persistence.account | persistence.profile          # JPA entities/repos + port adapters
    security                                           # BCrypt, Spring Security integration
    configuration                                      # Spring wiring and transaction proxies
  api
    auth | account | profile                            # DTO/HTTP mapping only
  platform.security                                    # filter-chain / authenticated-principal adapter
```

`port.in` defines commands, queries, results, and use-case interfaces used by API adapters.
`port.out` defines persistence, password-hashing, export, time, and transaction-facing
collaborators required by those use cases. Both port packages must be Java/Spring-free. JPA
entities and Spring Data repositories remain implementation details of `infrastructure`.

## Refactor plan

### Phase 0 — Lock observable behavior (prerequisite)

1. Provide a repeatable Docker test runner before refactoring: either commit a Maven wrapper or
   add a dedicated `backend-test` Compose service with a supported Testcontainers Docker endpoint.
   The current `backend` service can run Maven but does not expose a Docker daemon to its nested
   Testcontainers PostgreSQL process.
2. Run the existing backend test suite in that runner and record the baseline. Current audit run:
   71 non-container tests passed; 54 integration tests could not initialize Testcontainers because
   no valid Docker environment was available inside the Maven container (zero assertion failures).
3. Add contract-focused tests for all existing account/profile endpoints: status, body, error
   code, ownership isolation, date behavior, and export ordering.
4. Add unit characterization tests for profile-to-engine mapping, including currency, inactive
   lines, effective dates, and missing-profile behavior.
5. Treat REST paths, JSON fields, Flyway migrations, and owner-scoped query semantics as frozen.

**Exit gate**: baseline passes and every migration/API change is explicitly rejected by tests.

### Phase 1 — Establish ports and application models

1. Create input ports per use case instead of one infrastructure-aware service:
   - profile: `GetProfile`, `PutProfile`, `AddIncome`, `UpdateIncome`, `DeleteIncome`,
     `AddExpense`, `UpdateExpense`, `DeleteExpense`;
   - identity: `RegisterOwner`, `AuthenticateOwner`, `GetAccount`, `ExportOwnerData`,
     `DeleteOwner`.
2. Move commands, results, and `OwnerId` to context-owned application models. Keep `OwnerId`
   explicitly in Identity & Access; profile ports may accept it as an external actor identifier but
   must not depend on Spring Security types.
3. Define output ports in each bounded context, returning application/domain records rather than
   `*Entity` or `JpaRepository` types. Examples: `ProfileStore`, `IncomeStore`, `ExpenseStore`,
   `AccountStore`, `PasswordHashing`, `OwnerDataSection`, and `BusinessDate`.
4. Move the profile persistence-to-domain conversion into a dedicated application assembler that
   consumes port records. It may call the pure `FinancialEngine`; no port or JPA type may enter
   `domain`.

**Exit gate**: no source under `application` imports `infrastructure`, `jakarta.persistence`,
or Spring Data types; use-case unit tests run with fake output ports.

### Phase 2 — Implement infrastructure adapters and wiring

1. Keep existing JPA entities and Spring Data repositories in `infrastructure.persistence`, but
   make them private collaborators of adapter classes such as `JpaProfileStore` and
   `JpaAccountStore`.
2. Map JPA entities to/from immutable application persistence records only inside these adapters.
   Preserve current owner predicates and database-level uniqueness/cascade constraints.
3. Move `BCryptPasswordHasher` to `infrastructure.security`; expose it through the
   `PasswordHashing` output port. Keep password rules as a context policy configured through a
   small application configuration record, bound from Spring in infrastructure/configuration.
4. Move Spring stereotypes and transaction demarcation out of use-case classes. Register use cases
   through explicit configuration. Apply transactions via a Spring adapter/decorator at the input
   port boundary; maintain the current read-only vs write behavior.
5. Convert the profile/account export participants to output-port adapters so `DataExport` does
   not query JPA repositories directly.

**Exit gate**: application compiles with no Spring, JPA, repository, entity, or platform-security
imports. Infrastructure is the only layer importing Spring Data/JPA/BCrypt configuration.

### Phase 3 — Thin adapters and explicit technical dependencies

1. Change REST controllers to map DTO strings to input-port commands only. Do not create domain
   objects, access repositories, or calculate dates in controllers.
2. Supply `LocalDate` through a `BusinessDate`/clock output port implemented in infrastructure;
   tests provide a fixed date. The pure financial engine continues to receive an explicit `asOf`.
3. Keep `CurrentOwnerProvider` as a platform/security adapter; controllers resolve the actor and
   pass only the application `OwnerId` value into input ports.
4. Map application results to API DTOs in the API package; remove response-shaped
   `Map<String, String>` construction from use cases where a typed result is feasible without a
   REST contract change.

**Exit gate**: controllers contain only HTTP binding/validation, authenticated-actor retrieval,
input-port invocation, and response mapping.

### Phase 4 — Enforce architecture and remove transitional code

1. Extend architecture tests with package rules:
   - `domain` imports none of API/application/infrastructure/platform/framework types;
   - `application` imports only `domain`, its own context models/ports, and JDK;
   - `api` never imports infrastructure persistence;
   - infrastructure persistence is the only location containing JPA entities/repositories;
   - financial formulas remain in domain, not API/application/infrastructure.
2. Add tests that verify every output port has at least one infrastructure adapter and that Spring
   wiring supplies the production adapters.
3. Delete direct repository/entity constructor paths and any temporary compatibility façade after
   all callers use input ports.
4. Update `README.md` and the relevant feature plans with the final package map and test commands.

**Exit gate**: architecture tests, unit tests, integration tests, and endpoint contract tests pass;
no application-to-infrastructure compile-time dependency remains.

## Delivery order and risk controls

1. Deliver one vertical slice first: profile read + write, including its ports, JPA adapter,
   configuration, and contract tests.
2. Migrate income/expense CRUD next, then account registration/authentication, then export/delete.
3. Do not combine this refactor with new financial features, database migrations, authentication
   changes, or REST API redesign.
4. Keep adapters backward-compatible until each slice is green; remove transitional paths only in
   Phase 4.

## Acceptance criteria

- The Financial Domain Engine remains framework-free and identity-agnostic.
- Application code has no direct dependency on JPA entities, Spring Data repositories, Spring
  annotations, `AuthProperties`, or `CurrentOwnerProvider`.
- All existing API and ownership behavior remains unchanged.
- `LocalDate.now()` is absent from API/application financial use cases; business date is explicit
  and controllable in tests.
- Automated dependency rules fail on prohibited imports.
- Existing unit, integration, security, export, and profile journey tests pass in a Maven-capable
  environment.

## Non-goals

- Splitting into separate Maven/Gradle modules.
- Changing financial formulas, domain terminology, or the engine contract.
- Introducing a generic repository abstraction, event sourcing, CQRS infrastructure, or aggregate
  hierarchy without a demonstrated business invariant.
- Changing database schema or public REST contracts solely for architectural aesthetics.

## Delivery record (2026-09-24)

Baseline before the refactor: `125 tests, 0 failures` (`mvn -B test`, JDK 21, Docker/Testcontainers).
After the refactor: `162 tests, 0 failures` — every pre-existing API, ownership, security, export and
schema test still passes unchanged.

### Phase 0

- Committed a Maven wrapper (`backend/mvnw`, Maven 3.9.9 — the container image's version) so the
  suite is reproducible without a machine Maven install.
- Fixed the `backend-test` Compose service: it now mounts the host Docker socket and sets
  `TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal`, so Testcontainers can start its own
  PostgreSQL inside the container. Run with `docker compose --profile test run --rm backend-test`.
- Recorded the baseline and kept REST paths, JSON fields, Flyway migrations and owner-scoped query
  semantics frozen; every existing contract test still asserts them.

### Phase 1

- `application/account/{model,port.in,port.out,usecase}` and
  `application/profile/{model,port.in,port.out,usecase}` created.
- Input ports added per use case: `RegisterOwner`, `AuthenticateOwner`, `GetAccount`,
  `ExportOwnerData`, `DeleteOwner`, `GetProfile`, `PutProfile`, `AddIncome`, `UpdateIncome`,
  `DeleteIncome`, `AddExpense`, `UpdateExpense`, `DeleteExpense`.
- Output ports added: `AccountStore`, `PasswordHashing`, `OwnerDataSection` (Identity & Access);
  `ProfileStore`, `IncomeStore`, `ExpenseStore`, `BusinessDate` (Financial Profile). Ports exchange
  immutable application records (`AccountRecord`, `ProfileRecord`, `IncomeRecord`, `ExpenseRecord`)
  and never an entity, repository or HTTP DTO.
- `OwnerId`, `AccountView`, `ExportBundle` and `ProfileModels` moved into context-owned `model`
  packages; `PasswordRules` + pure `PasswordPolicy` replace the `AuthProperties.Password` coupling;
  `AuthRequiredException` moved from `platform.security` to `application.account`.
- `ProfileAssembler` owns the persistence-record → domain → engine → typed-view conversion.
- **Exit gate met**: `application` imports no `infrastructure`, `jakarta.persistence`, Spring Data or
  `org.springframework` type (enforced by `ArchitectureGuardTest`), and all use-case unit tests run
  against fake output ports (`FakeAccountStore`, `FakePasswordHashing`, port mocks, a lambda
  `BusinessDate`).

### Phase 2

- `JpaAccountStore`, `JpaProfileStore`, `JpaIncomeStore`, `JpaExpenseStore` keep the existing JPA
  entities and Spring Data repositories as private collaborators, mapping to/from the port records.
  Owner predicates, the case-insensitive unique email index and the FK cascade are unchanged.
- `BCryptPasswordHasher` moved to `infrastructure.security` behind `PasswordHashing`.
- `SystemBusinessDate` (`infrastructure.time`) is the only production reader of the wall clock.
- Spring stereotypes and `@Transactional` removed from use cases. `UseCaseTransactions` applies the
  boundary as a JDK proxy over the input port; read/write behaviour is unchanged
  (`register`/`delete`/profile mutations read-write, `authenticate`/`me`/`getProfile` read-only).
- Export participants became output-port adapters (`ProfileExportSection`, `IncomeExportSection`,
  `ExpenseExportSection`) so the export use case never touches a repository.
- **Exit gate met**: application compiles with no Spring, JPA, repository, entity or
  platform-security import.

### Phase 3

- Controllers now only bind DTOs, resolve the authenticated actor, invoke an input port and map the
  result. No `BigDecimal` parsing, no `LocalDate.now()`, no repository access.
- `ProfileView` was typed (`MoneyView`, `IncomeLineView`, `ExpenseLineView`) and the export bundle
  became `ExportBundle`; `api.account.ExportBundleJson` renders the documented JSON document, keeping
  the byte-identical, id-sorted export contract.
- `ProfileBusinessDateTest` proves end-to-end that the evaluated date is the server business date and
  that a client-supplied `asOf`/`effectiveFrom` is not bound.

### Phase 4

- `ArchitectureGuardTest` (new, `src/test/java/com/financialgps/architecture`) fails when:
  application imports a framework/infrastructure/api/platform type; a port references a framework
  type; `api` reaches persistence; a JPA entity or Spring Data repository appears outside
  `infrastructure.persistence`; a financial rule (`FinancialEngine`, `CashFlowCalculator`,
  `FinancialPolicy`, `Money.of`) appears outside `domain` and the sanctioned `ProfileAssembler`;
  `api`/`application` reads `LocalDate.now()` or `java.time.Clock`; an input port is not wired and
  transaction-demarcated; an output port has no infrastructure adapter.
- `UseCaseTransactionsTest` replaces the old annotation-reflection assertion with behaviour:
  read-only vs read-write declaration, commit on success, rollback and propagation on runtime
  failure, and that the decorator exposes the port interface rather than the use-case class.
- Transitional code removed: no compatibility façade, and no direct repository/entity dependency
  from application or API code.
- README updated with the final package map and test commands.

### Known, deliberate deviations

- `OwnerId` remains owned by `application.account` and is accepted by profile ports as an external
  actor identifier (plan Phase 1 step 2); the guard therefore forbids context →
  infrastructure/api/platform/framework imports rather than every cross-context import.
- `ExportOwnerData` is intentionally not transaction-wrapped: it was never transactional and only
  reads, so wrapping it would change behaviour without benefit.
- The test-only ownership fixture (`com.financialgps.testfixture`) still talks to its own JPA
  repository directly; the guards scan production sources (`src/main/java`) only.
- `CashFlowCalculator` keeps its legacy two-argument `LocalDate.now()` convenience overload; no
  API/application/infrastructure code calls a clock outside `SystemBusinessDate`.

