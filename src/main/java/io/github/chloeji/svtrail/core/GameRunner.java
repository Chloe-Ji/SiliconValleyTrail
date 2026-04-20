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
    private static final int MARKETING_MIN_CASH = 1500;
    private static final int HEAVY_TRAFFIC_MORALE_DROP = 5;

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
        while (!state.isGameOver()) {
            Location current = routeMap.getLocation(state.getCurrentIndex());

            if (routeMap.isDestination(state.getCurrentIndex())) {
                display.printWin(state);
                return;
            }

            WeatherData weather = weatherService.fetchWeather(current);
            int progress = routeMap.progressPercent(state.getCurrentIndex());
            display.printDayStatus(state, current, weather, progress);
            display.printActionMenu();

            int choice = inputHandler.getUserChoice(1, 8);
            switch (choice) {
                case 1 -> travel(weather);
                case 2 -> rest();
                case 3 -> workOnProduct();
                case 4 -> fixBugs();
                case 5 -> marketingPush();
                case 6 -> boostEnergy();
                case 7 -> saveManager.save(state);
                case 8 -> {
                    return;
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
        applyMapboxEffects(origin, next);

        state.travelToNextStop(weather.isBadWeather());
        Location arrived = routeMap.getLocation(state.getCurrentIndex());
        System.out.println("\n🚗 Your team hits the road...");
        System.out.println("✅ Arrived at " + arrived.name() + "!");
        triggerEvent(weather);
    }

    /**
     * Queries Mapbox for the current leg and, when traffic is heavy,
     * applies a morale drop before the base travel cost is charged.
     * Silently no-ops when Mapbox is unavailable.
     */
    private void applyMapboxEffects(Location origin, Location next) {
        if (next == null) return;
        RouteInfo info = mappingService.getRouteInfo(origin, next);
        if (info == null) return;

        if (info.heavyTraffic()) {
            System.out.println();
            System.out.println("🚦 Heavy traffic on the route — "
                    + info.trafficMinutes() + " min vs usual "
                    + info.freeFlowMinutes() + " min. Team stuck in the car.");
            state.applyEventEffects(new Effects(0, -HEAVY_TRAFFIC_MORALE_DROP, 0, 0, 0, 0));
        }
    }

    private void rest() {
        System.out.println("\n😴 Your team takes a break...");
        state.rest();
        System.out.println("✅ Team feels refreshed!");
        inputHandler.waitForEnter();
    }

    private void workOnProduct() {
        System.out.println("\n💻 Your team focuses on the product...");
        System.out.println("\n✅ Productive day, but team is tired.");
        state.buildProduct();
        inputHandler.waitForEnter();
    }

    private void fixBugs() {
        if (state.getBugs() <= 0) {
            System.out.println("\n✅ No bugs to fix! Codebase is clean.");
            return;
        }
        state.fixBugs();
        System.out.println("\n🔧 Team spent the day squashing bugs. Tiring but necessary.");
        inputHandler.waitForEnter();
    }

    private void marketingPush() {
        System.out.println("\n📢 You launch a marketing campaign...");

        // Bail out without consuming a day if the player cannot afford the
        // campaign — this lets them reselect a different action.
        if (state.getCash() < MARKETING_MIN_CASH) {
            System.out.println("\n❌ Not enough cash for marketing! Need $" + MARKETING_MIN_CASH);
            return;
        }
        state.marketingPush();
        System.out.println("\n📢 Campaign launched! Hype increased. (Cost: $" + MARKETING_MIN_CASH + ")");
        inputHandler.waitForEnter();
    }

    private void boostEnergy() {
        if (state.getHasBoostedToday()) {
            System.out.println("\n❌ Already had extra coffee today!");
        } else if (state.coffeeBoost()) {
            System.out.println("\n🍵 Extra coffee! Team feels energized!");
        } else {
            System.out.println("\n❌ Not enough coffee for a boost!");
        }
    }

    private void triggerEvent(WeatherData weather) {
        Event event = eventManager.getRandomEvent(weather);
        display.printEventDescription(event);

        // Null choice labels indicate a "nothing happens" event — apply its
        // (typically zero) effects silently rather than prompting the player.
        if (event.choice1() == null) {
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
