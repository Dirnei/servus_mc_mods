# lib/config Specification

## Purpose

Servus Lib owns a common config file for knobs that are not datapack content. Options are read when used, so edits to the file apply without a restart.

## Requirements

### Requirement: Common config file exists
Servus Lib SHALL register a common config so that `config/servus_lib-common.toml` is created with documented defaults the first time the game runs with the mod.

#### Scenario: First launch
- **WHEN** the game starts with Servus Lib and no config file exists
- **THEN** `servus_lib-common.toml` is written with every option at its default and a comment describing it

### Requirement: Wrench rotation option
The config SHALL have a boolean option `wrenchRotatesBlocks`, default true, that controls whether the wrench rotates blocks that do not implement `Wrenchable`.

#### Scenario: Option disabled
- **WHEN** `wrenchRotatesBlocks` is false and the player uses the wrench on a furnace
- **THEN** the furnace keeps its facing

#### Scenario: Option does not affect Wrenchable blocks
- **WHEN** `wrenchRotatesBlocks` is false and the player uses the wrench on a `Wrenchable` block
- **THEN** the block's own handler still runs
