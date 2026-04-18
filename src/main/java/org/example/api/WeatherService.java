package org.example.api;
import io.github.cdimascio.dotenv.Dotenv;
import org.example.model.Location;
import org.example.model.WeatherData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Random;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

//In Open-Meteo Doc：
//0 = Clear sky
//1-3 = Partly cloudy
//45-48 = Fog
//51-57 = Drizzle
//61-67 = Rain
//95+ = Thunderstorm


public class WeatherService {
    private final HttpClient httpClient;
    private final Random random;
    private final String apiKey;

    public WeatherService() {
        this(HttpClient.newHttpClient());
    }

    public WeatherService(HttpClient httpClient) {
        this.httpClient = httpClient;
        random = new Random();

        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();
        this.apiKey = dotenv.get("OPENWEATHER_API_KEY");
    }

    public WeatherData fetchWeather(Location location) {
        if (apiKey != null && !apiKey.isEmpty()) {
            WeatherData result = fetchFromOpenWeather(location, apiKey);
            if (result != null) return randomizeWeather(result);
        }
        return randomizeWeather(fallbackWeather());
    }

    // ==========================================
    // OpenWeatherMap (requires API key)
    // ==========================================
    private WeatherData fetchFromOpenWeather(Location location, String apiKey) {
        try {
            String url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?lat=%.4f&lon=%.4f&appid=%s&units=imperial",
                    location.latitude(), location.longitude(), apiKey
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseWeather(response.body());
            }
        } catch (Exception e) {
            System.out.println("⚠️ OpenWeatherMap unavailable, trying fallback...");
        }
        return null;
    }

    private WeatherData randomizeWeather(WeatherData real) {
        Random random = new Random();
        //shift the temp: -10 --- +10
        int tempShift = random.nextInt(21) - 10;
        int adjustedTemp = real.temperature() + tempShift;
        int[] weatherCodes = {800, 800, 801, 802, 701, 300, 500, 501, 601, 200};
        int code = weatherCodes[random.nextInt(weatherCodes.length)];

        String condition = describeWeatherCode(code);
        boolean isBad = code < 700;

        return new WeatherData(condition, adjustedTemp, isBad);
    }
    private WeatherData parseWeather(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        int temp = root.getAsJsonObject("main").get("temp").getAsInt();
        int weatherId = root.getAsJsonArray("weather")
                .get(0).getAsJsonObject().get("id").getAsInt();

        String condition = describeWeatherCode(weatherId);
        boolean isBad = weatherId < 700;

        return new WeatherData(condition, temp, isBad);
    }
    private String describeWeatherCode(int code) {
        if (code >= 200 && code < 300) return "Thunderstorm";
        if (code >= 300 && code < 400) return "Drizzle";
        if (code >= 500 && code < 600) return "Rain";
        if (code >= 600 && code < 700) return "Snow";
        if (code >= 700 && code < 800) return "Foggy";
        if (code == 800) return "Sunny";
        if (code > 800) return "Partly cloudy";
        return "Unknown";
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
