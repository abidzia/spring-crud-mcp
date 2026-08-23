# CLAUDE.md — order-mcp

An MCP **server** for customer orders. Follows the exact same pattern as `product-mcp`
(dual REST + MCP over one service) — see `../product-mcp/CLAUDE.md` and the root
`../CLAUDE.md` for the shared conventions and stack (do not change versions).

## Specifics

- Base package **`com.example.ordermcp`**. Runs on **port 8081**.
- REST under `/api/orders`, MCP under `/mcp`. Both delegate to a single `OrderService`
  (single source of truth), backed by JPA + in-memory H2 (`ordersdb`).
- Seeded with 6 sample orders on startup (`config/OrderSeeder`), with repeating customers
  and varied statuses (NEW / SHIPPED / CANCELLED) for realistic tool testing.

## Gotchas

- `Order` maps to **`@Table(name = "orders")`** — `ORDER` is a reserved SQL keyword, so the
  entity must not use the default table name.
- **Tool names are domain-prefixed** (`list_orders`, `create_order`,
  `search_orders_by_customer`, `update_order_status`, `get_order`, `delete_order`) so they
  never collide with `product-mcp`'s tools when both servers are connected to the agent.

## Layout

```
model/       Order              JPA entity (@Table "orders")
repository/  OrderRepository    Spring Data JPA
service/     OrderService       business logic, @Transactional, single source of truth
controller/  OrderController    REST surface (/api/orders) — minimal, for curl checks
tool/        OrderTools         MCP surface (@McpTool methods, auto-scanned)
config/      OrderSeeder        CommandLineRunner seed data
exception/   OrderNotFoundException
```
