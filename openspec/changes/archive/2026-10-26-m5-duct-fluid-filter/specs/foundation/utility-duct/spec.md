## ADDED Requirements

### Requirement: Per side fluid filters
Each side of a duct's fluid line SHALL have a whitelist of up to nine fluids. An entry SHALL name a fluid type and match that fluid whatever its components. An empty list SHALL pass every fluid. Fluid drained from a side's block, fluid filled into a side's block and fluid pushed into the duct through a side SHALL match that side's list. A side whose block holds several fluids SHALL give only the listed ones.

#### Scenario: Only lava to the furnace
- **WHEN** a side with fluid In drains a tank of water and a tank of lava onto channel 1, and a machine on channel 1 touches a side with only Out whose fluid filter lists lava
- **THEN** the machine receives only lava

#### Scenario: Several tanks in one block
- **WHEN** a side with fluid In whose fluid filter lists Mercury touches a block that holds a Sulfur tank and a Mercury tank
- **THEN** only Mercury leaves that block

#### Scenario: Empty filter
- **WHEN** a side with only Out has no fluid filter entries
- **THEN** its block receives any fluid it accepts, as before

#### Scenario: Pushed in through a filtered side
- **WHEN** another mod's pump pushes water into a side whose fluid filter lists lava
- **THEN** the duct accepts no water through that side

