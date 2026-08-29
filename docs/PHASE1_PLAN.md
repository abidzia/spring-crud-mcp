# Phase 1 — Authentication & Authorization Plan

The first real security architecture layer from [`SECURITY.md`](SECURITY.md): put identity
and access control in front of the currently-anonymous REST + MCP surfaces, authenticate the
agent → server calls, and enforce authorization **server-side** so the LLM's tool choice can
never bypass it.

> Status: **planned** (no code yet). Builds on the Phase 0 hardening already on `main`.

## Goal & guiding principle

Today `product-mcp` and `order-mcp` accept any caller on both `/api/**` and `/mcp`. Phase 1
makes every request carry a verified identity with explicit permissions.

**Principle:** the model decides *which* tool to call, but **authorization is enforced in the
service layer** (the single source of truth) — never trusted from the caller or the model.

## Trust boundaries

```
   caller ──▶ agent-client ──▶ (OAuth2 token) ──▶ product-mcp / order-mcp ──▶ service ──▶ DB
              (LLM caller)       Bearer JWT          (Resource Servers)        @PreAuthorize
```

- **agent-client → servers:** service-to-service; the agent must obtain and attach a token.
- **servers:** OAuth2 **Resource Servers** that validate the token and map scopes to authorities.
- **Authorization server (IdP):** enterprise IdP (Entra ID / Okta) in real use; a local IdP
  (Keycloak or Spring Authorization Server) for dev.

## Scopes / roles

| Scope | Grants |
|-------|--------|
| `catalog:read` | list/get/search products |
| `catalog:write` | create/update/delete products |
| `orders:read` | list/get/search orders |
| `orders:write` | create/update/cancel orders |

Least privilege: the agent's service identity gets only the scopes it needs; destructive
tools sit behind the `*:write` scopes.

## Components (Spring)

- **Both servers** add `spring-boot-starter-oauth2-resource-server`:
  - a `SecurityFilterChain` securing `/api/**` and `/mcp` (Bearer JWT), leaving health checks
    (if any) `permitAll`;
  - JWT scope → `SCOPE_*` authority mapping;
  - `@EnableMethodSecurity` with `@PreAuthorize("hasAuthority('SCOPE_catalog:write')")` on the
    **service** methods (covers REST and MCP tools with one rule).
- **agent-client** adds `spring-boot-starter-oauth2-client`:
  - obtains a token via the **client-credentials** grant (service identity);
  - attaches the `Authorization: Bearer` header to the MCP client's HTTP calls (transport
    customization) and to any REST calls.
- **Token type:** start with **JWT** (self-contained, no introspection round-trip); opaque +
  introspection is an alternative if central revocation is required.

## MCP-specific considerations

- MCP defines its own OAuth2-based **authorization** flow (servers advertise auth metadata;
  clients obtain and present tokens). Evaluate **`spring-ai-community/mcp-security`** (Spring
  Security integration for MCP) before hand-rolling header injection.
- Whatever the transport, the **authorization decision lives in the service layer**, so a
  prompt-injected tool call still can't exceed the caller's scopes.
- Tool-level allow-lists and human-in-the-loop for destructive tools are **Phase 3**, not here.

## HTTPS & security headers (carried over from Phase 0)

These were deferred because they pair naturally with Spring Security:

- **TLS:** enable HTTPS on both servers — self-signed keystore via a `tls` profile (keystore
  path/password from env) for dev; a real certificate in prod. No keystore committed.
- **Security headers:** Spring Security adds sensible defaults (HSTS, `X-Content-Type-Options`,
  frame options); configure a Content-Security-Policy and tighten as needed.

## Sub-PRs (each small, independently mergeable)

1. **Resource Server + JWT** on both servers — secure `/api/**` + `/mcp`, scope→authority
   mapping. *Tests:* 401 without token, 403 with wrong scope, 200 with the right scope.
2. **Method authorization** — `@PreAuthorize` on the service methods; verify REST **and** MCP
   paths honor scopes. *Tests:* per-scope allow/deny.
3. **agent-client as OAuth2 client** — client-credentials token + Bearer on MCP/REST calls;
   end-to-end run against the secured servers.
4. **HTTPS + security headers** — Spring Security TLS/headers config, `tls` dev profile.
5. **Local IdP for dev** *(optional)* — Keycloak (or Spring Authorization Server) as a
   `compose.yaml` service under a profile, pre-seeded with the scopes and a client for the agent.

## Testing strategy

- `@WebMvcTest` + `spring-security-test`: `SecurityMockMvcRequestPostProcessors.jwt()` with
  specific `authorities(...)` to assert the 401/403/200 matrix per endpoint and scope.
- Service-layer `@PreAuthorize` tests with `@WithMockUser` / mock authentication.

## Decisions to make before starting

1. **IdP choice** — Entra ID (already in the stack via Azure), Okta, or Keycloak/Spring
   Authorization Server for a self-contained demo?
2. **Agent identity** — pure service identity (client-credentials), or on-behalf-of a real
   user (token pass-through / OBO)?
3. **JWT vs opaque tokens** — self-contained vs introspection/revocation.
4. **MCP auth mechanism** — adopt `spring-ai-community/mcp-security`, or attach the Bearer
   header via a custom MCP client transport customizer?

## Deferred to later phases

- **Phase 2:** mTLS, API gateway + rate limiting, Key Vault / managed identity (passwordless
  Azure OpenAI).
- **Phase 3:** per-scope MCP tool allow-lists, human-in-the-loop approvals, guardrails,
  tool-call audit, token/cost budgets.
