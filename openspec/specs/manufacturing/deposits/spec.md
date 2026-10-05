# manufacturing/deposits Specification

## Purpose

Deposits are the infinite resource sources of Servus Manufacturing: visible surface patches that extractors mine forever. Their definitions and placement live in datapack JSON.

## Requirements

### Requirement: Four Tier 1 deposit blocks
Servus Manufacturing SHALL provide the blocks iron deposit, copper deposit, coal deposit and stone deposit, each with its own texture and name, listed in the Servus creative tab.

#### Scenario: Creative tab
- **WHEN** the player opens the Servus creative tab
- **THEN** all four deposit blocks are listed and can be placed

### Requirement: Deposits cannot be mined
Deposit blocks SHALL be unbreakable in survival and SHALL not drop anything, so a site is permanent.

#### Scenario: Survival player breaks a deposit
- **WHEN** a survival player tries to mine an iron deposit with any tool
- **THEN** the block does not break

### Requirement: Deposit definitions are data
A deposit definition SHALL be a JSON file under `data/<namespace>/deposit/` naming a block and the item stack one extraction yields. Definitions SHALL be reloaded with `/reload` and modpacks SHALL be able to add, override or remove them.

#### Scenario: Built in definitions
- **WHEN** the server starts with Servus Manufacturing
- **THEN** definitions for iron, copper, coal and stone deposits are loaded and the log reports four deposits

#### Scenario: Yields
- **WHEN** the definitions are queried for the four built in deposit blocks
- **THEN** iron yields one raw iron, copper one raw copper, coal one coal and stone one cobblestone (example values, retunable in data)

#### Scenario: Datapack override
- **WHEN** a datapack provides `data/servus_manufacturing/deposit/iron.json` with a different yield
- **THEN** after `/reload` the iron deposit yields the overridden stack

### Requirement: Deposits generate as surface patches
Each deposit type SHALL generate in the overworld as a flat patch of several blocks on the surface, replacing soil and stone, so it is visible from above. Patch size and rarity SHALL be defined in worldgen JSON.

#### Scenario: New chunks
- **WHEN** new overworld chunks generate
- **THEN** patches of each deposit type appear on land at the surface, not inside water

#### Scenario: Placing the feature by command
- **WHEN** an operator runs `/place feature servus_manufacturing:iron_deposit`
- **THEN** a patch of iron deposit blocks is placed at the position
