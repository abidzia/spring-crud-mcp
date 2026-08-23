package com.example.agentclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Scanner;

/**
 * Interactive console agent for the whole workspace.
 *
 * <p>On startup it discovers the tools exposed by every connected MCP server
 * (product-mcp and order-mcp) via the auto-configured MCP client, hands the
 * combined tool set to an LLM-backed {@link ChatClient}, and then loops on
 * stdin: whatever you type is sent to the model, which decides on its own which
 * tool to call — {@code list_products}, {@code search_orders_by_customer},
 * {@code create_order}, etc. — to answer you. One agent serves both domains;
 * the model does the routing.
 */
@Component
public class CatalogAgent implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogAgent.class);

    private static final String SYSTEM_PROMPT = """
            You are a store assistant. You manage the product catalog and customer
            orders through the provided tools. Use the tools to look things up or
            make changes rather than guessing. When you create or update a product
            or an order, confirm back the resulting id and key fields. Be concise.
            """;

    private final ChatClient chatClient;
    private final ToolCallback[] tools;

    public CatalogAgent(ChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
        this.tools = toolCallbackProvider.getToolCallbacks();
        log.info("Discovered {} MCP tool(s): {}", tools.length, toolNames(tools));

        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(tools)
                .build();
    }

    @Override
    public void run(String... args) {
        // --selftest: invoke the list_products tool directly (no LLM / no API key)
        // to prove the client -> MCP -> server -> DB path end to end, then exit.
        if (java.util.Arrays.asList(args).contains("--selftest")) {
            runSelfTest();
            return;
        }

        System.out.println();
        System.out.println("Store assistant ready. Ask me about products or orders.");
        System.out.println("Examples:");
        System.out.println("  - what products do we have?");
        System.out.println("  - which orders did Alice place?");
        System.out.println("  - add a product: Standing Fan, 40W desk fan, price 29.99, qty 12");
        System.out.println("  - create an order: Bob, USB-C Hub, qty 2");
        System.out.println("Type 'exit' to quit.");
        System.out.println();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("you > ");
                if (!scanner.hasNextLine()) {
                    break;
                }
                String input = scanner.nextLine().trim();
                if (input.isEmpty()) {
                    continue;
                }
                if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
                    break;
                }

                try {
                    String answer = chatClient.prompt()
                            .user(input)
                            .call()
                            .content();
                    System.out.println("agent > " + answer);
                } catch (Exception e) {
                    log.error("Request failed", e);
                    System.out.println("agent > (error: " + e.getMessage() + ")");
                }
                System.out.println();
            }
        }

        System.out.println("Bye.");
    }

    private void runSelfTest() {
        System.out.println();
        System.out.println("=== SELF TEST (no LLM) : calling list_products directly ===");
        ToolCallback listProducts = null;
        for (ToolCallback t : tools) {
            if (t.getToolDefinition().name().equals("list_products")) {
                listProducts = t;
                break;
            }
        }
        if (listProducts == null) {
            System.out.println("FAIL: list_products tool not found among discovered tools.");
            return;
        }
        try {
            String result = listProducts.call("{}");
            System.out.println("list_products returned:");
            System.out.println(result);
            System.out.println("=== SELF TEST PASSED : client -> MCP -> server -> DB works ===");
        } catch (Exception e) {
            log.error("Self test failed", e);
            System.out.println("=== SELF TEST FAILED : " + e.getMessage() + " ===");
        }
    }

    private static String toolNames(ToolCallback[] tools) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tools.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(tools[i].getToolDefinition().name());
        }
        return sb.toString();
    }
}
