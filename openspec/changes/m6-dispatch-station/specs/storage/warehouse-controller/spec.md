# Spec Delta

## MODIFIED Requirements

### Requirement: Stock operations
The network SHALL answer counts per item, SHALL extract a requested amount of an item from its sources, and SHALL insert a stack by the put-away routing of the zone rules: the matching filtered zone, else general storage, then the overflow zone, and within the chosen blocks sources that already hold that item, then empty crates, then other storage nodes, then foreign inventories that already hold something. A stack with no permitted room SHALL be returned as not fitting.

#### Scenario: Extract across sources
- **WHEN** one stack of cobblestone is requested and two sources hold 50 and 30
- **THEN** 64 are returned, taken from both sources, and the index drops to 16

#### Scenario: Insert without zones
- **WHEN** no zone exists and 20 cobblestone are inserted while one crate holds cobblestone and another is empty
- **THEN** the cobblestone joins the crate already holding it

#### Scenario: Insert respects a zone
- **WHEN** "Ores" lists `c:ores`, an unzoned crate is empty and raw iron is inserted at the terminal
- **THEN** the raw iron lands in "Ores" and the unzoned crate stays empty

### Requirement: Warehouse command
`/servus_warehouse info <pos>` SHALL print the online state, source count and the indexed items with counts for the network containing the storage node at the position. `/servus_warehouse take <pos> <item> <count>` SHALL extract up to one stack of the item from that network and drop it above the position. `/servus_warehouse zones <pos>` SHALL list every zone of that network with its block count, filter items, tags and whether it is the overflow zone.

#### Scenario: Op checks a warehouse
- **WHEN** an op runs the command on a crate position
- **THEN** chat lists each indexed item with its total

#### Scenario: Op takes items
- **WHEN** an op runs the take command for 64 cobblestone on a crate position
- **THEN** a stack of up to 64 cobblestone drops above the crate and the index shrinks by that amount

#### Scenario: Op lists zones
- **WHEN** an op runs the zones command on a crate of a warehouse with "Ores" (3 blocks, `c:ores`) and "Spare" (overflow)
- **THEN** chat lists both zones with their counts, rules and the overflow mark

### Requirement: Summary reports shuttles
The controller's summary SHALL state the shuttle count, how many are busy, how many jobs wait, how many of those are put-aways, and whether a home rail exists.

#### Scenario: Busy fleet
- **WHEN** an op right clicks a controller with four shuttles of which one is on a job
- **THEN** the summary says four shuttles, one busy

#### Scenario: Put-aways waiting
- **WHEN** two put-away jobs wait in the queue
- **THEN** the summary says two queued of which two are put-aways

## ADDED Requirements

### Requirement: Controller holds the zone rules
The controller SHALL store the zone rules of its warehouse, keep them across reloads, hand them to whichever network it belongs to, and keep them in the dropped item when dismantled with the Configuration Carrot so placing it again restores them. Breaking the controller by hand SHALL lose the rules.

#### Scenario: Reload
- **WHEN** a warehouse with the rule "Ores" lists `c:ores` is unloaded and loaded again
- **THEN** raw iron still routes to "Ores"

#### Scenario: Carrot dismantle
- **WHEN** the controller is dismantled with the carrot and placed again
- **THEN** the station GUI shows the same zone rules
