package com.justjournal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link Auth} against a local stub of the justjournal server.
 *
 * @author Lucas Holt
 */
public class AuthTest {

    private static final String USER = "testuser";
    private static final String PASS = "testpass";

    private StubServer server;

    @Before
    public void setUp() throws IOException {
        server = new StubServer();
    }

    @After
    public void tearDown() {
        server.close();
    }

    private Auth auth(final String user, final String pass) {
        return new Auth(user, pass, server.url());
    }

    // restLogin

    @Test
    public void testRestLoginSuccess() {
        server.contentType = "application/json";
        server.responseBody = "{}";
        assertTrue(auth(USER, PASS).restLogin());
    }

    @Test
    public void testRestLoginSendsJsonCredentials() throws IOException {
        auth(USER, PASS).restLogin();

        assertEquals("POST", server.requestMethod);
        assertEquals("/api/login", server.requestPath);
        assertTrue(server.requestContentType.startsWith("application/json"));

        final JsonNode json = new ObjectMapper().readTree(server.requestBody);
        assertEquals(USER, json.get("username").asText());
        assertEquals(PASS, json.get("password").asText());
    }

    @Test
    public void testRestLoginUnauthorized() {
        server.status = 401;
        assertFalse(auth(USER, PASS).restLogin());
    }

    @Test
    public void testRestLoginServerError() {
        server.status = 500;
        assertFalse(auth(USER, PASS).restLogin());
    }

    @Test
    public void testRestLoginServerUnavailable() {
        server.close();
        assertFalse(auth(USER, PASS).restLogin());
    }

    // secureCheckAccount

    @Test
    public void testSecureCheckAccountSuccess() {
        server.responseBody = "JJ.LOGIN.OK";
        assertTrue(auth(USER, PASS).secureCheckAccount());
        assertEquals("POST", server.requestMethod);
        assertEquals("/loginAccount", server.requestPath);
        assertEquals("JustJournal", server.requestUserAgent);
    }

    @Test
    public void testSecureCheckAccountRejected() {
        server.responseBody = "JJ.LOGIN.FAIL";
        assertFalse(auth(USER, PASS).secureCheckAccount());
    }

    @Test
    public void testSecureCheckAccountEncodesCredentials() {
        server.responseBody = "JJ.LOGIN.OK";
        final String pass = "p&ss=wo+rd&username=other";
        auth("  " + USER + " ", pass).secureCheckAccount();

        final Map<String, String> fields = server.formFields();
        assertEquals(2, fields.size());
        assertEquals(USER, fields.get("username"));
        assertEquals(pass, fields.get("password"));
    }

    @Test
    public void testSecureCheckAccountServerError() {
        server.status = 500;
        server.responseBody = "JJ.LOGIN.OK";
        assertFalse(auth(USER, PASS).secureCheckAccount());
    }

    @Test
    public void testSecureCheckAccountServerUnavailable() {
        server.close();
        assertFalse(auth(USER, PASS).secureCheckAccount());
    }
}
