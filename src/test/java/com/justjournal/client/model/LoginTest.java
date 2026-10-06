package com.justjournal.client.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Lucas Holt
 */
public class LoginTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSerialize() throws IOException {
        final Login login = new Login();
        login.setUsername("testuser");
        login.setPassword("testpass");

        final JsonNode json = mapper.readTree(mapper.writeValueAsString(login));
        assertEquals(2, json.size());
        assertEquals("testuser", json.get("username").asText());
        assertEquals("testpass", json.get("password").asText());
    }

    @Test
    public void testDeserialize() throws IOException {
        final Login login = mapper.readValue("{\"username\":\"testuser\",\"password\":\"testpass\"}", Login.class);
        assertEquals("testuser", login.getUsername());
        assertEquals("testpass", login.getPassword());
    }
}
