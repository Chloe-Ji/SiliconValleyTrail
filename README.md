# 🚀 Silicon Valley Trail

A replayable CLI game inspired by Oregon Trail, set in the heart of Silicon Valley. Guide a scrappy startup team from San Jose to San Francisco to pitch for Series A funding — managing cash, morale, coffee, hype, compute credits, and bugs along the way.

## Quick Start
### Prerequisites
  Java 17+
  Maven 3.6+

### Build & Run
  git clone https://github.com/Chloe-Ji/SiliconValleyTrail.git
  cd SiliconValleyTrail
  mvn clean compile exec:java

### API Key Setup 
  The game uses OpenWeatherMap API for real-time weather data. Weather affects travel cost and team morale.
  OPENWEATHER_API_KEY=


### Running Without an API Key (Mock Mode)
  No setup needed. If no API key is found or the API is unavailable, the game automatically uses randomized mock weather data. Simply run:
  mvn clean compile exec:java

  The game is fully playable without any API key.

### Run Tests
  mvn test

  To run a specific test class:
  mvn test -Dtest=StartupStateTest


## How to Play

  You manage a startup team traveling through 10 real Silicon Valley locations. Each day you choose an action, encounter random events, and try to reach San Francisco   before your resources run out.

### Actions

| # | Action | Effect |
|---|--------|--------|
| 1 | Travel to next location | Spend cash, morale drops (worse in bad weather) 
| 2 | Rest and recover | Morale restored significantly 
| 3 | Work on product | Uses compute credits, hype increases, bugs increase 
| 4 | Fix bugs | Bugs decrease, morale drops slightly 
| 5 | Marketing push | Costs $1,500, hype increases 
| 6 | Coffee boost | Uses coffee, morale boost (once per day) 
| 7 | Save game | Save progress to file 
| 8 | Quit to menu | Return to main menu 

### Resources

| Resource | Description |
|----------|-------------|
| 💰 Cash | Funds for travel and operations. Hits zero = bankruptcy (game over) 
| 😊 Morale | Team happiness (0–100). Hits zero = burnout (game over) 
| ☕ Coffee | Essential fuel. 2 days without daily supply = morale collapse 
| 📢 Hype | Public interest (0–100). Higher hype boosts event rewards 
| 💻 Compute | Cloud credits for building product 
| 🐛 Bugs | Code defects. Hits 50 = product death (game over) 

### Win & Lose Conditions
  Win: Reach San Francisco with resources intact
  
  Lose: Cash ≤ 0 (bankrupt), Morale ≤ 0 (burnout), or Bugs ≥ 50 (product dead)

### Example Session
```
============================================================
SILICON VALLEY TRAIL - Main Menu
============================================================
1. New Game
2. Load Game
3. Quit

Enter choice (1-3): 1

============================================================
🚀 SILICON VALLEY TRAIL 🚀
============================================================
Your scrappy startup team is embarking on a journey
from San Jose to San Francisco to pitch for Series A funding!

Manage your resources wisely:
  💰 Cash - Don't run out!
  😊 Morale - Keep your team happy
  ☕ Coffee - Essential fuel (2 days without = game over)
  📢 Hype - Public interest in your startup
  💻 Compute - Cloud credits for building product
  🐛 Bugs - Keep them under control

Good luck, founder!
============================================================

Press Enter to begin your journey...

============================================================
Day 1 | San Jose
============================================================
💰 Cash: $50,000 | 😊 Morale: 100/100 | ☕ Coffee: 100
📢 Hype: 10/100 | 💻 Compute: 100 | 🐛 Bugs: 0
📍 Progress: 0% to San Francisco
============================================================
🌤️  Weather: Sunny, 72°F

------------------------------------------------------------
What will you do?
------------------------------------------------------------
1. Travel to next location
   → 💰 spend $200 | 😊 morale drops slightly
   → ⛈️ bad weather: 💰 spend $700 | 😊 morale drops more
2. Rest and recover
   → 😊 morale restored significantly
3. Work on product
   → 💻 uses compute credits | 📢 hype increases | 🐛 bugs increase
   → ⚠️ no compute: 😊 morale drops, build fails
4. Fix bugs
   → 🐛 bugs decrease | 😊 morale drops slightly
5. Marketing push
   → 💰 costs $1,500 | 📢 hype increases
6. Coffee boost
   → ☕ uses coffee | 😊 morale boost
7. Save game
8. Quit to menu

Enter choice (1-8): 1

🚗 Your team hits the road...
✅ Arrived at Santa Clara!

!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
📰 EVENT: VC Pitch Opportunity — A VC firm wants to hear your pitch!
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
1. Prepare and pitch (risky but big reward)
   💰+8000 😊+10 ☕-5 📢+15
2. Decline politely (safe)

Enter choice (1-2): 1

Press Enter to continue...
```

