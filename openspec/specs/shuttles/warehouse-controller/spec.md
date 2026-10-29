# shuttles/warehouse-controller Specification

## Purpose

The controller turns touching storage blocks and their neighbours into one searchable stock.

## Requirements

### Requirement: One controller makes a network online
A warehouse network SHALL be online when it contains exactly one Warehouse Controller. With none or more than one, it SHALL be offline and report that state.

#### Scenario: Two controllers
- **WHEN** two controllers touch the same rail run
- **THEN** the network reports offline with the reason "more than one controller"

### Requirement: Batched polling
The network SHALL poll at most `sources_per_tick` sources per tick (default 4, from `data/<ns>/shuttles/warehouse.json`), round robin, and SHALL refresh a storage node's snapshot on the tick after it reports a change.

#### Scenario: Large warehouse
- **WHEN** a network has 40 sources
- **THEN** every source is refreshed at least once per 10 ticks and no tick reads more than 4

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

### Requirement: Summary reports shuttles
The controller's summary SHALL state the garage count, the shuttle count, how many are busy, how many jobs wait, how many of those are put-aways, and how many shuttles are parked without a rail. The controller GUI SHALL show it as the tooltip of its online state.

#### Scenario: Busy fleet
- **WHEN** a player opens the GUI of a controller of a warehouse with two garages and four shuttles of which one is on a job and hovers the online state
- **THEN** the summary says two garages, four shuttles, one busy

#### Scenario: Put-aways waiting
- **WHEN** two put-away jobs wait in the queue
- **THEN** the summary says two queued of which two are put-aways

### Requirement: Controller exposes stock
A Warehouse Controller SHALL expose the Lib stock provider capability on every side for its online warehouse: counts SHALL come from the index, and taking from the provider SHALL give nothing, because items leave a warehouse only by shuttle (other mods and the data line ask the supplier capability, see `shuttles/packing-station`). The controller SHALL NOT expose a stock sink. An offline controller SHALL report no stock.

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
Every Warehouse Controller SHALL be a site with a name of at most 32 characters, set in its GUI. A controller without a name SHALL be shown as "Warehouse at <x>, <z>" with its block position. The name SHALL be kept when the world is saved and SHALL travel in the dropped item when the controller is mined or dismantled with the Configuration Carrot. The controller SHALL remember the team of the player who placed it.

#### Scenario: Unnamed
- **WHEN** a controller is placed at 120, 64, -40 and nobody names it
- **THEN** its GUI title reads "Warehouse at 120, -40"

#### Scenario: Named
- **WHEN** the player types "North Mine" into the name field and presses Enter
- **THEN** the GUI title reads "North Mine" and the name is still there after a reload

#### Scenario: Carrot dismantle
- **WHEN** a controller named "North Mine" is dismantled with the carrot and placed again
- **THEN** it is still named "North Mine"

#### Scenario: Mined
- **WHEN** a controller named "North Mine" is mined with a pickaxe and its item placed again
- **THEN** it is still named "North Mine"

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

### Requirement: Over capacity is an error
A controller with at least one Anchor Upgrade whose site spans more chunks than its capacity SHALL be in an error state: its GUI SHALL say the site is over capacity in red and its activity lamp SHALL be red. A controller in the no power state SHALL show the red lamp as well, and its GUI SHALL say which of the two applies. Otherwise the lamp SHALL be green while online. A controller without upgrades SHALL show no capacity error.

#### Scenario: Over capacity
- **WHEN** a controller with one upgrade of 9 chunks spans 10 chunks
- **THEN** its lamp is red and the GUI says 10 of 9 chunks, over capacity

#### Scenario: Back within capacity
- **WHEN** a second upgrade goes into that controller
- **THEN** the lamp turns green and the GUI says 10 of 18 chunks

#### Scenario: No upgrade
- **WHEN** a controller without upgrades and with FE spans 3 chunks
- **THEN** its lamp is green

#### Scenario: No power
- **WHEN** a controller within capacity holds no FE
- **THEN** its lamp is red and the GUI says the warehouse has no power

