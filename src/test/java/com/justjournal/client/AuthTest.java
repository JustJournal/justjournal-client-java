package com.justjournal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link Auth} against a local stub of the justjournal server.
 *
 * @author Lucas Holt
 */
public class AuthTest {

    private static final String USER = "testuser";
    private static final String PASS = "testpass";
    static final String LOGIN_OK = "{\"status\":\"JJ.LOGIN.OK\",\"username\":\"testuser\",\"twoFactorMethod\":null}";
    private static final String LOGIN_2FA = "{\"status\":\"JJ.LOGIN.2FA\",\"username\":\"testuser\",\"twoFactorMethod\":\"TOTP\"}";

    private StubServer server;

    @Before
    public void setUp() throws IOException {
        server = new StubServer();
        server.contentType = "application/json";
        server.responseBody = LOGIN_OK;
    }

    @After
    public void tearDown() {
        server.close();
    }

    private Auth auth(final String user, final String pass) {
        return new Auth(user, pass, server.url());
    }

    /**
     * @return the cookie header the login sends with its next request
     */
    private String nextCookieHeader(final Auth auth) {
        final Client client = ClientBuilder.newClient();
        try {
            server.requestCookie = null;
            auth.request(client, "next").get().close();
            return server.requestCookie;
        } finally {
            client.close();
        }
    }

    // restLogin

    @Test
    public void testRestLoginSuccess() {
        final Auth auth = auth(USER, PASS);
        assertTrue(auth.restLogin());
        assertTrue(auth.isLoggedIn());
        assertFalse(auth.isSecondFactorRequired());
    }

    @Test
    public void testRestLoginSendsJsonCredentials() throws IOException {
        auth(USER, PASS).restLogin();

        assertEquals("POST", server.requestMethod);
        assertEquals("/api/login", server.requestPath);
        assertTrue(server.requestContentType.startsWith("application/json"));
        assertEquals("JustJournal", server.requestUserAgent);

        final JsonNode json = new ObjectMapper().readTree(server.requestBody);
        assertEquals(USER, json.get("username").asText());
        assertEquals(PASS, json.get("password").asText());
    }

    @Test
    public void testRestLoginLowercasesUsername() throws IOException {
        auth(" TestUser ", PASS).restLogin();
        final JsonNode json = new ObjectMapper().readTree(server.requestBody);
        assertEquals(USER, json.get("username").asText());
    }

    @Test
    public void testRestLoginInvalidNotSent() {
        assertFalse(auth("ab", PASS).restLogin());
        assertFalse(auth("bad-name", PASS).restLogin());
        assertFalse(auth(USER, "abcd").restLogin());
        assertFalse(auth(USER, "pässword").restLogin());
        assertNull(server.requestMethod);
    }

    @Test
    public void testRestLoginSuccessHasNoError() {
        final Auth auth = auth(USER, PASS);
        assertTrue(auth.restLogin());
        assertNull(auth.getLastError());
    }

    @Test
    public void testRestLoginOkWithoutStatusFails() {
        server.responseBody = "{}";
        final Auth auth = auth(USER, PASS);
        assertFalse(auth.restLogin());
        assertFalse(auth.isLoggedIn());
        assertEquals(Auth.UNEXPECTED_REPLY, auth.getLastError());
    }

    @Test
    public void testRestLoginOkWithFailStatusFails() {
        server.responseBody = "{\"status\":\"JJ.LOGIN.FAIL\"}";
        final Auth auth = auth(USER, PASS);
        assertFalse(auth.restLogin());
        assertFalse(auth.isLoggedIn());
    }

    @Test
    public void testRestLoginOkNotJsonFails() {
        server.contentType = "text/html";
        server.responseBody = "<html>login</html>";
        final Auth auth = auth(USER, PASS);
        assertFalse(auth.restLogin());
        assertEquals(Auth.UNEXPECTED_REPLY, auth.getLastError());
    }

    @Test
    public void testRestLoginUnauthorized() {
        server.status = 401;
        server.responseBody = "{\"status\":\"JJ.LOGIN.FAIL\"}";
        final Auth auth = auth(USER, PASS);
        assertFalse(auth.restLogin());
        assertEquals(Auth.BAD_LOGIN, auth.getLastError());
    }

    @Test
    public void testRestLoginServerError() {
        server.status = 502;
        final Auth auth = auth(USER, PASS);
        assertFalse(auth.restLogin());
        assertTrue(auth.getLastError(), auth.getLastError().contains("HTTP 502"));
    }

    @Test
    public void testRestLoginServerUnavailable() {
        server.close();
        final Auth auth = auth(USER, PASS);
        assertFalse(auth.restLogin());
        assertTrue(auth.getLastError(), auth.getLastError().startsWith("Couldn't connect to 127.0.0.1"));
    }

    @Test
    public void testRestLoginInvalidError() {
        final Auth auth = auth("ab", PASS);
        assertFalse(auth.restLogin());
        assertEquals(Credentials.usernameProblem("ab"), auth.getLastError());
    }

    // session cookies

    @Test
    public void testSessionCookieSentAfterLogin() {
        server.reply("/api/login", 200, LOGIN_OK, "JSESSIONID=abc123; Path=/; Secure; HttpOnly; SameSite=Lax");
        final Auth auth = auth(USER, PASS);
        assertTrue(auth.restLogin());
        assertEquals("JSESSIONID=abc123", nextCookieHeader(auth));
    }

