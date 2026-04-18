package org.example.model;

//The core state machine of the game, tracking all resources and progress.

public class StartupState {
    private static final int INITIAL_CASH = 50000;
    private static final int DAILY_COFFEE_DRAIN = 3;
    private static final int EXTRA_COFFEE_COST = 5;
    private static final int INITIAL_HYPE = 50;
    private static final int INITIAL_MORALE = 100;
    private static final int INITIAL_COFFEE = 100;
    private static final int INITIAL_COMPUTE = 100;
    private static final int DAILY_EXPENSE = 1000;
    private static final int FLAT_TRAVEL_COST = 200;
    private static final int INITIAL_BUGS = 0;

    // Resource stats tracking
    int cash;
    int hype;
    int morale;
    int coffee;
    int computeCredits;
    int bugs;



    // Survival and Progress tracking
    int daysWithoutCoffee;
    int currentIndex;
    int currentDay;
    boolean hasBoostedToday;

    public StartupState() {
        cash = INITIAL_CASH;
        hype = INITIAL_HYPE;
        morale = INITIAL_MORALE;
        coffee = INITIAL_COFFEE;
        computeCredits = INITIAL_COMPUTE;
        bugs = INITIAL_BUGS;
        daysWithoutCoffee = 0;
        currentIndex = 0;
        currentDay = 1;
        hasBoostedToday = false;
    }

    // ==========================================
    // Getters
    // ==========================================
    public int getCash() { return cash; }
    public int getMorale() { return morale; }
    public int getCoffee() { return coffee; }
    public int getComputeCredits() { return computeCredits; }
    public int getHype() { return hype; }
    public int getBugs() { return bugs; }
    public int getCurrentIndex() { return currentIndex; }
    public int getCurrentDay() { return currentDay; }
    public int getDaysWithoutCoffee() { return daysWithoutCoffee; }
    public boolean getHasBoostedToday() { return hasBoostedToday; }

    // ==========================================
    // Setters
    // ==========================================
    public void setCash(int cash) { this.cash = cash; }
    public void setMorale(int morale) { this.morale = morale; }
    public void setCoffee(int coffee) { this.coffee = coffee; }
    public void setComputeCredits(int credits) { this.computeCredits = credits; }
    public void setHype(int hype) { this.hype = hype; }
    public void setBugs(int bugs) { this.bugs = bugs; }

    // ==========================================
    // State Queries (Read-Only)
    // ==========================================

    //Checks if the game is over due to any failure condition.
    public boolean isGameOver() {
        return isBankrupt() || isBurnOut();
    }

    //Checks if the startup has run out of cash.
    public boolean isBankrupt() {
        return cash <= 0;
    }

    //Checks if the team has lost all morale.
    public boolean isBurnOut() {
        return morale <= 0;
    }

    // ==========================================
    // Private Helpers
    // ==========================================

    //Advancing time and settling daily resource
    private void endDayAndSettle() {
        if (coffee >= DAILY_COFFEE_DRAIN) {
            coffee -= DAILY_COFFEE_DRAIN;
            daysWithoutCoffee = 0;
        } else {
            daysWithoutCoffee++;
        }
        if (daysWithoutCoffee >= 2) {
            System.out.println("⚠️ [CRITICAL] The team is collapsing without caffeine!");
            morale = -30;
        } else if (daysWithoutCoffee == 1) {
            System.out.println("🔔 [WARNING]  The team is getting caffeine withdrawal headaches...");
            morale -= 10;
        }
        currentDay++;
        cash -= DAILY_EXPENSE;
        hasBoostedToday = false;
    }

    // ==========================================
    // State Mutations (Player Actions)
    // ==========================================
    public boolean coffeeBoost() {
        // boost once at most per day
        if (hasBoostedToday) {
            return false;
        }
        if (coffee >= EXTRA_COFFEE_COST) {
            coffee -= EXTRA_COFFEE_COST;
            morale += 15;
            morale = Math.min(100, morale);
            hasBoostedToday = true;
            return true;
        }
        return false;
    }

    // Rests for a day, recovering morale.
    public void rest() {
        morale += 30;
        if (morale > 100) morale = 100;
        endDayAndSettle();
    }

    // Build product for a day
    public void buildProduct() {
        if (computeCredits > 0) {
            computeCredits -= 10;
            hype += 20;
            System.out.println("🚀 Product updated! Hype increased to " + hype);
        } else {
            morale -= 15;
            System.out.println("⚠️ Compute exhausted! Local build crashed the system. Morale dropped！");
        }
        bugs += 5;
        endDayAndSettle();
    }
    //Fix bugs
    public void fixBugs() {
        if (bugs > 0) {
            bugs -= 5;
            morale -= 10;
        }
        bugs = Math.max(0, bugs);
        endDayAndSettle();
    }

    //Advances to the next location today, applying travel costs and weather effects.
    public void travelToNextStop(boolean isBadWeather) {
        int cost = FLAT_TRAVEL_COST;
        if (isBadWeather) {
            cost += 500;
            morale -= 15;
        }
        cash -= cost;
        morale -= 5;
        endDayAndSettle();
        currentIndex++;
    }
    public void marketingPush() {
        cash -= 1500;
        hype += 15;
        hype = Math.min(100, hype);
        endDayAndSettle();
    }

    // ==========================================
    // State Mutations (Random Events， Weather)
    // ==========================================
    public void applyEventEffects(Effects effects) {
        cash += effects.cash();
        morale += effects.morale();
        computeCredits += effects.compute();
        coffee += effects.coffee();
        hype += effects.hype();
        bugs += effects.bugs();


        morale = Math.min(100, morale); //  morale <= 100
        computeCredits = Math.max(0, computeCredits); //只需要防止负数，没有上限
        coffee = Math.max(0, coffee); //只需要防止负数，没有上限
        hype = Math.min(100, Math.max(0, hype)); // 0 <= hype <= 100
        bugs = Math.max (0, bugs);
    }


}
