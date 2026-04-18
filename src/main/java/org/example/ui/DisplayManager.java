package org.example.ui;

import org.example.model.Event;
import org.example.model.Location;
import org.example.model.StartupState;
import org.example.model.WeatherData;

public class DisplayManager {
    public void printMainMenu() {
        System.out.println("\n============================================================");
        System.out.println("SILICON VALLEY TRAIL - Main Menu");
        System.out.println("============================================================");
        System.out.println("1. New Game");
        System.out.println("2. Load Game");
        System.out.println("3. Quit");
    }

    public void printIntro() {
        System.out.println("\n============================================================");
        System.out.println("🚀 SILICON VALLEY TRAIL 🚀");
        System.out.println("============================================================");
        System.out.println("Your scrappy startup team is embarking on a journey");
        System.out.println("from San Jose to San Francisco to pitch for Series A funding!");
        System.out.println("\nManage your resources wisely:");
        System.out.println("  💰 Cash - Don't run out!");
        System.out.println("  😊 Morale - Keep your team happy");
        System.out.println("  ☕️ Coffee - Essential fuel (2 days without = Morale drops dramatically)");
        System.out.println("  📢 Hype - Public interest in your startup");
        System.out.println("  💻 Compute Credits - Cloud credits for building product");
        System.out.println("\nGood luck, founder!");
        System.out.println("============================================================");
    }

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
                + " | 🐛 Bug: " + state.getBugs());

        System.out.println("📍 Progress: " + progress + "% to San Francisco");
        System.out.println("============================================================");
        System.out.println("🌤️  Weather: " + weather.condition() + ", " + weather.temperature() + "°F");
        if (weather.isBadWeather()) {
            System.out.println("   Bad weather may slow your progress!");
        }
    }

    public void printActionMenu() {
        System.out.println("\n------------------------------------------------------------");
        System.out.println("What will you do?");
        System.out.println("------------------------------------------------------------");
        System.out.println("1. Travel to next location");
        System.out.println("   → spend 200 ｜ morale drops 5 (bad weather: spend 500 ｜ morale drops 15)");
        System.out.println("2. Rest and recover");
        System.out.println("   → morale boost 30");
        System.out.println("3. Work on product");
        System.out.println("   → uses 10 compute credits ｜ hype increases 20 (no compute credits: morale drops 15) | 🐛 bugs increase");
        System.out.println("4. Fix bugs");
        System.out.println("   → 🐛 bugs decrease 5 | 😊 morale drops 10");
        System.out.println("5. Marketing push (costs $1500)");
        System.out.println("   → spend 1500 ｜ hype increases 15");
        System.out.println("6. Coffee boost (extra coffee for morale)");
        System.out.println("   → uses 5 coffee ｜ morale boost 15");
        System.out.println("7. Save game");
        System.out.println("8. Quit to menu");
    }


    public void printEventDescription(Event event) {
        System.out.println("\n!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("📰 EVENT: " + event.description());
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    }
    public void printEventChoices(Event event) {
        System.out.println("🌟Choice 1: " + event.choice1());
        System.out.println("🌟Choice 2: " + event.choice2());
    }

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
