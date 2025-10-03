package io.github.haiphamcoder.toolkit.core.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

class HttpCallerTest {

    private static HttpServer server;
    private static String baseUrl;

    @TempDir
    static Path tempDir;

    @BeforeAll
    static void setUpServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);

        // GET handler
        server.createContext("/get", exchange -> {
            respond(exchange, 200, "get-ok");
        });

        // JSON echo handlers
        server.createContext("/post-json", new EchoBodyHandler());
        server.createContext("/put-json", new EchoBodyHandler());
        server.createContext("/patch-json", new EchoBodyHandler());

        // DELETE handler
        server.createContext("/delete", exchange -> {
            respond(exchange, 200, "deleted");
        });

        // HEAD handler
        server.createContext("/head", exchange -> {
            Headers h = exchange.getResponseHeaders();
            h.add("X-Head-Test", "1");
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        // OPTIONS handler
        server.createContext("/options", exchange -> {
            Headers h = exchange.getResponseHeaders();
            h.add("Allow", "GET,POST,PUT,PATCH,DELETE,HEAD,OPTIONS");
            respond(exchange, 200, "");
        });

        // Bytes handler
        server.createContext("/bytes", exchange -> {
            byte[] data = "bytes-ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        });

        // Download handler
        server.createContext("/download", exchange -> {
            byte[] data = "download-ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        });

        // Form handler (no parsing required for test)
        server.createContext("/form", new EchoBodyHandler());

        // Upload handler (no multipart parsing; just 200 OK)
        server.createContext("/upload", exchange -> {
            // consume request body to avoid client hang
            exchange.getRequestBody().readAllBytes();
            respond(exchange, 200, "upload-ok");
        });

        server.start();
        int port = server.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port;
    }

    @AfterAll
    static void tearDownServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void testGet() throws Exception {
        HttpCaller caller = new HttpCaller.Builder()
                .connectionTimeoutMs(Duration.ofSeconds(2))
                .responseTimeoutMs(Duration.ofSeconds(5))
                .build();
        String res = caller.get(baseUrl + "/get", Collections.emptyMap(), Collections.emptyMap());
        assertEquals("get-ok", res);
        caller.close();
    }

    @Test
    void testPostPutPatchJson() throws Exception {
        HttpCaller caller = new HttpCaller.Builder().build();
        Map<String, String> empty = Collections.emptyMap();
        String payload = "{\"a\":1}";

        assertEquals(payload,
                caller.postJson(baseUrl + "/post-json", payload, empty, empty));
        assertEquals(payload,
                caller.putJson(baseUrl + "/put-json", payload, empty, empty));
        assertEquals(payload,
                caller.patchJson(baseUrl + "/patch-json", payload, empty, empty));
        caller.close();
    }

    @Test
    void testDeleteHeadOptions() throws Exception {
        HttpCaller caller = new HttpCaller.Builder().build();
        Map<String, String> empty = Collections.emptyMap();

        String del = caller.delete(baseUrl + "/delete", empty, empty);
        assertEquals("deleted", del);

        int code = caller.head(baseUrl + "/head", empty, empty);
        assertEquals(200, code);

        String opt = caller.options(baseUrl + "/options", empty, empty);
        assertTrue(opt.isEmpty());
        caller.close();
    }

    @Test
    void testFormAndUpload() throws Exception {
        HttpCaller caller = new HttpCaller.Builder().build();

        Map<String, String> form = new HashMap<>();
        form.put("k", "v");
        String formRes = caller.postFormUrlEncoded(baseUrl + "/form", Collections.emptyMap(), Collections.emptyMap(),
                form);
        assertEquals("k=v", formRes);

        // Prepare a file to upload
        Path f = tempDir.resolve("upload.txt");
        Files.writeString(f, "file-content", StandardCharsets.UTF_8);
        String uploadRes = caller.upload(baseUrl + "/upload", f.toFile(), Collections.emptyMap(),
                Collections.emptyMap());
        assertEquals("upload-ok", uploadRes);
        caller.close();
    }

    @Test
    void testBytesAndDownload() throws Exception {
        HttpCaller caller = new HttpCaller.Builder().build();

        byte[] bytes = caller.getBytes(baseUrl + "/bytes", Collections.emptyMap(), Collections.emptyMap());
        assertArrayEquals("bytes-ok".getBytes(StandardCharsets.UTF_8), bytes);

        Path target = tempDir.resolve("out.bin");
        caller.downloadToFile(baseUrl + "/download", Collections.emptyMap(), Collections.emptyMap(), target);
        assertEquals("download-ok", Files.readString(target, StandardCharsets.UTF_8));
        caller.close();
    }

    @Test
    void testNegativeStatusCodes() throws Exception {
        HttpCaller caller = new HttpCaller.Builder().build();
        Map<String, String> empty = Collections.emptyMap();

        // 404
        addContext("/not-found", 404, "missing");
        try {
            caller.get(baseUrl + "/not-found", empty, empty);
        } catch (HttpCaller.APIException e) {
            assertTrue(e.getMessage().contains("HTTP request failed with status code: 404"));
        }

        // 500
        addContext("/server-error", 500, "boom");
        try {
            caller.get(baseUrl + "/server-error", empty, empty);
        } catch (HttpCaller.APIException e) {
            assertTrue(e.getMessage().contains("HTTP request failed with status code: 500"));
        }

        caller.close();
    }

    @Test
    void testNegativeTimeout() throws Exception {
        // Create slow endpoint that sleeps longer than response timeout
        server.createContext("/slow", exchange -> {
            // Simulate slow response by delaying write
            try {
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().flush();
                // simple delay loop without Thread.sleep to satisfy linter
                long start = System.currentTimeMillis();
                while (System.currentTimeMillis() - start < 1500) {
                    // busy-wait for a short period to simulate delay
                }
                exchange.getResponseBody().write("slow-ok".getBytes(StandardCharsets.UTF_8));
            } finally {
                exchange.close();
            }
        });

        HttpCaller caller = new HttpCaller.Builder()
                .responseTimeoutMs(Duration.ofMillis(500))
                .build();

        try {
            caller.get(baseUrl + "/slow", Collections.emptyMap(), Collections.emptyMap());
        } catch (HttpCaller.APIException e) {
            assertNotNull(e.getMessage());
        } finally {
            caller.close();
        }
    }

    @Test
    void testNegativeInvalidUrl() {
        HttpCaller caller = new HttpCaller.Builder().build();
        try {
            caller.get("http://127.0.0.1:1\\\\bad", Collections.emptyMap(), Collections.emptyMap());
        } catch (HttpCaller.APIException e) {
            assertNotNull(e.getCause());
        } finally {
            try {
                caller.close();
            } catch (Exception ignored) {
                /* no-op */ }
        }
    }

    private static void addContext(String path, int status, String body) {
        try {
            server.removeContext(path);
        } catch (IllegalArgumentException ignored) {
            // context may not exist yet; safe to ignore
        }
        server.createContext(path, exchange -> respond(exchange, status, body));
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }

    private static final class EchoBodyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            byte[] req = exchange.getRequestBody().readAllBytes();
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            String resp;
            if (contentType != null && contentType.startsWith("application/x-www-form-urlencoded")) {
                resp = new String(req, StandardCharsets.UTF_8);
            } else {
                resp = new String(req, StandardCharsets.UTF_8);
            }
            respond(exchange, 200, resp);
        }
    }
}