### Requirement: Controller GUI
Right clicking a controller with an empty hand SHALL open its GUI with the site name as title, a name field, the online state with the warehouse summary as its tooltip, the chunk count and capacity, a chunk map, the storage lines, the Anchor Upgrade slots, the Wireless Upgrade slot, the FE stored with the capacity, the "Other sites" panel and the player inventory. The chunk map SHALL draw every chunk of the span as a square around the controller's chunk, mark the controller's chunk, and draw in red while over capacity. The storage lines SHALL sit between the map row and the upgrade row: an Items line with the rack slots in use and the total, and a Fluids line with the mB in the Fluid Rack sections and their capacity, each with a bar below it that turns to the alert colour when full. A warehouse without racks SHALL say so on the Items line, one without Fluid Racks on the Fluids line, both without a bar. The Items tooltip SHALL name the number of rack bays and the full zones; the Fluids tooltip SHALL name the number of Fluid Rack sections. While the controller is not online the storage lines SHALL be left out. The GUI SHALL refresh while open and SHALL close when the player leaves reach.

#### Scenario: Map
- **WHEN** a site spans its own chunk and the chunk to the east
- **THEN** the map shows two squares side by side with the controller's chunk marked

#### Scenario: Energy shown
- **WHEN** a controller holds 12,000 of 100,000 FE and a player opens its GUI
- **THEN** the GUI shows 12,000 of 100,000 FE and the value follows while FE arrives

#### Scenario: Storage shown
- **WHEN** a warehouse has 412 of 729 rack slots in use and 24,000 of 96,000 mB in its Fluid Racks and a player opens its controller GUI
- **THEN** the GUI shows Items 412 / 729 slots and Fluids 24k / 96k mB, each with a bar

#### Scenario: Storage follows
- **WHEN** the controller GUI is open and a shuttle puts a stack into an empty rack slot
- **THEN** the Items line counts one more slot in use within a few seconds

#### Scenario: Full zone in the tooltip
- **WHEN** the zone "Ores" is full and the player hovers the Items line
- **THEN** the tooltip names the rack bays and lists Ores as full

#### Scenario: No gantry
- **WHEN** a warehouse has racks but no Fluid Rack
- **THEN** the Fluids line says there is no fluid storage and shows no bar

#### Scenario: Fluid Racks in the tooltip
- **WHEN** a warehouse has two Fluid Rack sections and the player hovers the Fluids line
- **THEN** the tooltip names 2 Fluid Rack sections

### Requirement: Controller shows the wake
While its site is awake by a wake, the controller's activity lamp SHALL be orange, unless the site is over capacity or in the no power state (red). The controller GUI SHALL show a wake line: awake for a transfer, staying awake for the grace time with the seconds left, or waiting to wake with the reason; nothing while the site is not being woken.

#### Scenario: Awake
- **WHEN** a site with FE is woken and a player opens its controller GUI
- **THEN** the lamp is orange and the GUI says the site is awake

#### Scenario: Waiting
- **WHEN** a wake waits because the team limit is reached and a player opens the controller GUI
- **THEN** the GUI says the site is waiting to wake because the team limit is reached

### Requirement: Controller lists the team's sites
The controller GUI SHALL have an "Other sites" panel. While the controller is wireless capable the panel SHALL list the other sites of its team in its dimension by name, each with its state: online, sleeping and able to wake, or sleeping and unable to wake. A site whose controller is not wireless capable SHALL show its name in orange. Hovering a site SHALL show only its name and its state. While the controller is not wireless capable the panel SHALL list nothing and show a short hint instead: "Insert a Wireless Upgrade to trade with other sites" without the upgrade, or that the controller needs FE for wireless with the upgrade but without power. A controller without a team SHALL list nothing. No text of the panel SHALL use the word team.

#### Scenario: Two outposts
- **WHEN** the base's controller is wireless capable, its GUI is open, and the team has an online outpost and a sleeping one without upgrades
- **THEN** the panel shows both by name, the first online, the second sleeping and unable to wake

#### Scenario: Outpost without power
- **WHEN** the outpost's controller has no Wireless Upgrade or no power
- **THEN** the base's panel shows the outpost's name in orange and its hover shows only the name and the state

