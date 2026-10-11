## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Summary reports shuttles
The controller's summary SHALL state the garage count, the shuttle count, how many are busy, how many jobs wait, how many of those are put-aways, and how many shuttles are parked without a rail. The controller GUI SHALL show it as the tooltip of its online state.

#### Scenario: Busy fleet
- **WHEN** a player opens the GUI of a controller of a warehouse with two garages and four shuttles of which one is on a job and hovers the online state
- **THEN** the summary says two garages, four shuttles, one busy

#### Scenario: Put-aways waiting
- **WHEN** two put-away jobs wait in the queue
- **THEN** the summary says two queued of which two are put-aways

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
