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
A Crafting CPU SHALL plan with the patterns of its data line in addition to the recipes producers report. A pattern SHALL be usable when a producer on the line can execute it; every Crafting Station on the line can execute every crafting pattern. A pattern step SHALL run on such a producer like any step, with the pattern's items as inputs (for an Any pattern the items the CPU chose, see "The CPU uses the most stocked variant") and its result as output, stations that are not busy preferred.

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

### Requirement: Exact or Any patterns
Every pattern SHALL have a mode, Exact or Any, saved with it. An Exact pattern SHALL use exactly its own items. An Any pattern SHALL let the CPU use, for each used cell, any item the recipe's ingredient at that cell accepts. A pattern saved before modes existed SHALL load as Exact.

#### Scenario: Old pattern
- **WHEN** a world with a sticks pattern from before this change is loaded
- **THEN** the pattern is Exact and orders use its own planks

#### Scenario: Any sticks
- **WHEN** the line has an Any sticks pattern saved with oak planks
- **THEN** an order for sticks may use spruce planks from stock

### Requirement: The CPU uses the most stocked variant
When a Crafting CPU plans a step with an Any pattern, it SHALL choose for each ingredient of the recipe one accepted item, the one the warehouse holds most of after earlier reservations, ties going to the pattern's own item, and use it in every cell with that ingredient. With none of the accepted items in stock it SHALL use the pattern's own item, so the plan can craft it as today. The step SHALL keep its chosen items until it is done.

#### Scenario: Spruce rather than oak
- **WHEN** the warehouse holds 3 oak planks and 40 spruce planks and a terminal orders 16 sticks with an Any sticks pattern saved with oak planks
- **THEN** the step takes 8 spruce planks

#### Scenario: Nothing in stock
- **WHEN** the warehouse holds no planks and oak logs, and the line has an Any sticks pattern saved with oak planks and an oak planks pattern
- **THEN** the order crafts oak planks first and then the sticks

### Requirement: One Any pattern per recipe and result
Saving an Any pattern SHALL be refused like a duplicate when the line already has an Any pattern for the same recipe whose result is the same item in the same count, whatever items either was saved with and whatever material their results carry. An Exact pattern SHALL NOT count as a duplicate of an Any pattern unless it has the same recipe and the same items.

#### Scenario: Second Any sticks
- **WHEN** the line has an Any sticks pattern saved with oak planks and the player saves an Any sticks pattern with birch planks
- **THEN** nothing is stored and the action bar says the line already has this pattern

#### Scenario: Second Any rack
- **WHEN** the line has an Any rack pattern saved with oak planks and the player saves an Any rack pattern with spruce planks
- **THEN** nothing is stored and the action bar says the line already has this pattern

#### Scenario: Exact rack beside an Any rack
- **WHEN** the line has an Any rack pattern saved with oak planks and the player saves an Exact rack pattern with spruce planks
- **THEN** the Exact pattern is stored

### Requirement: When a pattern can be Any
A pattern SHALL be allowed to be Any when some used cell accepts other items and every accepted item, put in place of the pattern's item in all cells holding it, makes a result of the same item and count. When the result's components change with the items (the rack's wood, the Elevator's material) the pattern SHALL be a material pattern. Saving Any where this does not hold SHALL save Exact.

#### Scenario: Chest from any planks
- **WHEN** the player saves a chest pattern from eight oak planks as Any
- **THEN** the pattern is Any and not a material pattern, since a chest from any planks is the same chest

#### Scenario: Rack follows the planks
- **WHEN** the player saves a Heavy Duty Rack pattern with oak planks as Any
- **THEN** the pattern is an Any material pattern

#### Scenario: Iron block stays Exact
- **WHEN** the player saves an iron block pattern as Any
- **THEN** the pattern is Exact, since its cells take no other items

### Requirement: Material patterns make every variant
A material pattern SHALL let the CPU make each variant its accepted items give: one item per ingredient, used in every cell of that ingredient, and the result the recipe makes from them. A request, stock rule or recipe step that needs one variant (a spruce rack) SHALL use that variant's items and SHALL craft them first when they are not in stock.

#### Scenario: Spruce rack from an oak pattern
- **WHEN** the line has an Any rack pattern saved with oak planks, the warehouse holds iron and 20 spruce planks, and a request asks for 3 spruce Heavy Duty Racks
- **THEN** the step takes 3 spruce planks and the iron, and 3 spruce racks are made

#### Scenario: Planks crafted for the variant
- **WHEN** the same line has an Any spruce planks pattern from logs, the warehouse holds spruce logs but no spruce planks, and a request asks for 3 spruce racks
- **THEN** the order crafts spruce planks from spruce logs first and then the spruce racks

### Requirement: Generic orders take the most stocked variant
An order for the item of a material pattern without its material (a Heavy Duty Rack without a wood) SHALL make the variant whose ingredients the warehouse holds most of after earlier reservations, ties going to the pattern's own items, none in stock to the pattern's own items. The order and its request SHALL then name that variant.

#### Scenario: Most stocked wood
- **WHEN** the line has an Any rack pattern saved with oak planks, the warehouse holds 5 oak planks, 40 birch planks and iron, and the player crafts 2 Heavy Duty Racks from the generic entry
- **THEN** 2 birch racks are made from birch planks and the request lists birch Heavy Duty Racks

#### Scenario: Nothing stocked
- **WHEN** the warehouse holds no planks but oak logs, and an oak planks pattern is on the line
- **THEN** the generic order crafts oak planks and then oak racks

### Requirement: Cells with a material accept stocked and made variants
A cell whose item carries a material (an Elevator's Rail) SHALL accept, in a material pattern, every stack of that item the warehouse holds and every one a recipe of the line makes, each with its own material.

#### Scenario: Spruce Elevator uses a spruce Rail
- **WHEN** the line has an Any Elevator pattern saved with an oak Rail, the warehouse holds a spruce Rail and the rest, and a request asks for a spruce Elevator
- **THEN** the step takes the spruce Rail and a spruce Elevator is made

#### Scenario: Rail crafted for the Elevator
- **WHEN** no spruce Rail is in stock and an Any Rail pattern and spruce planks are on the line
- **THEN** the order crafts a spruce Rail first and then the spruce Elevator

### Requirement: Exact patterns stay fixed below
An Exact pattern SHALL need exactly its own items; a material pattern below it SHALL make exactly the variant the Exact pattern needs.

#### Scenario: Exact oak Elevator
- **WHEN** the line has an Exact Elevator pattern with an oak Rail, an Any Rail pattern saved with spruce planks, oak planks in stock and no Rail, and a request asks for an oak Elevator
- **THEN** the order crafts an oak Rail from oak planks and then the oak Elevator
