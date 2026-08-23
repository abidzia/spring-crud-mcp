# CLAUDE.md — product-mcp

Context for Claude Code when working on this repository.

## What this project is

A Spring Boot CRUD service for a product catalog that exposes the **same operations
over two surfaces**:

- a **REST API** under `/api/products`
- an **MCP server** under `/mcp` (Streamable HTTP transport)

Both surfaces delegate to a single `ProductService`, so REST and MCP behaviour never
drift apart. Add new capabilities to the service first, then expose them from the
controller and/or the tool class.

## Stack (do not silently change these versions)

- **Java 21**
- **Spring Boot 4.1.0** (Boot 3.5 went EOL 2026-06-30; 4.x is the supported line)
- **Spring AI 2.0.0** via `spring-ai-bom` (the Boot 4 / Spring Framework 7 line)
- **JPA (Hibernate)** + **H2** in-memory database
- **MCP:** `spring-ai-starter-mcp-server-webmvc`

Notes that follow from these versions:
- Spring Boot 4 uses **Jackson 3** (`tools.jackson`), not Jackson 2. Do not import
  `com.fasterxml.jackson.*` in new code.
- MCP server annotations live in **`org.springframework.ai.mcp.annotation`**
  in Spring AI 2.0 (`spring-ai-mcp-annotations`). (In the 1.1 line they were in the
  `org.springaicommunity.mcp` incubator — do not use that package here.)
- Spring AI 2.0 defaults to **Streamable HTTP**; SSE is deprecated. Keep
  `spring.ai.mcp.server.protocol: STREAMABLE`.

## Layout

Base package `com.example.productmcp`. Everything must stay under it so component
scanning (including the `@McpTool` annotation scanner) finds it.

```
model/       Product              JPA entity
repository/  ProductRepository    Spring Data JPA
service/     ProductService       business logic, @Transactional, single source of truth
controller/  ProductController    REST surface (/api/products)
tool/        ProductTools         MCP surface (@McpTool methods)
exception/   ProductNotFoundException, GlobalExceptionHandler
```

## Conventions

- **Constructor injection only** (no field `@Autowired`).
- REST: proper HTTP semantics — `201 Created` + `Location` on POST, `204 No Content`
  on DELETE, `ProblemDetail` (RFC 9457) for errors.
- Service `update(...)` is **full-replace / PUT semantics** (all fields required).
  If partial updates are needed, add a separate `patch` method rather than changing this.
- MCP tools: name in `snake_case`; every `@McpToolParam` gets a clear `description`
  (the model reads these). Prefer flat params and concrete return types for clean
  JSON-schema generation.
- Error contract: a thrown `RuntimeException` (e.g. `ProductNotFoundException`) is
  converted to an MCP error result and conveyed to the model. If this server is ever
  exposed to untrusted clients, add an exception handler to sanitize messages before
  they reach the model.

## Run

```
mvn spring-boot:run
```

- REST:        http://localhost:8080/api/products
- MCP:         http://localhost:8080/mcp
- H2 console:  http://localhost:8080/h2-console  (JDBC URL `jdbc:h2:mem:productdb`, user `sa`, empty password)

The database is in-memory, so data resets on restart.

## Not done yet (open work)

- Seed data (`data.sql` or a `CommandLineRunner`) — the catalog starts empty.
- Bean validation + request DTOs (would add `spring-boot-starter-validation` and
  decouple the API from the JPA entity).
- Tests (`@WebMvcTest` for the controller, a slice test for the MCP tools).

If you add validation, do it at the controller/tool boundary, not on the entity.
