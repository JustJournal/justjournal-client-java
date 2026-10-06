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

/**
 * @author caryn
 */
public class Auth {

    final Logger log = LoggerFactory.getLogger(Auth.class);

    private static final String JJ_LOGIN_OK = "JJ.LOGIN.OK";
    private static final String SITE_URL = "https://www.justjournal.com/";
    private final String siteUrl;
    private String userName;
    private String password;

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
        userName = username;
        this.password = password;
        this.siteUrl = siteUrl;
    }

    /**
     * Checks account over secure channel
     *
     * @return true if account is valid
     */
    public boolean secureCheckAccount() {
        try {
            // sending the post request
            userName = userName.trim();
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
        } catch (Exception e) {
            log.error("Unable to validate secure login", e);
        }
        return false;
    }

    /**
     * Checks account using the REST API
     *
     * @return true if login was successful
     */
    public boolean restLogin() {
        final Client client = ClientBuilder.newClient();
        try {
            final Login login = new Login();
            login.setUsername(userName);
            login.setPassword(password);

            final Response res = client.target(siteUrl).path("api/login")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(login, MediaType.APPLICATION_JSON));
            try {
                if (res.getStatus() == Response.Status.OK.getStatusCode()) {
                    return true;
                }
                log.error("Failed login, status {}", res.getStatus());
                return false;
            } finally {
                res.close();
            }
        } catch (Exception e) {
            log.error("Unexpected error, failed login", e);
            return false;
        } finally {
            client.close();
        }
    }

}
