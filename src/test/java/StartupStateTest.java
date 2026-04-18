import org.example.model.StartupState;
import org.example.model.Effects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StartupStateTest {
    private StartupState state;

    @BeforeEach
    void setUp() {
        state = new StartupState();
    }

    // ==========================================
    // Initial State
    // ==========================================

    @Test
    void initialState_hasCorrectDefaults() {
        assertEquals(50000, state.getCash());
        assertEquals(100, state.getMorale());
        assertEquals(100, state.getCoffee());
        assertEquals(100, state.getComputeCredits());
        assertEquals(50, state.getHype());
        assertEquals(0, state.getBugs());
        assertEquals(1, state.getCurrentDay());
        assertEquals(0, state.getCurrentIndex());
    }

    // ==========================================
    // Game Over Conditions
    // ==========================================

    @Test
    void isBankrupt_whenCashZero() {
        state.setCash(0);
        assertTrue(state.isBankrupt());
        assertTrue(state.isGameOver());
    }

    @Test
    void isBankrupt_whenCashNegative() {
        state.setCash(-100);
        assertTrue(state.isBankrupt());
    }

    @Test
    void isNotBankrupt_whenCashPositive() {
        assertFalse(state.isBankrupt());
    }

    @Test
    void isBurnOut_whenMoraleZero() {
        state.setMorale(0);
        assertTrue(state.isBurnOut());
        assertTrue(state.isGameOver());
    }

    @Test
    void isNotGameOver_whenResourcesHealthy() {
        assertFalse(state.isGameOver());
    }

    // ==========================================
    // Rest
    // ==========================================

    @Test
    void rest_increasesMorale() {
        state.setMorale(50);
        state.rest();
        assertEquals(80, state.getMorale());
    }

    @Test
    void rest_moraleCapAt100() {
        state.setMorale(90);
        state.rest();
        assertEquals(100, state.getMorale());
    }

    @Test
    void rest_advancesDay() {
        state.rest();
        assertEquals(2, state.getCurrentDay());
    }

    @Test
    void rest_deductsDailyExpense() {
        state.rest();
        assertEquals(49000, state.getCash());
    }

    // ==========================================
    // Build Product
    // ==========================================

    @Test
    void buildProduct_withCompute_increasesHypeAndBugs() {
        int initialHype = state.getHype();
        int initialBugs = state.getBugs();
        state.buildProduct();
        assertEquals(initialHype + 20, state.getHype());
        assertEquals(initialBugs + 5, state.getBugs());
    }

    @Test
    void buildProduct_withCompute_reducesCompute() {
        state.buildProduct();
        assertEquals(90, state.getComputeCredits());
    }

    @Test
    void buildProduct_withoutCompute_dropsMorale() {
        state.setComputeCredits(0);
        int initialMorale = state.getMorale();
        state.buildProduct();
        assertTrue(state.getMorale() < initialMorale);
    }

    @Test
    void buildProduct_withoutCompute_stillAddsBugs() {
        state.setComputeCredits(0);
        state.buildProduct();
        assertEquals(5, state.getBugs());
    }

    // ==========================================
    // Fix Bugs
    // ==========================================

    @Test
    void fixBugs_reducesBugs() {
        state.setBugs(10);
        state.fixBugs();
        assertEquals(5, state.getBugs());
    }

    @Test
    void fixBugs_doesNotGoBelowZero() {
        state.setBugs(2);
        state.fixBugs();
        assertEquals(0, state.getBugs());
    }

    @Test
    void fixBugs_noBugs_noMoraleDrop() {
        state.setBugs(0);
        int moraleBefore = state.getMorale();
        state.fixBugs();
        // bugs == 0, so no morale -= 10, only endDayAndSettle runs
        assertEquals(moraleBefore, state.getMorale());
    }

    @Test
    void fixBugs_withBugs_reducesMorale() {
        state.setBugs(10);
        state.setMorale(50);
        state.fixBugs();
        assertTrue(state.getMorale() < 50);
    }

    // ==========================================
    // Travel
    // ==========================================

    @Test
    void travel_goodWeather_normalCost() {
        state.travelToNextStop(false);
        assertEquals(50000 - 200 - 1000, state.getCash());
    }

    @Test
    void travel_badWeather_extraCost() {
        state.travelToNextStop(true);
        assertEquals(50000 - 700 - 1000, state.getCash());
    }

    @Test
    void travel_badWeather_extraMoraleDrop() {
        state.travelToNextStop(false);
        int moraleGoodWeather = state.getMorale();

        state = new StartupState();
        state.travelToNextStop(true);
        int moraleBadWeather = state.getMorale();

        assertTrue(moraleBadWeather < moraleGoodWeather);
    }

    @Test
    void travel_advancesIndex() {
        state.travelToNextStop(false);
        assertEquals(1, state.getCurrentIndex());
    }

    @Test
    void travel_advancesDay() {
        state.travelToNextStop(false);
        assertEquals(2, state.getCurrentDay());
    }

    // ==========================================
    // Marketing Push
    // ==========================================

    @Test
    void marketingPush_costsCash() {
        state.marketingPush();
        assertEquals(50000 - 1500 - 1000, state.getCash());
    }

    @Test
    void marketingPush_increasesHype() {
        int initialHype = state.getHype();
        state.marketingPush();
        assertEquals(initialHype + 15, state.getHype());
    }

    @Test
    void marketingPush_hypeCapsAt100() {
        state.setHype(95);
        state.marketingPush();
        assertEquals(100, state.getHype());
    }

    // ==========================================
    // Coffee Boost
    // ==========================================

    @Test
    void coffeeBoost_success() {
        state.setMorale(70);
        assertTrue(state.coffeeBoost());
        assertEquals(85, state.getMorale());
        assertEquals(95, state.getCoffee());
    }

    @Test
    void coffeeBoost_moraleCapAt100() {
        state.setMorale(95);
        state.coffeeBoost();
        assertEquals(100, state.getMorale());
    }

    @Test
    void coffeeBoost_failsWhenAlreadyBoosted() {
        state.coffeeBoost();
        assertFalse(state.coffeeBoost());
    }

    @Test
    void coffeeBoost_failsWhenNotEnoughCoffee() {
        state.setCoffee(2);
        assertFalse(state.coffeeBoost());
    }

    @Test
    void coffeeBoost_resetsAfterEndDay() {
        state.coffeeBoost();
        assertTrue(state.getHasBoostedToday());
        state.rest();
        assertFalse(state.getHasBoostedToday());
    }

    // ==========================================
    // Coffee Withdrawal
    // ==========================================

    @Test
    void coffeeWithdrawal_day1_warningAndMoraleDrop() {
        state.setCoffee(0);
        state.rest();
        assertEquals(1, state.getDaysWithoutCoffee());
        assertTrue(state.getMorale() <= 90);
    }

    @Test
    void coffeeWithdrawal_day2_moraleCollapse() {
        state.setCoffee(0);
        state.rest();
        state.rest();
        assertTrue(state.getMorale() <= 0);
        assertTrue(state.isGameOver());
    }

    @Test
    void coffeeWithdrawal_resetsWhenCoffeeAvailable() {
        state.setCoffee(0);
        state.rest();
        assertEquals(1, state.getDaysWithoutCoffee());
        state.setCoffee(10);
        state.rest();
        assertEquals(0, state.getDaysWithoutCoffee());
    }

    // ==========================================
    // Apply Event Effects
    // ==========================================

    @Test
    void applyEventEffects_appliesAllChanges() {
        Effects effects = new Effects(1000, 10, 5, 3, 5, 2);
        state.applyEventEffects(effects);
        assertEquals(51000, state.getCash());
        assertEquals(55, state.getHype());
        assertEquals(103, state.getCoffee());
        assertEquals(2, state.getBugs());
    }

    @Test
    void applyEventEffects_clampsMoraleTo100() {
        Effects effects = new Effects(0, 50, 0, 0, 0, 0);
        state.applyEventEffects(effects);
        assertEquals(100, state.getMorale());
    }

    @Test
    void applyEventEffects_clampsHypeTo100() {
        Effects effects = new Effects(0, 0, 0, 0, 200, 0);
        state.applyEventEffects(effects);
        assertEquals(100, state.getHype());
    }

    @Test
    void applyEventEffects_clampsHypeToZero() {
        Effects effects = new Effects(0, 0, 0, 0, -200, 0);
        state.applyEventEffects(effects);
        assertEquals(0, state.getHype());
    }

    @Test
    void applyEventEffects_clampsComputeToZero() {
        Effects effects = new Effects(0, 0, -500, 0, 0, 0);
        state.applyEventEffects(effects);
        assertEquals(0, state.getComputeCredits());
    }

    @Test
    void applyEventEffects_clampsCoffeeToZero() {
        Effects effects = new Effects(0, 0, 0, -500, 0, 0);
        state.applyEventEffects(effects);
        assertEquals(0, state.getCoffee());
    }

    @Test
    void applyEventEffects_clampsBugsToZero() {
        Effects effects = new Effects(0, 0, 0, 0, 0, -500);
        state.applyEventEffects(effects);
        assertEquals(0, state.getBugs());
    }

    @Test
    void applyEventEffects_cashCanGoNegative() {
        Effects effects = new Effects(-100000, 0, 0, 0, 0, 0);
        state.applyEventEffects(effects);
        assertTrue(state.getCash() < 0);
        assertTrue(state.isBankrupt());
    }
}