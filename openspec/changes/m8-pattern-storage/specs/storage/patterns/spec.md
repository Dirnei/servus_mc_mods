# Spec Delta

## Purpose

Patterns are how the warehouse knows crafting recipes: the player encodes a vanilla crafting recipe with concrete items once, Pattern Storage blocks on the data line keep the patterns, and the Crafting CPU plans with them.

## ADDED Requirements

### Requirement: Blank and encoded patterns
A Blank Pattern SHALL be craftable from cheap vanilla materials (example: paper and redstone). An encoded Pattern SHALL hold one vanilla crafting recipe: its recipe id, a 3x3 template with one concrete item per used cell, and its result. Its tooltip SHALL show the result and the inputs with counts. A Pattern crafted alone in a crafting grid SHALL give back a Blank Pattern.

#### Scenario: Tooltip
- **WHEN** the player hovers a Pattern encoded with a plank above a plank
- **THEN** the tooltip shows 4 Stick from 2 Oak Planks

#### Scenario: Wipe
- **WHEN** the player crafts a Pattern alone in a crafting grid
- **THEN** the result is one Blank Pattern

### Requirement: Pattern Storage holds patterns
Pattern Storage SHALL be a horizontal facing block that holds `patterns_per_block` encoded Patterns (example 9, from `data/<ns>/storage/pattern_storage.json`) in a slot GUI opened by right click. Its slots SHALL accept encoded Patterns only. It SHALL drop itself and its patterns when broken.

#### Scenario: Only patterns
- **WHEN** the player tries to put a Blank Pattern or a stick into a Pattern Storage slot
- **THEN** it is refused

#### Scenario: Capacity from data
- **WHEN** `patterns_per_block` is 9
- **THEN** a Pattern Storage has 9 slots

### Requirement: Pattern Storage joins the data line
A Pattern Storage touching a data line SHALL be a member of that line with the link role. The patterns of a data line SHALL be the encoded Patterns of every Pattern Storage on it, Pattern Storages by lowest position first, slots in order. The capacity of a line SHALL be the sum of the slots of its Pattern Storages.

#### Scenario: Two blocks
- **WHEN** two Pattern Storages with 9 slots each are on one data line and hold 3 patterns together
- **THEN** the line has 3 patterns and a capacity of 18

### Requirement: Encoding from the terminal
Saving a new pattern in the Production Terminal's editor SHALL match the 3x3 ghost grid to a vanilla crafting recipe, take one Blank Pattern from the player's inventory, encode it with the recipe id, the grid items and the result, and put it into the first Pattern Storage of the terminal's line with a free slot. It SHALL refuse with an action bar message and keep the editor open when the grid is empty or matches nothing, when the player has no Blank Pattern, when the line already has a pattern for the same recipe with the same items, or when no Pattern Storage has room. Saving an existing pattern SHALL rewrite it in place without a Blank Pattern. Deleting a pattern SHALL remove it and give the player a Blank Pattern.

#### Scenario: New pattern
- **WHEN** the player has a Blank Pattern, opens the editor with plus, puts a plank above a plank and presses Save
- **THEN** the Blank Pattern is gone, a sticks Pattern is in the first Pattern Storage with room, and the Patterns tab lists it

#### Scenario: No blank
- **WHEN** the player has no Blank Pattern and saves a matching grid
- **THEN** nothing is stored and the action bar says a Blank Pattern is needed

#### Scenario: Full
- **WHEN** every Pattern Storage of the line is full and the player saves a new pattern
- **THEN** nothing is stored, the Blank Pattern stays with the player and the action bar says the Pattern Storage is full

#### Scenario: Delete
- **WHEN** the player clicks the sticks pattern in the Patterns tab and presses Delete
- **THEN** the pattern is gone from its Pattern Storage and the player gets a Blank Pattern

### Requirement: CPU plans with patterns
A Crafting CPU SHALL plan with the patterns of its data line in addition to the recipes producers report. A pattern SHALL be usable when a producer on the line can execute it; until Crafting Stations are the CPU's hands, a Crafting Station can execute a pattern when its own recipe list holds the same recipe id with the same items. A pattern step SHALL run on such a producer like any step, with the pattern's items as inputs and its result as output.

#### Scenario: Pattern with a matching station
- **WHEN** the line has a sticks pattern and a Crafting Station configured with the same sticks recipe, and a terminal requests 8 sticks that are not in stock
- **THEN** the CPU makes an order whose step runs the sticks pattern on that station

#### Scenario: Pattern without a producer
- **WHEN** the line has a sticks pattern and no producer can execute it
- **THEN** the pattern does not make sticks orderable
