package io.github.chloeji.svtrail.model;

/**
 * Immutable route snapshot returned by the mapping service for a single leg
 * of travel between two {@link Location}s.
 * <p>
 * {@code isHeavyTraffic} is derived from the ratio of traffic-aware duration
 * to free-flow duration and drives the "stuck in traffic" gameplay event.
 *
 * @param miles            real driving distance in miles
 * @param trafficMinutes   current traffic-aware driving duration in minutes
 * @param freeFlowMinutes  baseline driving duration without traffic in minutes
 * @param isHeavyTraffic   {@code true} when current traffic significantly exceeds free-flow
 */
public record RouteInfo(double miles, int trafficMinutes, int freeFlowMinutes, boolean isHeavyTraffic) {}
