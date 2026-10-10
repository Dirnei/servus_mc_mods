# Spec Delta

## ADDED Requirements

### Requirement: Example facades in the creative tab
The Servus creative tab SHALL list Facades of a few common full blocks, for example stone, stone bricks, oak planks, bricks and deepslate tiles, each named after its block.

#### Scenario: Creative tab
- **WHEN** a player opens the Servus creative tab
- **THEN** it shows "Facade (Stone)", "Facade (Oak Planks)" and the other example facades

### Requirement: Facades in JEI
When JEI is installed, every example facade SHALL appear in JEI's item list as its own entry, told apart by its block. Each SHALL have a crafting recipe showing its block and one nugget giving six facades of that block. Each SHALL also have an information page saying that any full block without a block entity works the same way and that the facade is used on a Utility Duct. Without JEI the game SHALL load and facades SHALL work unchanged.

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
- **THEN** the facade example recipes are among them

#### Scenario: No JEI
- **WHEN** the game runs without JEI
- **THEN** it loads, and the example facades still show in the creative tab
