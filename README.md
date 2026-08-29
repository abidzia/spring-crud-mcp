# spring-crud-mcp

A small, end-to-end demo of the **Model Context Protocol (MCP)** on the Spring stack.
It shows the standard MCP shape: several **MCP servers** that each expose a domain's
operations as tools, and a single **LLM-powered agent** that discovers those tools and
calls them on the user's behalf.

- Two independent **MCP servers** (`product-mcp`, `order-mcp`), each a normal Spring Boot
  service that exposes the *same* operations over **both** a REST API and an MCP endpoint.
- One **agent client** (`agent-client`) that connects to both servers, hands their tools to
  an LLM, and runs an interactive console.

## Architecture

```
                 ┌──────────────────────────────┐
   you (stdin) ─▶│  agent-client (CatalogAgent)  │──── HTTPS ───▶  LLM provider
                 │  the ONLY LLM caller          │◀──────────────  (your model)
                 └──────┬────────────────┬───────┘
                        │ MCP            │ MCP        JSON-RPC over Streamable HTTP —
                        │ (no LLM)       │ (no LLM)   plain tool calls, no AI
                        ▼                ▼
                  product-mcp         order-mcp       deterministic Java + H2;
                  :8080               :8081           no model, no API key
```

**Key idea:** the intelligence is centralized in the agent. The MCP servers are dumb,
deterministic tool providers — they have no LLM dependency and no API key. One agent
holds the union of every connected server's tools and lets the model choose which to call,
so there is no need for a separate agent per domain.

## Modules

| Module | Port | What it is |
|--------|------|------------|
| [`product-mcp`](product-mcp) | 8080 | MCP server + REST API for a product catalog |
| [`order-mcp`](order-mcp) | 8081 | MCP server + REST API for customer orders |
| [`agent-client`](agent-client) | — (console) | LLM agent that connects to both servers and drives their tools |

### `product-mcp`
A Spring Boot CRUD service for a product catalog.

- Package `com.example.productmcp`. REST under `/api/products`, MCP under `/mcp`.
- Both surfaces delegate to a single `ProductService` (single source of truth), backed by
  JPA + an in-memory H2 database (`productdb`). Seeded with 12 products on startup.
- **MCP tools:** `list_products`, `get_product`, `search_products`, `create_product`,
  `update_product`, `delete_product`.

### `order-mcp`
A second MCP microservice for customer orders, mirroring the same dual REST + MCP shape.

- Package `com.example.ordermcp`. REST under `/api/orders`, MCP under `/mcp`.
- Delegates to `OrderService`, backed by its own H2 database (`ordersdb`). Seeded with 6
  orders on startup.
- **MCP tools:** `list_orders`, `get_order`, `search_orders_by_customer`, `create_order`,
  `update_order_status`, `delete_order`.
- Tool names are domain-prefixed so they never collide with `product-mcp`'s tools when both
  servers are connected to the same agent.

### `agent-client`
An interactive console agent (`CatalogAgent`) — the only component that talks to an LLM.

- Uses the Spring AI **MCP client** to connect to both servers (one entry per server under
  `spring.ai.mcp.client.streamable-http.connections`) and merges their tools into one list.
- Uses an **LLM over an OpenAI-compatible API** as the model, configured from environment
  variables (see the connection settings below). A custom `@Configuration` builds the chat
  model, and the auto-configured models are disabled (`spring.ai.model.*: none`) so it is the
  only one; the app runs headless (`web-application-type: none`).
- Ships a `--selftest` flag that calls an MCP tool directly (no LLM, no API key) to verify
  the client → server → DB path.

## Tech stack

- **Java 21**, **Spring Boot 4.1.0**
- **Spring AI 2.0.0** (`spring-ai-bom`) — MCP server (`spring-ai-starter-mcp-server-webmvc`),
  MCP client (`spring-ai-starter-mcp-client`), and OpenAI-compatible model
  (`spring-ai-starter-model-openai`)
- **MCP transport:** Streamable HTTP
- **Persistence:** Spring Data JPA + in-memory H2 (data resets on restart)

## Build

