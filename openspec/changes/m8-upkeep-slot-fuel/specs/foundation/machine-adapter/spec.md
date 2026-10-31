# Spec Delta

## MODIFIED Requirements

### Requirement: Machine profiles
Machine profiles SHALL be datapack JSON files under `data/<ns>/machine_profiles/`. A profile SHALL name one or more blocks, a recipe type, and for each of the six sides of the machine relative to its facing (front, back, left, right, top, bottom) a role: ingredients, upkeep, output or off; sides it leaves out are off. It MAY name a default upkeep item and level, and an upkeep batch (how many must be missing before the adapter asks again, at least 1, example default 8). Foundation SHALL ship profiles for the vanilla furnace (smelting), smoker (smoking) and blast furnace (blasting) with ingredients on top, output at the bottom and upkeep on the back, left and right, and coal at level 16 as default upkeep. Profiles SHALL reload with datapacks.

#### Scenario: Furnace profile
- **WHEN** the default datapack is loaded
- **THEN** a furnace's top side is ingredients, its bottom side output and its back, left and right sides upkeep

#### Scenario: Pack adds a profile
- **WHEN** a datapack adds a profile for a modded smelter with the recipe type `minecraft:smelting`
- **THEN** an adapter on that smelter reports smelting recipes after a reload

#### Scenario: Pack retunes the batch
- **WHEN** a datapack sets the furnace profile's upkeep batch to 1 and the server reloads
- **THEN** an adapter on a furnace asks again as soon as one upkeep item is missing

### Requirement: Upkeep stays stocked
The adapter SHALL expose the upkeep capability for the machine's upkeep side (its upkeep item, its level, its profile's batch, the count in the machine), so the data network's shared upkeep keeper keeps that side stocked at the level, independent of orders. Like a burner machine, it SHALL ask only once at least a batch is missing or the machine holds none, and then ask for everything missing up to the level. It SHALL save its open upkeep request with its block. Recipes SHALL never contain upkeep. Upkeep the adapter was told is coming SHALL go through the upkeep side until it has arrived or the request timed out. The GUI SHALL show the upkeep item as a ghost slot, the level with buttons to change it, and the count in the machine; an empty ghost slot means no upkeep.

#### Scenario: Refill
- **WHEN** an adapter with upkeep coal at level 16 and a batch of 8 faces a furnace holding 4 coal and a warehouse with coal is on the line
- **THEN** the adapter asks for 12 coal and the furnace ends up with 16

#### Scenario: Waits for a batch
- **WHEN** an adapter with upkeep coal at level 16 and a batch of 8 faces a furnace holding 10 coal
- **THEN** nothing is asked for until the furnace holds 8 coal or less, then the adapter asks for everything missing

#### Scenario: Order without fuel
- **WHEN** a CPU plans 8 stone through an adapter
- **THEN** the order's steps contain cobblestone only, no coal

#### Scenario: Change the upkeep
- **WHEN** the player puts charcoal into the ghost slot and lowers the level to 8
- **THEN** the adapter keeps 8 charcoal in the machine from then on
