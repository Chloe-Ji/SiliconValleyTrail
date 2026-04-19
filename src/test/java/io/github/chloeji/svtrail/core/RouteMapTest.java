package io.github.chloeji.svtrail.core;

import io.github.chloeji.svtrail.model.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RouteMapTest {
    private RouteMap routeMap;

    @BeforeEach
    void setUp() {
        routeMap = new RouteMap();
    }

    // ==========================================
    // Basic Structure
    // ==========================================

    @Test
    void hasTenLocations() {
        assertEquals(10, routeMap.totalStops());
    }

    @Test
    void firstLocation_isSanJose() {
        assertEquals("San Jose", routeMap.getLocation(0).name());
    }

    @Test
    void lastLocation_isSanFrancisco() {
        assertEquals("San Francisco", routeMap.getLocation(9).name());
    }

    // ==========================================
    // Destination Check
    // ==========================================

    @Test
    void isDestination_atLastIndex() {
        assertTrue(routeMap.isDestination(9));
    }

    @Test
    void isNotDestination_atFirstIndex() {
        assertFalse(routeMap.isDestination(0));
    }

    @Test
    void isNotDestination_atMiddleIndex() {
        assertFalse(routeMap.isDestination(5));
    }

    // ==========================================
    // Progress
    // ==========================================

    @Test
    void progressPercent_atStart_isZero() {
        assertEquals(0, routeMap.progressPercent(0));
    }

    @Test
    void progressPercent_atEnd_is100() {
        assertEquals(100, routeMap.progressPercent(9));
    }

    @Test
    void progressPercent_atMiddle_isBetween() {
        int progress = routeMap.progressPercent(5);
        assertTrue(progress > 0 && progress < 100);
    }

    // ==========================================
    // Distance
    // ==========================================

    @Test
    void distanceBetween_adjacentStops_isPositive() {
        int distance = routeMap.distanceBetween(0, 1);
        assertTrue(distance > 0);
    }

    @Test
    void distanceBetween_startAndEnd_isTotal() {
        int total = routeMap.distanceBetween(0, 9);
        assertTrue(total > 0);
    }

    @Test
    void distanceBetween_sameStop_isZero() {
        assertEquals(0, routeMap.distanceBetween(0, 0));
    }
}
