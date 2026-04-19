package io.github.chloeji.svtrail.core;

import io.github.chloeji.svtrail.api.WeatherService;
import io.github.chloeji.svtrail.model.Event;
import io.github.chloeji.svtrail.model.Location;
import io.github.chloeji.svtrail.model.StartupState;
import io.github.chloeji.svtrail.model.WeatherData;
import io.github.chloeji.svtrail.ui.DisplayManager;
import io.github.chloeji.svtrail.util.InputHandler;
import io.github.chloeji.svtrail.util.SaveManager;

/**
 * Top-level game controller. Wires together route, events, weather, input,
 * display, and persistence, and drives the main menu and per-day game loop.
 */
public class GameRunner {
    private static final int MARKETING_MIN_CASH = 1500;

    private final RouteMap routeMap;
    private final EventManager eventManager;
    private final WeatherService weatherService;
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
        state.travelToNextStop(weather.isBadWeather());
        Location arrived = routeMap.getLocation(state.getCurrentIndex());
        System.out.println("\n🚗 Your team hits the road...");
        System.out.println("✅ Arrived at " + arrived.name() + "!");
        triggerEvent();
    }

    private void rest() {
        System.out.println("\n😴 Your team takes a break...");
        state.rest();
        System.out.println("✅ Team feels refreshed!");
        triggerEvent();
    }

    private void workOnProduct() {
        System.out.println("\n💻 Your team focuses on the product...");
        System.out.println("\n✅ Productive day, but team is tired.");
        state.buildProduct();
        triggerEvent();
    }

    private void fixBugs() {
        if (state.getBugs() <= 0) {
            System.out.println("\n✅ No bugs to fix! Codebase is clean.");
            return;
        }
        state.fixBugs();
        System.out.println("\n🔧 Team spent the day squashing bugs. Tiring but necessary.");
        triggerEvent();
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
        triggerEvent();
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

    private void triggerEvent() {
        Event event = eventManager.getRandomEvent();
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
