package com.justjournal.client;

import java.net.URI;

/**
 * @author Lucas Holt
 */
public class HttpUtils {

    /** The justjournal site the client talks to. */
    static final String SITE_URL = "https://www.justjournal.com/";

    static final String USER_AGENT = "JustJournal";
    public static final String USER_AGENT_HEADER = "User-Agent";

    private HttpUtils() {
    }

    /**
     * Describes an HTTP error status for the user.
     *
     * @param status HTTP status code
     * @return message for the user
     */
    static String describeStatus(final int status) {
        if (status >= 500) {
            return "The server had a problem (HTTP " + status + "). Try again later.";
        }
        return "The server refused the request (HTTP " + status + ").";
    }

    /**
     * Describes a failure to talk to the server, such as no network connection, for the user.
     *
     * @param url the server's url
     * @return message for the user
     */
    static String describeFailure(final String url) {
        String host = url;
        try {
            host = URI.create(url).getHost();
        } catch (IllegalArgumentException ignored) {
            // fall back to the whole url
        }
        return "Couldn't connect to " + host + ". Check your network connection and try again.";
    }
}
