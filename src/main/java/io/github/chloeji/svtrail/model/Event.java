package io.github.chloeji.svtrail.model;

/**
 * Immutable random event presented to the player during the game loop.
 * <p>
 * An event with non-null {@code choice1} and {@code choice2} prompts the player
 * to pick one of two outcomes; an event with {@code null} choices is resolved
 * automatically using {@code choice1Effects}.
 *
 * @param description      flavor text describing what happened
 * @param choice1          label of the first choice, or {@code null} for a no-choice event
 * @param choice2          label of the second choice, or {@code null} for a no-choice event
 * @param choice1Effects   resource deltas applied when choice 1 is selected (or automatically)
 * @param choice2Effects   resource deltas applied when choice 2 is selected
 */
public record Event(String description, String choice1, String choice2,
                    Effects choice1Effects, Effects choice2Effects) {}
