# Spec Delta

## MODIFIED Requirements

### Requirement: Machines tab
The Machines tab SHALL list every member of the data line with the producer or the upkeep role with its block name and its state: working on an item when the producer reports an active recipe, idle when it has recipes and none runs, no recipes when it has none. A Crafting Station SHALL instead show crafting an item with the crafts left while it has a job, or idle, and one line saying it crafts any pattern. A Machine Adapter SHALL be named after the machine it faces with "(adapter)" added. Every member with upkeep SHALL show its upkeep item with the count it holds and the level. A member with upkeep but without the producer role SHALL show the state "upkeep only" instead of a recipe state. Under each other producer it SHALL list the recipes it reports as outputs from inputs with their time. The list SHALL scroll with the mouse wheel when it does not fit.

#### Scenario: Crafting Station with sticks
- **WHEN** a Crafting Station without a job is on a line that has a sticks pattern
- **THEN** the Machines tab lists the Crafting Station as idle and says it crafts any pattern, with no recipe list

#### Scenario: Crafting Station at work
- **WHEN** a Crafting Station has a sticks job with 2 crafts left
- **THEN** the Machines tab lists it as crafting sticks, 2 left

#### Scenario: No producers
- **WHEN** the line has no producer
- **THEN** the Machines tab says that no machines are on the line

#### Scenario: Adapter on a furnace
- **WHEN** a Machine Adapter faces a furnace holding 12 coal with an upkeep level of 16
- **THEN** the Machines tab lists "Furnace (adapter)" with upkeep Coal 12 of 16 and its smelting recipes

#### Scenario: Coal Generator
- **WHEN** a Coal Generator holding 9 coal with an upkeep level of 16 is on the line
- **THEN** the Machines tab lists Coal Generator as upkeep only with upkeep Coal 9 of 16

#### Scenario: Burner Smelter
- **WHEN** a Burner Smelter is on the line
- **THEN** the Machines tab lists it with its state, its upkeep and its smelting recipes
