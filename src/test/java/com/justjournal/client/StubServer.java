package com.justjournal.client;

import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Local stand-in for the justjournal.com server. Paths given a {@link #reply} answer with it,
 * every other path answers with the configured status and body. The last request is recorded,
 * and the last body and cookie header sent to each path.
 *
 * @author Lucas Holt
 */
class StubServer implements AutoCloseable {

    /** A canned JSON response for one path. */
    record Reply(int status, String body, List<String> setCookies) {
    }

    private final HttpServer server;
    private final Map<String, Reply> replies = new ConcurrentHashMap<>();

    volatile int status = 200;
    volatile String contentType = "text/plain";
    volatile String responseBody = "";

    volatile String requestMethod;
    volatile String requestPath;
    volatile String requestContentType;
    volatile String requestUserAgent;
    volatile String requestBody;
    volatile String requestCookie;
    final Map<String, String> requestBodies = new ConcurrentHashMap<>();
    final Map<String, String> requestCookies = new ConcurrentHashMap<>();

    StubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            requestMethod = exchange.getRequestMethod();
            requestPath = exchange.getRequestURI().getPath();
            requestContentType = exchange.getRequestHeaders().getFirst("Content-Type");
            requestUserAgent = exchange.getRequestHeaders().getFirst("User-Agent");
            requestCookie = exchange.getRequestHeaders().getFirst("Cookie");
            requestBody = readAll(exchange.getRequestBody());
            requestBodies.put(requestPath, requestBody);
            if (requestCookie != null) {
                requestCookies.put(requestPath, requestCookie);
            }

            final Reply reply = replies.get(requestPath);
            final byte[] response = (reply != null ? reply.body() : responseBody).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", reply != null ? "application/json" : contentType);
            if (reply != null) {
                for (final String cookie : reply.setCookies()) {
                    exchange.getResponseHeaders().add("Set-Cookie", cookie);
                }
            }
            exchange.sendResponseHeaders(reply != null ? reply.status() : status,
                    response.length == 0 ? -1 : response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.start();
    }

    /**
     * Makes one path answer with a JSON body.
     *
     * @param path       request path, starting with /
     * @param status     HTTP status
     * @param body       JSON body
     * @param setCookies Set-Cookie header values to send
     */
    void reply(final String path, final int status, final String body, final String... setCookies) {
        replies.put(path, new Reply(status, body, Arrays.asList(setCookies)));
    }

    /**
     * @return base url of the stub, ending in /
     */
    String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/";
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
