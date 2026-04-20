package io.github.chloeji.svtrail.model;

/**
 * Core state machine of the game, tracking all resources and progress.
 * <p>
 * This class is the only mutable domain object in the game. Player actions,
 * daily settlement, and random events all funnel through its mutation methods
 * so that resource invariants (e.g. morale bounded to [0, 100], non-negative
 * coffee and compute) can be enforced in one place.
 */
public class StartupState {
    private static final int INITIAL_CASH = 20000;
    private static final int DAILY_COFFEE_DRAIN = 3;
    private static final int EXTRA_COFFEE_COST = 5;
    private static final int INITIAL_MORALE = 70;
    private static final int INITIAL_COFFEE = 50;
    private static final int INITIAL_COMPUTE = 100;
    private static final int DAILY_EXPENSE = 1000;
    private static final int FLAT_TRAVEL_COST = 200;

    private static final int MAX_MORALE = 100;
    private static final int REST_MORALE_GAIN = 30;
    private static final int BOOST_MORALE_GAIN = 15;
    private static final int BUILD_COMPUTE_COST = 10;
    private static final int BUILD_CASH_GAIN = 1500;
    private static final int BUILD_FAIL_MORALE_DROP = 10;
    private static final int BAD_WEATHER_EXTRA_COST = 500;
    private static final int BAD_WEATHER_MORALE_DROP = 15;
    private static final int TRAVEL_MORALE_DROP = 5;
    private static final int COFFEE_WITHDRAWAL_CRITICAL_DAYS = 2;
    private static final int COFFEE_WITHDRAWAL_CRITICAL_MORALE = -30;
    private static final int COFFEE_WITHDRAWAL_WARNING_MORALE_DROP = 10;

    // Resource stats tracking
    int cash;
    int morale;
    int coffee;
    int computeCredits;

    // Survival and progress tracking
    int daysWithoutCoffee;
    int currentIndex;
    int currentDay;
    boolean hasBoostedToday;

    /**
     * Creates a new startup state initialized with the default resource values
     * used at the start of a fresh game.
     */
    public StartupState() {
        cash = INITIAL_CASH;
        morale = INITIAL_MORALE;
        coffee = INITIAL_COFFEE;
        computeCredits = INITIAL_COMPUTE;
        daysWithoutCoffee = 0;
        currentIndex = 0;
        currentDay = 1;
        hasBoostedToday = false;
    }

    // ==========================================
    // Getters
    // ==========================================

    /** @return current cash balance in dollars; may be negative after a bankrupting event */
    public int getCash() { return cash; }

    /** @return current team morale in [0, 100] */
    public int getMorale() { return morale; }

    /** @return current coffee units remaining */
    public int getCoffee() { return coffee; }

    /** @return current cloud compute credits remaining */
    public int getComputeCredits() { return computeCredits; }

    /** @return index of the current location on the route */
    public int getCurrentIndex() { return currentIndex; }

    /** @return current in-game day, starting at 1 */
    public int getCurrentDay() { return currentDay; }

    /** @return number of consecutive days the team has gone without coffee */
    public int getDaysWithoutCoffee() { return daysWithoutCoffee; }

    /** @return {@code true} if the player has already used the coffee boost today */
    public boolean getHasBoostedToday() { return hasBoostedToday; }

    // ==========================================
    // Setters
    // ==========================================

    /** Sets the current cash balance. Primarily used by tests and save/load. */
    public void setCash(int cash) { this.cash = cash; }

    /** Sets the current morale. Primarily used by tests and save/load. */
    public void setMorale(int morale) { this.morale = morale; }

    /** Sets the current coffee count. Primarily used by tests and save/load. */
    public void setCoffee(int coffee) { this.coffee = coffee; }

    /** Sets the current compute credits. Primarily used by tests and save/load. */
    public void setComputeCredits(int credits) { this.computeCredits = credits; }

    // ==========================================
    // State Queries (Read-Only)
    // ==========================================

    /**
     * Checks whether any game-over condition has been met.
     *
     * @return {@code true} if the player is bankrupt or the team has burned out
     */
    public boolean isGameOver() {
        return isBankrupt() || isBurnOut();
    }

    /**
     * @return {@code true} when cash has dropped to zero or below
     */
    public boolean isBankrupt() {
        return cash <= 0;
    }

