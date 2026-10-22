# alchemy/calciner Specification

## Purpose
The Calciner is the first alchemical machine: it burns ordinary matter into its Salt, Sulfur and Mercury, the principles every later product is made from.

## Requirements

### Requirement: Calcination recipes are data
A calcination recipe SHALL be a datapack recipe of type `servus_alchemy:calcination` with one ingredient (an item or a tag), the mB of Salt, Sulfur and Mercury it gives (each may be 0) and a time in ticks. Any matching item SHALL be accepted; an item without a recipe SHALL be refused.

#### Scenario: Cobblestone
- **WHEN** the shipped data holds a recipe for `#c:cobblestones` giving 100 mB Salt and 10 mB Mercury in 80 ticks
- **THEN** a Calciner turns one cobblestone into 100 mB Salt and 10 mB Mercury in 4 seconds

#### Scenario: Unknown item
- **WHEN** a hopper offers a diamond and no calcination recipe matches diamonds
- **THEN** the Calciner takes nothing

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

### Requirement: Calciner crafting
The Calciner SHALL be crafted at a crafting table from vanilla materials without research. The Calciner block is the furnace mouth of the structure; the fire lives in the Firebox.

#### Scenario: Recipe
- **WHEN** the player crafts the Calciner pattern of Stone Bricks, iron bars and a cauldron
- **THEN** one Calciner is produced

### Requirement: Calciner reports to the journal
The Calciner SHALL remember the player who formed it as its owner and SHALL report every finished cycle to the journal of the owner's team: one `calcine` event for the item, and one `principle` event per principle with the mB it made.

#### Scenario: Cobblestone counts
- **WHEN** a Calciner a player formed calcines one cobblestone
- **THEN** the player's team gets a `calcine` event for cobblestone and `principle` events of 100 Salt and 10 Mercury

#### Scenario: Formed by another player
- **WHEN** player A placed the Calciner and player B formed it
- **THEN** the events go to player B's team