#### Scenario: No upgrade here
- **WHEN** the base's controller has no Wireless Upgrade and its GUI is open
- **THEN** the panel lists no site and says "Insert a Wireless Upgrade to trade with other sites"

#### Scenario: Upgrade without power here
- **WHEN** the base's controller holds a Wireless Upgrade and no FE
- **THEN** the panel lists no site and says the controller needs FE for wireless

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

### Requirement: Wireless Upgrade slot
The controller SHALL have one slot that takes only the Wireless Upgrade, apart from its Anchor Upgrade slots. A second Wireless Upgrade SHALL add nothing and does not fit. The upgrade SHALL drop when the controller is broken or dismantled. Hoppers and ducts SHALL NOT reach the slot.

#### Scenario: Insert the upgrade
- **WHEN** the player puts a Wireless Upgrade into the controller's wireless slot
- **THEN** the slot takes it and the controller reports that it holds a Wireless Upgrade

#### Scenario: Wrong item
- **WHEN** the player tries to put a Anchor Upgrade or a Speed Upgrade into the wireless slot
- **THEN** the slot refuses it

#### Scenario: Dismantled
- **WHEN** a controller holding a Wireless Upgrade is dismantled with the Configuration Carrot
- **THEN** the Wireless Upgrade drops beside the controller item

### Requirement: Controller FE buffer
The controller SHALL have an FE buffer of `controller_energy_capacity` (example 100,000, `shuttles/power.json`) that takes FE from every side through the standard energy capability, for example from the Utility Duct power line, and never gives FE out. The FE stored SHALL be saved with the controller and SHALL travel in the dropped item when the controller is mined or dismantled with the Configuration Carrot; a placed controller SHALL start with the FE its item carries, at most its capacity. The controller GUI SHALL show the FE stored and the capacity.

#### Scenario: Power line
- **WHEN** a Utility Duct with the power line fed by a Coal Generator touches the controller
- **THEN** the duct shows a power arm toward the controller and its FE stored rises

#### Scenario: No FE out
- **WHEN** a machine next to the controller tries to pull FE from it
- **THEN** it gets none

#### Scenario: Mined with FE
- **WHEN** a controller holding 12,000 FE is mined and its item placed again
- **THEN** the placed controller holds 12,000 FE

#### Scenario: Dismantled with FE
- **WHEN** a controller holding 12,000 FE is dismantled with the carrot and placed again
- **THEN** the placed controller holds 12,000 FE

#### Scenario: More than the capacity
- **WHEN** a controller item carries 150,000 FE and the capacity is 100,000
- **THEN** the placed controller holds 100,000 FE

#### Scenario: New controller
- **WHEN** a freshly crafted controller is placed
- **THEN** it holds no FE

### Requirement: Wireless capable controller
A controller SHALL be wireless capable while it holds a Wireless Upgrade and is powered: its buffer holds at least the cost of one item at the smallest cost, `wireless_fe_per_item` rounded up and at least 1 FE (example 4). A site whose controller is not wireless capable SHALL be offline for wireless: it neither sends nor receives. The state SHALL follow upgrade and FE changes, never a scan.

#### Scenario: Upgrade and power
- **WHEN** a controller holds a Wireless Upgrade and 500 FE with 4 FE per item
- **THEN** it is wireless capable

#### Scenario: Upgrade without power
- **WHEN** a controller holds a Wireless Upgrade and 2 FE with 4 FE per item
- **THEN** it is not wireless capable and its GUI says it needs 4 FE for wireless

#### Scenario: Power without upgrade
- **WHEN** a controller holds 50,000 FE and no Wireless Upgrade
- **THEN** it is not wireless capable

### Requirement: Transfers between sites
A wireless capable site SHALL be able to send an amount of an item to another wireless capable site of the same team in the same dimension: it SHALL plan the picks like a shipment, by shuttle from its racks, into the To network buffer of its Packing Station with the shortest shuttle trip, and that station SHALL move the items to a Packing Station of the receiving site, whose warehouse puts them away into its racks by shuttle. The transfer SHALL report planned and delivered amounts and whether it ended. It SHALL be refused when either site is not wireless capable, either site has no Packing Station, the sending site has none of the item, or no shuttle of the sending site reaches the racks holding it.

