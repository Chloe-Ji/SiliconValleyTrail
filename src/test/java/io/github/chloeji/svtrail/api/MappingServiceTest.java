package io.github.chloeji.svtrail.api;

import io.github.chloeji.svtrail.model.Location;
import io.github.chloeji.svtrail.model.RouteInfo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

public class MappingServiceTest {

    private static final Location SAN_JOSE = new Location("San Jose", 0, 37.3382, -121.8863);
    private static final Location SANTA_CLARA = new Location("Santa Clara", 5, 37.3541, -121.9552);

    // ==========================================
    // isConfigured
    // ==========================================

    @Test
    void isConfigured_falseWhenTokenNull() {
        MappingService service = new MappingService(HttpClient.newHttpClient(), null);
        assertFalse(service.isConfigured());
    }

    @Test
    void isConfigured_falseWhenTokenBlank() {
        MappingService service = new MappingService(HttpClient.newHttpClient(), "   ");
        assertFalse(service.isConfigured());
    }

    @Test
    void isConfigured_trueWhenTokenPresent() {
        MappingService service = new MappingService(HttpClient.newHttpClient(), "pk.abc123");
        assertTrue(service.isConfigured());
    }

    // ==========================================
    // getRouteInfo - unconfigured
    // ==========================================

    @Test
    void getRouteInfo_returnsNullWhenUnconfigured() {
        MappingService service = new MappingService(new AlwaysFailingHttpClient(), null);
        assertNull(service.getRouteInfo(SAN_JOSE, SANTA_CLARA));
    }

    @Test
    void getRouteInfo_doesNotCallHttpWhenUnconfigured() {
        CountingHttpClient client = new CountingHttpClient("{}", 200);
        MappingService service = new MappingService(client, "   ");
        service.getRouteInfo(SAN_JOSE, SANTA_CLARA);
        assertEquals(0, client.callCount, "Should skip network entirely when token is blank");
    }

    // ==========================================
    // getRouteInfo - happy path
    // ==========================================

    @Test
    void getRouteInfo_parsesDistanceAndDuration() {
        String body = "{\"routes\":[{\"distance\":8046.72,\"duration\":600}]}";
        MappingService service = new MappingService(new StubHttpClient(body, 200), "pk.test");

        RouteInfo info = service.getRouteInfo(SAN_JOSE, SANTA_CLARA);

        assertNotNull(info);
        assertEquals(5.0, info.miles(), 0.1);
        assertEquals(10, info.trafficMinutes());
    }

    @Test
    void getRouteInfo_flagsHeavyTrafficWhenRatioExceedsThreshold() {
        // Both HTTP calls return the same body in the stub, so both durations
        // come from the same JSON. We vary the response per call via a
        // sequence stub for this test.
        String trafficBody = "{\"routes\":[{\"distance\":8046.72,\"duration\":1200}]}"; // 20 min
        String freeFlowBody = "{\"routes\":[{\"distance\":8046.72,\"duration\":600}]}";  // 10 min
        MappingService service = new MappingService(
                new SequenceHttpClient(trafficBody, freeFlowBody, trafficBody), "pk.test");

        RouteInfo info = service.getRouteInfo(SAN_JOSE, SANTA_CLARA);

        assertNotNull(info);
        assertTrue(info.isHeavyTraffic(),
                "20-minute traffic vs 10-minute free-flow should flag as heavy");
    }

    @Test
    void getRouteInfo_doesNotFlagLightTraffic() {
        String body = "{\"routes\":[{\"distance\":8046.72,\"duration\":600}]}";
        MappingService service = new MappingService(new StubHttpClient(body, 200), "pk.test");

        RouteInfo info = service.getRouteInfo(SAN_JOSE, SANTA_CLARA);

        assertNotNull(info);
        assertFalse(info.isHeavyTraffic());
    }

    // ==========================================
    // getRouteInfo - failure paths
    // ==========================================

    @Test
    void getRouteInfo_returnsNullOnNon200Status() {
        MappingService service = new MappingService(new StubHttpClient("Server error", 500), "pk.test");
        assertNull(service.getRouteInfo(SAN_JOSE, SANTA_CLARA));
    }

