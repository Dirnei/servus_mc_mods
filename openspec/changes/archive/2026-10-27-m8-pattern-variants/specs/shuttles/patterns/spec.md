## ADDED Requirements

### Requirement: Exact or Any patterns
Every pattern SHALL have a mode, Exact or Any, saved with it. An Exact pattern SHALL use exactly its own items. An Any pattern SHALL let the CPU use, for each used cell, any item the recipe's ingredient at that cell accepts. A pattern saved before modes existed SHALL load as Exact.

#### Scenario: Old pattern
- **WHEN** a world with a sticks pattern from before this change is loaded
- **THEN** the pattern is Exact and orders use its own planks

#### Scenario: Any sticks
- **WHEN** the line has an Any sticks pattern saved with oak planks
- **THEN** an order for sticks may use spruce planks from stock

### Requirement: Any only where the result stays the same
A pattern SHALL be allowed to be Any only when, for every used cell, each item the recipe's ingredient there accepts, put in place of the pattern's item, makes the same result. Saving a pattern as Any when that does not hold SHALL save it as Exact.

#### Scenario: Chest from any planks
- **WHEN** the player saves a chest pattern from eight oak planks
- **THEN** Any is possible, since a chest from any planks is the same chest

#### Scenario: Rack stays Exact
- **WHEN** the player saves a Heavy Duty Rack pattern with oak planks
- **THEN** Any is not possible, since spruce planks would make a spruce rack, and the pattern is Exact

### Requirement: The CPU uses the most stocked variant
When a Crafting CPU plans a step with an Any pattern, it SHALL choose for each ingredient of the recipe one accepted item, the one the warehouse holds most of after earlier reservations, ties going to the pattern's own item, and use it in every cell with that ingredient. With none of the accepted items in stock it SHALL use the pattern's own item, so the plan can craft it as today. The step SHALL keep its chosen items until it is done.

#### Scenario: Spruce rather than oak
- **WHEN** the warehouse holds 3 oak planks and 40 spruce planks and a terminal orders 16 sticks with an Any sticks pattern saved with oak planks
- **THEN** the step takes 8 spruce planks

#### Scenario: Nothing in stock
- **WHEN** the warehouse holds no planks and oak logs, and the line has an Any sticks pattern saved with oak planks and an oak planks pattern
- **THEN** the order crafts oak planks first and then the sticks

### Requirement: One Any pattern per recipe and result
Saving an Any pattern SHALL be refused like a duplicate when the line already has an Any pattern for the same recipe and result, whatever items either was saved with.

#### Scenario: Second Any sticks
- **WHEN** the line has an Any sticks pattern saved with oak planks and the player saves an Any sticks pattern with birch planks
- **THEN** nothing is stored and the action bar says the line already has this pattern

## MODIFIED Requirements

### Requirement: CPU plans with patterns
A Crafting CPU SHALL plan with the patterns of its data line in addition to the recipes producers report. A pattern SHALL be usable when a producer on the line can execute it; every Crafting Station on the line can execute every crafting pattern. A pattern step SHALL run on such a producer like any step, with the pattern's items as inputs (for an Any pattern the items the CPU chose, see "The CPU uses the most stocked variant") and its result as output, stations that are not busy preferred.

#### Scenario: Pattern with a matching station
- **WHEN** the line has a sticks pattern and a Crafting Station without any job, and a terminal requests 8 sticks that are not in stock
- **THEN** the CPU makes an order whose step runs the sticks pattern on that station

#### Scenario: Pattern without a producer
- **WHEN** the line has a sticks pattern and no Crafting Station
- **THEN** the pattern does not make sticks orderable
