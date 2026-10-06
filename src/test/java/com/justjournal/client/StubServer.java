package com.justjournal.client;

import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Local stand-in for the justjournal.com server. Every path answers with the
 * configured status and body, and the last request is recorded.
 *
 * @author Lucas Holt
 */
class StubServer implements AutoCloseable {

    private final HttpServer server;

    volatile int status = 200;
    volatile String contentType = "text/plain";
    volatile String responseBody = "";

    volatile String requestMethod;
    volatile String requestPath;
    volatile String requestContentType;
    volatile String requestUserAgent;
    volatile String requestBody;

    StubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestMethod = exchange.getRequestMethod();
            requestPath = exchange.getRequestURI().getPath();
            requestContentType = exchange.getRequestHeaders().getFirst("Content-Type");
            requestUserAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            requestBody = readAll(exchange.getRequestBody());

            final byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(status, response.length == 0 ? -1 : response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
    }

    /**
     * @return base url of the stub, ending in /
     */
    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    /**
     * @return the last request body decoded as form fields
     */
    Map<String, String> formFields() {
        final Map<String, String> fields = new HashMap<>();
        for (final String pair : requestBody.split("&")) {
            final String[] kv = pair.split("=", 2);
            fields.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                    kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "");
        }
        return fields;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static String readAll(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        in.transferTo(out);
        return out.toString(StandardCharsets.UTF_8);
    }
}
