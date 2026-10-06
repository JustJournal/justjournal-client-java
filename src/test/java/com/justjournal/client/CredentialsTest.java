package com.justjournal.client;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Lucas Holt
 */
public class CredentialsTest {

    @Test
    public void testNormalizeUsername() {
        assertEquals("laffer1", Credentials.normalizeUsername("  Laffer1 "));
        assertEquals("i_title", Credentials.normalizeUsername("I_TITLE"));
        assertEquals("", Credentials.normalizeUsername(null));
    }

    @Test
    public void testValidUsernames() {
        for (final String u : new String[]{"abc", "test_user", "user123", "a".repeat(50), "UPPER"}) {
            assertNull(u, Credentials.usernameProblem(u));
        }
    }

    @Test
    public void testInvalidUsernames() {
        for (final String u : new String[]{null, "", "ab", "a".repeat(51), "bad-name", "bad name",
                "bad.name", "user@x", "üser"}) {
            assertNotNull(u, Credentials.usernameProblem(u));
        }
    }

    @Test
    public void testValidPasswords() {
        for (final String p : new String[]{"abcde", "p@ss.w0rd!", "&*#$?^_", "with space", "a".repeat(50)}) {
            assertNull(p, Credentials.passwordProblem(p));
        }
    }

    @Test
    public void testInvalidPasswords() {
        for (final String p : new String[]{null, "", "abcd", "a".repeat(51), "pass=word", "pass+word",
                "pass%word", "pass-word", "pässword", "tab\there"}) {
            assertNotNull(p, Credentials.passwordProblem(p));
        }
    }

    @Test
    public void testProblems() {
        assertEquals(Collections.emptyList(), Credentials.problems("testuser", "testpass"));
        assertEquals(Arrays.asList("Enter a username.", "Enter a password."), Credentials.problems("", ""));
        assertEquals(1, Credentials.problems("testuser", "abc").size());
    }
}
