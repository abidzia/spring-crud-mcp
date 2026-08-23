# CLAUDE.md — spring-crud-mcp (workspace)

Context for Claude Code when working anywhere in this multi-module workspace.
Each module also has (or may have) its own `CLAUDE.md` with module-specific detail —
`product-mcp/CLAUDE.md` is the canonical example for the server pattern.

## What this workspace is

A demo of the **Model Context Protocol (MCP)** on the Spring stack: two MCP **servers**,
each exposing a domain's operations as tools, and one **agent client** that discovers those
tools and lets an LLM call them.

```
spring-crud-mcp/            aggregator pom (reactor build only)
├── product-mcp/    :8080   MCP server + REST for products   (pkg com.example.productmcp)
├── order-mcp/      :8081   MCP server + REST for orders      (pkg com.example.ordermcp)
└── agent-client/   console LLM agent (CatalogAgent) — the ONLY LLM caller
```

## Stack (do not silently change these versions)

- **Java 21**, **Spring Boot 4.1.0**
- **Spring AI 2.0.0** via `spring-ai-bom` (the Boot 4 / Spring Framework 7 line)
- **MCP transport:** Streamable HTTP (`spring.ai.mcp.server.protocol: STREAMABLE`)
- **Persistence:** Spring Data JPA + in-memory H2 (data resets on restart)

Version-driven notes:
- Spring Boot 4 uses **Jackson 3** (`tools.jackson`); do not import `com.fasterxml.jackson.*`.
- MCP server annotations are in **`org.springframework.ai.mcp.annotation`** (`@McpTool`,
  `@McpToolParam`). Not the `org.springaicommunity.mcp` incubator package.
- Spring AI 2.0.0's `spring-ai-bom` ships **only** the plain `spring-ai-starter-model-openai`
  chat starter (no `-azure-openai`, no `-openai-sdk` starters — those are later versions).

## Architecture rules

1. **Servers are dumb tool providers.** `product-mcp` and `order-mcp` have **no LLM
   dependency and no API key**. Keep them that way — the intelligence lives in the agent.
2. **The agent is the only LLM caller.** It hands every discovered tool to one `ChatClient`
   and the model routes. There is **one** agent for all servers (`CatalogAgent`) — do **not**
   add a second agent per server. Add a server by adding one entry under
   `spring.ai.mcp.client.streamable-http.connections`; the tools flow in automatically.
3. **Each server keeps a single source of truth.** REST controller and `@McpTool` class both
   delegate to the same `*Service`. Add capabilities to the service first, then expose them.
4. **Domain-prefix tool names** so they never collide across servers when the agent connects
   to both (e.g. `list_products` vs `list_orders`). Names are `snake_case`; every
   `@McpToolParam` needs a clear `description` (the model reads these).
5. `@McpTool`-annotated `@Component`s are **auto-scanned** — no manual `ToolCallbackProvider`
   bean is needed on the server side. Everything must stay under the module's base package.

## agent-client LLM wiring — non-obvious, read before touching

The agent uses the **OpenAI starter** but is wired to **Azure OpenAI**, because Spring AI 2.0
wraps the official OpenAI Java SDK, which has native Azure support. Key facts:

- `AzureOpenAiConfig` builds the chat model from a custom `OpenAIClient` **and**
  `OpenAIClientAsync` (both are required — supplying only one makes `OpenAiChatModel.build()`
  try to build the other from env and fail with *"At least one credential source..."*).
  Azure auth uses `AzureApiKeyCredential` + `azureServiceVersion` (the SDK sets the `api-key`
  header and the `api-version` query param).
- `spring.ai.model.*: none` (chat, embedding, image, moderation, audio) **disables all
  auto-configured OpenAI models** so the custom Azure chat model is the only one. Do not
  re-enable them without a credential, or startup fails.
- `pom.xml` adds `com.openai:openai-java-client-okhttp` explicitly (not in the BOM) and pins
  `com.github.victools:jsonschema-module-jackson` to **5.0.0** in `dependencyManagement` — the
  OpenAI SDK drags in 4.38.0, which lacks `JacksonSchemaModule` and breaks tool JSON-schema
  generation. Keep that pin.
- Runs headless (`web-application-type: none`) as a console `CommandLineRunner`.
- **Config is env-driven; nothing sensitive is committed.** `AZURE_OPENAI_KEY` and
  `AZURE_OPENAI_ENDPOINT` (host only, no path) are **required, no defaults**. `deployment` and
  `api-version` have defaults. Do not hardcode a real endpoint back into `application.yml`.
- `--selftest` calls the `list_products` tool directly (no LLM / no key) to verify the
  client → MCP → server → DB path.

## Build & run

```bash
mvn compile                    # aggregator: builds all three modules
mvn -pl order-mcp compile      # build one module via the reactor
```

Servers are long-lived — run each in its own terminal (a reactor can't run them at once):

```bash
cd product-mcp   && mvn spring-boot:run     # :8080
cd order-mcp     && mvn spring-boot:run     # :8081
# agent needs AZURE_OPENAI_KEY + AZURE_OPENAI_ENDPOINT set:
cd agent-client  && mvn spring-boot:run
```

## Conventions

- **Constructor injection only** (no field `@Autowired`).
- Server REST: proper HTTP semantics; `ProblemDetail` (RFC 9457) for errors.
- Service `update(...)` is **full-replace / PUT semantics**; add a separate `patch` method if
  partial updates are needed rather than changing it.
- A thrown `RuntimeException` from a tool becomes an MCP error result conveyed to the model.
  If a server is ever exposed to untrusted clients, sanitize messages before they reach the model.
