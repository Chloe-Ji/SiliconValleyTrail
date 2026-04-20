package io.github.chloeji.svtrail.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.chloeji.svtrail.model.Location;
import io.github.chloeji.svtrail.model.WeatherData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Random;

/**
 * Fetches current weather for a {@link Location} from the Open-Meteo API and
 * maps the raw response onto the game's {@link WeatherData} model.
 * <p>
 * Open-Meteo is used because it requires no API key and imposes generous rate
 * limits for non-commercial use; the two-layer degradation below keeps the
 * game playable even when the network is unavailable.
 * <p>
 * The returned weather is deliberately perturbed (temperature jitter and
 * occasional condition substitution) because the ten Silicon Valley stops are
 * physically close enough that real weather would be nearly identical across
 * the whole route; the jitter gives travel decisions meaningful variance.
 * <p>
 * Two-layer graceful degradation:
 * <ol>
 *   <li>Call Open-Meteo; on any failure (network, non-2xx, malformed JSON) fall back.</li>
 *   <li>Fall back to a randomized mock weather pool.</li>
 * </ol>
 * <p>
 * Open-Meteo uses WMO weather interpretation codes:
 * 0 = clear, 1-3 = mainly clear / partly cloudy / overcast,
 * 45-48 = fog, 51-57 = drizzle, 61-67 = rain, 71-77 = snow,
 * 80-82 = rain showers, 85-86 = snow showers, 95-99 = thunderstorm.
 */
public class WeatherService {
    private static final int HTTP_OK = 200;

    // Temperature is randomized uniformly within [-TEMP_JITTER, +TEMP_JITTER]
    // degrees Fahrenheit to produce variety between closely-spaced locations.
    private static final int TEMP_JITTER = 10;
    private static final int TEMP_JITTER_RANGE = TEMP_JITTER * 2 + 1;

    // WMO codes 51 and above (drizzle, rain, snow, thunderstorm) count as
    // bad weather for travel penalties. 0-3 (clear/cloudy) and 45-48 (fog)
    // do not penalize travel.
    private static final int BAD_WEATHER_CODE_FLOOR = 51;

    // Weighted pool of WMO codes used by randomizeWeather. Clear-sky codes
    // (0) appear twice so sunny days stay the most common outcome.
    private static final int[] WEIGHTED_WMO_CODES =
            {0, 0, 1, 2, 3, 45, 51, 61, 63, 71, 95};

    private final HttpClient httpClient;
    private final Random random;

    /**
     * Creates a service backed by the default {@link HttpClient}.
     */
    public WeatherService() {
        this(HttpClient.newHttpClient());
    }

    /**
     * Creates a service with a caller-supplied {@link HttpClient}, used by
     * tests to stub out network calls.
     *
     * @param httpClient the HTTP client used to call Open-Meteo
     */
    public WeatherService(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.random = new Random();
    }

    /**
     * Fetches weather for the given location from Open-Meteo, falling back to
     * randomized mock data on any failure. Always returns a non-null value;
     * the game never crashes from network failures.
     *
     * @param location the location to look up
     * @return a non-null {@link WeatherData}
     */
    public WeatherData fetchWeather(Location location) {
        WeatherData result = fetchFromOpenMeteo(location);
        if (result != null) return randomizeWeather(result);
        return randomizeWeather(fallbackWeather());
    }

    // ==========================================
    // Open-Meteo (no API key required)
    // ==========================================

    private WeatherData fetchFromOpenMeteo(Location location) {
        try {
            // Pin Locale.ROOT so non-English locales don't render doubles with
            // comma separators and break the URL.
            String url = String.format(Locale.ROOT,
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f"
                            + "&current=temperature_2m,weather_code&temperature_unit=fahrenheit",
                    location.latitude(), location.longitude()
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == HTTP_OK) {
                return parseWeather(response.body());
            }
        } catch (Exception e) {
            // Network failure, malformed JSON, interrupted send, etc. —
            // degrade gracefully to the randomized fallback so the game
            // keeps running. The specific cause is not actionable for the
            // player, so we only surface a short notice.
            System.out.println("⚠️ Open-Meteo unavailable, using mock weather...");
        }
        return null;
    }

    private WeatherData randomizeWeather(WeatherData real) {
        int tempShift = random.nextInt(TEMP_JITTER_RANGE) - TEMP_JITTER;
        int adjustedTemp = real.temperature() + tempShift;
        int code = WEIGHTED_WMO_CODES[random.nextInt(WEIGHTED_WMO_CODES.length)];

        String condition = describeWeatherCode(code);
        boolean isBad = code >= BAD_WEATHER_CODE_FLOOR;

        return new WeatherData(condition, adjustedTemp, isBad);
    }

    private WeatherData parseWeather(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonObject current = root.getAsJsonObject("current");
        int temp = current.get("temperature_2m").getAsInt();
        int weatherCode = current.get("weather_code").getAsInt();

        String condition = describeWeatherCode(weatherCode);
        boolean isBad = weatherCode >= BAD_WEATHER_CODE_FLOOR;

        return new WeatherData(condition, temp, isBad);
    }

    private String describeWeatherCode(int code) {
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75, 77 -> "Snow";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Unknown";
        };
    }

    private WeatherData fallbackWeather() {
        WeatherData[] fallbacks = {
                new WeatherData("Sunny", 75, false),
                new WeatherData("Partly cloudy", 65, false),
                new WeatherData("Foggy", 58, false),
                new WeatherData("Rain", 52, true),
                new WeatherData("Drizzle", 55, true),
                new WeatherData("Thunderstorm", 48, true)
        };
        return fallbacks[random.nextInt(fallbacks.length)];
    }
}