The root `pom.xml` is a Maven **aggregator** (reactor) — each module keeps its own
`spring-boot-starter-parent`, so this builds all three in one pass:

```bash
mvn compile          # build everything
mvn -pl order-mcp compile   # build just one module
```

## Run

Each server is a long-lived process, so run the three in separate terminals.

```bash
# terminal 1 — product server (8080)
cd product-mcp && mvn spring-boot:run
```
```bash
# terminal 2 — order server (8081)
cd order-mcp && mvn spring-boot:run
```
```bash
# terminal 3 — the agent (set your LLM provider's credentials first — see Configuration)
cd agent-client && mvn spring-boot:run
```

Then, at the `you >` prompt, try:

- `what products do we have?` → calls `list_products`
- `which orders did Alice place?` → calls `search_orders_by_customer`
- `add a product: Standing Fan, 40W desk fan, price 29.99, qty 12` → `create_product`
- `create an order: Bob, USB-C Hub, qty 2` → `create_order`

## Run with Docker (recommended)

Compose builds each module into a slim JRE image and runs the two servers on a shared
network. The agent is an interactive console app, so it is gated behind an `agent` profile
and run on demand.

```bash
# build images and start both servers (product-mcp :8080, order-mcp :8081)
docker compose up --build -d

# run the interactive agent (set your LLM provider's env vars first — see Configuration)
docker compose --profile agent run --rm agent-client

# verify the client -> server path without an LLM (no key needed)
docker compose --profile agent run --rm agent-client --selftest

# stop everything
docker compose down
```

Inside the network the agent reaches the servers by service name (`http://product-mcp:8080`,
`http://order-mcp:8081`), injected via `PRODUCT_MCP_URL` / `ORDER_MCP_URL`. Nothing sensitive
is baked into the images — LLM credentials are passed through from your host environment.

Builds use a BuildKit **`.m2` cache mount**, so Maven dependencies are downloaded once per
machine and reused across rebuilds — a code-only change rebuilds in a few seconds.

## Configuration (agent-client)

The agent talks to an LLM over an OpenAI-compatible API. Before running it, provide your
provider's connection details as environment variables. The **exact variable names and any
defaults** are defined in
[`agent-client/src/main/resources/application.yml`](agent-client/src/main/resources/application.yml)
— set them to your own values (placeholders shown here):

| Setting | Placeholder | Required? | Purpose |
|---------|-------------|-----------|---------|
| API key | `<your-api-key>` | yes | authenticates to the LLM provider |
| Endpoint | `<your-llm-endpoint>` | yes | provider base URL (host only, no path) |
| Model / deployment | `<your-model>` | no (has default) | which model the agent uses |
| API version | `<your-api-version>` | no (has default) | required by some providers |

The API key and endpoint have **no defaults** — the agent won't start until both are set.

MCP server ports the agent dials (real env vars, optional):

| Variable | Default | Purpose |
|----------|---------|---------|
| `PRODUCT_MCP_PORT` | `8080` | port for product-mcp |
| `ORDER_MCP_PORT` | `8081` | port for order-mcp |

## Verify without an LLM

The servers and the MCP wiring can be checked with no API key and no cost:

```bash
# full client -> MCP -> server -> DB path (calls a tool directly, no model)
cd agent-client && mvn spring-boot:run -Dspring-boot.run.arguments=--selftest

# or hit the REST surfaces directly
curl http://localhost:8080/api/products
curl "http://localhost:8081/api/orders?customer=Alice"
```

Each server also exposes an H2 console at `/h2-console`
(JDBC `jdbc:h2:mem:productdb` / `jdbc:h2:mem:ordersdb`, user `sa`, empty password).

## Extending

- **Add another domain:** create a new MCP server module the same way as `order-mcp`
  (domain-prefixed tool names), then add one entry under the agent's `connections` map.
  No agent code changes — the new tools flow into the same `CatalogAgent` automatically.
- **Swap the LLM provider:** change only `agent-client`; the servers never change.
- **When to add a second agent:** not per server — only when tool count, per-domain
  policies, or per-domain models grow enough to warrant a router in front of specialists.
