# storage/warehouse-controller Specification

## Purpose

The controller turns touching storage blocks and their neighbours into one searchable stock.

## Requirements

### Requirement: One controller makes a network online
A warehouse network SHALL be online when it contains exactly one Warehouse Controller. With none or more than one, it SHALL be offline and report that state.

#### Scenario: Two controllers
- **WHEN** two controllers touch the same crates
- **THEN** the network reports offline with the reason "more than one controller"

### Requirement: Sources are nodes and adjacent inventories
The index SHALL include every storage node of the network and every block with an item handler adjacent to a node that is not itself a node. Adjacent inventories SHALL be discovered when a node is placed and when a neighbour of a node changes.

#### Scenario: Chest beside a crate
- **WHEN** a chest with 10 bread is placed next to a crate of an online network
- **THEN** the index soon reports 10 bread from that chest

#### Scenario: Chest removed
- **WHEN** that chest is broken
- **THEN** the bread leaves the index

### Requirement: Batched polling
The network SHALL poll at most `sources_per_tick` sources per tick (default 4, from `data/<ns>/storage/warehouse.json`), round robin, and SHALL refresh a storage node's snapshot on the tick after it reports a change.

#### Scenario: Large warehouse
- **WHEN** a network has 40 sources
- **THEN** every source is refreshed at least once per 10 ticks and no tick reads more than 4

### Requirement: Stock operations
The network SHALL answer counts per item, SHALL extract a requested amount of an item from its sources, and SHALL insert a stack preferring sources that already hold that item, then empty crates, then other storage nodes, then foreign inventories that already hold something.

#### Scenario: Extract across sources
- **WHEN** one stack of cobblestone is requested and two sources hold 50 and 30
- **THEN** 64 are returned, taken from both sources, and the index drops to 16

### Requirement: Warehouse command
`/servus_warehouse info <pos>` SHALL print the online state, source count and the indexed items with counts for the network containing the storage node at the position. `/servus_warehouse take <pos> <item> <count>` SHALL extract up to one stack of the item from that network and drop it above the position.

#### Scenario: Op checks a warehouse
- **WHEN** an op runs the command on a crate position
- **THEN** chat lists each indexed item with its total

#### Scenario: Op takes items
- **WHEN** an op runs the take command for 64 cobblestone on a crate position
- **THEN** a stack of up to 64 cobblestone drops above the crate and the index shrinks by that amount
