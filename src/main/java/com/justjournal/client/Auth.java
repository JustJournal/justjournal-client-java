package com.justjournal.client;

import com.justjournal.client.model.Login;
import com.justjournal.client.model.LoginResponse;
import com.justjournal.client.model.TwoFactorRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Logs in through the REST API and keeps the session cookie for later requests.
 *
 * @author caryn
 */
public class Auth {

    final Logger log = LoggerFactory.getLogger(Auth.class);

    private static final String JJ_LOGIN_OK = "JJ.LOGIN.OK";
    private static final String JJ_LOGIN_2FA = "JJ.LOGIN.2FA";
    private static final String JJ_LOGIN_NONE = "JJ.LOGIN.NONE";
    static final String BAD_LOGIN = "The username or password is incorrect. "
            + "After a failed login the server makes you wait a few seconds before trying again.";
    static final String BAD_CODE = "That code is incorrect. Check it and try again.";
    static final String TOO_MANY_CODES = "Too many incorrect codes. Log in again to get a new one.";
    static final String UNEXPECTED_REPLY = "The server sent an unexpected reply. Try again later.";
    private final String siteUrl;
    private final String userName;
    private final String password;
    // the session lives only in memory, it is never saved
    private final Map<String, String> cookies = new LinkedHashMap<>();
    private boolean loggedIn;
    private String secondFactorMethod;
    private String lastError;

