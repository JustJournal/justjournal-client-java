package com.justjournal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link Auth#restLogin()} against a local stub of the REST API.
 *
 * @author Lucas Holt
 */
public class AuthTest {

    private static final String USER = "testuser";
    private static final String PASS = "testpass";

    private HttpServer server;
    private String apiUrl;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> requestMethod = new AtomicReference<>();
    private final AtomicReference<String> requestContentType = new AtomicReference<>();
    private final AtomicReference<String> requestBody = new AtomicReference<>();

    @Before
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/login", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            requestBody.set(readAll(exchange.getRequestBody()));

            final byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status.get(), response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
        apiUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/";
    }

    @After
    public void tearDown() {
        server.stop(0);
    }

    @Test
    public void testRestLoginSuccess() {
        status.set(200);
        assertTrue(new Auth(USER, PASS, apiUrl).restLogin());
    }

    @Test
    public void testRestLoginSendsJsonCredentials() throws IOException {
        new Auth(USER, PASS, apiUrl).restLogin();

        assertEquals("POST", requestMethod.get());
        assertTrue(requestContentType.get().startsWith("application/json"));

        final JsonNode json = new ObjectMapper().readTree(requestBody.get());
        assertEquals(USER, json.get("username").asText());
        assertEquals(PASS, json.get("password").asText());
    }

    @Test
    public void testRestLoginUnauthorized() {
        status.set(401);
        assertFalse(new Auth(USER, PASS, apiUrl).restLogin());
    }

    @Test
    public void testRestLoginServerError() {
        status.set(500);
        assertFalse(new Auth(USER, PASS, apiUrl).restLogin());
    }

    @Test
    public void testRestLoginServerUnavailable() {
        server.stop(0);
        assertFalse(new Auth(USER, PASS, apiUrl).restLogin());
    }

    private static String readAll(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
