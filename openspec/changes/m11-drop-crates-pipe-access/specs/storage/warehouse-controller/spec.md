## MODIFIED Requirements

### Requirement: One controller makes a network online
A warehouse network SHALL be online when it contains exactly one Warehouse Controller. With none or more than one, it SHALL be offline and report that state.

#### Scenario: Two controllers
- **WHEN** two controllers touch the same rail run
- **THEN** the network reports offline with the reason "more than one controller"

### Requirement: Warehouse command
`/servus_warehouse info <pos>` SHALL print the online state, source count and the indexed items with counts for the network containing the storage node at the position, and for an online network the site name, the chunk count and the capacity. `/servus_warehouse take <pos> <item> <count>` SHALL, as an op tool that needs no shuttle, take up to one stack of the item straight out of that network's racks and drop it above the position. `/servus_warehouse zones <pos>` SHALL list every zone name of that network with its block count and the filter items and tags its blocks carry.

#### Scenario: Op checks a warehouse
- **WHEN** an op runs the command on a rack position
- **THEN** chat lists each indexed item with its total

#### Scenario: Op checks a site
- **WHEN** an op runs the info command on a rack of the site "North Mine" spanning 2 chunks with one upgrade of 9
- **THEN** chat names "North Mine" with 2 of 9 chunks

#### Scenario: Op takes items
- **WHEN** an op runs the take command for 64 cobblestone on a rack position of a warehouse without a garage
- **THEN** a stack of up to 64 cobblestone drops above the rack and the index shrinks by that amount

#### Scenario: Op lists zones
- **WHEN** an op runs the zones command on a rack of a warehouse with "Ores" (3 blocks, `c:ores`) and "Food" (1 block, bread)
- **THEN** chat lists both zones with their counts and filters

### Requirement: Controller exposes stock
A Warehouse Controller SHALL expose the Lib stock provider capability on every side for its online warehouse: counts SHALL come from the index, and taking from the provider SHALL give nothing, because items leave a warehouse only by shuttle (other mods and the data line ask the supplier capability, see `storage/packing-station`). The controller SHALL NOT expose a stock sink. An offline controller SHALL report no stock.

#### Scenario: Another mod counts stock
- **WHEN** a mod asks the controller's stock provider for cobblestone while racks hold 80
- **THEN** it gets 80

#### Scenario: Taking gives nothing
- **WHEN** a mod asks the controller's stock provider to take 20 cobblestone while racks hold 80
- **THEN** it gets nothing and the racks still hold 80

#### Scenario: Another mod hands items in
- **WHEN** a mod looks for a stock sink on the controller to hand in 20 bread
- **THEN** there is none and the bread stays with the mod

#### Scenario: Offline
- **WHEN** the network has two controllers
- **THEN** the stock provider reports zero

### Requirement: Producer and feed commands
`/servus_warehouse producers <pos>` SHALL list every Outbound Station of the network at the position with the block it faces and that block's recipes. `/servus_warehouse feed <station> <item> <count>` SHALL ask the warehouse to deliver the items into that Outbound Station's buffer and SHALL answer with the refusal reason when it cannot.

#### Scenario: Op lists producers
- **WHEN** an op runs the producers command on a rack of a warehouse with one Outbound Station facing an Assembler
- **THEN** chat lists that station with the Assembler's recipes

#### Scenario: Op feeds a dock
- **WHEN** an op runs the feed command for 4 iron ingots on an Outbound Station on the rails while a rack holds 10 and a garage holds a shuttle
- **THEN** a shuttle brings 4 ingots into the station's buffer

### Requirement: Site chunk span
The controller SHALL know the set of chunks its site spans: every chunk that holds a member of its network. The set SHALL be updated from member join and leave events, at most once per tick after a change, without scanning blocks. A chunk whose content unloads SHALL stay in the set; a chunk SHALL leave the set only while every chunk of the set is loaded and it holds nothing of the site. The set SHALL be saved on the controller, SHALL NOT travel in a dismantled item, and a placed controller SHALL start with its own chunk. Only a network with exactly one controller SHALL update that controller's set.

