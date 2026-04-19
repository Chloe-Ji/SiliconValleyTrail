# 🚀 Silicon Valley Trail

A replayable CLI game inspired by Oregon Trail, set in the heart of Silicon Valley. Guide a scrappy startup team from San Jose to San Francisco to pitch for Series A funding — managing cash, morale, coffee, hype, compute credits, and bugs along the way.

## Quick Start
### Prerequisites

- Java 21+
- Maven 3.6+

### Build & Run

```bash
git clone https://github.com/Chloe-Ji/SiliconValleyTrail.git
cd SiliconValleyTrail
mvn clean compile exec:java
```

### External APIs

The game integrates two public APIs that change gameplay. Both degrade gracefully — the game runs fully offline or without any tokens.

**1. Open-Meteo (weather) — no key required**
The game calls the free [Open-Meteo](https://open-meteo.com/) API for real-time weather. Bad weather (rain, drizzle, snow, thunderstorm) adds $500 to travel cost and drops morale. Certain events (Power Outage, Server Overheating, Foggy 101 Accident) are gated on live weather conditions and only fire when the weather matches. On network failure the game falls back to randomized mock weather.

**2. Mapbox Directions (traffic & distance) — optional, requires free token**
When a `MAPBOX_TOKEN` is configured, each travel leg fetches real driving distance and traffic-aware duration between the two cities. Heavy traffic (>1.5× free-flow duration) triggers a morale dip; long legs (>6 miles) add a small fuel surcharge. Without a token the game prints a one-line hint at startup and skips these features.

**Setting up Mapbox (optional):**
```bash
cp .env.example .env         # local copy ignored by git
# paste your token into .env, then:
export MAPBOX_TOKEN=pk.xxx
mvn exec:java
# or inline:
MAPBOX_TOKEN=pk.xxx mvn exec:java
```
Sign up for a free Mapbox token (no credit card) at https://account.mapbox.com/access-tokens/.

### Run Tests

```bash
mvn test
```

To run a specific test class:

```bash
mvn test -Dtest=StartupStateTest
```


## How to Play

You manage a startup team traveling through 10 real Silicon Valley locations. Each day you choose an action, encounter random events, and try to reach San Francisco before your resources run out.

### Actions

| # | Action | Effect |
|---|--------|--------|
| 1 | Travel to next location | Spend cash, morale drops (worse in bad weather) |
| 2 | Rest and recover | Morale restored significantly |
| 3 | Work on product | Uses compute credits, hype increases, bugs increase |
| 4 | Fix bugs | Bugs decrease, morale drops slightly |
| 5 | Marketing push | Costs $1,500, hype increases |
| 6 | Coffee boost | Uses coffee, morale boost (once per day) |
| 7 | Save game | Save progress to file |
| 8 | Quit to menu | Return to main menu |

### Resources

| Resource | Description |
|----------|-------------|
| 💰 Cash | Funds for travel and operations. Hits zero = bankruptcy (game over) |
| 😊 Morale | Team happiness (0–100). Hits zero = burnout (game over) |
| ☕ Coffee | Essential fuel. 2 days without daily supply = morale collapse |
| 📢 Hype | Public interest (0–100). Higher hype boosts event rewards |
| 💻 Compute | Cloud credits for building product |
| 🐛 Bugs | Code defects. Accumulate when building product; fix them to keep the codebase healthy |

### Win & Lose Conditions

- **Win**: Reach San Francisco with resources intact
- **Lose**: Cash ≤ 0 (bankrupt) or Morale ≤ 0 (burnout)

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

All sources live under the `io.github.chloeji.svtrail` package.

```
src/main/java/io/github/chloeji/svtrail/
├── Main.java                  # Entry point
├── model/
│   ├── StartupState.java      # Game state and resource management
│   ├── Location.java          # Location record (name, miles, lat/lon)
│   ├── Event.java             # Event record with two choices + optional weather predicate
│   ├── Effects.java           # Resource change bundle
│   ├── WeatherData.java       # Weather record
│   └── RouteInfo.java         # Distance/duration result from MappingService
├── core/
│   ├── GameRunner.java        # Main game loop and action handling
│   ├── RouteMap.java          # 10 real locations from San Jose to SF
│   └── EventManager.java      # Random event pool with weather filtering
├── api/
│   ├── WeatherService.java    # Open-Meteo integration with fallback
│   └── MappingService.java    # Mapbox Directions (optional, token-gated)
├── ui/
│   └── DisplayManager.java    # All display/print logic
└── util/
    ├── InputHandler.java      # Input validation
    └── SaveManager.java       # JSON save/load with Gson
```

### Dependencies

| Library | Purpose |
|---------|---------|
| Gson 2.10.1 | JSON parsing for API responses and save/load |
| JUnit 5 | Unit testing |

## Design Notes
### Game Loop & Balance

Each turn follows: Display Status → Show Weather → Player Chooses Action → Resolve Action → (on Travel only) Trigger Random Event → Check Win/Lose.

Random events fire **only on arrival at a new city** — i.e. after a Travel action. Rest, Build, Fix Bugs, and Marketing resolve their effect and end the day without firing an event. This matches the spec's "event at each location after movement" rule and prevents players from farming positive events by resting in place.

Balance is built around resource tension. Cash drains daily ($1,000 fixed expense + action costs). Coffee depletes automatically (3 per day) and must be managed — 2 days without triggers morale collapse. Building product increases hype but adds bugs. Marketing costs cash. Every action has a tradeoff, forcing strategic decisions.

Coffee boost does not consume a turn (limited to once per day), allowing tactical use alongside other actions. Save and quit also do not consume a turn.

### Why Open-Meteo & How It Affects Gameplay

Why this API: Weather is a natural gameplay modifier — it affects travel cost and morale. Open-Meteo provides a free, key-less HTTP endpoint with a minimal JSON response, so the setup story for a player running the game is simply "clone and run". It uses the standard WMO weather interpretation codes, which map cleanly onto the game's condition categories.

How it affects gameplay: Bad weather (drizzle, rain, snow, thunderstorm — WMO code ≥ 51) increases travel cost by $500 and reduces morale by an extra 15 points. Clear, cloudy, and foggy conditions (codes 0–48) have no penalty. Temperature is jittered ±10°F from the real value and weather type is partially randomized on top of real API data to add variety between the closely-spaced Silicon Valley locations.

The weather also gates **three conditional events** in the event pool: `Power Outage` only fires during thunderstorms, `Server Overheating` only when the temperature exceeds 80°F, and `Foggy 101 Accident` only when the condition is foggy. `Event.condition` is a nullable `Predicate<WeatherData>` — unconditional events have `null` and are always eligible.

Fallback strategy: Two-layer graceful degradation:
1. Call Open-Meteo → real weather, randomized for variety
2. On network failure or non-2xx response → randomized mock weather data

### Why Mapbox Directions & How It Affects Gameplay

Why this API: The spec asked for at least one external API and suggested Mapping/Routing as one of the categories. Mapbox returns real driving distance and a traffic-aware duration between two lat/lon points, which maps directly to the "long leg today" and "stuck in traffic" gameplay hooks the spec called out. Free tier signup takes under a minute, no credit card required.

How it affects gameplay: On each Travel action, `MappingService.getRouteInfo` makes two calls (`driving-traffic` for current conditions, `driving` for the free-flow baseline). If `trafficDuration > 1.5 × freeFlowDuration` the leg is flagged "heavy traffic" and the team takes a -5 morale hit. If distance > 6 miles the leg is a "long leg" and a $100 fuel surcharge is deducted. Both effects stack with the base travel cost and the weather penalty.

Token handling: The token is read from `MAPBOX_TOKEN` at construction time. `.env.example` documents the variable and is the only piece of Mapbox config checked into git — the actual token lives outside source control. When the token is missing or blank, `MappingService.isConfigured()` returns false; `GameRunner.start` prints a one-line hint at launch and the Mapbox code path short-circuits. No network is contacted and the game runs with the base travel cost. Any network / JSON / non-2xx failure is caught and degraded to the same "no Mapbox" code path.

### Data Modeling

- Records (`Location`, `Event`, `Effects`, `WeatherData`, `RouteInfo`) for immutable data structures, leveraging Java's record feature for concise, readable code.
- `StartupState` is the only mutable class. Mutation is funneled through dedicated action methods (`rest()`, `buildProduct()`, `travelToNextStop()`, `fixBugs()`, `marketingPush()`, `coffeeBoost()`, `applyEventEffects()`). Each method applies inline clamping so morale and hype stay within `[0, 100]` and coffee/compute/bugs never go negative. Cash is intentionally left unclamped so negative balances can trigger the bankruptcy game-over condition.
- `Effects` record bundles six resource changes (cash, morale, compute, coffee, hype, bugs) into a single object, used by both the event system and the display layer. This avoids passing loose integers and makes the code self-documenting.
- Events contain a description, two choice labels, two `Effects` objects — one per choice — and an optional `Predicate<WeatherData>` condition. Unconditional events have `null` for the predicate. This enables risk-vs-reward decisions at every event. Events with `null` choices (e.g., "Nothing eventful today", "Press Feature in TechCrunch") are applied automatically without player input. The pool currently contains **12 events**: 9 unconditional (8 with choices, 1 quiet day) plus 3 weather-conditional.
- `RouteInfo` captures the single leg result from `MappingService` (miles, traffic-aware minutes, free-flow minutes, heavy-traffic flag). It is never persisted — consumed and discarded inside `GameRunner.travel`.
- Save/Load via Gson serialization of `StartupState` to `save.json`. Single save slot — sufficient for a single-player CLI game.

### Error Handling

- Open-Meteo timeout/failure: caught in try-catch, falls back to mock weather with a warning message. Game never crashes due to network issues.
- Mapbox timeout/failure or missing token: `MappingService.getRouteInfo` returns `null`. The travel action silently skips the traffic/long-leg effects and applies base travel cost only. A single startup hint informs the user when the token is absent.
- Invalid user input: `InputHandler` loops with a clear error message until a valid number in range is entered.
- Missing save file: `SaveManager.load()` returns null, and `GameRunner` stays on the main menu with a message.
- Resource boundaries: inline clamping in each mutation method prevents morale and hype from exceeding 0–100, and coffee/compute/bugs from going negative. Cash is intentionally not clamped — negative cash triggers the `isBankrupt()` game-over condition.
- Both APIs down simultaneously: both degrade independently; the game uses randomized mock weather, skips Mapbox, and plays exactly like the offline version.

### Tradeoffs & "If I Had More Time"

- CLI over GUI: Prioritized game logic, clean architecture, and separation of concerns over visual polish. The `DisplayManager` class isolates all print logic, so a GUI layer could replace it without touching game logic.
- Single save slot: Sufficient for a single-player game. Multi-slot support would only require parameterizing the save filename (e.g., `save_{slot}.json`).
- Weather randomization: Real weather between nearby Silicon Valley cities is nearly identical. Added randomization on top of real API data as a compromise — the API integration is genuine, while the randomization ensures meaningful gameplay impact.
- Event pool size: Currently 12 events (9 unconditional + 3 weather-conditional). With more time, the pool would expand to 20+ including events conditional on resource levels (e.g., a "team mutiny" event when morale is below 30) and time-of-journey (e.g., "pre-pitch jitters" on the last leg).
- If more time: Add a scoring system based on final resources and days taken, implement difficulty levels, create a web-based UI with Spring Boot, add persistent high scores, and integrate a news/trends API (e.g., Hacker News Algolia) for hype-related events tied to real trending topics. The `MappingService` interface would also swap cleanly to Google Maps Directions by changing the URL and JSON path — handy if you already have a billing-enabled GCP project.

### Tests

Unit tests cover core game mechanics — 82 tests total across four classes:

- **StartupStateTest**: Resource mutations, boundary conditions (clamp behavior), game-over triggers (bankrupt, burnout), coffee withdrawal logic, and daily settlement. (42 tests)
- **EventManagerTest**: Random event generation, effect application, null-choice event handling, and weather-conditional filtering (verifies hot-weather events can't fire in clear skies and vice versa). (16 tests)
- **RouteMapTest**: Location retrieval, destination detection, progress calculation, distance between stops. (12 tests)
- **MappingServiceTest**: Token configuration detection, short-circuit when unconfigured, JSON parsing, heavy-traffic flagging (ratio > 1.5), and graceful degradation on network failure, non-2xx status, malformed JSON, and empty routes. Uses an in-test `HttpClient` stub so the tests run offline. (12 tests)

## AI Usage

AI assistance was used on this project for:

- **Boilerplate generation**: JavaDoc comments, unit test assertion scaffolding, and constructors for test stubs.
- **Static data lookup**: the 10-city lat/lon list, WMO weather-code → human-description mappings, and Mapbox Directions API URL format.
- **Prose polish**: README phrasing and the design-notes section.

I designed the package architecture, wrote the core game loop and `StartupState` mutation logic, chose the trade-offs (single mutable state class, records for everything else, event-only-after-travel rule, graceful API fallback strategy), wrote the weather-conditional event predicate design, and reviewed every AI suggestion before accepting it. The code here is intended to be fully owned — if you point at any line, I can explain why it's there.
