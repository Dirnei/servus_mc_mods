# shuttles/patterns Specification

## Purpose
Patterns are how the warehouse knows crafting recipes: the player encodes a vanilla crafting recipe with concrete items once, Pattern Storage blocks on the data line keep the patterns, and the Crafting CPU plans with them.

## Requirements

### Requirement: Pattern Storage joins the data line
A Pattern Storage touching a data line SHALL be a member of that line with the link role. The patterns of a data line SHALL be the patterns of every Pattern Storage on it, Pattern Storages by lowest position first, patterns in their kept order. The capacity of a line SHALL be the sum of the configured pattern places of its Pattern Storages.

#### Scenario: Two blocks
- **WHEN** two Pattern Storages with 9 places each are on one data line and hold 3 patterns together
- **THEN** the line has 3 patterns and a capacity of 18

### Requirement: Encoding from the terminal
Saving a new pattern in the Production Terminal's editor, or choosing "Save as pattern" in the Storage Terminal's order grid, SHALL match the 3x3 ghost grid to a vanilla crafting recipe and store a pattern with the recipe id, the grid items and the result in the first Pattern Storage of the terminal's line with a free place. It SHALL refuse with a message and keep the grid when the grid is empty or matches nothing, when the line already has a pattern for the same recipe with the same items, when the line has no Pattern Storage, or when no Pattern Storage has room; the Production Terminal says so in the action bar and the Storage Terminal on its message line. Saving an existing pattern SHALL rewrite it in place. Deleting a pattern SHALL remove it.

#### Scenario: New pattern
- **WHEN** the player opens the editor with plus, puts a plank above a plank and presses Save
- **THEN** a sticks pattern is in the first Pattern Storage with room and the Patterns tab lists it, and the player's inventory is unchanged

#### Scenario: No blank
- **WHEN** the player has an empty inventory and saves a matching grid
- **THEN** the pattern is stored; no item is needed

#### Scenario: No Pattern Storage
- **WHEN** the terminal's line has no Pattern Storage and the player saves a matching grid
- **THEN** nothing is stored and the action bar says there is no Pattern Storage on this data line

#### Scenario: Full
- **WHEN** every Pattern Storage of the line is full and the player saves a new pattern
- **THEN** nothing is stored and the action bar says the Pattern Storage is full

#### Scenario: Delete
- **WHEN** the player clicks the sticks pattern in the Patterns tab and presses Delete
- **THEN** the pattern is gone from its Pattern Storage and the player gets nothing

#### Scenario: Saved from the Storage Terminal
- **WHEN** the Storage Terminal's order grid holds four oak planks in a square and the player chooses "Save as pattern"
- **THEN** a crafting table pattern is in the first Pattern Storage of the line with room and the Production Terminal's Patterns tab lists it

### Requirement: CPU plans with patterns
A Crafting CPU SHALL plan with the patterns of its data line in addition to the recipes producers report. A pattern SHALL be usable when a producer on the line can execute it; every Crafting Station on the line can execute every crafting pattern. A pattern step SHALL run on such a producer like any step, with the pattern's items as inputs and its result as output, stations that are not busy preferred.

#### Scenario: Pattern with a matching station
- **WHEN** the line has a sticks pattern and a Crafting Station without any job, and a terminal requests 8 sticks that are not in stock
- **THEN** the CPU makes an order whose step runs the sticks pattern on that station

#### Scenario: Pattern without a producer
- **WHEN** the line has a sticks pattern and no Crafting Station
- **THEN** the pattern does not make sticks orderable

### Requirement: Patterns are data
A pattern SHALL be data kept by a Pattern Storage, never an item: one vanilla crafting recipe with its recipe id, a 3x3 template with one concrete item per used cell, and its result. No item SHALL represent a blank or an encoded pattern.

#### Scenario: No pattern items
- **WHEN** a player searches the creative inventory for patterns
- **THEN** there is no Blank Pattern and no Pattern item, only the Pattern Storage block

### Requirement: Pattern Storage keeps patterns as data
Pattern Storage SHALL be a horizontal facing block that keeps up to `patterns_per_block` patterns (example 9, from `data/<ns>/storage/pattern_storage.json`) as data. It SHALL have no slot GUI and expose no item handler. Right clicking it with an empty hand SHALL show how many patterns it holds of how many in the action bar. Patterns kept beyond a smaller configured size SHALL stay kept and count as used.

#### Scenario: Read out
- **WHEN** a player right clicks a Pattern Storage holding 2 patterns with an empty hand and `patterns_per_block` is 9
- **THEN** the action bar says 2 of 9 patterns

### Requirement: Patterns travel with the block
Breaking a Pattern Storage, or dismantling it by sneaking with the Configuration Carrot, SHALL drop the block as one item that carries its patterns, and placing that item SHALL restore them in the same order. An empty Pattern Storage SHALL drop a plain item.

#### Scenario: Break and place again
- **WHEN** a Pattern Storage holding a sticks pattern is mined with a pickaxe and placed again on the data line
- **THEN** it holds the sticks pattern and the line lists it again

### Requirement: Indicator lights
The Pattern Storage front SHALL show a 3x3 grid of lights with one lit light per stored pattern. When `patterns_per_block` is not 9, the lit count SHALL be the fill level scaled to 9, rounded up while any pattern is stored. The lights SHALL update when a pattern is saved or deleted and when the block is placed with patterns.

#### Scenario: Five patterns
- **WHEN** a Pattern Storage with room for 9 holds 5 patterns
- **THEN** 5 of its 9 lights are lit

#### Scenario: Other capacity
- **WHEN** `patterns_per_block` is 18 and the block holds 1 pattern
- **THEN** 1 light is lit

### Requirement: Pattern Storage online lamp
Pattern Storage SHALL show an online lamp on its front next to the pattern lights, lit while a formed Crafting CPU or an online warehouse is on its data line and dark otherwise, refreshed about once a second.

#### Scenario: On a line with a CPU
- **WHEN** a Pattern Storage touches a data duct whose line has a formed CPU
- **THEN** its lamp is lit within about a second, and the pattern lights keep showing the stored count

### Requirement: Pattern result is what the recipe makes
A pattern's result SHALL be the stack its recipe produces when crafted from the pattern's own template, including the components the recipe adds (for example the wood of the planks). Patterns kept from before SHALL get this result the first time their Pattern Storage is used on a loaded level, without the player saving them again.

#### Scenario: Rack from oak planks
- **WHEN** a pattern for the Heavy Duty Rack is saved with oak planks in its template
- **THEN** its result is an oak Heavy Duty Rack, the same item a Crafting Station crafts from it

#### Scenario: Requested racks arrive
- **WHEN** a player requests 2 Heavy Duty Racks that a CPU crafts from that pattern
- **THEN** the station's racks are collected and both arrive in the Storage Terminal

#### Scenario: Old pattern refreshed
- **WHEN** a Pattern Storage holding a rack pattern with a result without wood is loaded and its patterns are read
- **THEN** the pattern's result is the oak Heavy Duty Rack
