package io.github.chloeji.svtrail.ui;

import io.github.chloeji.svtrail.model.Effects;
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

    // Italic + cyan — used for the cost/effect hints under each menu option
    // and event choice. Cyan reads clearly on both light and dark terminals
    // without competing with the default-color action labels.
    private static final String ANSI_HINT = "\u001B[3;36m";
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
        System.out.println("  💻 Compute Credits - Ship product for revenue; running out makes builds drop morale");
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
                + " | 😊 Morale: " + state.getMorale() + "/100");
        System.out.println("☕ Coffee: " + state.getCoffee()
                + " | 💻 Compute: " + state.getComputeCredits());

        System.out.println("📍 Progress: " + progress + "% to San Francisco");
        System.out.println("============================================================");
        System.out.println("🌤️  Weather: " + weather.condition() + ", " + weather.temperature() + "°F");
        if (weather.isBadWeather()) {
            System.out.println("   ⚠️  Bad weather!");
            System.out.println(hint("   -> travel today: -$700 cash, -20 morale (vs -$200, -5 in clear weather)"));
        }
    }

    /**
     * Prints the per-day action menu with cost/effect summaries.
     * <p>
     * Options 1–3 advance the day and carry a fixed daily tax (−$1,000 cash,
     * −3 coffee) that's stated once at the top rather than duplicated on every
     * line. Options 4–6 are "free" — they don't advance the day — and are
     * rendered in a separate block so the distinction is visually obvious.
     */
    public void printActionMenu() {
        System.out.println("\n------------------------------------------------------------");
        System.out.println("What will you do?");
        System.out.println(hint("Options 1–3 advance the day: -$1,000 cash, -3 ☕"));
        System.out.println("------------------------------------------------------------");
        System.out.println("1. Travel to next location");
        System.out.println(hint("   -> spend 200 | morale drops 5 (bad weather: spend 700 | morale drops 20)"));
        System.out.println("2. Rest and recover");
        System.out.println(hint("   -> morale boost 30"));
        System.out.println("3. Work on product");
        System.out.println(hint("   -> uses 10 compute credits | earn $1500 revenue (no compute credits: morale drops 10)"));
        System.out.println(hint("------------------------------------------------------------"));
        System.out.println(hint("Free actions (no day advance):"));
        System.out.println("4. Coffee boost (once per day)");
        System.out.println(hint("   -> uses 5 coffee | morale boost 15"));
        System.out.println("5. Save game");
        System.out.println("6. Quit to menu");
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
     * Prints the two choice labels for a branching event, each followed by
     * a hint line summarizing its effect on the startup's resources — same
     * visual pattern as the per-day action menu so the player can weigh
     * choices without guessing.
     *
     * @param event the event whose choices should be displayed
     */
    public void printEventChoices(Event event) {
        System.out.println("🌟Choice 1: " + event.choice1());
        System.out.println(hint("   -> " + formatEffects(event.choice1Effects())));
        System.out.println("🌟Choice 2: " + event.choice2());
        System.out.println(hint("   -> " + formatEffects(event.choice2Effects())));
    }

    /**
     * Prints the effect hint for an automatic (no-choice) event, so the
     * player sees the resource impact of a passive event the same way they
     * see it for actions and branching events. Skips output when the event
     * has no resource impact to avoid visual noise on quiet days.
     *
     * @param effects the resource deltas the event will apply
     */
    public void printEventEffect(Effects effects) {
        if (isNoOp(effects)) return;
        System.out.println(hint("   -> " + formatEffects(effects)));
    }

    private static boolean isNoOp(Effects e) {
        return e.cash() == 0 && e.morale() == 0 && e.compute() == 0 && e.coffee() == 0;
    }

    private static String formatEffects(Effects e) {
        StringBuilder sb = new StringBuilder();
        appendCashDelta(sb, e.cash());
        appendDelta(sb, e.morale(), "morale");
        appendDelta(sb, e.compute(), "compute");
        appendDelta(sb, e.coffee(), "coffee");
        return sb.length() == 0 ? "no resource change" : sb.toString();
    }

    private static void appendCashDelta(StringBuilder sb, int value) {
        if (value == 0) return;
        sb.append(value > 0 ? "+$" : "-$").append(Math.abs(value)).append(" cash");
    }

    private static void appendDelta(StringBuilder sb, int value, String name) {
        if (value == 0) return;
        if (sb.length() > 0) sb.append(", ");
        sb.append(value > 0 ? "+" : "").append(value).append(" ").append(name);
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
        System.out.println("😊 Final Morale: " + state.getMorale() + "/100");
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
