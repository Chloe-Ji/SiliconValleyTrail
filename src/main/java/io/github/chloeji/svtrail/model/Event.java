package io.github.chloeji.svtrail.model;

import java.util.function.Predicate;

/**
 * Immutable random event presented to the player during the game loop.
 * <p>
 * An event with non-null {@code choice1} and {@code choice2} prompts the player
 * to pick one of two outcomes; an event with {@code null} choices is resolved
 * automatically using {@code choice1Effects}.
 * <p>
 * When {@code condition} is non-null the event is only eligible when the
 * predicate returns {@code true} for the current weather — this gates the
 * weather-conditional events such as "Server Overheating" (hot days) or
 * "Power Outage" (thunderstorms).
 *
 * @param description      flavor text describing what happened
 * @param choice1          label of the first choice, or {@code null} for a no-choice event
 * @param choice2          label of the second choice, or {@code null} for a no-choice event
 * @param choice1Effects   resource deltas applied when choice 1 is selected (or automatically)
 * @param choice2Effects   resource deltas applied when choice 2 is selected
 * @param condition        optional weather predicate gating event eligibility; {@code null} = always eligible
 */
public record Event(String description, String choice1, String choice2,
                    Effects choice1Effects, Effects choice2Effects,
                    Predicate<WeatherData> condition) {

    /**
     * Convenience constructor for unconditional events. Equivalent to passing
     * {@code null} as the {@code condition}, meaning the event is always
     * eligible regardless of current weather.
     *
     * @param description    flavor text describing what happened
     * @param choice1        label of the first choice, or {@code null} for a no-choice event
     * @param choice2        label of the second choice, or {@code null} for a no-choice event
     * @param choice1Effects resource deltas applied when choice 1 is selected (or automatically)
     * @param choice2Effects resource deltas applied when choice 2 is selected
     */
    public Event(String description, String choice1, String choice2,
                 Effects choice1Effects, Effects choice2Effects) {
        this(description, choice1, choice2, choice1Effects, choice2Effects, null);
    }
}
