package io.github.chloeji.svtrail.ui;

import io.github.chloeji.svtrail.model.Event;
import io.github.chloeji.svtrail.model.Location;
import io.github.chloeji.svtrail.model.StartupState;
import io.github.chloeji.svtrail.model.WeatherData;

/**
 * Owns all console output for the game. Isolating print logic here keeps the
 * rest of the code I/O-free and lets the presentation layer be swapped (e.g.
 * for a GUI) without touching game logic.
 */
public class DisplayManager {

    // Dim + italic + gray — used to de-emphasize the cost/effect hints under
    // each menu option so the action labels stand out. Supported in most
    // modern terminals; legacy consoles fall back to plain text.
    private static final String ANSI_HINT = "\u001B[2;3;90m";
    private static final String ANSI_RESET = "\u001B[0m";

    private static String hint(String text) {
        return ANSI_HINT + text + ANSI_RESET;
    }

    /**
     * Prints the top-level menu shown when the game launches or returns from
     * a session.
     */
    public void printMainMenu() {
        System.out.println("\n============================================================");
        System.out.println("SILICON VALLEY TRAIL - Main Menu");
        System.out.println("============================================================");
        System.out.println("1. New Game");
        System.out.println("2. Load Game");
        System.out.println("3. Quit");
    }

    /**
     * Prints the introductory splash shown at the start of a new game.
     */
    public void printIntro() {
        System.out.println("\n============================================================");
        System.out.println("🚀 SILICON VALLEY TRAIL 🚀");
        System.out.println("============================================================");
        System.out.println("Your scrappy startup team is embarking on a journey");
        System.out.println("from San Jose to San Francisco to pitch for Series A funding!");
        System.out.println("\nManage your resources wisely:");
        System.out.println("  💰 Cash - Don't run out!");
        System.out.println("  😊 Morale - Keep your team happy");
        System.out.println("  ☕ Coffee - Essential fuel (2 days without = Morale drops dramatically)");
        System.out.println("  📢 Hype - Public interest in your startup");
        System.out.println("  💻 Compute Credits - Cloud credits for building product");
        System.out.println("\nGood luck, founder!");
        System.out.println("============================================================");
    }

    /**
     * Prints the per-day status header: day number, location, resources,
     * progress, and current weather.
     *
     * @param state    current game state
     * @param location the location the team is currently at
     * @param weather  the weather at that location
     * @param progress progress percent to the final destination
     */
    public void printDayStatus(StartupState state, Location location,
                                WeatherData weather, int progress) {
        System.out.println("\n============================================================");
        System.out.println("Day " + state.getCurrentDay() + " | " + location.name());
        System.out.println("============================================================");
        System.out.println("💰 Cash: $" + state.getCash()
                + " | 😊 Morale: " + state.getMorale() + "/100"
                + " | ☕ Coffee: " + state.getCoffee());
        System.out.println("📢 Hype: " + state.getHype() + "/100"
                + " | 💻 Compute: " + state.getComputeCredits()
                + " | 🐛 Bugs: " + state.getBugs());

        System.out.println("📍 Progress: " + progress + "% to San Francisco");
        System.out.println("============================================================");
        System.out.println("🌤️  Weather: " + weather.condition() + ", " + weather.temperature() + "°F");
        if (weather.isBadWeather()) {
            System.out.println("   Bad weather may slow your progress!");
        }
    }

    /**
     * Prints the per-day action menu with cost/effect summaries.
     */
    public void printActionMenu() {
        System.out.println("\n------------------------------------------------------------");
        System.out.println("What will you do?");
        System.out.println("------------------------------------------------------------");
        System.out.println("1. Travel to next location");
        System.out.println(hint("   -> spend 200 | morale drops 5 (bad weather: spend 500 | morale drops 15)"));
        System.out.println("2. Rest and recover");
        System.out.println(hint("   -> morale boost 30"));
        System.out.println("3. Work on product");
        System.out.println(hint("   -> uses 10 compute credits | hype increases 20 (no compute credits: morale drops 15) | 🐛 bugs increase"));
        System.out.println("4. Fix bugs");
        System.out.println(hint("   -> 🐛 bugs decrease 5 | 😊 morale drops 10"));
        System.out.println("5. Marketing push (costs $1500)");
        System.out.println(hint("   -> spend 1500 | hype increases 15"));
        System.out.println("6. Coffee boost (extra coffee for morale)");
        System.out.println(hint("   -> uses 5 coffee | morale boost 15"));
        System.out.println("7. Save game");
        System.out.println("8. Quit to menu");
    }

    /**
     * Prints the flavor-text banner for a random event.
     *
     * @param event the event being presented to the player
     */
    public void printEventDescription(Event event) {
        System.out.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("📰 EVENT: " + event.description());
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }

    /**
     * Prints the two choice labels for a branching event.
     *
     * @param event the event whose choices should be displayed
     */
    public void printEventChoices(Event event) {
        System.out.println("🌟Choice 1: " + event.choice1());
        System.out.println("🌟Choice 2: " + event.choice2());
    }

    /**
     * Prints the winning end-of-game screen.
     *
     * @param state final game state at the moment of victory
     */
    public void printWin(StartupState state) {
        System.out.println("\n============================================================");
        System.out.println("🎉 CONGRATULATIONS! 🎉");
        System.out.println("============================================================");
        System.out.println("Your startup made it to San Francisco!");
        System.out.println("💰 Final Cash: $" + state.getCash());
        System.out.println("📢 Final Hype: " + state.getHype() + "/100");
        System.out.println("You're ready for your Series A pitch!");
        System.out.println("============================================================");
    }

    /**
     * Prints the losing end-of-game screen, annotated with the specific
     * failure condition(s) that triggered the loss.
     *
     * @param state final game state at the moment of defeat
     */
    public void printGameOver(StartupState state) {
        System.out.println("\n============================================================");
        System.out.println("💀 GAME OVER 💀");
        System.out.println("============================================================");
        if (state.isBankrupt()) {
            System.out.println("Your startup ran out of cash. Bankruptcy filed.");
        }
        if (state.isBurnOut()) {
            System.out.println("Your team burned out. Everyone quit.");
        }
        System.out.println("============================================================");
    }
}
