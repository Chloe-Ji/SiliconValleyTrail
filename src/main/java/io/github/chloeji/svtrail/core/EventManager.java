package io.github.chloeji.svtrail.core;

import io.github.chloeji.svtrail.model.Effects;
import io.github.chloeji.svtrail.model.Event;
import io.github.chloeji.svtrail.model.WeatherData;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Holds the pool of random events the player may encounter and returns one at
 * random per call. The {@link Random} source is injectable so tests can drive
 * deterministic sequences.
 * <p>
 * Events may be unconditional (eligible any day) or weather-conditional — the
 * latter only enter the draw when the current {@link WeatherData} matches the
 * event's predicate. This lets bad-weather scenarios (outages, overheating,
 * fog accidents) surface only when the live weather justifies them.
 */
public class EventManager {
    private static final int HOT_WEATHER_THRESHOLD_F = 80;

    private static final Predicate<WeatherData> IS_THUNDERSTORM =
            w -> w.condition() != null && w.condition().toLowerCase().contains("thunder");
    private static final Predicate<WeatherData> IS_HOT =
            w -> w.temperature() > HOT_WEATHER_THRESHOLD_F;
    private static final Predicate<WeatherData> IS_FOGGY =
            w -> w.condition() != null && w.condition().toLowerCase().contains("fog");

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
                        new Effects(8000, 10, 0, -5, 15, 0),
                        new Effects(0, 0, 0, 0, 0, 0)),

                new Event("Server Outage — Your cloud provider is down!",
                        "Emergency all-nighter fix (costs morale, saves reputation)",
                        "Wait until morning (cheaper but hype drops)",
                        new Effects(-1000, -20, -10, -3, 5, -3),
                        new Effects(0, -5, 0, 0, -15, 5)),

                new Event("Hackathon Invitation — A 24-hour hackathon is happening nearby.",
                        "Participate (costs coffee, could boost product)",
                        "Skip it, stay focused on the road",
                        new Effects(2000, -10, 15, -8, 20, 3),
                        new Effects(0, 5, 0, 0, 0, 0)),

                new Event("Talent Poached — A FAANG recruiter is targeting your lead engineer.",
                        "Counter-offer with cash to retain them",
                        "Let them go, hire a junior instead",
                        new Effects(-5000, 10, 0, 0, 0, 0),
                        new Effects(0, -20, -10, 0, -5, 5)),

                new Event("Demo Day Slot — An accelerator offers you a demo slot tomorrow.",
                        "Pull an all-nighter to prep (big upside, ship bugs)",
                        "Decline, keep shipping quietly",
                        new Effects(6000, -10, 0, -5, 20, 3),
                        new Effects(0, 0, 0, 0, 0, 0)),

                new Event("PR Crisis on Twitter — Someone's ranting about your product.",
                        "Publicly apologize and patch fast (morale dip, hype recovers)",
                        "Ignore it and keep building",
                        new Effects(0, -10, 0, 0, 10, 0),
                        new Effects(0, 0, 0, 0, -15, 5)),

                new Event("Mentor Office Hours — A well-known founder has an open slot.",
                        "Attend (morale & compute credits offered)",
                        "Skip to save the day",
                        new Effects(0, 15, 20, 0, 0, 0),
                        new Effects(0, 0, 0, 0, 0, 0)),

                new Event("Press Feature in TechCrunch — A journalist wrote about your startup!",
                        null, null,
                        new Effects(0, 5, 0, 0, 20, 0),
                        new Effects(0, 0, 0, 0, 0, 0)),

                new Event("Nothing eventful today — A quiet day on the road.",
                        null, null,
                        new Effects(0, 0, 0, 0, 0, 0),
                        new Effects(0, 0, 0, 0, 0, 0)),

                // ----- Weather-conditional events -----
                new Event("Power Outage at the Office — Thunderstorm knocked out the grid!",
                        "Move to a café with a generator (spend cash, keep shipping)",
                        "Call it a day, wait it out",
                        new Effects(-1200, -5, 0, -3, 0, 0),
                        new Effects(0, -10, -10, 0, -5, 3),
                        IS_THUNDERSTORM),

                new Event("Server Overheating — Heat wave is spiking data-center temps.",
                        "Pay for emergency cooling (expensive, saves uptime)",
                        "Throttle services (cheap, hype drops)",
                        new Effects(-2000, 0, -5, 0, 0, 0),
                        new Effects(0, -5, 0, 0, -10, 5),
                        IS_HOT),

                new Event("Foggy 101 Accident — A pileup on the freeway snarls traffic.",
                        "Reroute through local streets (costs coffee, preserves schedule)",
                        "Wait it out (morale drops from boredom)",
                        new Effects(0, 0, 0, -6, 0, 0),
                        new Effects(0, -10, 0, 0, 0, 0),
                        IS_FOGGY)
        );
    }

    /**
     * Picks one event at random from the unconditional events in the pool,
     * using a clear-sky {@link WeatherData} as the default filter. Kept for
     * tests and call sites that don't have weather context.
     *
     * @return a non-null {@link Event} drawn from the event pool
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
