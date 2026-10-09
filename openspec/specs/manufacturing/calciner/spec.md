# manufacturing/calciner Specification

## Purpose
The Calciner is the first alchemical machine: it burns ordinary matter into its Salt, Sulfur and Mercury, the principles every later product is made from.

## Requirements

### Requirement: Calcination recipes are data
A calcination recipe SHALL be a datapack recipe of type `servus_manufacturing:calcination` with one ingredient (an item or a tag), the mB of Salt, Sulfur and Mercury it gives (each may be 0) and a time in ticks. Any matching item SHALL be accepted; an item without a recipe SHALL be refused.

#### Scenario: Cobblestone
- **WHEN** the shipped data holds a recipe for `#c:cobblestones` giving 100 mB Salt and 10 mB Mercury in 80 ticks
- **THEN** a Calciner turns one cobblestone into 100 mB Salt and 10 mB Mercury in 4 seconds

#### Scenario: Unknown item
- **WHEN** a hopper offers a diamond and no calcination recipe matches diamonds
- **THEN** the Calciner takes nothing

### Requirement: Calciner burns fuel
The Calciner SHALL be a burner machine following the shared machine rules: one input slot and one fuel slot that takes any furnace fuel, burning one item when the previous burn has ended and work is available, with no FE buffer and no energy capability. It SHALL start a cycle only when each of its three principle tanks has room for the whole output of the recipe. It SHALL report its fuel as upkeep, like the other burner machines, so a data line keeps it fuelled.

#### Scenario: Coal
- **WHEN** a Calciner holds cobblestone and one coal
- **THEN** it burns the coal and calcines cobblestone for the coal's burn time

#### Scenario: No fuel
- **WHEN** the fuel slot is empty and the burn has ended
- **THEN** progress stops and resumes when fuel is added

#### Scenario: Tanks full
- **WHEN** the Salt tank of a Calciner cannot take the 100 mB the next cobblestone would give
- **THEN** the Calciner waits without burning new fuel until the Salt is drained

#### Scenario: No power needed
- **WHEN** a Power Cable touches a Calciner
- **THEN** the cable finds no energy storage on it

### Requirement: Principles leave through the sides
The Calciner SHALL hold one tank per principle (capacity from machine stats, example 4,000 mB), declared as output tanks Salt, Sulfur and Mercury of its Fluids layer. It SHALL offer each tank through the fluid capability only on sides whose Fluids layer is set to that tank or to All, push a tank into touching fluid handlers on those sides, and never accept fluid.

#### Scenario: Tank beside the Calciner
- **WHEN** a Fluid Tank touches a side of a working Calciner whose Fluids layer is All
- **THEN** the principles the Calciner makes flow into the tank (a single tank keeps the first principle it receives)

#### Scenario: Pipe drains one principle
- **WHEN** a Fluid Pipe takes fluid from the Calciner and only Sulfur is wanted downstream
- **THEN** the Calciner hands out Sulfur and keeps its Salt and Mercury

#### Scenario: Separate pipes per principle
- **WHEN** the Calciner's left side is set to Salt, its right side to Mercury, and a pipe arm on each side leads to a separate Fluid Tank
- **THEN** one tank fills with Salt only and the other with Mercury only

### Requirement: Calciner GUI
The Calciner GUI SHALL show its input slot, its fuel slot with the burn flame, progress, the three principle gauges with amount and capacity on hover, and the shared side panel.

#### Scenario: Gauges
- **WHEN** the player hovers the Salt gauge of a Calciner holding 300 mB of Salt
- **THEN** the tooltip reads 300 / 4000 mB Salt

### Requirement: Calciner crafting
The Calciner SHALL be crafted at a crafting table from vanilla materials without research.

#### Scenario: Recipe
- **WHEN** the player crafts the Calciner pattern of bricks, iron ingots, a furnace and a cauldron
- **THEN** one Calciner is produced

### Requirement: Buckets from the gauges
Clicking a principle gauge in the Calciner GUI with an empty bucket or another fluid container on the cursor SHALL fill it from that principle's tank when the tank holds enough. The Calciner SHALL NOT hand out principles to a container used on the block from outside.

#### Scenario: Salt into a bucket
- **WHEN** the player clicks the Salt gauge with an empty bucket on the cursor and the Salt tank holds 1,500 mB
- **THEN** the cursor holds a Salt bucket and 500 mB Salt remain

### Requirement: Calciner reports to the journal
The Calciner SHALL remember the player who placed it as its owner and SHALL report every finished cycle to the journal of the owner's team: one `calcine` event for the item, and one `principle` event per principle with the mB it made.

#### Scenario: Cobblestone counts
- **WHEN** a player's Calciner calcines one cobblestone
- **THEN** the player's team gets a `calcine` event for cobblestone and `principle` events of 100 Salt and 10 Mercury
