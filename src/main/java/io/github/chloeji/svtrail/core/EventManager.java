package io.github.chloeji.svtrail.core;

import io.github.chloeji.svtrail.model.Effects;
import io.github.chloeji.svtrail.model.Event;

import java.util.List;
import java.util.Random;

/**
 * Holds the pool of random events the player may encounter and returns one at
 * random per call. The {@link Random} source is injectable so tests can drive
 * deterministic sequences.
 */
public class EventManager {
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

                new Event("Nothing eventful today — A quiet day on the road.",
                        null, null,
                        new Effects(0, 0, 0, 0, 0, 0),
                        new Effects(0, 0, 0, 0, 0, 0))
        );
    }

    /**
     * Picks one event at random from the pool with uniform probability.
     *
     * @return a non-null {@link Event} drawn from the event pool
     */
    public Event getRandomEvent() {
        return eventPool.get(random.nextInt(eventPool.size()));
    }
}
