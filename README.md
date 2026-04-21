# 🚀 Silicon Valley Trail

A replayable CLI game inspired by Oregon Trail, set in the heart of Silicon Valley. Guide a scrappy startup team from San Jose to San Francisco to pitch for Series A funding — managing cash, morale, coffee, and compute credits along the way.

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

The game integrates two public APIs. Both degrade gracefully — the game runs offline and without any tokens.

**1. Open-Meteo (weather) — no key required**
The game calls the free [Open-Meteo](https://open-meteo.com/) API for real-time weather. Bad weather (rain, drizzle, snow, thunderstorm) adds $500 to travel cost and drops morale. Two events (Power Outage, Foggy 101 Accident) are gated on live weather conditions and only fire when the weather matches. On network failure the game falls back to randomized mock weather.

**2. Mapbox Directions (traffic) — optional, requires free token**
When a `MAPBOX_TOKEN` is configured, each travel leg fetches the traffic-aware driving duration between the two cities (two calls: `driving-traffic` vs. `driving`). If current traffic is >1.5× the free-flow baseline, the leg triggers a morale dip. Without a token the game prints a one-line hint at startup and skips this feature — there is no mock fallback for Mapbox, it just disables cleanly.

**Setting up Mapbox (optional):**

Sign up for a free Mapbox token (no credit card) at https://account.mapbox.com/access-tokens/, then pick one of three ways to make it available:

1. Edit the committed `.env` file. Run `git update-index --skip-worktree .env` once so your token never gets staged, then set `MAPBOX_TOKEN=pk.your-token-here`:
   ```bash
   git update-index --skip-worktree .env
   # edit .env: MAPBOX_TOKEN=pk.your-token-here
   mvn exec:java
   ```

2. Export for the current shell session:
   ```bash
   export MAPBOX_TOKEN=pk.your-token-here
   mvn exec:java
   ```

3. One-shot inline:
   ```bash
   MAPBOX_TOKEN=pk.your-token-here mvn exec:java
   ```

Resolution order: `System.getenv("MAPBOX_TOKEN")` first, then a `MAPBOX_TOKEN=` line in `.env`. If neither is set, the game prints one warning at startup and plays normally.

**Running with mocks / offline:** the game runs fully offline out of the box. Open-Meteo failures fall back to a randomized mock weather pool automatically; Mapbox has no mock — it silently disables itself when unconfigured or unreachable. Save/load uses local disk, and tests stub `HttpClient` so `mvn test` passes offline.

### Run Tests

```bash
mvn test
```

To run a specific test class:

```bash
mvn test -Dtest=StartupStateTest
```

## How to Play

You manage a startup team across 10 real Silicon Valley locations. Each day you pick an action, may hit a random event, and try to reach San Francisco before resources run out.

### Actions

Options 1–3 advance the day and each pay the fixed daily tax (−$1,000 cash, −3 coffee) on top of their per-action effect. Options 4–6 are free.

| # | Action | Effect (on top of the daily tax for 1–3) | Advances day? |
|---|--------|--------|---|
| 1 | Travel to next location | Spend cash, morale drops (worse in bad weather). Only this action fires a random event on arrival. | ✅ |
| 2 | Rest and recover | Morale restored significantly (+30, capped at 100) | ✅ |
| 3 | Work on product | With compute: spend 10 compute, earn $1,500 revenue. Without compute: build fails, morale drops 10. | ✅ |
| 4 | Coffee boost | Spend 5 coffee, morale +15 (once per day) | ❌ |
| 5 | Save game (explicit) | Writes `save.json` with a "💾 Game saved!" banner | ❌ |
| 6 | Quit to menu | Returns to the main menu; **unsaved progress is lost** — save first if you want to resume | ❌ |

### Resources

All four resources are outcome-affecting — there are no vanity counters.

| Resource | Description |
|----------|-------------|
| 💰 Cash | Funds for travel, daily operations, and revenue from shipped product. Hits zero = bankruptcy (game over) |
| 😊 Morale | Team happiness (0–100). Hits zero = burnout (game over) |
| ☕ Coffee | Essential fuel. 2 days without daily supply forces morale to -30 = burnout |
| 💻 Compute | Cloud credits. With compute, "Work on product" earns $1,500 cash per day. Once exhausted, the same action instead drops morale by 10 — so compute is a cash-earning buffer whose depletion pushes you toward burnout. |

### Win & Lose Conditions

- **Win**: Reach San Francisco with resources intact
- **Lose**: Cash ≤ 0 (bankrupt) or Morale ≤ 0 (burnout)

### Saving and Exiting

Progress is written to `save.json` only when you explicitly save; there is no auto-save.

| Trigger | Effect |
|---|---|
| Menu option **5 — Save game** | Writes `save.json` and prints `💾 Game saved!` |
| Menu option **6 — Quit to menu** | Returns to the main menu; **unsaved progress is lost** |
| Main menu option **3 — Quit** | Exits the JVM; previously-saved progress remains on disk for the next launch |
| **Ctrl+C** / terminal close / SSH disconnect | Abrupt exit; **unsaved progress is lost** |

On next launch, pick **Load Game** to resume from your last save, or **New Game** to start fresh.

### Example Session

The live UI renders action-hint lines in cyan italics; they're shown here as plain text for readability.

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
Good luck, founder!
============================================================

============================================================
Day 1 | San Jose
============================================================
💰 Cash: $20000 | 😊 Morale: 70/100
☕ Coffee: 50 | 💻 Compute: 100
📍 Progress: 0% to San Francisco
============================================================
🌤️  Weather: Clear sky, 72°F

------------------------------------------------------------
What will you do?
Options 1–3 advance the day: -$1,000 cash, -3 ☕
------------------------------------------------------------
1. Travel to next location
   -> spend 200 | morale drops 5 (bad weather: spend 500 | morale drops 15)
2. Rest and recover
   -> morale boost 30
3. Work on product
   -> uses 10 compute credits | earn $1500 revenue (no compute credits: morale drops 10)
------------------------------------------------------------
Free actions (no day advance):
4. Coffee boost (once per day)
   -> uses 5 coffee | morale boost 15
5. Save game
6. Quit to menu
Enter choice (1-6): 1

🚗 Your team hits the road...
✅ Arrived at Santa Clara!

!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
📰 EVENT: VC Pitch Opportunity — A VC firm wants to hear your pitch!
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
🌟Choice 1: Prepare and pitch (risky but big reward)
   -> +$8000 cash, +10 morale, -5 coffee
🌟Choice 2: Decline politely (safe)
   -> no resource change
Enter choice (1-2): 1
```

## Beyond the Brief

Features built on top of the core requirements:

- **Live daily dashboard.** Each turn opens with a status header showing cash, morale, coffee, compute, progress-to-SF percentage, and current weather — all in one glance, so the player can plan without hunting through menus.
- **Upfront reward/penalty disclosure.** Every action and event choice lists its exact resource impact inline in the menu (e.g., `-$1,000 cash, -3 coffee` on the daily tax line, `+$8000 cash, +10 morale, -5 coffee` on event choices). No hidden costs — players always know what a choice will do before confirming.
- **Once-per-day coffee boost.** A free tactical action (#4) that spends 5 coffee for +15 morale, rate-limited to one use per day. Adds a meaningful short-term lever: burn coffee now for a morale lift, or hold it for a rougher day.
- **Two external APIs blended with probabilistic gameplay.** Open-Meteo (weather) and Mapbox (traffic) both feed live data into the game — bad weather raises travel cost and gates two conditional events, and heavy traffic is applied via a probabilistic roll so the feature stays lively whether the token is configured or not, and whether the player runs at rush hour or midnight.

## Architecture

All sources live under the `io.github.chloeji.svtrail` package.

```
src/main/java/io/github/chloeji/svtrail/
├── Main.java                  # Entry point
├── model/
│   ├── StartupState.java      # Game state and resource management
│   ├── Location.java          # Location record (name, milesFromStart, lat/lon)
│   ├── Event.java             # Event record with two choices + optional weather predicate
│   ├── Effects.java           # Resource change bundle
│   ├── WeatherData.java       # Weather record
│   └── RouteInfo.java         # Traffic-aware duration result from MappingService
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

Each turn follows: Display Status → Show Weather → Player Chooses Action → Resolve Action → (on Travel only) Trigger Random Event → Check Win/Lose. Weather is fetched once at the start of each day so free actions don't re-hit the API.

Random events fire **only on arrival at a new city** — after a Travel action. Rest and Build resolve their effect and end the day without an event, which prevents players from farming positive events by resting in place.

Balance is built around four outcome-affecting resources. The starting pile ($20,000 cash, 70 morale, 50 coffee, 100 compute) gives ~10–15 days of runway before daily costs force a loss, so bankruptcy and burnout are both reachable in a normal 10-stop run. Work on product is a compute-for-cash converter while compute lasts; once exhausted, the same action becomes a morale sink. Every action is a tradeoff — there is no dominant strategy.

### Why Open-Meteo & How It Affects Gameplay

Weather is a natural gameplay modifier for travel cost and morale, and Open-Meteo is free, key-less, and uses standard WMO interpretation codes. Bad weather (drizzle, rain, snow, thunderstorm — WMO ≥ 51) adds $500 to travel cost and costs 15 morale; clear, cloudy, and foggy conditions (codes 0–48) have no penalty. Temperature is jittered ±10°F and weather type is partially randomized on top of real API data, since real weather between these closely-spaced cities is nearly identical.

Weather also gates **two conditional events**: `Power Outage` only during thunderstorms and `Foggy 101 Accident` only in fog. `Event.condition` is a nullable `Predicate<WeatherData>` — unconditional events have `null` and are always eligible.

Fallback: on network failure or non-2xx response, `WeatherService` returns randomized mock weather.

### Why Mapbox Directions & How It Affects Gameplay

Mapping/Routing was a natural fit for the second API. Mapbox returns a traffic-aware driving duration between two lat/lon points, which maps directly to the "stuck in traffic" gameplay hook.

On each Travel action, `MappingService.getRouteInfo` makes two calls (`driving-traffic` and `driving`) and flags the leg as heavy when `trafficDuration > 1.5 × freeFlowDuration`. When the leg is ruled heavy traffic the team takes a -5 morale hit, stacking with the base travel cost and the weather penalty.

**Probabilistic application** — the heavy-traffic penalty is not applied every time Mapbox reports congestion; it is rolled per leg so the feature does not hinge on the player's real-world clock. The probability is a **25% baseline** on every leg, plus a **+50-percentage-point boost** when Mapbox is configured and reports the leg as heavy:

| Condition | P(heavy traffic this leg) |
| --- | --- |
| Mapbox configured, reports heavy | 75% |
| Mapbox configured, reports clear | 25% |
| Mapbox unconfigured / API failed | 25% |

Without this, off-peak players would never see the feature and rush-hour players would be penalized on every leg; the roll smooths both extremes while keeping Mapbox's real signal meaningful. The random baseline also means the game still has traffic variance when the token is absent — only the Mapbox-boost upside is gated on the API being available.

Token resolution checks `System.getenv("MAPBOX_TOKEN")` first, then a `MAPBOX_TOKEN=` line in `.env` at the project root. When both are missing, `GameRunner.start` prints one warning; any network, JSON, or non-2xx failure during play degrades silently to the 25%-baseline path.

### Data Modeling

- Records (`Location`, `Event`, `Effects`, `WeatherData`, `RouteInfo`) for immutable data structures.
- `StartupState` is the only mutable class. Mutation is funneled through dedicated action methods (`rest()`, `buildProduct()`, `travelToNextStop()`, `coffeeBoost()`, `applyEventEffects()`), each capping morale at 100 and keeping coffee/compute non-negative.
- `Effects` bundles four resource changes (cash, morale, compute, coffee) into one object, used by both the event system and the heavy-traffic penalty in `GameRunner`.
- Events hold a description, two choice labels, two `Effects` objects, and an optional `Predicate<WeatherData>` condition. Events with `null` choices (e.g., "Nothing eventful today") apply automatically. The pool has **7 events**: 5 unconditional (4 with choices, 1 quiet day) plus 2 weather-conditional (Power Outage on thunderstorm, Foggy 101 Accident on fog).
- `RouteInfo` captures one leg's result (traffic-aware minutes, free-flow minutes, heavy-traffic flag). Consumed and discarded inside `GameRunner.travel`; never persisted.
- Save/Load serializes `StartupState` to `save.json` via Gson. Single save slot, no auto-save.

### Error Handling

- Open-Meteo timeout/failure: caught and falls back to mock weather with a warning. Game never crashes on network issues.
- Mapbox timeout/failure or missing token: `MappingService.getRouteInfo` returns `null`, the travel action skips the heavy-traffic effect, and a single startup hint shows when the token is absent.
- Invalid user input: `InputHandler` loops with a clear error until a valid number in range is entered.
- Missing save file: `SaveManager.load()` returns null and `GameRunner` stays on the main menu with a message.
- Resource boundaries: inline caps keep morale ≤ 100 and coffee/compute ≥ 0. Cash and morale are unclamped on the low side — negative cash triggers `isBankrupt()`, negative morale triggers `isBurnOut()`.
- Both APIs down: each degrades independently, so the game uses mock weather, skips Mapbox, and plays like the offline version.
- Ctrl+C mid-turn: JVM exits immediately; unsaved progress is lost.

### Tradeoffs & "If I Had More Time"

**Tradeoffs:**

- **Mapbox: probability over raw signal.** The heavy-traffic penalty doesn't follow Mapbox's output exactly — it's mediated by a probabilistic roll. Upside: the feature stays alive even when Mapbox is unreachable or the player runs off-peak, so the game never goes quiet on this dimension. Downside: real-world traffic data is partially ignored. I took this tradeoff because the probabilistic roll makes the game livelier and more varied across sessions.
- **CLI over GUI:** Prioritized game logic, clean architecture, and separation of concerns over visual polish. The `DisplayManager` class isolates all print logic, so a GUI layer could replace it without touching game logic.

**If I had more time:**

- **Richer win/lose model.** Today only cash and morale determine the outcome. Success should also depend on things like how hot the startup's topic is, the quality of the final product, and whether the build ships with bugs. I'd expand `StartupState` and the end-of-game evaluation to reflect these dimensions.
- **Random events with real weight.** Events currently nudge resources but rarely swing the run. I'd tune effects so each event meaningfully pushes the game toward winning or losing — making the player's choice a decision that actually matters.

### Tests

Unit tests cover core game mechanics — 79 tests total across four classes:

- **StartupStateTest**: resource mutations, boundary clamping, game-over triggers, coffee withdrawal, and daily settlement. (32 tests)
- **EventManagerTest**: random event generation, effect application, null-choice handling, and weather-conditional filtering (thunderstorm/foggy events fire only under matching conditions). (14 tests)
- **RouteMapTest**: location retrieval, destination detection, progress calculation, distance between stops. (12 tests)
- **MappingServiceTest**: token detection, short-circuit when unconfigured, JSON parsing, heavy-traffic flagging (ratio > 1.5), graceful degradation on network/parse failure, and two-tier token resolution (env var → `.env` file). Uses a stubbed `HttpClient` and JUnit `@TempDir` so tests run offline. (21 tests)

## AI Usage

AI assistance was used for:

- Comments and JavaDoc throughout the codebase.
- Documentation — README phrasing and the Design Notes section.
- Git workflow prose — commit messages and pull request descriptions.
- Some unit tests, notably the `HttpClient` test doubles in `MappingServiceTest`.

I am responsible for everything else. AI also served as a sounding board on a few design choices.
