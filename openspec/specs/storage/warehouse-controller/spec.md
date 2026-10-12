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
The index SHALL include every storage node of the network that holds stock and every block with an item handler adjacent to such a node that is not itself a node. Connector nodes such as rails SHALL NOT be sources and their neighbours SHALL NOT be discovered. Adjacent inventories SHALL be discovered when a node is placed and when a neighbour of a node changes. Servus blocks whose slots are no stock SHALL never be discovered as adjacent inventories, whatever item capability they expose: the Storage Terminal (Input and Output rows), the Crafting Station, and every station (Inbound, Outbound, Packing and Wireless Packing Station buffers), so they are never stock, a put-away target or a source for picks.

#### Scenario: Chest beside a crate
- **WHEN** a chest with 10 bread is placed next to a crate of an online network
- **THEN** the index soon reports 10 bread from that chest

#### Scenario: Chest removed
- **WHEN** that chest is broken
- **THEN** the bread leaves the index

#### Scenario: Chest beside a rail
- **WHEN** a chest with 10 bread is placed next to a rail of an online network and touches no stock holding node
- **THEN** the index does not report the bread

#### Scenario: Terminal beside the controller
- **WHEN** a Storage Terminal with 5 diamonds in its Output row stands directly beside the Warehouse Controller
- **THEN** the index does not report the diamonds and put-away never chooses the terminal

#### Scenario: Station beside a crate
- **WHEN** a Wireless Packing Station with emeralds in its buffer touches a crate without a rail between them
- **THEN** the index does not report the emeralds

### Requirement: Batched polling
The network SHALL poll at most `sources_per_tick` sources per tick (default 4, from `data/<ns>/storage/warehouse.json`), round robin, and SHALL refresh a storage node's snapshot on the tick after it reports a change.

#### Scenario: Large warehouse
- **WHEN** a network has 40 sources
- **THEN** every source is refreshed at least once per 10 ticks and no tick reads more than 4

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

### Requirement: Warehouse command
`/servus_warehouse info <pos>` SHALL print the online state, source count and the indexed items with counts for the network containing the storage node at the position, and for an online network the site name, the chunk count and the capacity. `/servus_warehouse take <pos> <item> <count>` SHALL extract up to one stack of the item from that network and drop it above the position. `/servus_warehouse zones <pos>` SHALL list every zone name of that network with its block count and the filter items and tags its blocks carry.

#### Scenario: Op checks a warehouse
- **WHEN** an op runs the command on a crate position
- **THEN** chat lists each indexed item with its total

#### Scenario: Op checks a site
- **WHEN** an op runs the info command on a crate of the site "North Mine" spanning 2 chunks with one upgrade of 9
- **THEN** chat names "North Mine" with 2 of 9 chunks

#### Scenario: Op takes items
- **WHEN** an op runs the take command for 64 cobblestone on a crate position
- **THEN** a stack of up to 64 cobblestone drops above the crate and the index shrinks by that amount

#### Scenario: Op lists zones
- **WHEN** an op runs the zones command on a crate of a warehouse with "Ores" (3 blocks, `c:ores`) and "Food" (1 block, bread)
- **THEN** chat lists both zones with their counts and filters

### Requirement: Summary reports shuttles
The controller's summary SHALL state the garage count, the shuttle count, how many are busy, how many jobs wait, how many of those are put-aways, and how many shuttles are parked without a rail. The controller GUI SHALL show it as the tooltip of its online state.

#### Scenario: Busy fleet
- **WHEN** a player opens the GUI of a controller of a warehouse with two garages and four shuttles of which one is on a job and hovers the online state
- **THEN** the summary says two garages, four shuttles, one busy

#### Scenario: Put-aways waiting
- **WHEN** two put-away jobs wait in the queue
- **THEN** the summary says two queued of which two are put-aways

### Requirement: Controller exposes stock
A Warehouse Controller SHALL expose the Lib stock provider and stock sink capabilities on every side for its online warehouse: counts come from the index, taking uses the warehouse's instant extract, handing in uses the warehouse's insert with its zone routing. An offline controller SHALL report no stock and accept nothing.

#### Scenario: Another mod counts stock
- **WHEN** a mod asks the controller's stock provider for cobblestone while crates hold 80
- **THEN** it gets 80

#### Scenario: Another mod hands items in
- **WHEN** a mod hands 20 bread to the controller's stock sink while an empty crate is in the warehouse
- **THEN** the bread lands in the crate and nothing is returned

#### Scenario: Offline
- **WHEN** the network has two controllers
- **THEN** the stock provider reports zero and the sink returns the whole stack