#### Scenario: Iron from the outpost
- **WHEN** a Heavy Duty Rack at the outpost served by a shuttle holds 32 iron ingots and a transfer of 16 to the base is made, both controllers wireless capable and both sites with a Packing Station
- **THEN** a shuttle brings 16 iron ingots to the outpost's Packing Station, they go over the wireless line and end up in a rack of the base, and the transfer reports 16 delivered and ended

#### Scenario: No shuttle at the outpost
- **WHEN** the iron sits in a rack at the outpost and the outpost has no garage with a shuttle
- **THEN** the transfer is refused because the outpost needs a shuttle and nothing leaves the outpost

#### Scenario: No Packing Station at the outpost
- **WHEN** the outpost's controller is wireless capable but the outpost has no Packing Station on its rails
- **THEN** the transfer is refused and nothing leaves the outpost

### Requirement: Wireless cost per item and distance
Each item a transfer moves SHALL cost `wireless_fe_per_item` plus `wireless_fe_per_item_per_block` times the straight distance in blocks between the two controllers (examples 4 and 0.02, `shuttles/power.json`), paid from the receiving site's controller buffer when the item moves. The sending site SHALL pay nothing for the wireless hop. Without enough FE at the receiver nothing SHALL move and the transfer SHALL wait.

#### Scenario: Cost
- **WHEN** 10 items move between two sites whose controllers are 500 blocks apart, with 4 FE per item and 0.02 FE per item and block
- **THEN** the receiving controller's buffer loses 140 FE and the sending controller's buffer is unchanged

#### Scenario: Receiver runs dry
- **WHEN** the receiving controller holds FE for 3 items of a transfer of 10
- **THEN** 3 items move and the rest waits until FE arrives

### Requirement: Running transfers wait for power
A transfer whose sending or receiving controller stops being wireless capable SHALL stop moving items, keep the items already picked in the sending Packing Station, stay open as waiting for power, and go on once both controllers are wireless capable again. It SHALL be cancellable meanwhile; cancelled items go back into the sending site's storage.

#### Scenario: Power lost during a transfer
- **WHEN** the base controller's FE is taken away while iron is being sent to it
- **THEN** no more iron moves, the transfer shows waiting for power, and once FE arrives again the rest of the iron arrives and the transfer ends

#### Scenario: Upgrade taken out
- **WHEN** the player takes the Wireless Upgrade out of the outpost's controller during a transfer
- **THEN** the transfer waits for power and goes on once the upgrade is back

### Requirement: Wireless Upgrade is obtainable
The Wireless Upgrade SHALL be a Shuttles item crafted at a crafting table from an ender eye, iron ingots, redstone and a copper ingot (example), with a tooltip saying it lets the warehouse trade with other sites over a wireless line for FE. It SHALL NOT be a Foundation multiplier upgrade.

#### Scenario: Recipe
- **WHEN** the player crafts the pattern with an ender eye in the middle
- **THEN** one Wireless Upgrade is produced

### Requirement: The warehouse runs on the controller's FE
Every FE cost of a warehouse SHALL be paid from its controller's FE buffer: wireless transfers, shuttles driving, waking and Crafting Stations working. No other warehouse block SHALL take or hold FE, and nothing SHALL cost FE while idle: storing items, racks, rails, garages, stations, terminals and a CPU without running steps draw nothing. A draw SHALL be paid in full or not at all.

#### Scenario: One connection
- **WHEN** a warehouse with shuttles and a Crafting Station gets FE only through a power duct on its controller
- **THEN** its shuttles drive and the station crafts, and the controller's FE drops by what they used

#### Scenario: Idle warehouse
- **WHEN** a warehouse with 5,000 FE in its controller does nothing for a minute
- **THEN** its controller still holds 5,000 FE

#### Scenario: Draw larger than the buffer
- **WHEN** a shuttle wants to start a block costing 20 FE and the controller holds 15 FE
- **THEN** no FE is taken and the shuttle waits

