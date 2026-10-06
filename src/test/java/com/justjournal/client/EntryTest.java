package com.justjournal.client;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

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

    private StubServer server;
    private Entry entry;

    @Before
    public void setUp() throws IOException {
        server = new StubServer();
        server.responseBody = Entry.JJ_JOURNAL_UPDATE_OK;
        entry = new Entry("testuser", "testpass", server.url());
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

    @Test
    public void testUpdateSuccess() {
        assertTrue(post("Home", "Public", true, false, true));
        assertEquals("POST", server.requestMethod);
        assertEquals("/updateJournal", server.requestPath);
        assertEquals(HttpUtils.FORM_URLENCODED, server.requestContentType);
        assertEquals("JustJournal", server.requestUserAgent);
    }

    @Test
    public void testUpdateSendsFields() {
        entry.update("Hello & welcome", "Line one\nüñí=+", "Happy", "Home", "Public", "AC/DC",
                true, false, true);

        final Map<String, String> fields = server.formFields();
        assertEquals("testuser", fields.get("user"));
        assertEquals("testpass", fields.get("pass"));
        assertEquals("Hello & welcome", fields.get("subject"));
        assertEquals("Line one\nüñí=+", fields.get("body"));
        assertEquals("AC/DC", fields.get("music"));
        assertEquals("1", fields.get("mood"));
        assertEquals("1", fields.get("location"));
        assertEquals("2", fields.get("security"));
        assertEquals("checked", fields.get("aformat"));
        assertEquals("unchecked", fields.get("email_comment"));
        assertEquals("checked", fields.get("allow_comment"));
    }

    @Test
    public void testUpdateLocationValues() {
        final String[][] cases = {{"Home", "1"}, {"Work", "2"}, {"School", "3"}, {"Other", "5"}, {"Mars", "0"}};
        for (final String[] c : cases) {
            post(c[0], "Public", true, false, true);
            assertEquals(c[0], c[1], server.formFields().get("location"));
        }
    }

    @Test
    public void testUpdateSecurityValues() {
        final String[][] cases = {{"Private", "0"}, {"Friends Only", "1"}, {"Public", "2"}};
        for (final String[] c : cases) {
            post("Home", c[0], true, false, true);
            assertEquals(c[0], c[1], server.formFields().get("security"));
        }
    }

    @Test
    public void testUpdateMoodValues() {
        final String[][] cases = {{"Happy", "1"}, {"Sad", "2"}, {"Pissed off", "36"}, {"Sick", "125"},
                {"Not Specified", "12"}, {"happy", "12"}, {"Meh", "12"}, {null, "12"}};
        for (final String[] c : cases) {
            entry.update("subject", "body", c[0], "Home", "Public", "music", true, false, true);
            assertEquals(c[0], c[1], server.formFields().get("mood"));
        }
    }

    @Test
    public void testUpdateCheckboxesInverted() {
        post("Home", "Public", false, true, false);

        final Map<String, String> fields = server.formFields();
        assertEquals("unchecked", fields.get("aformat"));
        assertEquals("checked", fields.get("email_comment"));
        assertEquals("unchecked", fields.get("allow_comment"));
    }

    @Test
    public void testUpdateLowercasesUsername() {
        new Entry(" TestUser ", "testpass", server.url()).update("subject", "body", "Happy", "Home", "Public",
                "music", true, false, true);
        assertEquals("testuser", server.formFields().get("user"));
    }

    @Test
    public void testUpdateInvalidCredentialsNotSent() {
        final Entry bad = new Entry("testuser", "bad%pass", server.url());
        assertFalse(bad.update("subject", "body", "Happy", "Home", "Public", "music", true, false, true));
        assertNull(server.requestMethod);
        assertEquals(Credentials.passwordProblem("bad%pass"), bad.getLastError());
    }

    @Test
    public void testUpdateSuccessHasNoError() {
        assertTrue(post("Home", "Public", true, false, true));
        assertNull(entry.getLastError());
    }

    @Test
    public void testUpdateRejected() {
        server.responseBody = "JJ.JOURNAL.UPDATE.FAIL";
        assertFalse(post("Home", "Public", true, false, true));
        assertTrue(entry.getLastError(), entry.getLastError().startsWith("The server couldn't save the entry"));
    }

    @Test
    public void testUpdateBadLogin() {
        // the server writes the failure code twice when authentication fails
        server.responseBody = "JJ.LOGIN.FAILJJ.LOGIN.FAIL";
        assertFalse(post("Home", "Public", true, false, true));
        assertEquals(Auth.BAD_LOGIN, entry.getLastError());
    }

    @Test
    public void testUpdateUnexpectedResponse() {
        server.responseBody = "JJ.ERROR";
        assertFalse(post("Home", "Public", true, false, true));
        assertTrue(entry.getLastError(), entry.getLastError().startsWith("The server sent an unexpected reply"));
    }

    @Test
    public void testUpdateLongResponse() {
        server.responseBody = "x".repeat(500);
        assertFalse(post("Home", "Public", true, false, true));
    }

    @Test
    public void testUpdateServerError() {
        server.status = 500;
        server.responseBody = Entry.JJ_JOURNAL_UPDATE_OK;
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