## Architecture
```
src/
├── Main.java                  # Entry point
├── model/
│   ├── StartupState.java      # Game state and resource management
│   ├── Location.java          # Location record (name, miles, lat/lon)
│   ├── Event.java             # Event record with two choices
│   ├── Effects.java           # Resource change bundle
│   └── WeatherData.java       # Weather record
├── core/
│   ├── GameRunner.java        # Main game loop and action handling
│   ├── RouteMap.java          # 10 real locations from San Jose to SF
│   └── EventManager.java      # Random event pool with choices
├── api/
│   └── WeatherService.java    # OpenWeatherMap integration with fallback
├── ui/
│   └── DisplayManager.java    # All display/print logic
└── util/
    ├── InputHandler.java       # Input validation
    └── SaveManager.java        # JSON save/load with Gson
```

### Dependencies

| Library | Purpose |
|---------|---------|
| Gson 2.11.0 | JSON parsing for API responses and save/load |
| dotenv-java 3.0.0 | Load API keys from `.env` file |
| JUnit 5 | Unit testing |

## Design Notes
### Game Loop & Balance

Each turn follows: Display Status → Show Weather → Player Chooses Action → Resolve Action → Trigger Event → Check Win/Lose.

Balance is built around resource tension. Cash drains daily ($1,000 fixed expense + action costs). Coffee depletes automatically (3 per day) and must be managed — 2 days without triggers morale collapse. Building product increases hype but adds bugs. Marketing costs cash. Every action has a tradeoff, forcing strategic decisions.

Coffee boost does not consume a turn (limited to once per day), allowing tactical use alongside other actions. Save and quit also do not consume a turn.

### Why OpenWeatherMap & How It Affects Gameplay

Why this API: Weather is a natural gameplay modifier — it affects travel cost and morale. OpenWeatherMap provides a free tier with a straightforward JSON response format, and requires an API key, which demonstrates proper secret management with `.env` files.

How it affects gameplay: Bad weather (rain, thunderstorm, snow — weather ID < 700) increases travel cost by $500 and reduces morale by an extra 10 points. Good weather has no penalty. Temperature is randomized ±10°F from the real value and weather type is partially randomized to add variety between the closely-spaced Silicon Valley locations.

Fallback strategy: Two-layer graceful degradation:
1. With API key → real weather from OpenWeatherMap, randomized for variety
2. Without key or API failure → randomized mock weather data

### Data Modeling

- Records (`Location`, `Event`, `Effects`, `WeatherData`) for immutable data structures, leveraging Java's record feature for concise, readable code.
- `StartupState` is the only mutable class. All fields are private with controlled mutation through dedicated methods (`rest()`, `buildProduct()`, `travelToNextStop()`, `fixBugs()`, `marketingPush()`, `boostMorale()`). A central `clampAll()` method ensures all resource values stay within valid bounds after every mutation.
- `Effects` record bundles six resource changes (cash, morale, compute, coffee, hype, bugs) into a single object, used by both the event system and the display layer. This avoids passing loose integers and makes the code self-documenting.
- Events contain a description, two choice labels, and two `Effects` objects — one per choice. This enables risk-vs-reward decisions at every event. Events with `null` choices (e.g., "Nothing eventful today") are applied automatically without player input.
- Save/Load via Gson serialization of `StartupState` to `save.json`. Single save slot — sufficient for a single-player CLI game.

### Error Handling

- API timeout/failure: Caught in try-catch, falls back to mock weather with a warning message. Game never crashes due to network issues.
- Invalid user input: `InputHandler` loops with a clear error message until a valid number in range is entered.
- Missing `.env` file: `dotenv-java` configured with `ignoreIfMissing()` — the game runs without it, falling back to mock weather.
- Missing save file: `SaveManager.load()` returns null, and `GameRunner` stays on the main menu with a message.
- Resource boundaries: `clampAll()` prevents morale and hype from exceeding 0–100, and coffee/compute/bugs from going negative. Cash is intentionally not clamped — negative cash triggers the `isBankrupt()` game-over condition.

### Tradeoffs & "If I Had More Time"

- CLI over GUI: Prioritized game logic, clean architecture, and separation of concerns over visual polish. The `DisplayManager` class isolates all print logic, so a GUI layer could replace it without touching game logic.
- Single save slot: Sufficient for a single-player game. Multi-slot support would only require parameterizing the save filename (e.g., `save_{slot}.json`).
- Weather randomization: Real weather between nearby Silicon Valley cities is nearly identical. Added randomization on top of real API data as a compromise — the API integration is genuine, while the randomization ensures meaningful gameplay impact.
- Event pool size: Currently 7 events. With more time, the pool would expand to 20+ events, including conditional events based on resource levels (e.g., a "team mutiny" event when morale is below 30).
- If more time: Add a scoring system based on final resources and days taken, implement difficulty levels, create a web-based UI with Spring Boot, add persistent high scores, and integrate a second API (e.g., Hacker News Algolia API for hype-related events tied to real trending topics).

### Tests

Unit tests cover core game mechanics:

- StartupStateTest**: Resource mutations, boundary conditions (clamp behavior), game-over triggers (bankrupt, burnout, product dead), coffee withdrawal logic, and daily settlement.
- EventManagerTest: Random event generation, event effect application, null-choice event handling.
- RouteMapTest: Location retrieval, destination detection, progress calculation.
- WeatherServiceTest: API response parsing, fallback behavior when API is unavailable, weather randomization.
