package io.github.chloeji.svtrail.core;

import io.github.chloeji.svtrail.model.Effects;
import io.github.chloeji.svtrail.model.Event;
import io.github.chloeji.svtrail.model.WeatherData;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Holds the pool of random events the player may encounter and returns one at
 * random per call. The {@link Random} source is injectable so tests can drive
 * deterministic sequences.
 * <p>
 * Events may be unconditional (eligible any day) or weather-conditional — the
 * latter only enter the draw when the current {@link WeatherData} matches the
 * event's predicate. This lets bad-weather scenarios (outages, fog accidents)
 * surface only when the live weather justifies them.
 */
public class EventManager {
    // Match tokens gate weather-conditional events against the human-readable
    // condition string from WeatherService.describeWeatherCode. Kept as named
    // constants so the predicates aren't coupled to raw magic strings.
    private static final String THUNDER_TOKEN = "thunder";
    private static final String FOG_TOKEN = "fog";

    private static final Predicate<WeatherData> IS_THUNDERSTORM =
            w -> containsToken(w.condition(), THUNDER_TOKEN);
    private static final Predicate<WeatherData> IS_FOGGY =
            w -> containsToken(w.condition(), FOG_TOKEN);

    /**
     * Case-insensitive substring match with {@link Locale#ROOT} so locales
     * like Turkish (which lowercases {@code I} to {@code ı}) don't break the
     * weather filter.
     */
    private static boolean containsToken(String condition, String token) {
        return condition != null && condition.toLowerCase(Locale.ROOT).contains(token);
    }

    private final List<Event> eventPool;
    private final Random random;

    /**
     * Creates an event manager backed by a fresh, unseeded {@link Random}.
     * Intended for production use where full randomness is desired.
     */
    public EventManager() {
        this(new Random());
    }

    /**
     * Creates an event manager with a caller-supplied random source. Intended
     * for tests that need deterministic event sequences.
     *
     * @param random the random source used to pick events
     */
    public EventManager(Random random) {
        this.random = random;
        eventPool = List.of(
                // ----- Unconditional events -----
                new Event("VC Pitch Opportunity — A VC firm wants to hear your pitch!",
                        "Prepare and pitch (risky but big reward)",
                        "Decline politely (safe)",
                        new Effects(8000, 10, 0, -5),
                        new Effects(0, 0, 0, 0)),

                new Event("Server Outage — Your cloud provider is down!",
                        "Emergency all-nighter fix (costs morale, saves compute)",
                        "Wait until morning (cheaper but team is frustrated)",
                        new Effects(-1000, -20, -10, -3),
                        new Effects(0, -5, 0, 0)),

                new Event("Talent Poached — A FAANG recruiter is targeting your lead engineer.",
                        "Counter-offer with cash to retain them",
                        "Let them go, hire a junior instead",
                        new Effects(-5000, 10, 0, 0),
                        new Effects(0, -20, -10, 0)),

                new Event("Mentor Office Hours — A well-known founder has an open slot.",
                        "Attend (morale & compute credits offered)",
                        "Skip to save the day",
                        new Effects(0, 15, 20, 0),
                        new Effects(0, 0, 0, 0)),

                new Event("Nothing eventful today — A quiet day on the road.",
                        null, null,
                        new Effects(0, 0, 0, 0),
                        new Effects(0, 0, 0, 0)),

                // ----- Weather-conditional events -----
                new Event("Power Outage at the Office — Thunderstorm knocked out the grid!",
                        "Move to a café with a generator (spend cash, keep shipping)",
                        "Call it a day, wait it out",
                        new Effects(-1200, -5, 0, -3),
                        new Effects(0, -10, -10, 0),
                        IS_THUNDERSTORM),

                new Event("Foggy 101 Accident — A pileup on the freeway snarls traffic.",
                        "Reroute through local streets (costs coffee, preserves schedule)",
                        "Wait it out (morale drops from boredom)",
                        new Effects(0, 0, 0, -6),
                        new Effects(0, -10, 0, 0),
                        IS_FOGGY)
        );
    }

    /**
     * Convenience overload equivalent to {@link #getRandomEvent(WeatherData)}
     * called with a clear-sky {@link WeatherData} — thunderstorm and foggy
     * conditional events are therefore ineligible. Kept for tests and call
     * sites that don't have weather context.
     *
     * @return a non-null {@link Event} drawn from the unconditional subset of the pool
     */
    public Event getRandomEvent() {
        return getRandomEvent(new WeatherData("Clear sky", 70, false));
    }

    /**
     * Picks one event at random from the subset of the pool eligible under
     * the supplied weather. An event is eligible if its condition is
     * {@code null} or if its condition returns {@code true} for
     * {@code weather}.
     *
     * @param weather the current weather snapshot
     * @return a non-null {@link Event} drawn from the eligible subset
     */
    public Event getRandomEvent(WeatherData weather) {
        List<Event> eligible = eventPool.stream()
                .filter(e -> e.condition() == null || e.condition().test(weather))
                .toList();
        return eligible.get(random.nextInt(eligible.size()));
    }
}
