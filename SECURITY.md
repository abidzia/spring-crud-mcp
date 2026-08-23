# Security Plan — spring-crud-mcp

Enterprise security roadmap for this workspace: two MCP servers (`product-mcp`,
`order-mcp`) exposing REST + MCP, and an LLM `agent-client`. This plan treats the
**LLM/MCP layer as a first-class threat surface**, which is what distinguishes it from
generic Spring application security.

> Status: **planning only** — no controls implemented yet. Tracked on the
> `feature/security` branch.

## 1. Current posture (baseline gaps)

| Area | Today | Risk |
|---|---|---|
| AuthN / AuthZ | None — REST + MCP endpoints are anonymous | Anyone on the network can read/mutate the catalog and orders |
| Transport | Plain HTTP on 8080 / 8081 | Eavesdropping, MITM |
| H2 console | Enabled, no auth | Direct DB access, extra attack surface |
| Input validation | None; MCP/REST bind straight to JPA entities | Bad data, injection-shaped inputs |
| Error handling | Raw `RuntimeException` → model/client | Internal detail leakage |
| Secrets | Env vars (good — nothing committed) | No rotation, no vault; a key still exists |
| MCP tools | All tools exposed to any caller, incl. destructive `delete_*` | Confused-deputy / prompt-injection → unintended writes |
| Supply chain / SDLC | Manual only | No SCA, SAST, secret scanning, branch protection |

## 2. Threat model

Anchor on established frameworks:

- **OWASP Top 10** (web) and **OWASP API Security Top 10**
- **OWASP LLM Top 10** — critical here: LLM01 Prompt Injection, LLM02 Insecure Output
  Handling, LLM06 Excessive Agency, LLM08 Excessive tool permissions
- **OWASP ASVS** as the verification checklist
- **NIST 800-53 / SOC 2 / ISO 27001** for control mapping
- **STRIDE** pass per module and per trust boundary:
  `user → agent`, `agent → LLM`, `agent → MCP server`, `server → DB`

**Guiding principle:** the LLM decides *which* tool to call, but **authorization must be
enforced server-side in the service layer — never trust the model's decision.** The agent
is a semi-untrusted intermediary.

## 3. Security domains & target controls

### A. Identity & Access Management
- OAuth2 / OIDC via an enterprise IdP (Entra ID / Okta). REST + MCP become **OAuth2
  Resource Servers** (validate JWT/opaque tokens). Evaluate `spring-ai-community/mcp-security`
  for the MCP handshake.
- **Service-to-service:** `agent-client → MCP servers` authenticate via client-credentials
  tokens or mTLS. No anonymous access.
- **RBAC / scopes:** `catalog:read` / `catalog:write`, `orders:read` / `orders:write`;
  method-level `@PreAuthorize` on the service layer (the single source of truth).

### B. MCP / LLM-specific (the differentiator)
- **Per-client tool allow-lists** — a caller only sees/invokes the tools its scope permits.
  Destructive tools (`delete_*`, `update_*`) gated behind write scopes.
- **Human-in-the-loop approval** (or a policy engine) for write/destructive tools, so a
  prompt-injected instruction cannot silently delete or alter data.
- **Output / error sanitization** — a `GlobalExceptionHandler` on the MCP surface returns
  clean, non-leaky results (already flagged in `product-mcp/CLAUDE.md`).
- **LLM guardrails** — input/output filtering, and **token / cost budgets** in the agent
  (it calls a paid model).
- **Full audit of tool calls** — principal, tool, redacted params, outcome.

### C. Transport & network
- TLS on all endpoints; **mTLS** for `agent ↔ server` in a zero-trust posture. HSTS,
  strong cipher suites.
- Front everything with an **API gateway** (Spring Cloud Gateway / APIM): centralized
  authN, WAF, request-size limits, schema validation.

### D. Input validation & data integrity
- Bean Validation + **request DTOs decoupled from JPA entities** (already listed as open
  work), enforced at **both** REST and MCP boundaries. Business rules (non-negative
  price/quantity, status enums).

### E. Secrets & key management
- Move to **Azure Key Vault / HashiCorp Vault** with rotation.
- **Preferred: passwordless auth to Azure OpenAI via Managed / Workload Identity** — the
  OpenAI Java SDK supports `workloadIdentity`, which eliminates the API key entirely.

### F. Data protection & privacy
- Replace in-memory H2 with a **managed encrypted database** (e.g. Postgres: TLS in
  transit, encryption at rest). Orders hold **PII (customer names)** → data classification,
  log masking, retention policy, GDPR/CCPA posture.

### G. Rate limiting & resiliency
- Per-client / per-tool rate limits and quotas; Resilience4j (timeouts, circuit breakers,
  bulkheads) to contain abuse and DoS.

### H. Application hardening
- Disable H2 console outside `dev`; secure Actuator (expose only `health` / `info`, auth
  the rest); security headers (CSP, X-Frame-Options); strict CORS; no stack traces to clients.

### I. Supply chain & SDLC
- **CI gates:** SCA (Dependabot / OWASP Dependency-Check / Snyk), SAST (CodeQL / SonarQube),
  **secret scanning (gitleaks) as pre-commit + CI**, DAST.
- **SBOM** (CycloneDX), signed commits, branch protection on `main`, least-privilege CI tokens.

### J. Observability & detection
- OpenTelemetry tracing + metrics; centralized logging with correlation IDs; SIEM feed;
  **anomaly detection on tool-call patterns** (e.g. a burst of `delete_*`).

## 4. Phased roadmap (mapped to modules)

| Phase | Focus | Touches |
|---|---|---|
| **0 — Hygiene / quick wins** | TLS; disable H2 console + secure Actuator; error sanitization; input validation + DTOs; security headers; CI secret-scan + SCA; branch protection | both servers, CI |
| **1 — AuthN / AuthZ** | OAuth2 Resource Server on REST + MCP; scopes/roles; `@PreAuthorize`; service-to-service auth (agent → servers) | both servers, agent-client |
| **2 — Zero-trust & secrets** | mTLS; API gateway + rate limiting; Key Vault + **passwordless Azure OpenAI (managed identity)** | agent-client, infra |
| **3 — LLM / MCP controls** | per-scope tool allow-lists; human-in-the-loop for writes; guardrails; tool-call audit; token/cost budgets | both servers, agent-client |
| **4 — Data & compliance** | managed encrypted DB; PII handling; audit logging → SIEM; SBOM; pen test; control mapping (SOC 2 / ISO) | infra, both servers |

## 5. Phase 0 — definition of done (safe, high-value, low-risk)

1. HTTPS on both servers; H2 console disabled outside a `dev` profile; Actuator locked down.
2. `GlobalExceptionHandler` sanitizes MCP + REST errors (no stack traces / internal messages).
3. DTOs + Bean Validation at the controller and tool boundaries.
4. GitHub: branch protection on `main`; Dependabot + CodeQL + gitleaks in Actions.

## 6. Compliance mapping (targets)

- **OWASP ASVS** — verification baseline per release.
- **OWASP LLM Top 10** — explicit coverage of prompt injection, insecure output handling,
  excessive agency, and tool-permission scope.
- **SOC 2 / ISO 27001 / NIST 800-53** — control mapping once Phases 1–4 land.

---

*This is a living document. Update it as controls move from planned → in progress → done.*
