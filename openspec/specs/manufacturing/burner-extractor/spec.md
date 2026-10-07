# manufacturing/burner-extractor Specification

## Purpose

The Burner Extractor is the first machine: it sits on a deposit and turns fuel into the deposit's yield forever.

## Requirements

### Requirement: Extracts from the deposit below
A Burner Extractor SHALL produce the yield of the deposit block directly below it once per work period while it has fuel and the yield has somewhere to go: the fuel reserve when it wants the yield, otherwise the output slot. Without a deposit below it SHALL do nothing and burn no fuel.

#### Scenario: On iron deposit
- **WHEN** an extractor stands on an iron deposit with coal in its fuel slot
- **THEN** one raw iron appears in its output slot every 4 seconds (80 ticks by default, retunable in data)

#### Scenario: No deposit
- **WHEN** an extractor stands on dirt with coal in its fuel slot
- **THEN** no fuel is consumed and nothing is produced

#### Scenario: Output full
- **WHEN** the output slot holds a full stack of raw iron
- **THEN** the extractor pauses and does not burn new fuel until space is available

#### Scenario: Output full but reserve wants the yield
- **WHEN** an extractor on a coal deposit has a full output slot and fewer than the reserve target of coal in its fuel slot
- **THEN** the extractor keeps working and the next coal goes into the fuel slot

### Requirement: Fuel handling
The extractor SHALL burn any item vanilla furnaces accept as fuel, consuming one item when the previous burn ends and work is available. Burn time follows the item's furnace burn time.

#### Scenario: Coal
- **WHEN** one coal is in the fuel slot and work is available
- **THEN** the extractor runs for 80 seconds on it (1600 ticks), the vanilla coal burn time

#### Scenario: Fuel runs out
- **WHEN** the burn ends and the fuel slot is empty
- **THEN** progress stops and resumes from the same point when fuel is added

### Requirement: Crafting
The Burner Extractor SHALL be craftable from vanilla materials early in the game.

#### Scenario: Recipe
- **WHEN** the player crafts with iron ingots, cobblestone and a furnace in the extractor pattern
- **THEN** the result is one Burner Extractor

### Requirement: Fuel reserve from own yield
When the deposit yield is a furnace fuel, the extractor SHALL put the yield into its fuel slot instead of the output slot while the fuel slot is empty or holds the same item below the reserve target. The target is a balance number in the machine's datapack file (example: 4 for the Burner Extractor). A target of 0 disables the reserve.

#### Scenario: Placed on coal with one coal
- **WHEN** an extractor on a coal deposit is lit with a single coal
- **THEN** the first four coal it mines land in the fuel slot and the fifth onward land in the output slot

#### Scenario: Reserve refills after burning
- **WHEN** the extractor burns one coal from a full reserve of four
- **THEN** the next coal it mines goes back into the fuel slot and later ones go to output

#### Scenario: Reserve already full
- **WHEN** the fuel slot holds 64 coal
- **THEN** every mined coal goes to the output slot

#### Scenario: Different fuel in the slot
- **WHEN** the fuel slot holds charcoal and the extractor stands on a coal deposit
- **THEN** mined coal goes to the output slot and the charcoal keeps burning

#### Scenario: Yield is not a fuel
- **WHEN** the extractor stands on an iron deposit
- **THEN** raw iron never enters the fuel slot regardless of the target

#### Scenario: Reserve is not pulled out by automation
- **WHEN** a duct or hopper is attached on an extract side
- **THEN** it receives only the output slot and never the fuel slot reserve

### Requirement: Fuel reserve routing is pure logic
The decision whether a yield goes to the fuel slot or to the output slot SHALL be expressed as a function of the reserve target, the fuel slot count, whether the fuel slot is empty or holds the same item as the yield, and whether the yield is a fuel. It SHALL be unit testable without Minecraft.

#### Scenario: Unit tests
- **WHEN** the routing rule is called with target 4 and counts 0, 3, 4 and 64 of the same item, with a different item, with a non fuel yield and with target 0
- **THEN** it answers fuel slot for 0 and 3, output for 4 and 64, output for a different item, output for a non fuel and output for target 0
