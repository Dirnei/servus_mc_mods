# Spec Delta

## REMOVED Requirements

### Requirement: Controller holds the fleet
**Reason**: Shuttles live in Shuttle Garages; the controller only makes the network online.
**Migration**: Build a Shuttle Garage beside a rail and put the shuttle items into it.

## MODIFIED Requirements

### Requirement: Stock operations
The network SHALL answer counts per item, SHALL extract a requested amount of an item from its sources, and SHALL insert a stack by the put-away routing of the zones: zoned blocks listing the exact item, then zoned blocks matching by tag, then general storage, and within each tier sources that already hold that item, then empty crates, then other storage nodes, then foreign inventories that already hold something. Instant extracts and inserts SHALL skip Heavy Duty Racks; racks are reached only by shuttles. A stack with no permitted room SHALL be returned as not fitting.

#### Scenario: Extract across sources
- **WHEN** one stack of cobblestone is requested and two crates hold 50 and 30
- **THEN** 64 are returned, taken from both crates, and the index drops to 16

#### Scenario: Insert without zones
- **WHEN** no zone exists and 20 cobblestone are inserted while one crate holds cobblestone and another is empty
- **THEN** the cobblestone joins the crate already holding it

#### Scenario: Insert respects a zone
- **WHEN** a crate is zoned "Ores" with `c:ores`, an unzoned crate is empty and raw iron is inserted at the terminal
- **THEN** the raw iron lands in the Ores crate and the unzoned crate stays empty

#### Scenario: Racks are skipped instantly
- **WHEN** 20 bread are inserted instantly while only a rack has room
- **THEN** the 20 bread are returned as not fitting

### Requirement: Summary reports shuttles
The controller's summary SHALL state the garage count, the shuttle count, how many are busy, how many jobs wait, how many of those are put-aways, and how many shuttles are parked without a rail.

#### Scenario: Busy fleet
- **WHEN** an op right clicks a controller of a warehouse with two garages and four shuttles of which one is on a job
- **THEN** the summary says two garages, four shuttles, one busy

#### Scenario: Put-aways waiting
- **WHEN** two put-away jobs wait in the queue
- **THEN** the summary says two queued of which two are put-aways