    @Test
    public void testEveryCookieSentAsPlainPairs() {
        server.reply("/api/login", 200, LOGIN_OK, "JSESSIONID=abc123; Path=/", "other=xyz; Path=/");
        final Auth auth = auth(USER, PASS);
        assertTrue(auth.restLogin());
        // in any order
        assertEquals(new HashSet<>(Arrays.asList("JSESSIONID=abc123", "other=xyz")),
                new HashSet<>(Arrays.asList(nextCookieHeader(auth).split("; "))));
    }

    @Test
    public void testNoCookieBeforeLogin() {
        assertNull(nextCookieHeader(auth(USER, PASS)));
    }

    @Test
    public void testNewLoginDropsOldSession() {
        server.reply("/api/login", 200, LOGIN_OK, "JSESSIONID=old; Path=/");
        final Auth auth = auth(USER, PASS);
        assertTrue(auth.restLogin());

        server.reply("/api/login", 401, "{\"status\":\"JJ.LOGIN.FAIL\"}");
        assertFalse(auth.restLogin());
        assertFalse(auth.isLoggedIn());
        assertNull(nextCookieHeader(auth));
    }

    // two-factor login

    private Auth startSecondFactor() {
        server.reply("/api/login", 200, LOGIN_2FA, "JSESSIONID=pending; Path=/");
        final Auth auth = auth(USER, PASS);
        assertFalse(auth.restLogin());
        return auth;
    }

    @Test
    public void testRestLoginSecondFactorRequired() {
        final Auth auth = startSecondFactor();
        assertFalse(auth.isLoggedIn());
        assertTrue(auth.isSecondFactorRequired());
        assertEquals("TOTP", auth.getSecondFactorMethod());
        assertTrue(auth.getLastError(), auth.getLastError().contains("two-factor"));
    }

    @Test
    public void testVerifySecondFactorSuccess() throws IOException {
        final Auth auth = startSecondFactor();
        server.reply("/api/login/2fa", 200, LOGIN_OK, "JSESSIONID=rotated; Path=/");

        assertTrue(auth.verifySecondFactor(" 123456 "));
        assertTrue(auth.isLoggedIn());
        assertFalse(auth.isSecondFactorRequired());
        assertNull(auth.getLastError());

        // the code goes with the session from the password step
        assertEquals("JSESSIONID=pending", server.requestCookies.get("/api/login/2fa"));
        final JsonNode json = new ObjectMapper().readTree(server.requestBodies.get("/api/login/2fa"));
        assertEquals("123456", json.get("code").asText());
        assertFalse(json.has("password"));

        // and later requests use the session the server switched to
        assertEquals("JSESSIONID=rotated", nextCookieHeader(auth));
    }

    @Test
    public void testVerifySecondFactorWrongCode() {
        final Auth auth = startSecondFactor();
        server.reply("/api/login/2fa", 401, "{\"status\":\"JJ.LOGIN.FAIL\"}");

        assertFalse(auth.verifySecondFactor("000000"));
        assertFalse(auth.isLoggedIn());
        assertTrue(auth.isSecondFactorRequired());
        assertEquals(Auth.BAD_CODE, auth.getLastError());
    }

    @Test
    public void testVerifySecondFactorLoginForgotten() {
        final Auth auth = startSecondFactor();
        server.reply("/api/login/2fa", 401, "{\"status\":\"JJ.LOGIN.NONE\"}");

        assertFalse(auth.verifySecondFactor("123456"));
        assertFalse(auth.isSecondFactorRequired());
        assertTrue(auth.getLastError(), auth.getLastError().contains("Log in again"));
    }

    @Test
    public void testVerifySecondFactorLocked() {
        final Auth auth = startSecondFactor();
        server.reply("/api/login/2fa", 429, "{\"status\":\"JJ.LOGIN.FAIL\"}");

        assertFalse(auth.verifySecondFactor("123456"));
        assertFalse(auth.isLoggedIn());
        assertFalse(auth.isSecondFactorRequired());
        assertEquals(Auth.TOO_MANY_CODES, auth.getLastError());
    }

    @Test
    public void testVerifySecondFactorOkWithoutStatusFails() {
        final Auth auth = startSecondFactor();
        server.reply("/api/login/2fa", 200, "{}");

        assertFalse(auth.verifySecondFactor("123456"));
        assertFalse(auth.isLoggedIn());
        assertEquals(Auth.UNEXPECTED_REPLY, auth.getLastError());
    }

    @Test
    public void testVerifySecondFactorBlankNotSent() {
        final Auth auth = startSecondFactor();
        assertFalse(auth.verifySecondFactor("  "));
        assertFalse(server.requestBodies.containsKey("/api/login/2fa"));
        assertTrue(auth.isSecondFactorRequired());
    }

    @Test
    public void testVerifySecondFactorWithoutLoginNotSent() {
        final Auth auth = auth(USER, PASS);
        assertFalse(auth.verifySecondFactor("123456"));
        assertNull(server.requestMethod);
    }

    @Test
    public void testVerifySecondFactorServerUnavailable() {
        final Auth auth = startSecondFactor();
        server.close();
        assertFalse(auth.verifySecondFactor("123456"));
        assertTrue(auth.isSecondFactorRequired());
        assertTrue(auth.getLastError(), auth.getLastError().startsWith("Couldn't connect to 127.0.0.1"));
    }
}
