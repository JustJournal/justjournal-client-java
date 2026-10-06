package com.justjournal.client;/*
 * jjUpdate.java
 *
 * Created on October 17, 2005, 9:33 AM
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author caryn
 */
public class Entry {

    final Logger log = LoggerFactory.getLogger(Entry.class);

    public static final String JJ_JOURNAL_UPDATE_OK = "JJ.JOURNAL.UPDATE.OK";
    private static final String SITE_URL = "https://www.justjournal.com/";
    private final String siteUrl;
    // account information
    private String username;
    private String password;

    /**
     * Constructor
     *
     * @param jjUsername
     * @param jjPassword
     */
    public Entry(final String jjUsername, final String jjPassword) {
        this(jjUsername, jjPassword, SITE_URL);
    }

    /**
     * Constructor for a specific server
     *
     * @param jjUsername
     * @param jjPassword
     * @param siteUrl    base url of the justjournal site, ending in /
     */
    Entry(final String jjUsername, final String jjPassword, final String siteUrl) {
        username = jjUsername;
        password = jjPassword;
        this.siteUrl = siteUrl;
    }

    /**
     * Updates journal
     *
     * @param subject
     * @param body
     * @param mood
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

        // location, mood, security are all int values
        // aformat, subject, body, allow_comments, email_comments (checked, unchecked) are strings

        // translate location to integer value
        int locationInteger = 0;
        if (location.equals("Home"))
            locationInteger = 1;
        else if (location.equals("Work"))
            locationInteger = 2;
        else if (location.equals("School"))
            locationInteger = 3;
        else if (location.equals("Other"))
            locationInteger = 5;

        // translate security to integer value
        // default value is public
        int securityInteger = 2;
        if (security.equals("Private"))
            securityInteger = 0;
        else if (security.equals("Friends Only"))
            securityInteger = 1;

        // translate format, email and allow comments to string
        // boolean to string for auto format
        String strFormat = "checked";
        if (!format)
            strFormat = "unchecked";

        // boolean to string for email comments
        String strEmail = "unchecked";
        if (email)
            strEmail = "checked";

        String strAllow = "checked";
        if (!allowComment)
            strAllow = "unchecked";

        try {

            String data = "";
            data += "user=" + URLEncoder.encode(username, StandardCharsets.UTF_8.displayName());
            data += "&pass=" + URLEncoder.encode(password, StandardCharsets.UTF_8.displayName());
            data += "&security=" + securityInteger;
            data += "&location=" + locationInteger;
            data += "&mood=12";  // Not Specified value
            data += "&music=" + URLEncoder.encode(music, StandardCharsets.UTF_8.displayName());
            data += "&aformat=" + strFormat;
            data += "&allow_comment=" + strAllow;
            data += "&email_comment=" + strEmail;
            //data += "&date=" + URLEncoder.encode(strDate, "UTF-8"); This is handled server side for now
            data += "&subject=" + URLEncoder.encode(subject, StandardCharsets.UTF_8.displayName());
            data += "&body=" + URLEncoder.encode(body, StandardCharsets.UTF_8.displayName());

            final HttpURLConnection conn = HttpUtils.getConnection(siteUrl + "updateJournal");
            conn.setRequestProperty("Content-Type", HttpUtils.FORM_URLENCODED);
            conn.setRequestProperty("Content-Length", Integer.toString(data.length()));

            final OutputStreamWriter writer =
                    new OutputStreamWriter(conn.getOutputStream());

            writer.write(data);
            writer.flush();
            writer.close();
            // getting the response
            final BufferedReader input = new BufferedReader(new InputStreamReader
                    (conn.getInputStream()));
            int response = input.read();
            final char[] returnCode = new char[50];
            int i = 0;
            // for debugging
            while (response != -1 && i < returnCode.length) {
                returnCode[i] = (char) response;
                i++;
                response = input.read();
            }
            input.close();
            String code = new String(returnCode);
            code = code.trim();

            if (code.compareTo(JJ_JOURNAL_UPDATE_OK) == 0)
                return true;
            log.debug("JJUpdate(): {}", code);
        } catch (final Exception e) {
            log.error("Could not save entry", e);
        }

        // if we get this far, we failed
        // display error msg
        return false;
    }

}
