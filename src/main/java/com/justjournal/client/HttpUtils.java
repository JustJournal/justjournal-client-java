package com.justjournal.client;

import java.io.IOException;
import java.net.HttpURLConnection;
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
}
