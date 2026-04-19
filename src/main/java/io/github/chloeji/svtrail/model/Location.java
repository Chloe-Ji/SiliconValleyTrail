package io.github.chloeji.svtrail.model;

/**
 * Immutable point on the route from San Jose to San Francisco.
 *
 * @param name            human-readable city name
 * @param milesFromStart  cumulative distance from the starting location
 * @param latitude        geographic latitude in degrees
 * @param longitude       geographic longitude in degrees
 */
public record Location(String name, int milesFromStart, double latitude, double longitude) {}
