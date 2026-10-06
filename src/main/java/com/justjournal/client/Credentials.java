package com.justjournal.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Username and password rules, matching the justjournal server's Login.isUserName() and
 * Login.isPassword(). The server blocks the caller's ip for a few seconds after a login it
 * rejects, so it's worth catching bad input before sending it.
 *
 * @author Lucas Holt
 */
final class Credentials {

    static final int USERNAME_MIN_LENGTH = 3;
    static final int USERNAME_MAX_LENGTH = 50;
    static final int PASSWORD_MIN_LENGTH = 5;
    static final int PASSWORD_MAX_LENGTH = 50;

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern PASSWORD = Pattern.compile("[A-Za-z0-9_@.!&*#$?^ ]+");

    private Credentials() {
    }

    /**
     * Trims and lowercases a username the way the server stores it.
     *
     * @param username username as typed
     * @return normalized username, never null
     */
    static String normalizeUsername(final String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * @param username normalized username
     * @return why the server would reject the username, or null if it is valid
     */
    static String usernameProblem(final String username) {
        if (username == null || username.isEmpty()) {
            return "Enter a username.";
        }
        if (username.length() < USERNAME_MIN_LENGTH || username.length() > USERNAME_MAX_LENGTH) {
            return "Username must be " + USERNAME_MIN_LENGTH + " to " + USERNAME_MAX_LENGTH + " characters.";
        }
        if (!USERNAME.matcher(username).matches()) {
            return "Username may only contain letters, numbers and _.";
        }
        return null;
    }

    /**
     * @param password password as typed
     * @return why the server would reject the password, or null if it is valid
     */
    static String passwordProblem(final String password) {
        if (password == null || password.isEmpty()) {
            return "Enter a password.";
        }
        if (password.length() < PASSWORD_MIN_LENGTH || password.length() > PASSWORD_MAX_LENGTH) {
            return "Password must be " + PASSWORD_MIN_LENGTH + " to " + PASSWORD_MAX_LENGTH + " characters.";
        }
        if (!PASSWORD.matcher(password).matches()) {
            return "Password may only contain letters, numbers, spaces and _ @ . ! & * # $ ? ^";
        }
        return null;
    }

    /**
     * @param username normalized username
     * @param password password as typed
     * @return every problem with the pair, empty if both are valid
     */
    static List<String> problems(final String username, final String password) {
        final List<String> problems = new ArrayList<>();
        final String usernameProblem = usernameProblem(username);
        if (usernameProblem != null) {
            problems.add(usernameProblem);
        }
        final String passwordProblem = passwordProblem(password);
        if (passwordProblem != null) {
            problems.add(passwordProblem);
        }
        return problems;
    }
}
