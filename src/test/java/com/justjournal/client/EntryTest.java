package com.justjournal.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link Entry} against a local stub of the justjournal server.
 *
 * @author Lucas Holt
 */
public class EntryTest {

    private static final String SAVED = "{\"status\":\"ok\",\"id\":\"42\"}";

    private StubServer server;
    private Entry entry;

    @Before
    public void setUp() throws IOException {
        server = new StubServer();
        server.reply("/api/login", 200, AuthTest.LOGIN_OK, "JSESSIONID=abc123; Path=/; HttpOnly");
        server.contentType = "application/json";
        server.responseBody = SAVED;

        final Auth auth = new Auth("testuser", "testpass", server.url());
        assertTrue(auth.restLogin());
        entry = new Entry(auth);
    }

    @After
    public void tearDown() {
        server.close();
    }

    private boolean post(final String location, final String security,
                         final boolean format, final boolean email, final boolean allowComment) {
        return entry.update("subject", "body", "Happy", location, security, "music",
                format, email, allowComment);
    }

    private JsonNode sent() throws IOException {
        return new ObjectMapper().readTree(server.requestBodies.get("/api/entry"));
    }

    @Test
    public void testUpdateSuccess() {
        assertTrue(post("Home", "Public", true, false, true));
        assertEquals("POST", server.requestMethod);
        assertEquals("/api/entry", server.requestPath);
        assertTrue(server.requestContentType.startsWith("application/json"));
        assertEquals("JustJournal", server.requestUserAgent);
    }

    @Test
    public void testUpdateSendsSessionNotPassword() throws IOException {
        post("Home", "Public", true, false, true);
        assertEquals("JSESSIONID=abc123", server.requestCookies.get("/api/entry"));

        final JsonNode json = sent();
        assertFalse(json.has("user"));
        assertFalse(json.has("pass"));
        assertFalse(json.has("username"));
        assertFalse(json.has("password"));
    }

    @Test
    public void testUpdateSendsFields() throws IOException {
        entry.update("Hello & welcome", "Line one\nüñí=+\"", "Happy", "Home", "Public", "AC/DC",
                true, false, true);

        final JsonNode json = sent();
        assertEquals("Hello & welcome", json.get("subject").asText());
        assertEquals("Line one\nüñí=+\"", json.get("body").asText());
        assertEquals("AC/DC", json.get("music").asText());
        assertEquals(1, json.get("mood").asInt());
        assertEquals(1, json.get("location").asInt());
        assertEquals(2, json.get("security").asInt());
        assertTrue(json.get("security").isInt());
        assertTrue(json.get("autoFormat").asBoolean());
        assertFalse(json.get("emailComments").asBoolean());
        assertTrue(json.get("allowComments").asBoolean());
        assertEquals("TEXT", json.get("format").asText());
        assertFalse(json.get("draft").asBoolean());
        assertTrue(json.get("tags").isArray());
        assertEquals(0, json.get("tags").size());
    }

    @Test
    public void testUpdateLocationValues() throws IOException {
        final String[][] cases = {{"Home", "1"}, {"Work", "2"}, {"School", "3"}, {"Other", "5"}, {"Mars", "0"}};
        for (final String[] c : cases) {
            post(c[0], "Public", true, false, true);
            assertEquals(c[0], c[1], sent().get("location").asText());
        }
    }

    @Test
    public void testUpdateSecurityValues() throws IOException {
        final String[][] cases = {{"Private", "0"}, {"Friends Only", "1"}, {"Public", "2"}};
        for (final String[] c : cases) {
            post("Home", c[0], true, false, true);
            assertEquals(c[0], c[1], sent().get("security").asText());
        }
    }

    @Test
    public void testUpdateMoodValues() throws IOException {
        final String[][] cases = {{"Happy", "1"}, {"Sad", "2"}, {"Pissed off", "36"}, {"Sick", "125"},
                {"Not Specified", "12"}, {"happy", "12"}, {"Meh", "12"}, {null, "12"}};
        for (final String[] c : cases) {
            entry.update("subject", "body", c[0], "Home", "Public", "music", true, false, true);
            assertEquals(c[0], c[1], sent().get("mood").asText());
        }
    }