#### Scenario: Rail into the next chunk
- **WHEN** a controller sits in one chunk and a rail run carries a rack into the neighbouring chunk
- **THEN** the controller reports 2 chunks

#### Scenario: Chest across the border
- **WHEN** a chest in the neighbouring chunk touches a rack of the site and nothing else of the site is in that chunk
- **THEN** the controller reports 1 chunk

#### Scenario: Part of the site unloads
- **WHEN** the far chunk of a two chunk site unloads while the controller's chunk stays loaded
- **THEN** the controller still reports 2 chunks

#### Scenario: Rail removed
- **WHEN** the rail that linked the rack in the neighbouring chunk is broken while both chunks are loaded
- **THEN** the controller reports 1 chunk

## ADDED Requirements

### Requirement: Stock moves only by shuttle
The network SHALL answer counts per item from its index. Items SHALL leave and enter the warehouse's racks only by shuttle: a pick job takes them out of a rack for a Packing Station or an Outbound Station, a put-away job puts a stack from an Inbound Station or Packing Station into the rack chosen by the put-away routing of the zones. There SHALL be no instant extract or insert into the warehouse. A stack with no permitted room SHALL wait at its station as not fitting.

#### Scenario: Counts across racks
- **WHEN** two racks of an online warehouse hold 50 and 30 cobblestone
- **THEN** the network counts 80 cobblestone

#### Scenario: Taken by shuttle
- **WHEN** a request for one stack of cobblestone is made while those two racks hold it and the warehouse has a garage with a shuttle
- **THEN** a shuttle takes 64 cobblestone from the racks and the index drops to 16 when it loads them

#### Scenario: Insert respects a zone
- **WHEN** a rack is zoned "Ores" with `c:ores`, an unzoned rack is empty and raw iron is put away from an Inbound Station
- **THEN** a shuttle puts the raw iron into the Ores rack and the unzoned rack stays empty

#### Scenario: No shuttle
- **WHEN** 20 bread wait in an Inbound Station of a warehouse that has racks with room but no garage
- **THEN** the bread stays in the station and the station says it needs a shuttle

### Requirement: Sources are the warehouse's own racks
The index SHALL include only the storage nodes of the network that hold stock, which are the Heavy Duty Racks joined through a rail in front of a bay of their section. No other block SHALL be a source: inventories touching a rack, the controller, a rail, a station or any other member (vanilla chests, inventories of other mods, Servus blocks such as the Storage Terminal or the Crafting Station) SHALL never be discovered, indexed, put-away targets or sources for picks. Connector nodes such as rails SHALL NOT be sources.

#### Scenario: Chest beside the controller
- **WHEN** a chest with 10 bread is placed next to the controller of an online network
- **THEN** the index does not report the bread and put-away never chooses the chest

#### Scenario: Chest beside a rack
- **WHEN** a chest with 10 bread touches the side of a rack that belongs to an online network
- **THEN** the index does not report the bread

#### Scenario: Chest beside a rail
- **WHEN** a chest with 10 bread is placed next to a rail of an online network
- **THEN** the index does not report the bread

#### Scenario: Terminal beside the controller
- **WHEN** a Storage Terminal with 5 diamonds in its Output row stands directly beside the Warehouse Controller
- **THEN** the index does not report the diamonds and put-away never chooses the terminal

#### Scenario: Rack joins by rail
- **WHEN** a rack holding 10 bread gets a rail of an online network in front of it
- **THEN** the index soon reports 10 bread from that rack, and breaking the rail removes them again

## REMOVED Requirements

### Requirement: Stock operations
**Reason**: The instant extract and insert served crates and foreign inventories, which are no longer part of a warehouse.
**Migration**: See "Stock moves only by shuttle".

### Requirement: Sources are nodes and adjacent inventories
**Reason**: The warehouse no longer indexes inventories touching its nodes (vanilla chests, other mods' inventories, Servus blocks); crates are removed.
**Migration**: See "Sources are the warehouse's own racks". Put items into Heavy Duty Racks; a chest's contents reach the warehouse through an Inbound Station pulling from it.
