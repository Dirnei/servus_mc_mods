# Spec Delta

## Purpose

Manufacturing machines take part in on-demand production over the data line: burner machines get their fuel kept stocked as upkeep, and the Burner Smelter and Crusher run order steps for the Crafting CPU like the Assembler.

## ADDED Requirements

### Requirement: Burner machines keep fuel as upkeep
The Burner Extractor, Burner Smelter, Burner Pump and Coal Generator SHALL expose the upkeep capability for their fuel slot on every side: the upkeep item and level come from their machine stats, and the count is the number of that item in the fuel slot. Upkeep that arrives goes into the fuel slot like any fuel inserted by automation.

#### Scenario: Smelter fuel
- **WHEN** a Burner Smelter with 5 coal in its fuel slot is asked for its upkeep
- **THEN** it answers coal, level 16 and count 5

#### Scenario: Refilled from the warehouse
- **WHEN** a Burner Smelter with an empty fuel slot is on a data line with a warehouse holding coal and an Access Point
- **THEN** after a while its fuel slot holds 16 coal without any order

### Requirement: Upkeep stats
Machine stats SHALL have `upkeep_item` (an item id, empty for none) and `upkeep_level` (a count, 0 for none). The defaults SHALL be coal and 16 for the Burner Extractor, Burner Smelter, Burner Pump and Coal Generator and none for the other machines. A machine with no upkeep item or a level of 0 SHALL expose no upkeep. Values are examples; the real numbers live in datapack JSON.

#### Scenario: Retuned level
- **WHEN** a datapack sets `upkeep_level` of `coal_generator` to 32 and the server reloads
- **THEN** the Coal Generator's fuel slot is kept at 32 coal

#### Scenario: Upkeep switched off
- **WHEN** a datapack sets `upkeep_level` of `burner_pump` to 0
- **THEN** the Burner Pump exposes no upkeep and is no data member

### Requirement: Burner Smelter is a producer
The Burner Smelter SHALL expose the production capability: one recipe per input item of every vanilla smelting recipe with a single ingredient, that item in, the recipe's result out, the smelter's own work time as ticks; the recipe of the item in its input slot SHALL be reported as active.

#### Scenario: Iron ingot
- **WHEN** the production capability of a Burner Smelter is queried
- **THEN** it reports a recipe with one raw iron in, one iron ingot out and 80 ticks

#### Scenario: Order through the smelter
- **WHEN** a Crafting CPU plans 4 iron ingots on a line with a warehouse holding raw iron, an Access Point and a Burner Smelter
- **THEN** the step runs on the smelter, the raw iron arrives as packets and the 4 ingots are collected into storage, and the order has no coal in it

### Requirement: Crusher is a producer
The Crusher SHALL expose the production capability: one recipe per input item of every crushing recipe, that item with the recipe's input count in, the recipe's result out, the recipe's time as ticks; the recipe of its input slot SHALL be reported as active.

#### Scenario: Iron grit
- **WHEN** the production capability of a Crusher is queried
- **THEN** it reports a recipe with one raw iron in, two iron grit out and the crushing time
