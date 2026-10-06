package com.justjournal.client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * @author Lucas Holt
 */
public class HttpUtils {

    public static final String FORM_URLENCODED = "application/x-www-form-urlencoded";
    private static final String USER_AGENT = "JustJournal";
    public static final String HTTP_POST = "POST";
    public static final String USER_AGENT_HEADER = "User-Agent";

    private HttpUtils() {
    }

    /**
     * Opens a POST connection. An https url gives a TLS connection with the JDK's default certificate checks.
     *
     * @param url url to post to
     * @return connection ready for writing the request body
     * @throws IOException if the connection can't be opened
     */
    public static HttpURLConnection getConnection(final String url) throws IOException {
        final URL jj = new URL(url);
        final HttpURLConnection conn = (HttpURLConnection) jj.openConnection();

        // set user-agent header in POST request
        conn.setRequestProperty(USER_AGENT_HEADER, USER_AGENT);

        conn.setRequestMethod(HTTP_POST);
        conn.setDoOutput(true);
        conn.setDoInput(true);
        return conn;
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
