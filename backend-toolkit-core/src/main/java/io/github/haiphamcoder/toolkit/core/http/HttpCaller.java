package io.github.haiphamcoder.toolkit.core.http;

import java.io.Closeable;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.Credentials;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpOptions;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.routing.HttpRoutePlanner;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;

/**
 * Lightweight HTTP client wrapper built on Apache HttpClient 5.
 * <p>
 * Provides convenient helpers for common HTTP operations (GET, POST JSON, form,
 * multipart upload, PUT/PATCH/DELETE/HEAD/OPTIONS) and binary operations
 * (download to bytes/file), using the non-deprecated response handler APIs.
 * <p>
 * Instances are created via {@link HttpCaller.Builder} with sane defaults and
 * optional proxy, timeouts and connection pooling configuration.
 */
public class HttpCaller implements Closeable {

    private static final int CONNECTION_TIMEOUT_MS = 3_000;
    private static final int RESPONSE_TIMEOUT_MS = 15_000;
    private static final int CONNECTION_REQUEST_TIMEOUT_MS = 15_000;

    private final CloseableHttpClient httpClient;
    private final RequestConfig requestConfig;

    private HttpCaller(Builder builder) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setValidateAfterInactivity(TimeValue.ofSeconds(30))
                .setConnectTimeout(Timeout.ofMilliseconds(builder.connectionTimeoutMs))
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(builder.maxTotalConnections)
                .setMaxConnPerRoute(builder.maxConnectionsPerRoute)
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        this.requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(builder.responseTimeoutMs))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(builder.connectionRequestTimeoutMs))
                .build();

        HttpRoutePlanner routePlanner = null;
        BasicCredentialsProvider credentialsProvider = null;

        if (builder.useProxy) {
            routePlanner = new DefaultProxyRoutePlanner(new HttpHost(builder.proxyHost, builder.proxyPort));
            if (builder.proxyUsername != null && !builder.proxyUsername.isBlank()) {
                credentialsProvider = new BasicCredentialsProvider();
                Credentials credentials = new UsernamePasswordCredentials(builder.proxyUsername,
                        builder.proxyPassword == null ? new char[0] : builder.proxyPassword.toCharArray());
                credentialsProvider.setCredentials(new AuthScope(builder.proxyHost, builder.proxyPort), credentials);
            }
        }

        HttpClientBuilder httpClientBuilder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setUserAgent(Optional.ofNullable(builder.userAgent).orElse("HttpCaller/1.0.0 (+httpclient5)"))
                .evictExpiredConnections()
                .evictIdleConnections(TimeValue.ofMinutes(1));

        if (!builder.enableAutoRetry) {
            httpClientBuilder.disableAutomaticRetries();
        }

        if (routePlanner != null) {
            httpClientBuilder.setRoutePlanner(routePlanner);
        }
        if (credentialsProvider != null) {
            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
        }

        this.httpClient = httpClientBuilder.build();
    }

    /**
     * Builder for {@link HttpCaller}.
     * <p>
     * Defaults:
     * - Connection timeout: 3s, Response timeout: 15s, Request timeout: 15s
     * - Pool: 200 total, 50 per route
     * - User-Agent: "HttpCaller/1.0.0 (+httpclient5)"
     */
    public static class Builder {
        private int connectionTimeoutMs = CONNECTION_TIMEOUT_MS;
        private int responseTimeoutMs = RESPONSE_TIMEOUT_MS;
        private int connectionRequestTimeoutMs = CONNECTION_REQUEST_TIMEOUT_MS;
        private boolean useProxy = false;

        private int maxTotalConnections = 200;
        private int maxConnectionsPerRoute = 50;

        private String userAgent = "HttpCaller/1.0.0 (+httpclient5)";

        private String proxyHost;
        private int proxyPort;
        private String proxyUsername;
        private String proxyPassword;

        private boolean enableAutoRetry = false;

        /**
         * Set connection timeout.
         *
         * @param duration connection timeout
         * @return this builder
         */
        public Builder connectionTimeoutMs(Duration duration) {
            this.connectionTimeoutMs = (int) duration.toMillis();
            return this;
        }

        /**
         * Set response timeout.
         *
         * @param duration response timeout
         * @return this builder
         */
        public Builder responseTimeoutMs(Duration duration) {
            this.responseTimeoutMs = (int) duration.toMillis();
            return this;
        }

        /**
         * Set connection request timeout (time to wait for a connection from the pool).
         *
         * @param duration connection request timeout
         * @return this builder
         */
        public Builder connectionRequestTimeoutMs(Duration duration) {
            this.connectionRequestTimeoutMs = (int) duration.toMillis();
            return this;
        }

        /**
         * Configure the connection pool size.
         *
         * @param maxTotalConnections    maximum total connections
         * @param maxConnectionsPerRoute maximum per-route connections
         * @return this builder
         */
        public Builder pool(int maxTotalConnections, int maxConnectionsPerRoute) {
            this.maxTotalConnections = maxTotalConnections;
            this.maxConnectionsPerRoute = maxConnectionsPerRoute;
            return this;
        }

        /**
         * Set custom User-Agent header used by this client.
         *
         * @param userAgent user agent string
         * @return this builder
         */
        public Builder userAgent(String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Configure HTTP proxy.
         *
         * @param proxyHost     proxy hostname
         * @param proxyPort     proxy port
         * @param proxyUsername optional username (blank to skip auth)
         * @param proxyPassword optional password
         * @return this builder
         */
        public Builder proxy(String proxyHost, int proxyPort, String proxyUsername, String proxyPassword) {
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.proxyUsername = proxyUsername;
            this.proxyPassword = proxyPassword;
            return this;
        }

        /**
         * Enable or disable HttpClient automatic retries.
         *
         * @param enableAutoRetry true to enable, false to disable
         * @return this builder
         */
        public Builder enableAutoRetry(boolean enableAutoRetry) {
            this.enableAutoRetry = enableAutoRetry;
            return this;
        }

        /**
         * Build a new {@link HttpCaller} instance.
         *
         * @return configured HttpCaller
         */
        public HttpCaller build() {
            return new HttpCaller(this);
        }

    }

    /**
     * Execute a HTTP GET request and return the response body as String.
     *
     * @param endpointUrl absolute or base URL
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @return response body as UTF-8 String (empty if no entity)
     * @throws APIException on IO errors or non-2xx responses
     */
    public String get(String endpointUrl, Map<String, String> params, Map<String, String> headers) throws APIException {
        try {
            URI uri = buildUri(endpointUrl, params);
            HttpGet request = new HttpGet(uri);
            applyHeaders(request, headers);
            return executeToString(request);
        } catch (URISyntaxException e) {
            throw new APIException(e);
        }
    }

    /**
     * POST JSON payload.
     *
     * @param endpointUrl target URL
     * @param jsonBody    JSON payload (nullable/blank allowed)
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @return response body as String
     * @throws APIException on IO errors or non-2xx responses
     */
    public String postJson(String endpointUrl, String jsonBody, Map<String, String> params, Map<String, String> headers)
            throws APIException {
        try {
            URI uri = buildUri(endpointUrl, params);
            HttpPost request = new HttpPost(uri);
            applyHeaders(request, headers);
            if (jsonBody != null && !jsonBody.isBlank()) {
                request.setEntity(
                        new StringEntity(jsonBody, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
            }
            return executeToString(request);
        } catch (URISyntaxException e) {
            throw new APIException(e);
        }
    }

    /**
     * PUT JSON payload.
     *
     * @param endpointUrl target URL
     * @param jsonBody    JSON payload (nullable/blank allowed)
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @return response body as String
     * @throws APIException on IO errors or non-2xx responses
     */
    public String putJson(String endpointUrl, String jsonBody, Map<String, String> params, Map<String, String> headers)
            throws APIException {
        try {
            URI uri = buildUri(endpointUrl, params);
            HttpPut request = new HttpPut(uri);
            applyHeaders(request, headers);
            if (jsonBody != null && !jsonBody.isBlank()) {
                request.setEntity(
                        new StringEntity(jsonBody, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
            }
            return executeToString(request);
        } catch (URISyntaxException e) {
            throw new APIException(e);
        }
    }

    /**
     * PATCH JSON payload.
     *
     * @param endpointUrl target URL
     * @param jsonBody    JSON payload (nullable/blank allowed)
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @return response body as String
     * @throws APIException on IO errors or non-2xx responses
     */
    public String patchJson(String endpointUrl, String jsonBody, Map<String, String> params,
            Map<String, String> headers)
            throws APIException {
        try {
            URI uri = buildUri(endpointUrl, params);
            HttpPatch request = new HttpPatch(uri);
            applyHeaders(request, headers);
            if (jsonBody != null && !jsonBody.isBlank()) {
                request.setEntity(
                        new StringEntity(jsonBody, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8)));
            }
            return executeToString(request);
        } catch (URISyntaxException e) {
            throw new APIException(e);
        }
    }

    /**
     * DELETE request.
     *
     * @param endpointUrl target URL
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @return response body as String
     * @throws APIException on IO errors or non-2xx responses
     */
    public String delete(String endpointUrl, Map<String, String> params, Map<String, String> headers)
            throws APIException {
        try {
            URI uri = buildUri(endpointUrl, params);
            org.apache.hc.client5.http.classic.methods.HttpDelete request = new org.apache.hc.client5.http.classic.methods.HttpDelete(
                    uri);
            applyHeaders(request, headers);
            return executeToString(request);
        } catch (URISyntaxException e) {
            throw new APIException(e);
        }
    }

    /**
     * HEAD request.
     *
     * @param endpointUrl target URL
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @return HTTP status code
     * @throws APIException on IO errors
     */
    public int head(String endpointUrl, Map<String, String> params, Map<String, String> headers) throws APIException {
        try {
            URI uri = buildUri(endpointUrl, params);
            HttpHead request = new HttpHead(uri);
            applyHeaders(request, headers);
            return executeStatusCode(request);
        } catch (URISyntaxException e) {
            throw new APIException(e);
        }
    }

    /**
     * OPTIONS request.
     *
     * @param endpointUrl target URL
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @return response body as String
     * @throws APIException on IO errors or non-2xx responses
     */
    public String options(String endpointUrl, Map<String, String> params, Map<String, String> headers)
            throws APIException {
        try {
            URI uri = buildUri(endpointUrl, params);
            HttpOptions request = new HttpOptions(uri);
            applyHeaders(request, headers);
            return executeToString(request);
        } catch (URISyntaxException e) {
            throw new APIException(e);
        }
    }

    /**
     * POST application/x-www-form-urlencoded.
     *
     * @param endpointUrl target URL
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @param formData    form key-value pairs (nullable)
     * @return response body as String
     * @throws APIException on IO errors or non-2xx responses
     */
    public String postFormUrlEncoded(String endpointUrl, Map<String, String> params, Map<String, String> headers,
            Map<String, String> formData) throws APIException {
        try {
            URI uri = buildUri(endpointUrl, params);
            HttpPost request = new HttpPost(uri);
            applyHeaders(request, headers);
            List<NameValuePair> pair = new ArrayList<>();
            if (formData != null && !formData.isEmpty()) {
                for (Map.Entry<String, String> entry : formData.entrySet()) {
                    pair.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
            }
            request.setEntity(new UrlEncodedFormEntity(pair, StandardCharsets.UTF_8));
            return executeToString(request);
        } catch (URISyntaxException e) {
            throw new APIException(e);
        }
    }

    /**
     * Multipart upload of a single file under form field name "file".
     *
     * @param endpointUrl target URL
     * @param file        file to upload
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @return response body as String
     * @throws APIException on IO errors or non-2xx responses
     */
    public String upload(String endpointUrl, File file, Map<String, String> params, Map<String, String> headers)
            throws APIException {
        return upload(endpointUrl, Collections.singletonMap("file", file), params, headers);
    }

    /**
     * Multipart upload of multiple files.
     *
     * @param endpointUrl target URL
     * @param files       map of form field name to file
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @return response body as String
     * @throws APIException on IO errors or non-2xx responses
     */
    public String upload(String endpointUrl, Map<String, File> files, Map<String, String> params,
            Map<String, String> headers) throws APIException {
        try {
            URI uri = buildUri(endpointUrl, params);
            HttpPost request = new HttpPost(uri);
            applyHeaders(request, headers);

            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.STRICT);
            if (files != null && !files.isEmpty()) {
                for (Map.Entry<String, File> entry : files.entrySet()) {
                    File file = entry.getValue();
                    if (file != null && file.exists() && file.canRead()) {
                        addFilePart(builder, entry.getKey(), file);
                    }
                }
            }

            request.setEntity(builder.build());
            return executeToString(request);
        } catch (URISyntaxException e) {
            throw new APIException(e);
        }
    }

    /**
     * Close underlying {@link CloseableHttpClient} and free resources.
     */
    @Override
    public void close() throws IOException {
        httpClient.close();
    }

    private static URI buildUri(String endpointUrl, Map<String, String> params) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(endpointUrl);
        if (params != null && !params.isEmpty()) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    builder.addParameter(entry.getKey(), entry.getValue());
                }
            }
        }
        return builder.build();
    }

    private void applyHeaders(ClassicHttpRequest request, Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    request.addHeader(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private String executeToString(ClassicHttpRequest request) throws APIException {
        try {
            return httpClient.execute(request, response -> {
                int code = response.getCode();
                HttpEntity entity = response.getEntity();
                String responseBody = (entity != null) ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";
                if (code >= 200 && code < 300) {
                    return responseBody;
                }
                throw new IOException("HTTP request failed with status code: " + code);
            });
        } catch (IOException e) {
            throw new APIException(e.getMessage(), e);
        }
    }

    private int executeStatusCode(ClassicHttpRequest request) throws APIException {
        try {
            return httpClient.execute(request, HttpResponse::getCode);
        } catch (IOException e) {
            throw new APIException(e.getMessage(), e);
        }
    }

    /**
     * GET request and return response body as bytes.
     *
     * @param endpointUrl target URL
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @return response body bytes (empty if no entity)
     * @throws APIException on IO errors or non-2xx responses
     */
    public byte[] getBytes(String endpointUrl, Map<String, String> params, Map<String, String> headers)
            throws APIException {
        try {
            URI uri = buildUri(endpointUrl, params);
            HttpGet request = new HttpGet(uri);
            applyHeaders(request, headers);
            return executeToBytes(request);
        } catch (URISyntaxException e) {
            throw new APIException(e);
        }
    }

    /**
     * Download via GET directly to a file path (directories created if needed).
     *
     * @param endpointUrl target URL
     * @param params      query parameters (nullable)
     * @param headers     request headers (nullable)
     * @param target      target file path (overwrites if exists)
     * @throws APIException on IO errors or non-2xx responses
     */
    public void downloadToFile(String endpointUrl, Map<String, String> params, Map<String, String> headers, Path target)
            throws APIException {
        try {
            URI uri = buildUri(endpointUrl, params);
            HttpGet request = new HttpGet(uri);
            applyHeaders(request, headers);
            executeToFile(request, target);
        } catch (URISyntaxException e) {
            throw new APIException(e);
        }
    }

    private byte[] executeToBytes(ClassicHttpRequest request) throws APIException {
        try {
            return httpClient.execute(request, response -> {
                int code = response.getCode();
                HttpEntity entity = response.getEntity();
                byte[] bytes = (entity != null) ? EntityUtils.toByteArray(entity) : new byte[0];
                if (code >= 200 && code < 300) {
                    return bytes;
                }
                throw new IOException("HTTP request failed with status code: " + code);
            });
        } catch (IOException e) {
            throw new APIException(e.getMessage(), e);
        }
    }

    private void executeToFile(ClassicHttpRequest request, Path target) throws APIException {
        try {
            httpClient.execute(request, response -> {
                int code = response.getCode();
                HttpEntity entity = response.getEntity();
                if (code >= 200 && code < 300) {
                    writeEntityToFile(entity, target);
                    return null;
                }
                throw new IOException("HTTP request failed with status code: " + code);
            });
        } catch (IOException e) {
            throw new APIException(e.getMessage(), e);
        }
    }

    private void addFilePart(MultipartEntityBuilder builder, String fieldName, File file) {
        // Use file-based overload to avoid closing stream before HttpClient sends the request
        builder.addBinaryBody(fieldName, file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
    }

    private void writeEntityToFile(HttpEntity entity, Path target) throws IOException {
        if (entity == null) {
            return;
        }
        try (InputStream in = entity.getContent()) {
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Checked exception wrapping IO/network errors and non-2xx outcomes.
     */
    public static class APIException extends Exception {

        public APIException(String message) {
            super(message);
        }

        public APIException(String message, Throwable cause) {
            super(message, cause);
        }

        public APIException(Throwable cause) {
            super(cause);
        }

    }

}
