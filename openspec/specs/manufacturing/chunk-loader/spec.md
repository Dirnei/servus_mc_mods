# manufacturing/chunk-loader Specification

## Purpose

The Chunk Loader keeps a factory running while nobody is around, at an energy cost and within a per team limit.

## Requirements

### Requirement: Loaded while powered
An active Chunk Loader SHALL keep its own chunk loaded and ticking and SHALL spend its energy per tick while active. Without energy it SHALL release the chunk until energy returns.

#### Scenario: Overnight
- **WHEN** a powered chunk loader stands in a chunk with producing machines and no player is nearby
- **THEN** the machines keep producing

#### Scenario: Power lost
- **WHEN** the loader's buffer runs dry
- **THEN** the chunk is released and machines stop until power returns

### Requirement: Per team cap
A team SHALL have at most `maxChunkLoadersPerTeam` active loaders (config, default 4). A loader beyond the cap SHALL stay inactive and show the reason in its GUI.

#### Scenario: Fifth loader
- **WHEN** a team already has four active loaders and places a fifth
- **THEN** the fifth stays inactive with the status "team limit reached"

### Requirement: FTB Chunks respected
When FTB Chunks is installed and `chunkLoaderRespectsFtbChunks` is true (default), Servus chunk loaders SHALL stay inactive and show a status pointing to FTB Chunks.

#### Scenario: FTB Chunks present
- **WHEN** FTB Chunks is installed with the default config
- **THEN** a placed chunk loader is inactive and its GUI says to use FTB Chunks

### Requirement: Tickets survive and are cleaned up
Chunk tickets SHALL survive a restart while the loader stays active, and SHALL be released when the loader is broken or dismantled.

#### Scenario: Removed
- **WHEN** an active loader is broken
- **THEN** its chunk is no longer force loaded by it

### Requirement: Crafting
The Chunk Loader SHALL be assembled from iron gears, copper wire and an ender pearl.

#### Scenario: Recipe
- **WHEN** the recipe is configured on an assembler with the inputs present
- **THEN** a Chunk Loader is produced