### Requirement: Producer and feed commands
`/servus_warehouse producers <pos>` SHALL list every Outbound Station of the network at the position with the block it faces and that block's recipes. `/servus_warehouse feed <station> <item> <count>` SHALL ask the warehouse to deliver the items into that Outbound Station's buffer and SHALL answer with the refusal reason when it cannot.

#### Scenario: Op lists producers
- **WHEN** an op runs the producers command on a crate of a warehouse with one Outbound Station facing an Assembler
- **THEN** chat lists that station with the Assembler's recipes

#### Scenario: Op feeds a dock
- **WHEN** an op runs the feed command for 4 iron ingots on an Outbound Station while a crate holds 10
- **THEN** 4 ingots arrive in the station's buffer

### Requirement: Order commands
`/servus_warehouse orders <pos>` SHALL list every work order of the network at the position with item, amount, state, reason and steps. `/servus_warehouse order <pos> <item> <count>` SHALL create a work order and SHALL answer with the reason when none can be made.

#### Scenario: Op orders sticks
- **WHEN** an op runs the order command for 8 sticks while a dock faces a Crafting Station with the sticks recipe
- **THEN** chat confirms the order and the orders command lists it

#### Scenario: Op orders the impossible
- **WHEN** an op orders bread and nothing can make it
- **THEN** chat says there is no producer for bread

### Requirement: Controller holds the stock rules
The controller SHALL store the warehouse's stock rules, keep them across reloads, hand them to whichever network it belongs to, and keep them in the dropped item when dismantled with the Configuration Carrot.

#### Scenario: Carrot dismantle
- **WHEN** a controller with a sticks rule is dismantled with the carrot and placed again
- **THEN** the sticks rule is still there

### Requirement: Rule commands
`/servus_warehouse rules <pos>`, `/servus_warehouse rule <pos> <item> <min> <max>` and `/servus_warehouse unrule <pos> <item>` SHALL list, set and remove stock rules of the network at the position.

#### Scenario: Op lists rules
- **WHEN** an op runs the rules command on a warehouse with a sticks rule
- **THEN** chat lists the rule with its state

### Requirement: The network never loads chunks
The warehouse network SHALL NOT load a chunk when a member joins or leaves, when candidates are rebuilt, or when it looks up blocks of its members; a lookup in a chunk that is not loaded SHALL count as nothing there. A world whose warehouse spans several chunks SHALL save and stop like a world without one.

#### Scenario: Stop with a warehouse across chunks
- **WHEN** a server stops while a warehouse whose rails cross several chunk borders is loaded
- **THEN** every chunk unloads once, the worlds are saved and the server exits

#### Scenario: Member leaves beside an unloaded chunk
- **WHEN** a member leaves a network whose other members lie in a chunk that is not loaded
- **THEN** that chunk stays unloaded

### Requirement: Controller is a named site
Every Warehouse Controller SHALL be a site with a name of at most 32 characters, set in its GUI. A controller without a name SHALL be shown as "Warehouse at <x>, <z>" with its block position. The name SHALL be kept when the world is saved and SHALL travel in the dropped item when the controller is dismantled with the Configuration Carrot. The controller SHALL remember the team of the player who placed it.

#### Scenario: Unnamed
- **WHEN** a controller is placed at 120, 64, -40 and nobody names it
- **THEN** its GUI title reads "Warehouse at 120, -40"

#### Scenario: Named
- **WHEN** the player types "North Mine" into the name field and presses Enter
- **THEN** the GUI title reads "North Mine" and the name is still there after a reload

#### Scenario: Carrot dismantle
- **WHEN** a controller named "North Mine" is dismantled with the carrot and placed again
- **THEN** it is still named "North Mine"

### Requirement: Site chunk span
The controller SHALL know the set of chunks its site spans: every chunk that holds a member of its network or an indexed inventory touching a stock member. The set SHALL be updated from member join and leave events and source changes, at most once per tick after a change, without scanning blocks. A chunk whose content unloads SHALL stay in the set; a chunk SHALL leave the set only while every chunk of the set is loaded and it holds nothing of the site. The set SHALL be saved on the controller, SHALL NOT travel in a dismantled item, and a placed controller SHALL start with its own chunk. Only a network with exactly one controller SHALL update that controller's set.

#### Scenario: Rail into the next chunk
- **WHEN** a controller sits in one chunk and a rail run carries a crate into the neighbouring chunk
- **THEN** the controller reports 2 chunks

#### Scenario: Chest across the border
- **WHEN** a chest in the neighbouring chunk touches a crate of the site and nothing else of the site is in that chunk
- **THEN** the controller reports 2 chunks, and 1 after the chest is broken

