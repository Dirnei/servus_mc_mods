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