    /**
     * @return {@code true} when morale has dropped to zero or below
     */
    public boolean isBurnOut() {
        return morale <= 0;
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    /**
     * Advances time by one day and settles daily resource drains: daily coffee
     * consumption (or withdrawal penalties if out of coffee), fixed daily
     * operating expenses, and the once-per-day boost reset.
     */
    private void endDayAndSettle() {
        if (coffee >= DAILY_COFFEE_DRAIN) {
            coffee -= DAILY_COFFEE_DRAIN;
            daysWithoutCoffee = 0;
        } else {
            daysWithoutCoffee++;
        }
        if (daysWithoutCoffee >= COFFEE_WITHDRAWAL_CRITICAL_DAYS) {
            System.out.println("⚠️ [CRITICAL] The team is collapsing without caffeine!");
            // Forcing morale strongly negative guarantees isBurnOut() triggers
            // on the next game-over check — two days without coffee is a loss.
            morale = COFFEE_WITHDRAWAL_CRITICAL_MORALE;
        } else if (daysWithoutCoffee == 1) {
            System.out.println("🔔 [WARNING]  The team is getting caffeine withdrawal headaches...");
            morale -= COFFEE_WITHDRAWAL_WARNING_MORALE_DROP;
        }
        currentDay++;
        cash -= DAILY_EXPENSE;
        hasBoostedToday = false;
    }

    // ==========================================
    // State Mutations (Player Actions)
    // ==========================================

    /**
     * Applies an extra coffee boost for the day, recovering morale at the cost
     * of coffee stock. Limited to once per day and does not advance the day.
     *
     * @return {@code true} if the boost was applied; {@code false} if already
     *         used today or insufficient coffee
     */
    public boolean coffeeBoost() {
        if (hasBoostedToday) {
            return false;
        }
        if (coffee >= EXTRA_COFFEE_COST) {
            coffee -= EXTRA_COFFEE_COST;
            morale += BOOST_MORALE_GAIN;
            morale = Math.min(MAX_MORALE, morale);
            hasBoostedToday = true;
            return true;
        }
        return false;
    }

    /**
     * Rests for a day, recovering morale and advancing to the next day.
     */
    public void rest() {
        morale += REST_MORALE_GAIN;
        if (morale > MAX_MORALE) morale = MAX_MORALE;
        endDayAndSettle();
    }

    /**
     * Spends a day building the product. With compute credits available the
     * team ships a feature and earns revenue; without compute the local build
     * fails and morale suffers.
     */
    public void buildProduct() {
        if (computeCredits > 0) {
            computeCredits -= BUILD_COMPUTE_COST;
            cash += BUILD_CASH_GAIN;
            System.out.println("🚀 Product shipped! Earned $" + BUILD_CASH_GAIN + " in revenue.");
        } else {
            morale -= BUILD_FAIL_MORALE_DROP;
            System.out.println("⚠️ Compute exhausted! Local build crashed the system. Morale dropped!");
        }
        endDayAndSettle();
    }

    /**
     * Advances to the next location on the route, applying flat travel cost,
     * morale drain, and extra penalties under bad weather.
     *
     * @param isBadWeather whether the current weather is rated as bad
     */
    public void travelToNextStop(boolean isBadWeather) {
        int cost = FLAT_TRAVEL_COST;
        if (isBadWeather) {
            cost += BAD_WEATHER_EXTRA_COST;
            morale -= BAD_WEATHER_MORALE_DROP;
        }
        cash -= cost;
        morale -= TRAVEL_MORALE_DROP;
        endDayAndSettle();
        currentIndex++;
    }

    // ==========================================
    // State Mutations (Random Events, Weather)
    // ==========================================

    /**
     * Applies the resource deltas of an event outcome and clamps every
     * resource back within its valid range.
     *
     * @param effects resource deltas produced by an event choice
     */
    public void applyEventEffects(Effects effects) {
        cash += effects.cash();
        morale += effects.morale();
        computeCredits += effects.compute();
        coffee += effects.coffee();

        morale = Math.min(MAX_MORALE, morale);
        computeCredits = Math.max(0, computeCredits);
        coffee = Math.max(0, coffee);
    }
}