### Requirement: No power state
A controller SHALL show the no power state while its buffer is empty or a draw was refused during the last second; the state is a warning and stops nothing by itself. Each consumer SHALL stop only when its own draw is refused: a shuttle whose draw for the next block is refused waits at that block with its cargo, a new shuttle job does not start while its first draw cannot be paid, a Crafting Station whose draw for a tick is refused pauses, a wake does not start without its own FE and a running wake ends only when its own cost for a tick cannot be paid. A refused draw of one consumer SHALL NOT stop another. Every stopped consumer SHALL resume by itself where it stopped once its draw is paid, and the state SHALL end once no draw was refused for a second and the buffer holds FE. Stock SHALL stay visible and racks and their GUIs SHALL stay usable by hand.

#### Scenario: A shuttle draw fails, the wake stays
- **WHEN** a woken outpost holds enough FE for its wake but not for a long shuttle trip
- **THEN** the shuttle waits at the next block, the no power line shows, and the wake keeps the site loaded

#### Scenario: Generator runs out
- **WHEN** the only Coal Generator feeding a busy warehouse runs out of coal and the controller's buffer empties
- **THEN** the controller enters the no power state, shuttles stop at the next block and no new job starts

#### Scenario: Power returns
- **WHEN** coal goes back into the generator
- **THEN** the no power state ends, the stopped shuttles drive on with their cargo and waiting jobs start

#### Scenario: Take by hand
- **WHEN** a warehouse is in the no power state and the player opens a Heavy Duty Rack holding iron
- **THEN** the rack's GUI opens and the player takes the iron out

### Requirement: Controller shows power
The controller GUI SHALL show, next to the FE stored and the capacity, the FE used during the last second, and in the no power state a line in red saying the warehouse has no power and its automation waits. The warehouse summary SHALL say when it has no power.

#### Scenario: Use shown
- **WHEN** two shuttles drive at 3 blocks per second each with 20 FE per block
- **THEN** the controller GUI shows about 120 FE used per second

#### Scenario: No power line
- **WHEN** the controller is in the no power state and a player opens its GUI
- **THEN** the GUI shows the no power line in red

### Requirement: Anchor Upgrade slots
The controller SHALL have `anchor_upgrade_slots` upgrade slots (example 4, `warehouse.json`) that take only the Foundation Anchor Upgrade, one per slot. The site's capacity SHALL be the number of upgrades times `chunks_per_upgrade` (example 9, `warehouse.json`). Upgrades SHALL drop when the controller is broken or dismantled. Hoppers and ducts SHALL NOT reach the slots.

#### Scenario: Two upgrades
- **WHEN** a controller holds two Anchor Upgrades with 9 chunks per upgrade
- **THEN** its capacity is 18 chunks

#### Scenario: Wrong item
- **WHEN** the player tries to put a Speed Upgrade into a controller slot
- **THEN** the slot refuses it

### Requirement: Controller item tooltip
A Warehouse Controller item that carries a site name SHALL show the name in its tooltip, and one that carries FE SHALL show the FE it holds. An item that carries neither SHALL show neither line.

#### Scenario: Named item with FE
- **WHEN** a player hovers a controller item that was mined from "North Mine" holding 12,000 FE
- **THEN** the tooltip shows "North Mine" and 12,000 FE

#### Scenario: Fresh item
- **WHEN** a player hovers a freshly crafted controller item
- **THEN** the tooltip shows no site name and no FE line

### Requirement: Site list scrollbar
The "Other sites" panel SHALL show at most 16 sites at once. While it lists sites, the shared Foundation scrollbar SHALL show beside the rows; with more sites than rows, dragging the thumb, a press on the track and the mouse wheel over the panel SHALL scroll the list one site per wheel notch, stopping at the first and last site. Hovering a site SHALL show the tooltip of the site under the mouse at the current scroll position.

#### Scenario: Many outposts
- **WHEN** the team has 20 other sites and the panel shows 16
- **THEN** a scrollbar beside the rows shows the visible share, and dragging its thumb to the bottom shows sites 5 to 20

#### Scenario: Few outposts
- **WHEN** the team has two other sites
- **THEN** the panel lists both and the scrollbar shows a greyed thumb over the whole track

#### Scenario: No list
- **WHEN** the controller is not wireless capable and the panel shows its hint
- **THEN** no scrollbar shows
