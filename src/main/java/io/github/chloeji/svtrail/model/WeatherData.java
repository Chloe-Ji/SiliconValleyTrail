package io.github.chloeji.svtrail.model;

/**
 * Immutable weather snapshot for a location.
 *
 * @param condition      human-readable description (e.g. "Sunny", "Rain")
 * @param temperature    temperature in degrees Fahrenheit
 * @param isBadWeather   {@code true} if the condition incurs travel penalties
 */
public record WeatherData(String condition, int temperature, boolean isBadWeather) {}
