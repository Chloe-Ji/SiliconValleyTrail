package io.github.chloeji.svtrail.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.chloeji.svtrail.model.Location;
import io.github.chloeji.svtrail.model.RouteInfo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Calls the Mapbox Directions API to get real driving distance and
 * traffic-aware duration for a single leg of the route. The response drives
 * the "heavy traffic" banner and the long-leg surcharge in the game loop.
 * <p>
 * The service requires a Mapbox access token supplied via the
 * {@code MAPBOX_TOKEN} environment variable. When the token is missing or
 * blank the service disables itself — {@link #getRouteInfo} returns
 * {@code null} without making any network call and the game falls back to
 * the flat base travel cost. This keeps the code path fully playable for
 * anyone who clones the repo without a Mapbox account.
 * <p>
 * Two HTTP calls are issued per leg: one on the {@code driving-traffic}
 * profile (current traffic) and one on {@code driving} (free-flow baseline).
 * A ratio &gt; 1.5 flags the leg as heavy traffic.
 */
public class MappingService {
    private static final int HTTP_OK = 200;
    private static final double METERS_PER_MILE = 1609.344;
    private static final double HEAVY_TRAFFIC_RATIO = 1.5;

    private final HttpClient httpClient;
    private final String accessToken;

    /**
     * Creates a service backed by the default {@link HttpClient} and reads
     * the Mapbox token from the {@code MAPBOX_TOKEN} environment variable.
     */
    public MappingService() {
        this(HttpClient.newHttpClient(), System.getenv("MAPBOX_TOKEN"));
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

    private int fetchDurationSeconds(Location from, Location to, String profile) throws Exception {
        JsonObject route = firstRoute(callMapbox(from, to, profile));
        return route.get("duration").getAsInt();
    }

    private double fetchDistanceMeters(Location from, Location to) throws Exception {
        JsonObject route = firstRoute(callMapbox(from, to, "driving-traffic"));
        return route.get("distance").getAsDouble();
    }

    private String callMapbox(Location from, Location to, String profile) throws Exception {
        String url = String.format(
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
}
