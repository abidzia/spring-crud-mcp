# CLAUDE.md — agent-client

The console **LLM agent** (`CatalogAgent`) — the only component in the workspace that talks
to an LLM. See the root `../CLAUDE.md` section **"agent-client LLM wiring"** for the full,
non-obvious build details; this file is a quick orientation.

## What it does

- On startup, the Spring AI **MCP client** connects to every server under
  `spring.ai.mcp.client.streamable-http.connections` (currently `products` :8080 and
  `orders` :8081) and merges all their tools into one list.
- `CatalogAgent` hands that combined tool list to one `ChatClient` and loops on stdin —
  the model decides which tool to call. **One agent serves all servers; don't add a second
  agent per server.** Add a server = add a `connections` entry (no code change).
- Base package **`com.example.agentclient`**. Runs headless (`web-application-type: none`).

## Must-know before editing

- **LLM = Azure OpenAI via the OpenAI starter.** `AzureOpenAiConfig` builds the chat model
  from **both** a sync `OpenAIClient` and an async `OpenAIClientAsync` (supplying only one
  fails at startup). `spring.ai.model.*: none` disables the auto OpenAI models.
- `pom.xml` adds `com.openai:openai-java-client-okhttp` explicitly and **pins
  `jsonschema-module-jackson` to 5.0.0** — do not remove either (see root CLAUDE.md).
- **Config is env-driven; nothing sensitive is committed.** `AZURE_OPENAI_KEY` and
  `AZURE_OPENAI_ENDPOINT` (host only, no path) are **required, no defaults**. Never hardcode
  a real endpoint back into `application.yml`.
- `--selftest` calls the `list_products` tool directly (no LLM, no key) to verify the
  client → MCP → server → DB path:
  `mvn spring-boot:run -Dspring-boot.run.arguments=--selftest`

## Layout

```
AgentClientApplication   Spring Boot entry point
CatalogAgent             CommandLineRunner: discovers tools, runs the console loop, --selftest
AzureOpenAiConfig        builds the Azure-backed OpenAiChatModel (sync + async clients)
```