    @Test
    public void testUpdateCheckboxesInverted() throws IOException {
        post("Home", "Public", false, true, false);

        final JsonNode json = sent();
        assertFalse(json.get("autoFormat").asBoolean());
        assertTrue(json.get("emailComments").asBoolean());
        assertFalse(json.get("allowComments").asBoolean());
    }

    @Test
    public void testUpdateOnlyBodyRequired() throws IOException {
        assertTrue(entry.update(null, "body", null, null, null, null, true, false, true));

        final JsonNode json = sent();
        assertEquals("", json.get("subject").asText());
        assertEquals("", json.get("music").asText());
        assertEquals(Moods.NOT_SPECIFIED_ID, json.get("mood").asInt());
        assertEquals(0, json.get("location").asInt());
        assertEquals(2, json.get("security").asInt());
    }

    @Test
    public void testUpdateBlankBodyNotSent() {
        assertFalse(entry.update("subject", " \n ", "Happy", "Home", "Public", "music", true, false, true));
        assertFalse(server.requestBodies.containsKey("/api/entry"));
        assertTrue(entry.getLastError(), entry.getLastError().contains("body"));
    }

    @Test
    public void testUpdateNotLoggedInNotSent() {
        final Auth failed = new Auth("testuser", "bad%pass", server.url());
        assertFalse(failed.restLogin());

        final Entry notLoggedIn = new Entry(failed);
        assertFalse(notLoggedIn.update("subject", "body", "Happy", "Home", "Public", "music", true, false, true));
        assertFalse(server.requestBodies.containsKey("/api/entry"));
        assertTrue(notLoggedIn.getLastError(), notLoggedIn.getLastError().startsWith("Log in"));

        assertFalse(new Entry(null).update("subject", "body", "Happy", "Home", "Public", "music",
                true, false, true));
        assertFalse(server.requestBodies.containsKey("/api/entry"));
    }

    @Test
    public void testUpdateSuccessHasNoError() {
        assertTrue(post("Home", "Public", true, false, true));
        assertNull(entry.getLastError());
    }

    @Test
    public void testUpdateRejected() {
        server.status = 400;
        server.responseBody = "{\"error\":\"Entry does not contain a body.\"}";
        assertFalse(post("Home", "Public", true, false, true));
        assertEquals("The server couldn't save the entry: Entry does not contain a body.", entry.getLastError());
    }

    @Test
    public void testUpdateSessionExpired() {
        server.status = 403;
        server.responseBody = "{\"error\":\"The login timed out or is invalid.\"}";
        assertFalse(post("Home", "Public", true, false, true));
        assertEquals(Entry.SESSION_EXPIRED, entry.getLastError());
    }

    @Test
    public void testUpdateUnexpectedResponse() {
        server.responseBody = "{\"status\":\"error\"}";
        assertFalse(post("Home", "Public", true, false, true));
        assertTrue(entry.getLastError(), entry.getLastError().startsWith("The server sent an unexpected reply"));
    }

    @Test
    public void testUpdateNotJsonResponse() {
        server.contentType = "text/plain";
        server.responseBody = "JJ.JOURNAL.UPDATE.OK";
        assertFalse(post("Home", "Public", true, false, true));
        assertTrue(entry.getLastError(), entry.getLastError().startsWith("The server sent an unexpected reply"));
    }

    @Test
    public void testUpdateServerError() {
        server.status = 500;
        server.responseBody = SAVED;
        assertFalse(post("Home", "Public", true, false, true));
        assertEquals("The server had a problem (HTTP 500). Try again later.", entry.getLastError());
    }

    @Test
    public void testUpdateClientError() {
        server.status = 404;
        assertFalse(post("Home", "Public", true, false, true));
        assertEquals("The server refused the request (HTTP 404).", entry.getLastError());
    }

    @Test
    public void testUpdateServerUnavailable() {
        server.close();
        assertFalse(post("Home", "Public", true, false, true));
        assertTrue(entry.getLastError(), entry.getLastError().startsWith("Couldn't connect to 127.0.0.1"));
    }
}
