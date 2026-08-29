# Phase 0 — Application Hardening Plan

The app-level slice of Phase 0 from [`SECURITY.md`](SECURITY.md): close the "wide open"
gaps without new architecture — validate input, stop internal details leaking, and shut
down unnecessary attack surface. Authentication/authorization is **Phase 1**.

> Status: **done** — delivered in three commits on `security/phase-0-hardening` (PR #10),
> merged to `main`. Scope: `product-mcp` + `order-mcp` only.

## Goal

Every change here is low-risk and mostly config, and is guarded by the CI gates + the test
suite so regressions surface immediately.

## Work items

### 1. Input validation + DTOs — done
- Added `spring-boot-starter-validation` to both servers.
- Introduced request DTOs decoupled from the JPA entities (validate at the boundary, not on
  the entity): `ProductRequest`, `OrderRequest` with Bean Validation constraints
  (`@NotBlank`, `@DecimalMin`, `@PositiveOrZero`, `@Positive`, `@Pattern` for order status).
- Enforced at **both** boundaries: REST via `@Valid`, and the MCP tools via a programmatic
  `Validator` (so bad/negative data from the model is rejected too).

### 2. Error sanitization — done
- `GlobalExceptionHandler` now extends `ResponseEntityExceptionHandler` so framework
  exceptions keep correct statuses.
- Validation errors → **400** with per-field messages; a catch-all → **sanitized 500**
  (no stack traces / internal messages leak to clients or, via the tools, to the model).
- Added the previously-missing handler to **order-mcp**: an unknown order id now returns
  **404** instead of 500.

### 3. Surface lockdown — done
- H2 console **disabled by default** on both servers; the `dev` profile re-enables it
  (`mvn spring-boot:run -Dspring-boot.run.profiles=dev`).
- Verified live: `/h2-console` → 404 by default while REST still serves.

## Tests

Suite grew 21 → **26**: validation-rejection tests at the tool boundary, a REST **400** on
invalid product body, and the order **404**. All green via `mvn verify` and the CI gates.

## Deferred to Phase 1 (deliberately)

- Authentication / authorization (OAuth2 Resource Server, scopes, `@PreAuthorize`).
- Service-to-service auth (agent → servers).
- **HTTPS** and **security headers** — these pair naturally with Spring Security in Phase 1,
  so a standalone implementation now would be thrown away.
- Per-scope MCP tool allow-lists and human-in-the-loop for destructive tools.
