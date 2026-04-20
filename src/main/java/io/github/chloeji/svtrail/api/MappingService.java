package io.github.chloeji.svtrail.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.chloeji.svtrail.model.Location;
import io.github.chloeji.svtrail.model.RouteInfo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Calls the Mapbox Directions API to get real driving distance and
 * traffic-aware duration for a single leg of the route. The response drives
 * the "heavy traffic" banner and the long-leg surcharge in the game loop.
 * <p>
 * The service resolves its Mapbox access token from two sources, in order:
 * <ol>
 *   <li>the {@code MAPBOX_TOKEN} environment variable (preferred — standard for CI and shells);</li>
 *   <li>a {@code MAPBOX_TOKEN=} line inside a {@code .env} file at the project
 *       root (convenience for developers and reviewers who prefer the dotenv
 *       workflow — this file is git-ignored).</li>
 * </ol>
 * When both are missing or blank the service disables itself —
 * {@link #getRouteInfo} returns {@code null} without making any network call
 * and the game falls back to the flat base travel cost. This keeps the code
 * path fully playable for anyone who clones the repo without a Mapbox account.
 * <p>
 * Two HTTP calls are issued per leg: one on the {@code driving-traffic}
 * profile (current traffic) and one on {@code driving} (free-flow baseline).
 * A ratio &gt; 1.5 flags the leg as heavy traffic.
 */
public class MappingService {
    private static final int HTTP_OK = 200;
    private static final double METERS_PER_MILE = 1609.344;
    private static final double HEAVY_TRAFFIC_RATIO = 1.5;

    private static final Path DEFAULT_DOTENV_PATH = Path.of(".env");

    private final HttpClient httpClient;
    private final String accessToken;

    /**
     * Creates a service backed by the default {@link HttpClient}. The Mapbox
     * token is resolved from {@code MAPBOX_TOKEN} in the environment, and
     * failing that, from a {@code MAPBOX_TOKEN=} entry in {@code .env} at the
     * project root.
     */
    public MappingService() {
        this(HttpClient.newHttpClient(), resolveToken(DEFAULT_DOTENV_PATH));
    }

    /**
     * Creates a service with caller-supplied HTTP client and token. Used by
     * tests to stub the network and inject controlled token values.
     *
     * @param httpClient  the HTTP client used to call Mapbox
     * @param accessToken the Mapbox access token; {@code null} or blank disables the service
     */
    public MappingService(HttpClient httpClient, String accessToken) {
        this.httpClient = httpClient;
        this.accessToken = accessToken;
    }

    /**
     * @return {@code true} when a non-blank Mapbox token is available
     */
    public boolean isConfigured() {
        return accessToken != null && !accessToken.isBlank();
    }

    /**
     * Fetches distance and traffic-aware duration for a single leg.
     *
     * @param from origin location
     * @param to   destination location
     * @return populated {@link RouteInfo}, or {@code null} when the service
     *         is unconfigured or the API call fails
     */
    public RouteInfo getRouteInfo(Location from, Location to) {
        if (!isConfigured()) {
            return null;
        }
        try {
            int trafficSeconds = fetchDurationSeconds(from, to, "driving-traffic");
            int freeFlowSeconds = fetchDurationSeconds(from, to, "driving");
            double meters = fetchDistanceMeters(from, to);

            double miles = meters / METERS_PER_MILE;
            int trafficMinutes = Math.max(1, trafficSeconds / 60);
            int freeFlowMinutes = Math.max(1, freeFlowSeconds / 60);
            boolean isHeavy = trafficMinutes > freeFlowMinutes * HEAVY_TRAFFIC_RATIO;

            return new RouteInfo(miles, trafficMinutes, freeFlowMinutes, isHeavy);
        } catch (Exception e) {
            System.out.println("⚠️ Mapbox unavailable, using default travel cost...");
            return null;
        }
    }

    private int fetchDurationSeconds(Location from, Location to, String profile) throws IOException, InterruptedException {
        JsonObject route = firstRoute(callMapbox(from, to, profile));
        return route.get("duration").getAsInt();
    }

    private double fetchDistanceMeters(Location from, Location to) throws IOException, InterruptedException {
        JsonObject route = firstRoute(callMapbox(from, to, "driving-traffic"));
        return route.get("distance").getAsDouble();
    }

    private String callMapbox(Location from, Location to, String profile) throws IOException, InterruptedException {
        // Pin Locale.ROOT so non-English locales (e.g. de_DE) don't format
        // doubles with comma separators and break the URL query.
        String url = String.format(Locale.ROOT,
                "https://api.mapbox.com/directions/v5/mapbox/%s/%f,%f;%f,%f"
                        + "?access_token=%s&geometries=geojson&overview=false",
                profile,
                from.longitude(), from.latitude(),
                to.longitude(), to.latitude(),
                accessToken);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != HTTP_OK) {
            throw new RuntimeException("Mapbox returned " + response.statusCode());
        }
        return response.body();
    }

    private JsonObject firstRoute(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonArray routes = root.getAsJsonArray("routes");
        if (routes == null || routes.isEmpty()) {
            throw new RuntimeException("Mapbox returned no routes");
        }
        return routes.get(0).getAsJsonObject();
    }

    /**
     * Resolves the Mapbox token. Prefers the {@code MAPBOX_TOKEN} environment
     * variable; falls back to reading a {@code MAPBOX_TOKEN=} line from the
     * supplied dotenv file. Package-private so tests can inject a controlled
     * dotenv path.
     *
     * @param dotenvPath path to a dotenv file (conventionally {@code .env})
     * @return the resolved token, or {@code null} if neither source yields one
     */
    static String resolveToken(Path dotenvPath) {
        String fromEnv = System.getenv("MAPBOX_TOKEN");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        return readTokenFromDotEnv(dotenvPath);
    }

    /**
     * Parses a dotenv-style file for a {@code MAPBOX_TOKEN=} entry. Blank
     * lines and {@code #} comment lines are ignored. Values may be wrapped
     * in single or double quotes; surrounding quotes are stripped. Returns
     * {@code null} on missing file, I/O error, or missing key.
     */
    static String readTokenFromDotEnv(Path dotenvPath) {
        if (dotenvPath == null || !Files.exists(dotenvPath)) {
            return null;
        }
        try {
            for (String raw : Files.readAllLines(dotenvPath)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                if (!"MAPBOX_TOKEN".equals(line.substring(0, eq).trim())) continue;
                String value = line.substring(eq + 1).trim();
                return stripQuotes(value);
            }
        } catch (IOException ignored) {
            // Missing file would return via exists() above; any other I/O error
            // is non-fatal — the game keeps running without Mapbox.
        }
        return null;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
