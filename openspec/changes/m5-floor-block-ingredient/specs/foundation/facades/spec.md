# Spec Delta

## MODIFIED Requirements

### Requirement: Facades in JEI
When JEI is installed, every example facade SHALL appear in JEI's item list as its own entry, told apart by its block. Each SHALL have a crafting recipe showing its block and one nugget giving six facades of that block. Each SHALL also have an information page saying that any full block without a block entity works the same way and that the facade is used on a Utility Duct. JEI SHALL NOT show the generic facade recipe, whose result names no block. Without JEI the game SHALL load and facades SHALL work unchanged.

#### Scenario: Search
- **WHEN** a player types "facade" in JEI's search
- **THEN** every example facade shows as a separate entry

#### Scenario: Recipe of a facade
- **WHEN** a player looks up the recipe of "Facade (Oak Planks)" in JEI
- **THEN** a crafting recipe shows oak planks and a nugget giving six "Facade (Oak Planks)"

#### Scenario: Any block hint
- **WHEN** a player opens the information page of any example facade
- **THEN** it says that any full block plus one nugget gives six facades of that block

#### Scenario: Uses of a nugget
- **WHEN** a player looks up the uses of an iron nugget in JEI
- **THEN** the facade example recipes are among them, and no facade recipe with a nameless result

#### Scenario: Transfer an example facade
- **WHEN** a formed CPU and a Crafting Station are on a Storage Terminal's line and the player presses JEI's transfer button on the "Facade (Oak Planks)" recipe
- **THEN** the order grid shows oak planks and a nugget, and the result shows six "Facade (Oak Planks)"

#### Scenario: No JEI
- **WHEN** the game runs without JEI
- **THEN** it loads, and the example facades still show in the creative tab

### Requirement: Hollow examples
The Servus creative tab and JEI SHALL list a Hollow Facade for each example block next to the Facades, and JEI SHALL show the ring recipe for each. JEI SHALL NOT show the generic ring recipe, whose result names no block.

#### Scenario: Look up a hollow facade
- **WHEN** a player looks up the recipe of "Hollow Facade (Bricks)" in JEI
- **THEN** a crafting recipe shows eight brick Facades around an empty centre giving eight "Hollow Facade (Bricks)"

#### Scenario: Transfer a hollow facade
- **WHEN** the warehouse on a Production Terminal's line holds brick Facades and the player presses JEI's transfer button on the "Hollow Facade (Bricks)" recipe
- **THEN** the editor shows eight brick Facades around an empty centre and the preview shows eight "Hollow Facade (Bricks)"

## ADDED Requirements

### Requirement: Facade recipes are plain recipes
The Facade and Hollow Facade recipes SHALL be plain crafting recipes whose ingredients recipe viewers and crafting automation can read: the Facade recipe a shapeless block cell that lists every block a facade accepts plus a nugget cell, the Hollow Facade recipe a shaped ring of Facades. Crafting results, counts and the rules of Facade crafting and Hollow Facade crafting SHALL stay as they are, and facades crafted before SHALL keep their block.

#### Scenario: Crafting unchanged
- **WHEN** stone and an iron nugget are in the crafting grid
- **THEN** six "Facade (Stone)" result, as before

#### Scenario: Mixed ring still refused
- **WHEN** the ring holds seven stone Facades and one oak Facade
- **THEN** nothing results

#### Scenario: Old facades
- **WHEN** a world with facades crafted before this change is loaded
- **THEN** facade items and panels keep their blocks
