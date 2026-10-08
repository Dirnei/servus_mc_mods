# foundation/production-api Specification

## Purpose

The production API is the shared vocabulary for autocrafting: blocks say what they can make and what they hold, and a pure planner turns a wanted item into production steps, without any content mod knowing another.

## Requirements

### Requirement: Production capability
Lib SHALL define a sided block capability `servus_lib:production`. A provider SHALL list the recipes the block can run right now, each with a recipe id, the exact input stacks with counts, the output stacks with counts and the time in ticks, and SHALL say which recipe id it is working on, if any. Any mod SHALL be able to register a provider for its blocks and query one on any block.

#### Scenario: Query a producer
- **WHEN** a mod asks the production capability of a block whose provider knows one recipe making one iron gear from two iron ingots in 20 ticks
- **THEN** it gets one recipe with those inputs, that output and that time

#### Scenario: No producer
- **WHEN** a mod asks the production capability of a stone block
- **THEN** it gets nothing

### Requirement: Stock capabilities
Lib SHALL define sided block capabilities `servus_lib:stock_provider` and `servus_lib:stock_sink`. A stock provider SHALL answer how many of an item like a given stack are available and SHALL take up to a requested amount of it. A stock sink SHALL accept a stack and return what did not fit.

#### Scenario: Count and take
- **WHEN** a mod asks a stock provider for cobblestone that holds 50 and takes 20
- **THEN** the count was 50, 20 cobblestone are returned and the count is 30 afterwards

#### Scenario: Sink full
- **WHEN** a mod hands 64 bread to a stock sink with room for 10
- **THEN** 54 bread are returned

### Requirement: Order planner
Lib SHALL provide a pure planner that, given a wanted item key and amount, the stock on hand per key, and the recipes it may use, returns production steps leaves first, how much stock each key reserves, and how much of each raw material is missing. Stock on hand SHALL be reserved before anything is produced, surplus from a step SHALL serve later needs, several needs for one recipe SHALL be one step with the batches added up, and a recipe chain deeper than a given limit SHALL be treated as missing.

#### Scenario: In stock
- **WHEN** 10 gears are wanted and 12 are in stock
- **THEN** there are no steps and 10 gears are reserved

#### Scenario: One level
- **WHEN** 3 gears are wanted, none are in stock, 10 ingots are, and a recipe makes 1 gear from 2 ingots
- **THEN** one step runs the gear recipe 3 times and 6 ingots are reserved

#### Scenario: Two levels
- **WHEN** 1 pack is wanted from 1 gear and 2 wire, the gear needs 2 ingots, the wire recipe makes 2 wire from 1 copper, and only ingots and copper are in stock
- **THEN** the steps are gear, wire, pack in that order, with 2 ingots and 1 copper reserved

#### Scenario: Surplus reused
- **WHEN** 3 wire are wanted and the wire recipe makes 2 per batch
- **THEN** one step runs the wire recipe 2 times and nothing is left missing

#### Scenario: Missing raw material
- **WHEN** 1 gear is wanted, no ingots are in stock and nothing makes ingots
- **THEN** one step runs the gear recipe once and 2 ingots are missing

#### Scenario: Too deep
- **WHEN** recipe A needs B and recipe B needs A and A is wanted
- **THEN** the planner stops at the depth limit and reports the missing input instead of looping
