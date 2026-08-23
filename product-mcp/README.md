# product-mcp

Spring Boot CRUD service for a product catalog, exposed over **both** a REST API and an
**MCP server**. Both surfaces share a single `ProductService`.

## Stack

- Java 21, Spring Boot 4.1.0
- Spring AI 2.0.0 (`spring-ai-starter-mcp-server-webmvc`, Streamable HTTP)
- JPA/Hibernate + H2 (in-memory)

## Run

```bash
mvn spring-boot:run
```

| Surface     | URL                                  |
|-------------|--------------------------------------|
| REST API    | http://localhost:8080/api/products   |
| MCP server  | http://localhost:8080/mcp            |
| H2 console  | http://localhost:8080/h2-console     |

H2 console login: JDBC URL `jdbc:h2:mem:productdb`, user `sa`, empty password.

## REST endpoints

| Method | Path                       | Description              |
|--------|----------------------------|--------------------------|
| GET    | /api/products              | List all (or `?name=`)   |
| GET    | /api/products/{id}         | Fetch one                |
| POST   | /api/products              | Create (201 + Location)  |
| PUT    | /api/products/{id}         | Full update              |
| DELETE | /api/products/{id}         | Delete (204)             |

## MCP tools

`list_products`, `get_product`, `search_products`, `create_product`,
`update_product`, `delete_product`.

Point an MCP client (e.g. MCP Inspector, or an MCP-capable IDE/agent) at
`http://localhost:8080/mcp` to discover and call them.

See `CLAUDE.md` for architecture notes and conventions.