#### Scenario: Part of the site unloads
- **WHEN** the far chunk of a two chunk site unloads while the controller's chunk stays loaded
- **THEN** the controller still reports 2 chunks

#### Scenario: Rail removed
- **WHEN** the rail that linked the crate in the neighbouring chunk is broken while both chunks are loaded
- **THEN** the controller reports 1 chunk

### Requirement: Chunk Loader Upgrade slots
The controller SHALL have `chunk_loader_upgrade_slots` upgrade slots (example 4, `warehouse.json`) that take only the Chunk Loader Upgrade, one per slot. The site's capacity SHALL be the number of upgrades times `chunks_per_upgrade` (example 9, `warehouse.json`). Upgrades SHALL drop when the controller is broken or dismantled. Hoppers and ducts SHALL NOT reach the slots.

#### Scenario: Two upgrades
- **WHEN** a controller holds two Chunk Loader Upgrades with 9 chunks per upgrade
- **THEN** its capacity is 18 chunks

#### Scenario: Wrong item
- **WHEN** the player tries to put a Speed Upgrade into a controller slot
- **THEN** the slot refuses it

### Requirement: Over capacity is an error
A controller with at least one Chunk Loader Upgrade whose site spans more chunks than its capacity SHALL be in an error state: its GUI SHALL say the site is over capacity in red and its activity lamp SHALL be red. Otherwise the lamp SHALL be green while online. A controller without upgrades SHALL show no error and its GUI SHALL say the site cannot wake without a Chunk Loader Upgrade.

#### Scenario: Over capacity
- **WHEN** a controller with one upgrade of 9 chunks spans 10 chunks
- **THEN** its lamp is red and the GUI says 10 of 9 chunks, over capacity

#### Scenario: Back within capacity
- **WHEN** a second upgrade goes into that controller
- **THEN** the lamp turns green and the GUI says 10 of 18 chunks

#### Scenario: No upgrade
- **WHEN** a controller without upgrades spans 3 chunks
- **THEN** its lamp is green and the GUI says it cannot wake without a Chunk Loader Upgrade

### Requirement: Controller GUI
Right clicking a controller with an empty hand SHALL open its GUI with the site name as title, a name field, the online state with the warehouse summary as its tooltip, the chunk count and capacity, a chunk map, the upgrade slots and the player inventory. The chunk map SHALL draw every chunk of the span as a square around the controller's chunk, mark the controller's chunk, and draw in red while over capacity. The GUI SHALL refresh while open and SHALL close when the player leaves reach.

#### Scenario: Map
- **WHEN** a site spans its own chunk and the chunk to the east
- **THEN** the map shows two squares side by side with the controller's chunk marked

### Requirement: Chunk Loader Upgrade is obtainable
The Chunk Loader Upgrade SHALL be a Storage item crafted at a crafting table from a Foundation Chunk Loader, ender pearls, iron ingots and redstone (example), with a tooltip saying it lets a warehouse site wake for transfers. It SHALL NOT be a Foundation multiplier upgrade.

#### Scenario: Recipe
- **WHEN** the player crafts the pattern with a Chunk Loader in the middle
- **THEN** one Chunk Loader Upgrade is produced

### Requirement: Controller shows the wake
While its site is awake by a wake, the controller's activity lamp SHALL be orange, unless the site is over capacity (red). The controller GUI SHALL show a wake line: awake for a transfer, staying awake for the grace time with the seconds left, or waiting to wake with the reason; nothing while the site is not being woken.

#### Scenario: Awake
- **WHEN** a site is woken and a player opens its controller GUI
- **THEN** the lamp is orange and the GUI says the site is awake

#### Scenario: Waiting
- **WHEN** a wake waits because the team limit is reached and a player opens the controller GUI
- **THEN** the GUI says the site is waiting to wake because the team limit is reached

### Requirement: Controller lists the team's sites
The controller GUI SHALL list the other sites of its team in its dimension by name, each with its state: online, sleeping and able to wake, or sleeping and unable to wake. A site without a powered Wireless Packing Station of the team SHALL show its name in orange; hovering it SHALL say "No Wireless Packing Station" when it has none and "Wireless Packing Station has no power" when its stations of the team are all unpowered. A controller without a team SHALL list nothing.

#### Scenario: Two outposts
- **WHEN** the base's controller GUI is open and the team has an online outpost and a sleeping one without upgrades
- **THEN** the list shows both by name, the first online, the second sleeping and unable to wake

#### Scenario: Outpost without power
- **WHEN** the outpost's only Wireless Packing Station of the team has no FE
- **THEN** the base's list shows the outpost's name in orange and its hover says the Wireless Packing Station has no power
