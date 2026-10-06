package com.justjournal.client;

import com.justjournal.client.model.Login;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author caryn
 */
public class Auth {

    final Logger log = LoggerFactory.getLogger(Auth.class);

    private static final String JJ_LOGIN_OK = "JJ.LOGIN.OK";
    static final String BAD_LOGIN = "The username or password is incorrect. "
            + "After a failed login the server makes you wait a few seconds before trying again.";
    private static final String SITE_URL = "https://www.justjournal.com/";
    private final String siteUrl;
    private final String userName;
    private final String password;
    private String lastError;

    /**
     * Creates instance of jj_auth
     *
     * @param username justjournal.com username
     * @param password justjournal.com password
     */
    public Auth(final String username, final String password) {
        this(username, password, SITE_URL);
    }

    /**
     * Creates instance of jj_auth against a specific server
     *
     * @param username justjournal.com username
     * @param password justjournal.com password
     * @param siteUrl  base url of the justjournal site, ending in /
     */
    Auth(final String username, final String password, final String siteUrl) {
        userName = Credentials.normalizeUsername(username);
        this.password = password;
        this.siteUrl = siteUrl;
    }

    /**
     * Checks account over secure channel
     *
     * @return true if account is valid
     */
    public boolean secureCheckAccount() {
        if (!isValid()) {
            return false;
        }
        try {
            // sending the post request
            String data = "username=" + URLEncoder.encode(userName, StandardCharsets.UTF_8.displayName());
            data += "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8.displayName());
            final HttpURLConnection conn = HttpUtils.getConnection(siteUrl + "loginAccount");
            final OutputStreamWriter writer =
                    new OutputStreamWriter(conn.getOutputStream());

            writer.write(data);
            writer.flush();
            writer.close();
            // getting the response
            final BufferedReader input = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            final char[] returnCode = new char[512];
            int i = 0;
            int tempChar = input.read();
            while (tempChar != -1 && i < 512) {
                returnCode[i] = (char) tempChar;
                tempChar = input.read();
                i++;
            }

            String code = new String(returnCode);
            code = code.trim();
            input.close();

            log.debug("Code is {}", code);

            if (code.equals(JJ_LOGIN_OK))
                return true;
            lastError = BAD_LOGIN;
        } catch (Exception e) {
            log.error("Unable to validate secure login", e);
            lastError = HttpUtils.describeFailure(siteUrl);
        }
        return false;
    }

    /**
     * Checks account using the REST API
     *
     * @return true if login was successful
     */
    public boolean restLogin() {
        if (!isValid()) {
            return false;
        }
        final Client client = ClientBuilder.newClient();
        try {
            final Login login = new Login();
            login.setUsername(userName);
            login.setPassword(password);

            final Response res = client.target(siteUrl).path("api/login")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(login, MediaType.APPLICATION_JSON));
            try {
                final int status = res.getStatus();
                if (status == Response.Status.OK.getStatusCode()) {
                    return true;
                }
                log.error("Failed login, status {}", status);
                lastError = status == Response.Status.UNAUTHORIZED.getStatusCode()
                        ? BAD_LOGIN : HttpUtils.describeStatus(status);
                return false;
            } finally {
                res.close();
            }
        } catch (Exception e) {
            log.error("Unexpected error, failed login", e);
            lastError = HttpUtils.describeFailure(siteUrl);
            return false;
        } finally {
            client.close();
        }
    }

    /**
     * Skips the request when the server would reject the credentials anyway, since a rejected
     * login gets the caller's ip blocked for a while.
     */
    private boolean isValid() {
        final List<String> problems = Credentials.problems(userName, password);
        if (problems.isEmpty()) {
            return true;
        }
        log.error("Not sending login: {}", String.join(" ", problems));
        lastError = String.join(" ", problems);
        return false;
    }

    /**
     * @return why the last login failed, in words for the user, or null if it hasn't failed
     */
    public String getLastError() {
        return lastError;
    }

}
