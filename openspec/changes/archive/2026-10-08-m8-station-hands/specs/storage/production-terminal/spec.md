# Spec Delta

## MODIFIED Requirements

### Requirement: Machines tab
The Machines tab SHALL list every member of the data line with the producer role with its block name and its state: working on an item when the producer reports an active recipe, idle when it has recipes and none runs, no recipes when it has none. A Crafting Station SHALL instead show crafting an item with the crafts left while it has a job, or idle, and one line saying it crafts any pattern. Under each other producer it SHALL list the recipes it reports as outputs from inputs with their time. The list SHALL scroll with the mouse wheel when it does not fit.

#### Scenario: Crafting Station with sticks
- **WHEN** a Crafting Station without a job is on a line that has a sticks pattern
- **THEN** the Machines tab lists the Crafting Station as idle and says it crafts any pattern, with no recipe list

#### Scenario: Crafting Station at work
- **WHEN** a Crafting Station has a sticks job with 2 crafts left
- **THEN** the Machines tab lists it as crafting sticks, 2 left

#### Scenario: No producers
- **WHEN** the line has no producer
- **THEN** the Machines tab says that no machines are on the line
