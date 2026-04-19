package io.github.chloeji.svtrail.core;

import io.github.chloeji.svtrail.model.Location;

import java.util.List;

/**
 * Fixed, ordered list of the ten real Silicon Valley stops the player travels
 * through, from San Jose (start) to San Francisco (destination). Also provides
 * derived queries such as progress percentage and inter-stop distance.
 */
public class RouteMap {
    private final List<Location> route;

    /**
     * Builds the route with the default ten San Jose → San Francisco stops.
     */
    public RouteMap() {
        route = List.of(
                new Location("San Jose",              0, 37.3382, -121.8863),
                new Location("Santa Clara",           5, 37.3541, -121.9552),
                new Location("Sunnyvale",            11, 37.3688, -122.0363),
                new Location("Mountain View",        15, 37.3861, -122.0839),
                new Location("Palo Alto",            20, 37.4419, -122.1430),
                new Location("Redwood City",         27, 37.4852, -122.2364),
                new Location("San Mateo",            34, 37.5630, -122.3255),
                new Location("South San Francisco",  40, 37.6547, -122.4077),
                new Location("Daly City",            44, 37.6879, -122.4702),
                new Location("San Francisco",        48, 37.7749, -122.4194)
        );
    }

    /**
     * Returns the location at the given index, or {@code null} if the index is
     * out of range.
     *
     * @param index zero-based index into the route
     * @return the {@link Location} or {@code null} when {@code index} is invalid
     */
    public Location getLocation(int index) {
        if (index >= 0 && index < route.size()) {
            return route.get(index);
        }
        return null;
    }

    /**
     * @param index zero-based index into the route
     * @return {@code true} if the given index is at or past the final stop
     */
    public boolean isDestination(int index) {
        return index >= route.size() - 1;
    }

    /**
     * @return total number of stops on the route
     */
    public int totalStops() {
        return route.size();
    }

    /**
     * Computes the straight-line mileage between two stops on the route, using
     * their cumulative {@code milesFromStart}.
     *
     * @param fromIndex origin index
     * @param toIndex   destination index
     * @return miles from {@code fromIndex} to {@code toIndex} (may be negative)
     */
    public int distanceBetween(int fromIndex, int toIndex) {
        return route.get(toIndex).milesFromStart() - route.get(fromIndex).milesFromStart();
    }

    /**
     * Returns progress to the final destination as an integer percentage.
     *
     * @param index current position index on the route
     * @return progress in [0, 100]
     */
    public int progressPercent(int index) {
        int totalDistance = route.get(route.size() - 1).milesFromStart();
        return (int) ((route.get(index).milesFromStart() * 100.0) / totalDistance);
    }
}
