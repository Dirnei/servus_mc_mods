## MODIFIED Requirements

### Requirement: Calciner burns fuel
The Calciner SHALL be a burner machine following the shared machine rules: one input slot and one fuel slot that takes any furnace fuel, burning one item when the previous burn has ended and work is available, with no FE buffer and no energy capability. Its burn SHALL count down only while it calcines; while it has no work, a burn in progress SHALL pause with its remaining time kept and the Calciner SHALL show as not lit. It SHALL work only while its structure is formed. It SHALL start and finish a cycle only when its Sulfur tank and its Mercury tank have room for the recipe's Sulfur and Mercury and its Ash Pan has room for the recipe's Salt. It SHALL report its fuel as upkeep, like the other burner machines, so a data line keeps it fuelled. While it holds Ignis Perpetuus in its Firebox, the eternal flame SHALL take the place of fuel and upkeep.

#### Scenario: Coal
- **WHEN** a formed Calciner holds cobblestone and one coal
- **THEN** it burns the coal and calcines cobblestone for the coal's burn time

#### Scenario: Input runs out
- **WHEN** the last cobblestone of a burning Calciner is calcined
- **THEN** the Calciner goes unlit and keeps the rest of its burn for the next input

#### Scenario: No fuel
- **WHEN** the fuel slot is empty and the burn has ended
- **THEN** progress stops and resumes when fuel is added

#### Scenario: Eternal flame instead of fuel
- **WHEN** a formed Calciner holds cobblestone, Ignis Perpetuus in its Firebox and no fuel
- **THEN** it calcines without stopping for fuel

#### Scenario: Tanks full
- **WHEN** the Mercury tank of a formed Calciner cannot take the Mercury the next cobblestone would give
- **THEN** the Calciner waits without burning new fuel or its current burn until the tank has room

#### Scenario: Ash Pan full
- **WHEN** the Ash Pan of a Calciner cannot take the 100 mB of Salt the next cobblestone would give
- **THEN** the Calciner waits without burning new fuel or its current burn until Ash is taken out

#### Scenario: No power needed
- **WHEN** a Power Cable touches any block of a Calciner
- **THEN** the cable finds no energy storage on it

#### Scenario: Unformed does nothing
- **WHEN** a Calciner that is not formed holds cobblestone and coal
- **THEN** it burns nothing and makes nothing