    @Test
    void getRouteInfo_returnsNullOnNetworkFailure() {
        MappingService service = new MappingService(new AlwaysFailingHttpClient(), "pk.test");
        assertNull(service.getRouteInfo(SAN_JOSE, SANTA_CLARA));
    }

    @Test
    void getRouteInfo_returnsNullOnMalformedJson() {
        MappingService service = new MappingService(new StubHttpClient("not-json", 200), "pk.test");
        assertNull(service.getRouteInfo(SAN_JOSE, SANTA_CLARA));
    }

    @Test
    void getRouteInfo_returnsNullOnEmptyRoutes() {
        MappingService service = new MappingService(new StubHttpClient("{\"routes\":[]}", 200), "pk.test");
        assertNull(service.getRouteInfo(SAN_JOSE, SANTA_CLARA));
    }

    // ==========================================
    // Test doubles
    // ==========================================

    /** HttpClient that returns the same canned response on every send call. */
    private static class StubHttpClient extends HttpClient {
        private final String body;
        private final int status;

        StubHttpClient(String body, int status) {
            this.body = body;
            this.status = status;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> handler) throws IOException, InterruptedException {
            return new FakeResponse<>(body, status);
        }

        @Override public java.util.Optional<java.net.CookieHandler> cookieHandler() { return java.util.Optional.empty(); }
        @Override public java.util.Optional<java.time.Duration> connectTimeout() { return java.util.Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public java.util.Optional<java.net.ProxySelector> proxy() { return java.util.Optional.empty(); }
        @Override public javax.net.ssl.SSLContext sslContext() { return null; }
        @Override public javax.net.ssl.SSLParameters sslParameters() { return null; }
        @Override public java.util.Optional<java.net.Authenticator> authenticator() { return java.util.Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public java.util.Optional<java.util.concurrent.Executor> executor() { return java.util.Optional.empty(); }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> handler) { return null; }
        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, BodyHandler<T> handler, PushPromiseHandler<T> pushHandler) { return null; }
    }

    /** Returns bodies in sequence across successive send() calls. */
    private static class SequenceHttpClient extends StubHttpClient {
        private final String[] bodies;
        private int index = 0;

        SequenceHttpClient(String... bodies) {
            super(bodies[0], 200);
            this.bodies = bodies;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> handler) throws IOException, InterruptedException {
            String body = bodies[Math.min(index++, bodies.length - 1)];
            return new FakeResponse<>(body, 200);
        }
    }

    /** Tracks how many times send() was invoked. */
    private static class CountingHttpClient extends StubHttpClient {
        int callCount = 0;

        CountingHttpClient(String body, int status) {
            super(body, status);
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> handler) throws IOException, InterruptedException {
            callCount++;
            return super.send(request, handler);
        }
    }

    /** HttpClient that always throws on send. */
    private static class AlwaysFailingHttpClient extends StubHttpClient {
        AlwaysFailingHttpClient() {
            super("", 0);
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> handler) throws IOException, InterruptedException {
            throw new IOException("simulated network failure");
        }
    }

    /** Minimal HttpResponse that only exposes statusCode() and body(). */
    @SuppressWarnings("unchecked")
    private static class FakeResponse<T> implements HttpResponse<T> {
        private final String body;
        private final int status;

        FakeResponse(String body, int status) {
            this.body = body;
            this.status = status;
        }

        @Override public int statusCode() { return status; }
        @Override public T body() { return (T) body; }
        @Override public HttpRequest request() { return null; }
        @Override public java.util.Optional<HttpResponse<T>> previousResponse() { return java.util.Optional.empty(); }
        @Override public java.net.http.HttpHeaders headers() { return java.net.http.HttpHeaders.of(java.util.Map.of(), (a, b) -> true); }
        @Override public java.util.Optional<javax.net.ssl.SSLSession> sslSession() { return java.util.Optional.empty(); }
        @Override public java.net.URI uri() { return null; }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
