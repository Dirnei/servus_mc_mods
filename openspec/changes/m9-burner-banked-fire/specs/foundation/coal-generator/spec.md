## MODIFIED Requirements

### Requirement: Generates FE from furnace fuel
The generator SHALL burn any furnace fuel and produce a fixed amount of FE per tick while burning. One burn SHALL last the item's furnace burn time divided by the generator's fuel divisor, counted only on ticks the generator produces. Once its buffer is full the generator SHALL stop, keep its burn, and stay off until the buffer holds less than its `restart_below` fraction of the capacity (example 0.5), then run until the buffer is full again.

#### Scenario: Coal
- **WHEN** one coal is in the fuel slot and the buffer has room
- **THEN** the generator produces 40 FE/t for 400 ticks (20 seconds) with the default stats

#### Scenario: Buffer full
- **WHEN** the internal buffer is full
- **THEN** no new fuel is consumed, a burn in progress pauses with its remaining time kept, and the generator shows as not lit

#### Scenario: Small load
- **WHEN** a full generator feeds a consumer that takes 5 FE/t
- **THEN** the generator stays off and unlit while its buffer drops to half, then runs lit until the buffer is full again, without switching on and off every tick

#### Scenario: Restart below half
- **WHEN** the buffer of a generator that filled up is drained to 60 percent
- **THEN** the generator stays off; once the buffer drops below 50 percent it runs again

### Requirement: GUI and crafting
The generator SHALL have a GUI with the fuel slot, a flame while burning, an energy bar and a status line in place of the inventory label, and SHALL be craftable from vanilla materials. The status line SHALL read "Running" while it burns, "Waiting: restarts below N FE" while its restart threshold holds it off, with N the energy the buffer must drop below, and "Out of fuel" while its buffer has room but it has no fuel. While the restart threshold is below 1, the energy bar SHALL show a small mark at the restart level.

#### Scenario: Open GUI
- **WHEN** the player right clicks the generator
- **THEN** the GUI shows the fuel slot, the burn flame, the stored energy and the status line

#### Scenario: Waiting shown
- **WHEN** the player opens the GUI of a generator whose full buffer dropped to 60 percent
- **THEN** the status line reads "Waiting: restarts below 8000 FE" with the default stats, no flame shows, and the energy bar has a mark at half its height

#### Scenario: Out of fuel shown
- **WHEN** the player opens the GUI of a generator with room in its buffer and an empty fuel slot
- **THEN** the status line reads "Out of fuel"

#### Scenario: Recipe
- **WHEN** the player crafts with iron ingots, copper ingots and a furnace in the generator pattern
- **THEN** the result is one Coal Generator
