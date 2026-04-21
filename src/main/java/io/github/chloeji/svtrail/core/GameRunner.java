package io.github.chloeji.svtrail.core;

import io.github.chloeji.svtrail.api.MappingService;
import io.github.chloeji.svtrail.api.WeatherService;
import io.github.chloeji.svtrail.model.Effects;
import io.github.chloeji.svtrail.model.Event;
import io.github.chloeji.svtrail.model.Location;
import io.github.chloeji.svtrail.model.RouteInfo;
import io.github.chloeji.svtrail.model.StartupState;
import io.github.chloeji.svtrail.model.WeatherData;
import io.github.chloeji.svtrail.ui.DisplayManager;
import io.github.chloeji.svtrail.util.InputHandler;
import io.github.chloeji.svtrail.util.SaveManager;

import java.util.Random;

/**
 * Top-level game controller. Wires together route, events, weather, input,
 * display, and persistence, and drives the main menu and per-day game loop.
 * <p>
 * Events fire only on arrival at a new location via {@link #travel(WeatherData)}.
 * Other actions (rest, build, fix bugs, marketing) resolve their effect and
 * pause but never trigger a random event — matching the spec's "event at each
 * location after movement" rule.
 */
public class GameRunner {
    private static final int HEAVY_TRAFFIC_MORALE_DROP = 5;

    // Heavy-traffic probability per leg. Applied as a random roll so that
    // real-world time-of-day (rush hour vs. off-peak) does not make the
    // Mapbox feature feel either omnipresent or absent: every leg rolls a
    // 25% baseline, and a heavy Mapbox signal adds 50 percentage points on
    // top — yielding 75% with Mapbox + heavy, 25% everywhere else.
    private static final double BASELINE_HEAVY_PROBABILITY = 0.25;
    private static final double MAPBOX_HEAVY_BOOST = 0.5;

    // ANSI color escapes for the startup warning. Modern terminals (iTerm,
    // Terminal.app, Windows Terminal, most IDE consoles) render these; legacy
    // cmd.exe shows the raw codes but that's acceptable noise for a
    // non-critical informational message.
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_RESET = "\u001B[0m";

    private final RouteMap routeMap;
    private final EventManager eventManager;
    private final WeatherService weatherService;
    private final MappingService mappingService;
    private final SaveManager saveManager;
    private final InputHandler inputHandler;
    private final DisplayManager display;
    private final Random random;
    private StartupState state;

    /**
     * Creates a new runner with default collaborators.
     */
    public GameRunner() {
        routeMap = new RouteMap();
        eventManager = new EventManager();
        weatherService = new WeatherService();
        mappingService = new MappingService();
        saveManager = new SaveManager();
        inputHandler = new InputHandler();
        display = new DisplayManager();
        random = new Random();
    }

    // ==========================================
    // Main Menu
    // ==========================================

    /**
     * Entry point for the game. Displays the main menu and dispatches to the
     * game loop for new or loaded games until the player chooses to quit.
     */
    public void start() {
        printMapboxHintIfUnconfigured();
        while (true) {
            display.printMainMenu();
            int choice = inputHandler.getUserChoice(1, 3);
            switch (choice) {
                case 1 -> {
                    state = new StartupState();
                    display.printIntro();
                    inputHandler.waitForEnter();
                    gameLoop();
                }
                case 2 -> {
                    state = saveManager.load();
                    if (state != null) {
                        gameLoop();
                    }
                }
                case 3 -> {
                    System.out.println("Thanks for playing! Goodbye.");
                    return;
                }
            }
        }
    }

    /**
     * Prints a one-time informational banner if the Mapbox token is absent,
     * so a reviewer cloning the repo notices the optional feature and the
     * env-var setup without having to read the README end-to-end. Silent
     * when the token is configured — no nagging.
     */
    private void printMapboxHintIfUnconfigured() {
        if (!mappingService.isConfigured()) {
            System.out.println(ANSI_RED + "🗺️  Mapbox not configured — traffic-aware travel features disabled.");
            System.out.println("     Add MAPBOX_TOKEN to .env or export it in your shell to enable." + ANSI_RESET);
        }
    }

    // ==========================================
    // Core Game Loop
    // ==========================================

