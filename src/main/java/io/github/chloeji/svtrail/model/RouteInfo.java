package io.github.chloeji.svtrail.model;

/**
 * Immutable route snapshot returned by the mapping service for a single leg
 * of travel between two {@link Location}s.
 * <p>
 * {@code heavyTraffic} is derived from the ratio of traffic-aware duration
 * to free-flow duration and drives the "stuck in traffic" gameplay event.
 *
 * @param trafficMinutes   current traffic-aware driving duration in minutes
 * @param freeFlowMinutes  baseline driving duration without traffic in minutes
 * @param heavyTraffic     {@code true} when current traffic significantly exceeds free-flow
 */
public record RouteInfo(int trafficMinutes, int freeFlowMinutes, boolean heavyTraffic) {}
