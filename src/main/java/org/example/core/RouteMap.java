package org.example.core;

import org.example.model.Location;

import java.util.*;

public class RouteMap {
    private final List<Location> route;

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
    public Location getLocation(int index) {
        if (index >= 0 && index < route.size()) {
            return route.get(index);
        }
        return null;
    }
    public boolean isDestination(int index) {
        return index >= route.size() - 1;
    }

    // ========================================
    // 以下方法可有可无，之后再决定
    // ========================================
    public int totalStops() {
        return route.size();
    }
    public int distanceBetween(int fromIndex, int toIndex) {
        return route.get(toIndex).milesFromStart() - route.get(fromIndex).milesFromStart();
    }

    public int progressPercent(int index) {
        int totalDistance = route.get(route.size() - 1).milesFromStart();
        return (int) ((route.get(index).milesFromStart() * 100.0) / totalDistance);
    }


}