### Requirement: Setting fluid filter slots
A fluid filter slot SHALL be set to the fluid of an item that holds fluid (any mod's container) when clicked with it, the first free slot when such an item is shift clicked in the inventory, by dragging a fluid or a filled container from JEI, or by picking a fluid in the search popup. Right click SHALL clear a slot. An item without fluid SHALL change nothing. The player's item SHALL never be taken or emptied. A slot SHALL show the fluid's texture in its colour and name it in its tooltip.

#### Scenario: Water bucket sets water
- **WHEN** the player clicks the first fluid filter slot with a water bucket on the cursor
- **THEN** the slot shows water, the side passes only water, and the cursor still holds a water bucket

#### Scenario: Filled tank item
- **WHEN** the player shift clicks a dismantled Fluid Tank holding lava in the inventory
- **THEN** the first free fluid filter slot shows lava

#### Scenario: Item without fluid
- **WHEN** the player clicks a fluid filter slot with cobblestone on the cursor
- **THEN** the slot does not change

#### Scenario: Drag a fluid from JEI
- **WHEN** JEI is installed and the player drags Lava from the JEI fluid list onto the second fluid filter slot
- **THEN** the second slot shows lava

#### Scenario: Pick a fluid by search
- **WHEN** the player clicks an empty fluid filter slot with an empty cursor, types "lav" and picks Lava
- **THEN** the slot shows lava

#### Scenario: Clear
- **WHEN** the player shift right clicks a fluid filter slot that shows water
- **THEN** the slot is empty

### Requirement: Per side fluid priorities
Each side of a duct's fluid line SHALL have a priority from -9 to 9, default 0, set in the endpoint GUI, independent of the side's item priority. Routing SHALL offer fluid to the sinks of the highest priority on the channel first, sharing it fairly among them, and SHALL only offer what they cannot take to the next lower priority, and so on.

#### Scenario: Main tank first
- **WHEN** a source gives 1,000 mB a tick on channel 1, tank A touches a side with only Out and fluid priority 5 and tank B one with priority 0, both empty
- **THEN** tank A receives all the fluid until it is full, and only then tank B receives fluid

#### Scenario: Fair within a priority
- **WHEN** one source gives 100 mB in a tick and two empty tanks touch sides with only Out and fluid priority 3
- **THEN** each tank receives 50 mB that tick

#### Scenario: Item priority stays separate
- **WHEN** a side's item priority is 5 and the player sets its fluid priority to -2
- **THEN** after reload the item priority is 5 and the fluid priority is -2

### Requirement: Fluid routing with priorities is pure
Routing fluid by priority tiers and fair shares within a tier SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Tier tests
- **WHEN** the unit tests route one key to sinks of priorities 5, 5 and 0
- **THEN** the two priority 5 sinks share the supply fairly, and the priority 0 sink receives only what both could not take

## MODIFIED Requirements

### Requirement: Fluid line behaviour
Every tick a fluid line network SHALL, per channel, drain fluid from source blocks and fill it into sink blocks on the same channel. The pipe SHALL have no rate limit of its own: as much moves as the sources give and the sinks accept in that tick. Sinks SHALL be served by fluid priority, highest first, and short supply SHALL be shared fairly among the sinks of one priority. A side with only fluid In SHALL make its block a source and a side with only fluid Out its block a sink. Fluid SHALL only leave and enter a block when it passes that side's fluid filter, and a sink SHALL only receive fluid it accepts.

#### Scenario: Pump to tank
- **WHEN** a pump touches a duct's side with only In, a tank touches a side with only Out of a duct further along the run, both on channel 1, and the pump holds water
- **THEN** the tank's water rises every tick while the pump's buffer falls by the same amount

#### Scenario: No pipe limit
- **WHEN** a full 64,000 mB tank is a source and an empty tank of the same size a sink on channel 1
- **THEN** the whole content moves in one tick, as far as both tanks allow

#### Scenario: Two sinks share
- **WHEN** one source on channel 1 can give 100 mB in a tick and two empty tanks on channel 1 with the same fluid priority accept fluid
- **THEN** each tank receives 50 mB that tick

#### Scenario: Different channel
- **WHEN** the pump's side is on channel 1 and the tank's side on channel 2
- **THEN** no fluid moves

#### Scenario: Mixed fluids
- **WHEN** a tank of water and a tank of lava are sources on channel 1 and an empty tank and a water tank are sinks on channel 1, all without fluid filters
- **THEN** the water tank only receives water, and the empty tank receives one fluid and from then on only that fluid

### Requirement: Fluid pushed into a duct
A duct carrying the Fluid Pipe SHALL accept fluid filled into it on a side with fluid In whose fluid filter passes the fluid, and SHALL pass it straight on to sinks on that side's channel by fluid priority and fluid filter, keeping nothing itself. With no sink that accepts the fluid it SHALL accept nothing.

#### Scenario: Foreign pump pushes
- **WHEN** another mod's pump fills water into a duct side on channel 1 and a tank on channel 1 has room
- **THEN** the tank receives the water

#### Scenario: Nowhere to go
- **WHEN** every sink on that channel is full
- **THEN** the duct accepts no fluid

#### Scenario: Pushed fluid follows priority
- **WHEN** another mod's pump fills water into a duct side on channel 1 and two tanks on channel 1 have room, one on fluid priority 2 and one on 0
- **THEN** the priority 2 tank receives the water until it is full

### Requirement: Duct GUI
Using the Configuration Carrot, not sneaking, on any part of a duct (its centre or any arm) SHALL open the endpoint GUI. It SHALL open on the side of the clicked arm when that arm ends at a block other than a duct, and otherwise on the first selectable side in the order down, up, north, south, west, east. A duct without a selectable side SHALL open nothing.

The GUI SHALL show side tabs on top of the page, one fixed place per side in the order down, up, north, south, west, east. A tab SHALL show the item of the block beside that side (the item the pick block key would give), and the side's name in its tooltip together with the block's name. A side whose neighbour is a block other than a duct SHALL have a selectable tab, whatever the flags of its lines, also when no line on it connects. A side toward another duct SHALL show a greyed tab that SHALL NOT be selectable. A side with no block beside it SHALL show no tab, leaving its place free. The selected tab SHALL join the page, and picking another tab SHALL switch the GUI to that side without closing it, keeping the selected line tab when the duct carries it.

The page SHALL show the selected side's name and touched block name, one tab per present line on its left edge showing the line's item, and the page of the selected line. The item, power and fluid pages SHALL show a row of small icon buttons, each explained by its tooltip: In, Out and, on the Item Pipe page only, Pull as toggles that show whether they are on, the redstone setting as one button cycling Ignore redstone (gunpowder), While powered (lit redstone torch) and While unpowered (unlit redstone torch), and an On/Off button. Below the row a channel button SHALL show the side's channel with its colour, icon and name. The Item Pipe page SHALL also show the item filter slots and the item priority with minus and plus buttons, the Fluid Pipe page the fluid filter slots and the fluid priority with minus and plus buttons. The data line page SHALL only show an on and off button. Every page SHALL show a small trash can button in its bottom right corner, apart from the settings, whose tooltip says that it takes the line out of the whole duct and drops it, and that On/Off pauses instead. The player inventory SHALL be shown under every page. Item filter slots SHALL be hidden and inert on every page but the Item Pipe page, and fluid filter slots on every page but the Fluid Pipe page. Clicking a filter slot with an empty cursor SHALL open a search popup over the GUI that lists, on the Item Pipe page, every item with its icon and name, and on the Fluid Pipe page every fluid with its texture and name, filtered by the typed text, with a "none" entry first; picking an entry SHALL set the slot, "none" SHALL clear it. Bare hand right click SHALL open nothing.

#### Scenario: Configure one side
- **WHEN** the player uses the carrot on the west arm that touches a chest, switches Out off and Pull on for the item line and places cobblestone in a filter slot
- **THEN** the duct pulls only cobblestone from the west

#### Scenario: Two lines, two tabs
- **WHEN** the player opens the GUI of a duct with an Item Pipe and a Power Cable
- **THEN** two line tabs show, the Item Pipe page is selected, and clicking the Power Cable tab shows In, Out, the redstone button, On/Off, the channel button and the trash can, without Pull or filter slots

#### Scenario: Fluid Pipe tab
- **WHEN** the player opens the GUI of an arm whose duct carries all three lines and that touches a tank, and clicks the Fluid Pipe tab
- **THEN** the Fluid Pipe page shows In, Out, the redstone button, On/Off, the channel button, nine fluid filter slots, the fluid priority and the trash can, and no Pull or item filter slots

#### Scenario: Data tab
- **WHEN** the player opens the GUI of an arm toward a Warehouse Controller on a duct with the data line
- **THEN** the data page shows an on and off button and the trash can, without flag buttons, redstone button, channel button, filter slots or priority

#### Scenario: Trash can
- **WHEN** the player hovers the trash can on the Item Pipe page
- **THEN** the tooltip names "Remove Item Pipe" and says that it takes the line out of the whole duct and drops it, and that On/Off pauses instead; clicking it removes the Item Pipe from the duct and gives the player the item

#### Scenario: Redstone button
- **WHEN** the item line's side ignores redstone and the player clicks its redstone button twice
- **THEN** the button shows a lit redstone torch after the first click and an unlit one after the second, and its tooltip names the setting

#### Scenario: Open from the centre
- **WHEN** a duct has a furnace above it and a chest to the north and the player uses the carrot on its centre
- **THEN** the GUI opens on the up side, showing the furnace

#### Scenario: Side tabs show the neighbours
- **WHEN** a duct has a chest to the north, a furnace above, another duct to the east and nothing on the other sides, and the player opens its GUI
- **THEN** the north tab shows a chest, the up tab a furnace, the east tab a greyed duct, and down, south and west show no tab; only north and up can be picked

#### Scenario: Switch sides
- **WHEN** the GUI is open on the north side with the Item Pipe tab and the player picks the up tab
- **THEN** the GUI shows the up side's Item Pipe page with the furnace's name and that side's filter, flags and channel

#### Scenario: Duct to duct arm
- **WHEN** the player uses the carrot on an arm that leads to another duct, and the duct also touches a chest to the north
- **THEN** the GUI opens on the north side

#### Scenario: Nothing to configure
- **WHEN** a duct only touches other ducts and the player uses the carrot on it
- **THEN** no GUI opens

#### Scenario: Bare hand
- **WHEN** the player right clicks a duct without the data line with an empty hand
- **THEN** no GUI opens and the click passes through

#### Scenario: Search popup
- **WHEN** the player clicks an empty filter slot with an empty cursor
- **THEN** a popup with a search field and an item list opens over the GUI; typing narrows the list, Escape closes it without changing the slot

#### Scenario: Popup without JEI
- **WHEN** JEI is not installed and the player clicks a filter slot with an empty cursor
- **THEN** the search popup opens and works the same way

#### Scenario: Channel button
- **WHEN** the item line's side is on "Iron ore", red, with an iron ore icon
- **THEN** the channel button shows a red swatch, the iron ore icon and "Iron ore"

