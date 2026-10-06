# Spec Delta

## MODIFIED Requirements

### Requirement: Stock operations
The network SHALL answer counts per item, SHALL extract a requested amount of an item from its sources, and SHALL insert a stack by the put-away routing of the zones: zoned blocks listing the exact item, then zoned blocks matching by tag, then general storage, and within each tier sources that already hold that item, then empty crates, then other storage nodes, then foreign inventories that already hold something. A stack with no permitted room SHALL be returned as not fitting.

#### Scenario: Extract across sources
- **WHEN** one stack of cobblestone is requested and two sources hold 50 and 30
- **THEN** 64 are returned, taken from both sources, and the index drops to 16

#### Scenario: Insert without zones
- **WHEN** no zone exists and 20 cobblestone are inserted while one crate holds cobblestone and another is empty
- **THEN** the cobblestone joins the crate already holding it

#### Scenario: Insert respects a zone
- **WHEN** a crate is zoned "Ores" with `c:ores`, an unzoned crate is empty and raw iron is inserted at the terminal
- **THEN** the raw iron lands in the Ores crate and the unzoned crate stays empty

### Requirement: Warehouse command
`/servus_warehouse info <pos>` SHALL print the online state, source count and the indexed items with counts for the network containing the storage node at the position. `/servus_warehouse take <pos> <item> <count>` SHALL extract up to one stack of the item from that network and drop it above the position. `/servus_warehouse zones <pos>` SHALL list every zone name of that network with its block count and the filter items and tags its blocks carry.

#### Scenario: Op checks a warehouse
- **WHEN** an op runs the command on a crate position
- **THEN** chat lists each indexed item with its total

#### Scenario: Op takes items
- **WHEN** an op runs the take command for 64 cobblestone on a crate position
- **THEN** a stack of up to 64 cobblestone drops above the crate and the index shrinks by that amount

#### Scenario: Op lists zones
- **WHEN** an op runs the zones command on a crate of a warehouse with "Ores" (3 blocks, `c:ores`) and "Food" (1 block, bread)
- **THEN** chat lists both zones with their counts and filters

### Requirement: Summary reports shuttles
The controller's summary SHALL state the shuttle count, how many are busy, how many jobs wait, how many of those are put-aways, and whether a home rail exists.

#### Scenario: Busy fleet
- **WHEN** an op right clicks a controller with four shuttles of which one is on a job
- **THEN** the summary says four shuttles, one busy

#### Scenario: Put-aways waiting
- **WHEN** two put-away jobs wait in the queue
- **THEN** the summary says two queued of which two are put-aways
