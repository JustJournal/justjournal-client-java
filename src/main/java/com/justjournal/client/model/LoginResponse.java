package com.justjournal.client.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Reply from {@code /api/login} and {@code /api/login/2fa}.
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class LoginResponse {

    @JsonProperty("status")
    private String status;

    @JsonProperty("username")
    private String username;

    /** EMAIL or TOTP when the status is JJ.LOGIN.2FA */
    @JsonProperty("twoFactorMethod")
    private String twoFactorMethod;

    @JsonCreator
    public LoginResponse() {
        super();
    }

    public String getStatus() {
        return status;
    }

    public String getUsername() {
        return username;
    }

    public String getTwoFactorMethod() {
        return twoFactorMethod;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setTwoFactorMethod(final String twoFactorMethod) {
        this.twoFactorMethod = twoFactorMethod;
    }
}
