package com.example.agentclient;

import com.openai.azure.AzureOpenAIServiceVersion;
import com.openai.azure.credential.AzureApiKeyCredential;
import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.client.okhttp.OpenAIOkHttpClientAsync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the agent to a classic Azure OpenAI deployment.
 *
 * <p>Spring AI 2.0's OpenAI support is built on the official OpenAI Java SDK,
 * whose {@link OpenAIOkHttpClient} has native Azure support: given an
 * {@link AzureApiKeyCredential} and an {@link AzureOpenAIServiceVersion} it
 * authenticates with the {@code api-key} header and builds Azure-style URLs
 * ({@code {endpoint}/openai/deployments/{model}/chat/completions?api-version=...}),
 * where {@code model} is the deployment name.
 *
 * <p>{@link OpenAiChatModel} needs both a synchronous and an asynchronous client;
 * if only one is supplied it tries to build the other from environment variables
 * and fails ("At least one credential source must be specified"). So both are
 * built here from the same Azure settings.
 *
 * <p>The auto-configured OpenAI chat model is turned off via
 * {@code spring.ai.model.chat=none} so this bean is the only {@code ChatModel}.
 */
@Configuration
public class AzureOpenAiConfig {

    private static final Logger log = LoggerFactory.getLogger(AzureOpenAiConfig.class);

    @Bean
    OpenAIClient azureOpenAIClient(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.api-key}") String apiKey,
            @Value("${azure.openai.api-version}") String apiVersion) {

        log.info("Azure OpenAI client -> endpoint='{}', apiVersion='{}'", endpoint, apiVersion);

        return OpenAIOkHttpClient.builder()
                .baseUrl(endpoint)
                .credential(AzureApiKeyCredential.create(apiKey))
                .azureServiceVersion(AzureOpenAIServiceVersion.Companion.fromString(apiVersion))
                .build();
    }

    @Bean
    OpenAIClientAsync azureOpenAIClientAsync(
            @Value("${azure.openai.endpoint}") String endpoint,
            @Value("${azure.openai.api-key}") String apiKey,
            @Value("${azure.openai.api-version}") String apiVersion) {

        return OpenAIOkHttpClientAsync.builder()
                .baseUrl(endpoint)
                .credential(AzureApiKeyCredential.create(apiKey))
                .azureServiceVersion(AzureOpenAIServiceVersion.Companion.fromString(apiVersion))
                .build();
    }

    @Bean
    OpenAiChatModel azureChatModel(
            OpenAIClient azureOpenAIClient,
            OpenAIClientAsync azureOpenAIClientAsync,
            @Value("${azure.openai.deployment}") String deployment,
            @Value("${azure.openai.max-tokens}") Integer maxTokens,
            ObjectProvider<ToolCallingManager> toolCallingManager) {

        OpenAiChatModel.Builder builder = OpenAiChatModel.builder()
                .openAiClient(azureOpenAIClient)
                .openAiClientAsync(azureOpenAIClientAsync)
                .options(OpenAiChatOptions.builder()
                        .model(deployment)
                        .maxTokens(maxTokens)
                        .build());

        // Use the auto-configured tool-calling manager if one is present so the
        // model can actually invoke the discovered MCP tools.
        toolCallingManager.ifAvailable(builder::toolCallingManager);

        return builder.build();
    }
}
