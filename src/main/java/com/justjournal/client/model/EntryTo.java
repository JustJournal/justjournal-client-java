package com.justjournal.client.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Body for {@code POST /api/entry}. Every field is sent, since the server doesn't fill in
 * defaults for all of them.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public final class EntryTo {

    public static final String FORMAT_TEXT = "TEXT";

    @JsonProperty("subject")
    private String subject = "";

    @JsonProperty("body")
    private String body = "";

    /** 0 private, 1 friends, 2 public */
    @JsonProperty("security")
    private int security;

    /** location id, 0 for none */
    @JsonProperty("location")
    private int location;

    /** mood id, 0 for none */
    @JsonProperty("mood")
    private int mood;

    @JsonProperty("music")
    private String music = "";

    @JsonProperty("format")
    private String format = FORMAT_TEXT;

    @JsonProperty("autoFormat")
    private boolean autoFormat;

    @JsonProperty("allowComments")
    private boolean allowComments;

    @JsonProperty("emailComments")
    private boolean emailComments;

    @JsonProperty("draft")
    private boolean draft;

    // the server fails after saving the entry if tags is missing
    @JsonProperty("tags")
    private List<String> tags = new ArrayList<>();

    @JsonCreator
    public EntryTo() {
        super();
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public int getSecurity() {
        return security;
    }

    public int getLocation() {
        return location;
    }

    public int getMood() {
        return mood;
    }

    public String getMusic() {
        return music;
    }

    public String getFormat() {
        return format;
    }

    public boolean isAutoFormat() {
        return autoFormat;
    }

    public boolean isAllowComments() {
        return allowComments;
    }

    public boolean isEmailComments() {
        return emailComments;
    }

    public boolean isDraft() {
        return draft;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public void setSecurity(final int security) {
        this.security = security;
    }

    public void setLocation(final int location) {
        this.location = location;
    }

    public void setMood(final int mood) {
        this.mood = mood;
    }

    public void setMusic(final String music) {
        this.music = music;
    }

    public void setFormat(final String format) {
        this.format = format;
    }

    public void setAutoFormat(final boolean autoFormat) {
        this.autoFormat = autoFormat;
    }

    public void setAllowComments(final boolean allowComments) {
        this.allowComments = allowComments;
    }

    public void setEmailComments(final boolean emailComments) {
        this.emailComments = emailComments;
    }

    public void setDraft(final boolean draft) {
        this.draft = draft;
    }

    public void setTags(final List<String> tags) {
        this.tags = tags;
    }
}
