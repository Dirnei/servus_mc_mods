# Spec Delta

## ADDED Requirements

### Requirement: Grass Seed
Alchemy SHALL add the item Grass Seed, made only on the Athanor at Nigredo (tier 0) from Wheat Seeds and principles (example: 1 Wheat Seeds, 20 Sulfur and 20 Mercury give 1 Grass Seed in 100 ticks). It SHALL be its own item, not a vein seed, and SHALL have no crafting and no calcination recipe. The Athanor entry's list of works SHALL show it.

#### Scenario: Grass Seed on a black fire
- **WHEN** Wheat Seeds lie on a formed tier 0 Athanor whose tanks hold the Sulfur and the Mercury
- **THEN** one Grass Seed is made

#### Scenario: Book line
- **WHEN** the player reads the Athanor entry's works
- **THEN** a line says "Grass Seed from Wheat Seeds: 20 Sulfur, 20 Mercury"

#### Scenario: Not burned
- **WHEN** a pipe offers Grass Seeds to a formed Calciner
- **THEN** the Calciner accepts none

#### Scenario: Not a vein seed
- **WHEN** a player uses a Grass Seed on a Gnome Hollow
- **THEN** the Hollow is not seeded

### Requirement: Grass Seed greens Dirt
Using a Grass Seed on a Dirt block that grass could live on SHALL turn that block into a Grass Block and use up one seed; vanilla grass spreading does the rest. Grass can live on a block when the block above is not a full fluid and lets enough light through. On any other block, or on Dirt where grass could not live, nothing SHALL happen and the seed SHALL be kept. A player in creative mode SHALL keep the seed.

#### Scenario: Dirt turns green
- **WHEN** a player uses a Grass Seed on a Dirt block with air above
- **THEN** the block becomes a Grass Block and the player holds one Grass Seed less

#### Scenario: Covered Dirt
- **WHEN** a player uses a Grass Seed on a Dirt block under a Stone block
- **THEN** the block stays Dirt and the seed is kept

#### Scenario: Not Coarse Dirt
- **WHEN** a player uses a Grass Seed on Coarse Dirt
- **THEN** nothing happens
