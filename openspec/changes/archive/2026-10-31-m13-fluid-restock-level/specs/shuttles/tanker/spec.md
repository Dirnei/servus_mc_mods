## MODIFIED Requirements

### Requirement: Export restock runs
Every `fluid_check_interval` ticks an online warehouse SHALL check every Fluid Outbound keeping fluid above 0 mB; its have is its amount plus what runs on the way bring. While it restocks with no run on the way, a Tanker reaching it and shelves holding the fluid, one run SHALL be queued taking the least of the missing amount, the tanker's capacity and the shelf's free fluid, rounded down to whole buckets. Runs below `tanker_min_load` SHALL wait unless they bring every whole bucket missing.

#### Scenario: Pipe drains, tanker restocks
- **WHEN** a Fluid Outbound tank keeps 8 B of water with a restock level of 8 B, holds 2 B and a Fluid Rack shelf holds 20 B of water
- **THEN** a Tanker shuttle brings water from the rack until the Outbound tank holds 8 B

#### Scenario: Runs on the way count
- **WHEN** a run bringing 4 B of water to a Fluid Outbound tank is under way and the tank holds 2 B of a keep of 8 B
- **THEN** the next run brings at most 2 B

## ADDED Requirements

### Requirement: Restock level
A Fluid Outbound SHALL start to restock when its have is at or below its restock level and SHALL go on restocking until less than 1,000 mB is missing to its keep amount, even when the have rises above the restock level in between. A restock run SHALL never carry part of a bucket.

#### Scenario: Whole buckets only
- **WHEN** a Fluid Outbound keeps 8 B of water with a restock level of 8 B and holds 6,250 mB
- **THEN** a run brings 1,000 mB, and once the tank holds 7,250 mB no further run starts

#### Scenario: Waits for the restock level
- **WHEN** a Fluid Outbound keeps 8 B of water with a restock level of 4 B and a pipe drains it from 8 B
- **THEN** no run starts while it holds more than 4 B, and once it holds 4 B or less, runs bring water until less than one bucket is missing to 8 B

#### Scenario: Refill goes on above the restock level
- **WHEN** a Fluid Outbound keeps 16 B of water with a restock level of 2 B, holds 2 B and the only Tanker carries 8 B
- **THEN** a first run brings 8 B and a second run brings 6 B, though the tank holds more than 2 B after the first

#### Scenario: Less than a bucket in the rack
- **WHEN** a Fluid Outbound needs a restock and the only rack shelf with its fluid holds 600 mB
- **THEN** no run is queued