    /**
     * Creates instance of jj_auth
     *
     * @param username justjournal.com username
     * @param password justjournal.com password
     */
    public Auth(final String username, final String password) {
        this(username, password, HttpUtils.SITE_URL);
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
     * Logs in using the REST API. Accounts with two-factor login are not logged in yet when this
     * returns; check {@link #isSecondFactorRequired()} and finish with {@link #verifySecondFactor}.
     *
     * @return true if login was successful
     */
    public boolean restLogin() {
        loggedIn = false;
        secondFactorMethod = null;
        cookies.clear();
        if (!isValid()) {
            return false;
        }
        final Client client = ClientBuilder.newClient();
        try {
            final Login login = new Login();
            login.setUsername(userName);
            login.setPassword(password);

            final Response res = request(client, "api/login")
                    .post(Entity.entity(login, MediaType.APPLICATION_JSON));
            try {
                keepCookies(res);
                final int status = res.getStatus();
                if (status != Response.Status.OK.getStatusCode()) {
                    log.error("Failed login, status {}", status);
                    lastError = status == Response.Status.UNAUTHORIZED.getStatusCode()
                            ? BAD_LOGIN : HttpUtils.describeStatus(status);
                    return false;
                }
                final LoginResponse reply = readLoginResponse(res);
                final String code = reply == null ? null : reply.getStatus();
                if (JJ_LOGIN_OK.equals(code)) {
                    loggedIn = true;
                    lastError = null;
                    return true;
                }
                if (JJ_LOGIN_2FA.equals(code)) {
                    log.info("Login needs a second factor ({})", reply.getTwoFactorMethod());
                    secondFactorMethod = reply.getTwoFactorMethod() == null ? "" : reply.getTwoFactorMethod();
                    lastError = "This account uses two-factor login. Enter the code to finish logging in.";
                    return false;
                }
                log.error("Failed login, unexpected status {}", code);
                lastError = UNEXPECTED_REPLY;
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
     * Finishes a two-factor login with the code from the e-mail, the authenticator app or the
     * recovery list. A wrong code can be retried until the server gives up on the login.
     *
     * @param code code the user typed
     * @return true if login was successful
     */
    public boolean verifySecondFactor(final String code) {
        if (!isSecondFactorRequired()) {
            lastError = "Log in with your username and password first.";
            return false;
        }
        if (code == null || code.trim().isEmpty()) {
            lastError = "Enter the code.";
            return false;
        }
        final Client client = ClientBuilder.newClient();
        try {
            final TwoFactorRequest body = new TwoFactorRequest();
            body.setCode(code.trim());

            final Response res = request(client, "api/login/2fa")
                    .post(Entity.entity(body, MediaType.APPLICATION_JSON));
            try {
                keepCookies(res);
                final int status = res.getStatus();
                if (status == Response.Status.OK.getStatusCode()) {
                    final LoginResponse reply = readLoginResponse(res);
                    if (reply != null && JJ_LOGIN_OK.equals(reply.getStatus())) {
                        loggedIn = true;
                        secondFactorMethod = null;
                        lastError = null;
                        return true;
                    }
                    log.error("Failed second factor, unexpected status {}",
                            reply == null ? null : reply.getStatus());
                    lastError = UNEXPECTED_REPLY;
                    return false;
                }
                log.error("Failed second factor, status {}", status);
                if (status == Response.Status.UNAUTHORIZED.getStatusCode()) {
                    final LoginResponse reply = readLoginResponse(res);
                    if (reply != null && JJ_LOGIN_NONE.equals(reply.getStatus())) {
                        // the server forgot the half finished login, so the password is needed again
                        secondFactorMethod = null;
                        lastError = "The login timed out. Log in again.";
                    } else {
                        lastError = BAD_CODE;
                    }
                } else if (status == Response.Status.TOO_MANY_REQUESTS.getStatusCode()) {
                    secondFactorMethod = null;
                    lastError = TOO_MANY_CODES;
                } else {
                    lastError = HttpUtils.describeStatus(status);
                }
                return false;
            } finally {
                res.close();
            }
        } catch (Exception e) {
            log.error("Unexpected error, failed second factor", e);
            lastError = HttpUtils.describeFailure(siteUrl);
            return false;
        } finally {
            client.close();
        }
    }

    /**
     * Starts a JSON request to the site that carries this login's session.
     *
     * @param client client to send it with
     * @param path   path under the site url, without a leading /
     * @return request ready to send
     */
    Invocation.Builder request(final Client client, final String path) {
        final Invocation.Builder builder = client.target(siteUrl).path(path)
                .request(MediaType.APPLICATION_JSON)
                .header(HttpUtils.USER_AGENT_HEADER, HttpUtils.USER_AGENT);
        if (!cookies.isEmpty()) {
            // plain name=value pairs; JAX-RS Cookie objects would be sent in the old RFC 2109 form
            final StringBuilder header = new StringBuilder();
            for (final Map.Entry<String, String> cookie : cookies.entrySet()) {
                if (header.length() > 0) {
                    header.append("; ");
                }
                header.append(cookie.getKey()).append('=').append(cookie.getValue());
            }
            builder.header("Cookie", header.toString());
        }
        return builder;
    }

    /**
     * Remembers the cookies the server set, such as a new session id after logging in.
     */
    private void keepCookies(final Response res) {
        for (final NewCookie cookie : res.getCookies().values()) {
            if (cookie.getMaxAge() == 0) {
                cookies.remove(cookie.getName());
            } else {
                cookies.put(cookie.getName(), cookie.getValue());
            }
        }
    }

    private LoginResponse readLoginResponse(final Response res) {
        try {
            return res.readEntity(LoginResponse.class);
        } catch (ProcessingException | IllegalStateException e) {
            log.error("Could not read login reply", e);
            return null;
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
     * @return true once the login has finished, including any second factor
     */
    public boolean isLoggedIn() {
        return loggedIn;
    }

    /**
     * @return true if the password was accepted and the server is waiting for a two-factor code
     */
    public boolean isSecondFactorRequired() {
        return secondFactorMethod != null;
    }

    /**
     * @return EMAIL or TOTP while a two-factor code is needed, otherwise null. Empty if the
     * server didn't say.
     */
    public String getSecondFactorMethod() {
        return secondFactorMethod;
    }

    String getSiteUrl() {
        return siteUrl;
    }

    /**
     * @return why the last login failed, in words for the user, or null if it hasn't failed
     */
    public String getLastError() {
        return lastError;
    }

}
