# Spec Delta

## MODIFIED Requirements

### Requirement: Order planner
Lib SHALL provide a pure planner that, given a wanted item key and amount, the stock on hand per key, and the recipes it may use, returns production steps leaves first, how much stock each key reserves, and how much of each raw material is missing. Stock on hand SHALL be reserved before anything is produced, surplus from a step SHALL serve later needs, several needs for one recipe SHALL be one step with the batches added up, and a recipe chain deeper than a given limit SHALL be treated as missing. When several recipes make the wanted item, the planner SHALL first use those whose inputs are covered by stock on hand, the one covering the most batches first, splitting the amount across them when one is not enough; only then SHALL it produce the inputs of the alternative whose plan misses nothing with the fewest new steps, or, when every alternative misses something, report the missing material of the one missing the least. Ties SHALL go to an alternative already used for that item, then by recipe order.

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

#### Scenario: Stocked alternative
- **WHEN** 4 charcoal are wanted, an acacia recipe and an oak recipe each make 1 charcoal from 1 log in that order, and only 10 oak logs are in stock
- **THEN** one step runs the oak recipe 4 times and 4 oak logs are reserved

#### Scenario: Split across alternatives
- **WHEN** 10 charcoal are wanted and 6 oak logs and 4 birch logs are in stock
- **THEN** the oak recipe runs 6 times and the birch recipe 4 times, and nothing is missing

#### Scenario: Produce the inputs of an alternative
- **WHEN** 2 sticks are wanted, a bamboo recipe needs bamboo nobody makes, a planks recipe needs planks, no planks are in stock and logs are
- **THEN** the steps make planks from logs and then sticks from planks, and nothing is missing

#### Scenario: Missing of the best alternative
- **WHEN** 4 charcoal are wanted, no logs are in stock except 3 birch logs, and no recipe makes logs
- **THEN** the birch recipe runs 3 times and 1 birch log is missing, not an acacia log

#### Scenario: Tie by order
- **WHEN** 2 charcoal are wanted and 5 acacia logs and 5 oak logs are in stock
- **THEN** the acacia recipe runs twice
