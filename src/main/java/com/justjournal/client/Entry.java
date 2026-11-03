package com.justjournal.client;/*
 * jjUpdate.java
 *
 * Created on October 17, 2005, 9:33 AM
 */

import com.justjournal.client.model.EntryTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * Posts journal entries to {@code /api/entry} using the session from {@link Auth}.
 *
 * @author caryn
 */
public class Entry {

    final Logger log = LoggerFactory.getLogger(Entry.class);

    private static final String STATUS_OK = "ok";
    static final String SESSION_EXPIRED = "Your login has expired. Log in again, then post the entry.";
    private final Auth auth;
    private String lastError;

    /**
     * Constructor
     *
     * @param auth a login that has succeeded
     */
    public Entry(final Auth auth) {
        this.auth = auth;
    }

    /**
     * Updates journal
     *
     * @param subject
     * @param body
     * @param mood         mood title from {@link Moods}; unknown titles are sent as "Not Specified"
     * @param location
     * @param security
     * @param music
     * @param format
     * @param email
     * @param allowComment
     * @return true if update succeeded.
     */
    public boolean update(final String subject, final String body, final String mood,
                          final String location, final String security, final String music,
                          final boolean format, final boolean email, final boolean allowComment) {

        if (auth == null || !auth.isLoggedIn()) {
            log.error("Not sending entry: not logged in");
            lastError = "Log in before posting an entry.";
            return false;
        }
        if (body == null || body.trim().isEmpty()) {
            lastError = "Write something in the body before posting.";
            return false;
        }

        // translate location to integer value
        int locationInteger = 0;
        if ("Home".equals(location))
            locationInteger = 1;
        else if ("Work".equals(location))
            locationInteger = 2;
        else if ("School".equals(location))
            locationInteger = 3;
        else if ("Other".equals(location))
            locationInteger = 5;

        // translate security to integer value
        // default value is public
        int securityInteger = 2;
        if ("Private".equals(security))
            securityInteger = 0;
        else if ("Friends Only".equals(security))
            securityInteger = 1;

        final EntryTo entry = new EntryTo();
        entry.setSubject(subject == null ? "" : subject);
        entry.setBody(body);
        entry.setSecurity(securityInteger);
        entry.setLocation(locationInteger);
        entry.setMood(Moods.idFor(mood));
        entry.setMusic(music == null ? "" : music);
        entry.setAutoFormat(format);
        entry.setAllowComments(allowComment);
        entry.setEmailComments(email);

        final Client client = ClientBuilder.newClient();
        try {
            final Response res = auth.request(client, "api/entry")
                    .post(Entity.entity(entry, MediaType.APPLICATION_JSON));
            try {
                final int status = res.getStatus();
                final Map<String, String> reply = readReply(res);
                if (status == Response.Status.OK.getStatusCode()) {
                    if (reply != null && STATUS_OK.equals(reply.get("status"))) {
                        lastError = null;
                        return true;
                    }
                    log.error("Unexpected reply saving entry: {}", reply);
                    lastError = "The server sent an unexpected reply, so the entry may not have been saved.";
                    return false;
                }
                log.error("Could not save entry, status {} {}", status, reply);
                final String error = reply == null ? null : reply.get("error");
                if (status == Response.Status.FORBIDDEN.getStatusCode()
                        || status == Response.Status.UNAUTHORIZED.getStatusCode()) {
                    lastError = SESSION_EXPIRED;
                } else if (status == Response.Status.BAD_REQUEST.getStatusCode() && error != null) {
                    lastError = "The server couldn't save the entry: " + error;
                } else {
                    lastError = HttpUtils.describeStatus(status);
                }
            } finally {
                res.close();
            }
        } catch (final Exception e) {
            log.error("Could not save entry", e);
            lastError = HttpUtils.describeFailure(auth.getSiteUrl());
        } finally {
            client.close();
        }

        // if we get this far, we failed
        // display error msg
        return false;
    }

    /**
     * @return the JSON object the server replied with, or null if it isn't one
     */
    private Map<String, String> readReply(final Response res) {
        try {
            return res.readEntity(new GenericType<Map<String, String>>() {
            });
        } catch (ProcessingException | IllegalStateException e) {
            log.debug("Reply was not a JSON object", e);
            return null;
        }
    }

    /**
     * @return why the last update failed, in words for the user, or null if it hasn't failed
     */
    public String getLastError() {
        return lastError;
    }

}