    private void gameLoop() {
        // Outer loop = one day. Weather and progress are fetched once per day
        // so a mid-day action (coffee boost, save) doesn't re-hit the API.
        while (!state.isGameOver()) {
            Location current = routeMap.getLocation(state.getCurrentIndex());

            if (routeMap.isDestination(state.getCurrentIndex())) {
                display.printWin(state);
                return;
            }

            WeatherData weather = weatherService.fetchWeather(current);
            int progress = routeMap.progressPercent(state.getCurrentIndex());
            int dayBefore = state.getCurrentDay();

            // Inner loop = actions within this same day. Stays until either
            // the day advances (travel / rest / work / fix / marketing) or
            // the game ends. Coffee boost and Save keep us in the inner loop
            // so the player can stack them with another action on the same day.
            //
            // After a coffee boost we skip the next status reprint: the boost
            // message already prints the updated coffee/morale inline, and
            // re-rendering the full day header right after it reads as a new
            // day starting, which confused players.
            boolean skipNextStatus = false;
            while (state.getCurrentDay() == dayBefore && !state.isGameOver()) {
                if (!skipNextStatus) {
                    display.printDayStatus(state, current, weather, progress);
                }
                skipNextStatus = false;
                display.printActionMenu();

                int choice = inputHandler.getUserChoice(1, 6);
                switch (choice) {
                    case 1 -> travel(weather);
                    case 2 -> rest();
                    case 3 -> workOnProduct();
                    case 4 -> {
                        boostEnergy();
                        skipNextStatus = true;
                    }
                    case 5 -> saveManager.save(state);
                    case 6 -> {
                        return;
                    }
                }
            }
        }
        display.printGameOver(state);
    }

    // ==========================================
    // Player Actions
    // ==========================================

    private void travel(WeatherData weather) {
        Location origin = routeMap.getLocation(state.getCurrentIndex());
        Location next = routeMap.getLocation(state.getCurrentIndex() + 1);
        applyTrafficEffects(origin, next);

        state.travelToNextStop(weather.isBadWeather());
        Location arrived = routeMap.getLocation(state.getCurrentIndex());
        System.out.println("\n🚗 Your team hits the road...");
        System.out.println("✅ Arrived at " + arrived.name() + "!");
        triggerEvent(weather);
    }

    /**
     * Decides whether this leg counts as heavy traffic and, if so, applies
     * a morale drop before the base travel cost is charged. The probability
     * is a 25% random baseline plus a 50-point boost when Mapbox is configured
     * and reports the leg as heavy. This decouples gameplay impact from the
     * player's real-world clock so the feature feels present at any hour, while
     * still letting real Mapbox data raise the chance during actual congestion.
     * When Mapbox is unconfigured or the API call fails, only the 25% baseline
     * applies.
     */
    private void applyTrafficEffects(Location origin, Location next) {
        if (next == null) return;
        RouteInfo info = mappingService.getRouteInfo(origin, next);
        boolean mapboxSignalsHeavy = info != null && info.heavyTraffic();

        double probability = BASELINE_HEAVY_PROBABILITY
                + (mapboxSignalsHeavy ? MAPBOX_HEAVY_BOOST : 0.0);
        if (random.nextDouble() >= probability) return;

        System.out.println();
        if (mapboxSignalsHeavy) {
            System.out.println("🚦 Heavy traffic on the route — "
                    + info.trafficMinutes() + " min vs usual "
                    + info.freeFlowMinutes() + " min. Team stuck in the car.");
        } else {
            System.out.println("🚦 Heavy traffic on the route — team stuck in the car.");
        }
        state.applyEventEffects(new Effects(0, -HEAVY_TRAFFIC_MORALE_DROP, 0, 0));
    }

    private void rest() {
        System.out.println("\n😴 Your team takes a break...");
        state.rest();
        System.out.println("✅ Team feels refreshed!");
        inputHandler.waitForEnter();
    }

    private void workOnProduct() {
        System.out.println("\n💻 Your team focuses on the product...");
        state.buildProduct();
        inputHandler.waitForEnter();
    }

    private void boostEnergy() {
        if (state.getHasBoostedToday()) {
            System.out.println("\n❌ Already had extra coffee today!");
        } else if (state.coffeeBoost()) {
            System.out.println("\n🍵 Extra coffee! Team feels energized!");
            // Inline the updated resources so the player sees the boost's
            // effect without waiting for the next day's status header.
            System.out.println("   ☕ Coffee: " + state.getCoffee()
                    + " | 😊 Morale: " + state.getMorale() + "/100");
        } else {
            System.out.println("\n❌ Not enough coffee for a boost!");
        }
    }

    private void triggerEvent(WeatherData weather) {
        Event event = eventManager.getRandomEvent(weather);
        display.printEventDescription(event);

        // Null choice labels indicate a "nothing happens" event — show the
        // effect hint (skipped when all zero) and apply the effects without
        // prompting the player.
        if (event.choice1() == null) {
            display.printEventEffect(event.choice1Effects());
            state.applyEventEffects(event.choice1Effects());
        } else {
            display.printEventChoices(event);
            int choice = inputHandler.getUserChoice(1, 2);
            if (choice == 1) {
                state.applyEventEffects(event.choice1Effects());
            } else {
                state.applyEventEffects(event.choice2Effects());
            }
        }
        // Pause so the player has time to read the event outcome before the
        // next day's status overwrites the terminal.
        inputHandler.waitForEnter();
    }
}
