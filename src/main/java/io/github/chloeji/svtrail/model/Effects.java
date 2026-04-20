package io.github.chloeji.svtrail.model;

/**
 * Immutable bundle of resource deltas applied by an event outcome.
 * Values can be positive or negative; {@link StartupState#applyEventEffects}
 * is responsible for clamping the resulting resource values.
 *
 * @param cash     change in cash balance
 * @param morale   change in team morale
 * @param compute  change in compute credits
 * @param coffee   change in coffee stock
 */
public record Effects(int cash, int morale, int compute, int coffee) {}
