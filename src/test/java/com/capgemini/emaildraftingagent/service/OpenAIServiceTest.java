package com.capgemini.emaildraftingagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class OpenAIServiceTest {

    @Test
    void retriesRateLimitAndReturnsTypedIntentClassification() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            if (requests.getAndIncrement() == 0) {
                respond(exchange, 429, "{\"error\":\"rate limited\"}");
            } else {
                respond(exchange, 200, """
                        {"choices":[{"message":{"role":"assistant","content":"{\\"intent\\":\\"BOTH\\",\\"confidence\\":95}"}}]}
                        """);
            }
        });
        try {
            OpenAIService service = service(server, 1, 0, 1000);

            IntentClassificationResult result = service.classifyIntent("Meet tomorrow and send the report.");

            assertEquals(new IntentClassificationResult(
                    com.capgemini.emaildraftingagent.model.EmailIntent.BOTH, 95), result);
            assertEquals(2, requests.get());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportsMalformedProviderResponseAsInvalid() throws IOException {
        HttpServer server = startServer(exchange -> respond(exchange, 200, "{\"choices\":[]}"));
        try {
            OpenAIService service = service(server, 0, 0, 1000);

            AiAnalysisException exception = assertThrows(AiAnalysisException.class,
                    () -> service.classifyIntent("Send the report."));

            assertEquals("The AI provider returned an empty analysis response.", exception.getMessage());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void retriesRequestTimeoutAndSurfacesFailureAfterRetryLimit() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = startServer(exchange -> {
            requests.incrementAndGet();
            try {
                Thread.sleep(1200);
                respond(exchange, 200, """
                        {"choices":[{"message":{"role":"assistant","content":"{\\"intent\\":\\"TASK\\",\\"confidence\\":90}"}}]}
                        """);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                exchange.close();
            } catch (IOException exception) {
                exchange.close();
            }
        });
        try {
            OpenAIService service = service(server, 1, 0, 1);

            AiAnalysisException exception = assertThrows(AiAnalysisException.class,
                    () -> service.classifyIntent("Send the report."));

            assertEquals("OpenAI-compatible API request timed out after retries.", exception.getMessage());
            assertEquals(2, requests.get());
        } finally {
            server.stop(0);
        }
    }

    private static OpenAIService service(HttpServer server, int maxRetries, long retryDelayMillis,
                                         int timeoutSeconds) {
        return new OpenAIService(RestClient.builder(), new ObjectMapper(),
                "http://localhost:" + server.getAddress().getPort() + "/v1",
                "test-key", "test-model", timeoutSeconds, maxRetries, retryDelayMillis);
    }

    private static HttpServer startServer(HttpHandler handler) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/v1/chat/completions", handler::handle);
        server.start();
        return server;
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }

    @FunctionalInterface
    private interface HttpHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
