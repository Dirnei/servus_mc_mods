# foundation/utility-duct Specification

## Purpose

The Utility Duct is Foundation's one pipe: channels for items and energy in the same block, configured per side.

## Requirements

### Requirement: Channels are placed as items
An Item Pipe, Power Cable, Fluid Pipe or Data Cable item SHALL place a Utility Duct carrying that line, or add the line to a clicked duct that lacks it. Clicking a duct that already carries the line SHALL place a new duct against the clicked face like any block. Sneaking SHALL use the placement assist with the run axis: the run and its connections are the ducts and arms of the held line, and otherwise the direction is the clicked face. When the spot beside the clicked duct is free, sneaking places there as before. A duct SHALL drop its line items when broken or dismantled, and removing the last line SHALL remove the duct.

#### Scenario: Add a second channel
- **WHEN** a player uses a Power Cable on a duct that carries only an Item Pipe
- **THEN** the duct carries both and one Power Cable is consumed

#### Scenario: Add the fluid line
- **WHEN** a player uses a Fluid Pipe on a duct that carries an Item Pipe and a Power Cable
- **THEN** the duct carries all three lines and one Fluid Pipe is consumed

#### Scenario: Add the data line
- **WHEN** a player uses a Data Cable on a duct that carries an Item Pipe
- **THEN** the duct carries both and one Data Cable is consumed

#### Scenario: Extend a run
- **WHEN** a player uses an Item Pipe on the east face of a duct that already carries an Item Pipe
- **THEN** a new Item Pipe duct is placed east of it

#### Scenario: Sneak places
- **WHEN** a sneaking player uses a Fluid Pipe on the top of a duct that lacks the Fluid Pipe
- **THEN** a new Fluid Pipe duct is placed above it and the clicked duct stays unchanged

#### Scenario: Sneak extends a run
- **WHEN** six ducts carrying an Item Pipe stand in a line from west to east and a sneaking player uses an Item Pipe on the east face of the second one
- **THEN** a new Item Pipe duct is placed east of the sixth duct

#### Scenario: Top of a horizontal pipe
- **WHEN** six Item Pipe ducts stand in a line from west to east and a sneaking player uses an Item Pipe on the top face of the fourth one, east of its centre
- **THEN** a new Item Pipe duct is placed east of the sixth duct, not above the fourth

#### Scenario: Sneak extends upwards
- **WHEN** a column of four Power Cable ducts rises from the floor and a sneaking player uses a Power Cable on the top face of the lowest duct
- **THEN** a new Power Cable duct is placed on top of the column

#### Scenario: Run of another line stops the walk
- **WHEN** a sneaking player uses a Fluid Pipe on the east face of a Fluid Pipe duct whose east neighbour is a duct carrying only an Item Pipe
- **THEN** nothing is placed

#### Scenario: Broken duct
- **WHEN** a duct with all three lines is broken
- **THEN** one Item Pipe, one Power Cable and one Fluid Pipe drop

#### Scenario: Old item ids
- **WHEN** a command refers to `servus_logistics:item_pipe`, `servus_logistics:item_channel` or `servus_foundation:energy_channel`
- **THEN** no such item exists; the items are `servus_foundation:item_pipe`, `servus_foundation:power_cable` and `servus_foundation:fluid_pipe`

### Requirement: Per side modes per channel
Each item, power and fluid line of a duct SHALL have three flags per side, In, Out and Pull, kept with the block and set through the In, Out and Pull buttons of the line's page. The default SHALL be In and Out. In SHALL mean the side takes from its block, Out that it delivers to its block, and Pull that the duct also extracts items from its block. Pull SHALL only exist on the Item Pipe and only together with In: switching In off SHALL switch Pull off, and switching Pull on SHALL also switch In on. A side with neither In nor Out SHALL not connect visually or logically, and SHALL stay reachable in the endpoint GUI through the side tabs so its flags can be set again. The data line SHALL only be connected or not, through its on and off button.

#### Scenario: Set a mode
- **WHEN** the player switches Pull on for the item line's west side, which is In and Out
- **THEN** the side is In, Out and Pull, and the duct keeps that after reload and the GUI shows it

#### Scenario: Pull switches In on
- **WHEN** the item line's west side is only Out and the player clicks its Pull button
- **THEN** the side is In, Out and Pull

#### Scenario: In off drops Pull
- **WHEN** the item line's west side is In and Pull and the player switches In off
- **THEN** the side has neither In nor Out nor Pull and its arm goes away

#### Scenario: Enable an unconnected side again
- **WHEN** the item line's west side toward a chest has neither In nor Out, so the duct shows no arm there, and the player uses the carrot on the duct's centre, picks the West tab and switches In and Out on
- **THEN** the arm toward the chest is back and items reach the chest again

#### Scenario: Older worlds
- **WHEN** a world saved before the flags is loaded, with sides in and out, pull in, push out and disabled
- **THEN** on the Item Pipe they load as In and Out, In and Pull, Out, and neither, and a fluid or power pull in side loads as In

### Requirement: Item channel behaviour
Items pushed into a duct on a side with item In SHALL be routed round robin to inventories on sides with item Out whose item channel equals the inserting side's channel, never back to the inserting inventory, within the per tick budget. Every `pull_interval` ticks each side with item Pull SHALL extract up to `items_per_pull` items from its inventory and route them on its own channel, never back to that inventory, also when the side has Out. A side with In but without Pull SHALL take only what its block pushes into the duct and SHALL never extract from it.

#### Scenario: Pull from a chest into another
- **WHEN** chest A touches a duct's side with In and Pull, chest B touches a side with Out on the same channel and chest A holds 20 cobblestone
- **THEN** within a few seconds chest B holds the 20 cobblestone

#### Scenario: In only does not pull
- **WHEN** chest A touches a side with only In, chest B a side with Out on the same channel, and chest A holds 20 cobblestone
- **THEN** chest A keeps its cobblestone, and 5 dirt pushed into the duct through chest A's side reach chest B

#### Scenario: In, Out and Pull
- **WHEN** chest A touches a side with In, Out and Pull, chest B a side with Out on the same channel, and chest A holds 20 cobblestone
- **THEN** the cobblestone moves to chest B and none goes back into chest A

#### Scenario: Disabled side
- **WHEN** chest B's side has neither In nor Out
- **THEN** chest B receives nothing

#### Scenario: Different channel
- **WHEN** chest B's side is on channel 2 and chest A's side on channel 1
- **THEN** chest B receives nothing

### Requirement: Energy channel behaviour
Every tick an energy line network SHALL, per channel, move energy from storages on sides allowing input that can extract to storages on sides allowing output that can receive and share the channel, water filling, up to `energy_per_tick`.

#### Scenario: Generator to machine through ducts
- **WHEN** a coal generator touches one end of three energy ducts and a powered machine the other, both sides on channel 1
- **THEN** the machine's buffer fills and it works

#### Scenario: Two channels in one run
- **WHEN** generator A and machine A are on channel 1 and generator B and machine B on channel 2 along the same ducts, and only generator A has fuel
- **THEN** machine A works and machine B's buffer stays empty

### Requirement: Separate channel networks
Ducts SHALL connect per line: an item line network only spans ducts that carry the Item Pipe, an energy line network only ducts that carry the Power Cable, a fluid line network only ducts that carry the Fluid Pipe, and a data network only ducts that carry the data line.

#### Scenario: Gap in a channel
- **WHEN** three ducts in a row carry items but only the outer two carry energy
- **THEN** items flow end to end and energy does not

#### Scenario: Gap in the fluid line
- **WHEN** three ducts in a row carry the Item Pipe but only the outer two the Fluid Pipe
- **THEN** items flow end to end and fluid does not

### Requirement: Crafting
Item Pipes SHALL be crafted from copper ingots, iron ingots, glass and a chest; Power Cables from copper ingots, redstone and glass; Fluid Pipes from copper ingots, iron ingots, glass and a bucket; eight each. Data Cables SHALL be crafted from two copper ingots and a redstone dust, four at a time.

#### Scenario: Recipes
- **WHEN** the player crafts any of the three pipe and cable patterns
- **THEN** eight of the line item are produced

#### Scenario: Copper and iron pipes
- **WHEN** the player lays copper ingots in the four corners, iron ingots in the middle of the top and bottom rows, glass left and right and a chest in the centre
- **THEN** eight Item Pipes are produced, and with a bucket in the centre eight Fluid Pipes

#### Scenario: Data Cable recipe
- **WHEN** the player crafts a copper ingot, a redstone dust and a copper ingot in a row
- **THEN** four Data Cables are produced

### Requirement: Per side item filters
Each side of a duct's item line SHALL have a whitelist of up to nine ghost items. A slot SHALL be set by dragging an item from JEI onto it, by picking an item in the search popup, by clicking it with an item on the cursor or by shift clicking an inventory item; right clicking a slot SHALL clear it. Setting a slot SHALL never take the item from the player. An empty list passes everything. Items pulled in from a side and items pushed out to a side SHALL match that side's list.

#### Scenario: Sorting pull
- **WHEN** a chest holding cobblestone and dirt touches a pull in side whose filter lists cobblestone
- **THEN** only the cobblestone leaves the chest

#### Scenario: Sorting delivery
- **WHEN** chest A's side lists iron ingots and chest B's side has no filter and iron and copper ingots arrive
- **THEN** chest A receives only iron ingots and chest B receives the copper ingots

#### Scenario: Filter for an item the player does not own
- **WHEN** a player with an empty inventory opens the endpoint GUI, clicks the first filter slot, types "diamond" and picks Diamond
- **THEN** the slot shows a diamond, the side only passes diamonds, and the player's inventory is still empty

#### Scenario: Drag from JEI
- **WHEN** JEI is installed and the player drags Iron Ingot from the JEI list onto the third filter slot
- **THEN** the third slot shows an iron ingot and the side passes iron ingots

#### Scenario: Clear by right click
- **WHEN** the player right clicks a filter slot that shows cobblestone
- **THEN** the slot is empty

#### Scenario: Cursor item is kept
- **WHEN** the player clicks a filter slot while holding a stack of 32 cobblestone on the cursor
- **THEN** the slot shows cobblestone and the cursor still holds 32 cobblestone

### Requirement: Per side priorities
Each side of a duct's item line SHALL have a priority from -9 to 9, default 0, set in the endpoint GUI. Routing SHALL offer an item to all outputs of the highest priority on its channel first, round robin among them, and only move to lower priorities when none of them accepts.

#### Scenario: Preferred output
- **WHEN** two chests touch push out sides with priorities 5 and 0 and 20 items arrive
- **THEN** all 20 land in the priority 5 chest

#### Scenario: Overflow
- **WHEN** the priority 5 chest is full
- **THEN** further items go to the priority 0 chest

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

### Requirement: In and out sides for fluids
On a side with fluid In and Out, a block that accepts the fluid offered on its channel SHALL be a sink, and a block that gives fluid but accepts none SHALL be a source. So a pump drains into the pipe and a tank fills from it with the default flags, and fluid never runs back and forth between two tanks. A side with only fluid In SHALL make its block a source, and a side with only fluid Out its block a sink.

#### Scenario: Default settings
- **WHEN** a pump and a tank touch the two ends of a fluid duct run with every side left on In and Out
- **THEN** water flows from the pump into the tank

#### Scenario: Emptying a tank
- **WHEN** tank A touches a side with only In and tank B a side with In and Out on the same channel
- **THEN** fluid flows from tank A to tank B and never back

#### Scenario: Two tanks on default sides
- **WHEN** a full tank A and an empty tank B of the same size touch In and Out sides on the same channel
- **THEN** A's content moves into B and stays there; B keeps acting as a sink until the side's configuration changes

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

### Requirement: Named channel per line per side
Each side of the item, power and fluid line SHALL be on one channel from the channel list, Default unless set, chosen in the endpoint GUI and kept with the block. Channels of the item, power and fluid lines on the same side are independent. Items, energy and fluid SHALL only move between sides on the same channel.

#### Scenario: Select a channel
- **WHEN** the player opens the endpoint GUI of the east side and picks "Iron ore" for the item line
- **THEN** the item line's east side stays on "Iron ore" after reload and the power and fluid lines' east sides stay on Default

#### Scenario: Routing by name
- **WHEN** chest A's pull side and chest B's push side are on "Iron ore" and chest C's push side is on Default
- **THEN** items from chest A reach chest B and never chest C

### Requirement: Channel popup
The channel button of a line page SHALL open a popup over the GUI with a search field, a toggle between "This cable" and "All", the matching channels with colour, icon and name, and "+ New". "This cable" SHALL list Default and the channels any loaded side of this line's connected run uses; "All" SHALL list Default and the player's team's channels. Picking an entry SHALL put the side on that channel. "+ New" SHALL open a form for name, colour and icon and, on save, create the channel and put the side on it.

#### Scenario: This cable
- **WHEN** the run of the item line uses "Iron ore" and "Gold" and the team also has "Main grid" used only on power lines
- **THEN** "This cable" lists Default, "Gold" and "Iron ore", and "All" lists "Main grid" too

#### Scenario: New channel for this side
- **WHEN** the player clicks "+ New", types "Copper", picks orange and saves
- **THEN** the side is on "Copper", its ring turns orange, and "Copper" is listed under "This cable"

#### Scenario: Search
- **WHEN** the popup is on "All" and the player types "ore"
- **THEN** only channels whose name contains "ore" are listed

### Requirement: Ducts stand in water
A Utility Duct SHALL be waterloggable. Placed into a water source it SHALL keep the water. A water bucket used on a dry duct SHALL waterlog it, and an empty bucket used on a waterlogged duct SHALL take the water out. Breaking a waterlogged duct SHALL leave a water source. Water SHALL flow out of a waterlogged duct as from any source. The duct's lines SHALL not take or give the water it stands in.

#### Scenario: Duct into a pond
- **WHEN** a player places a duct into a water source block of a pond
- **THEN** the duct is placed waterlogged and the pond has no gap

#### Scenario: Bucket on a dry duct
- **WHEN** a player uses a water bucket on a dry duct
- **THEN** the duct is waterlogged and the player holds an empty bucket

#### Scenario: Empty bucket on a waterlogged duct
- **WHEN** a player uses an empty bucket on a waterlogged duct
- **THEN** the duct is dry and the player holds a water bucket

#### Scenario: Breaking a waterlogged duct
- **WHEN** a waterlogged duct is broken
- **THEN** a water source stays where the duct was

#### Scenario: Fluid pipe in water
- **WHEN** a waterlogged duct carries a Fluid Pipe whose side toward a tank pushes out on channel 1 and no source is on channel 1
- **THEN** the tank receives no water

### Requirement: Ducts hold back other fluids
Flowing water and lava SHALL stop at a Utility Duct like at a solid wall: they SHALL neither enter the duct's block nor wash the duct away. A duct SHALL never hold lava. Placing a duct into a lava source SHALL fail without a message and the lava SHALL stay. Placing a duct into flowing water or lava SHALL place it dry, and the duct SHALL then dam the flow.

#### Scenario: Water flows against a dry duct
- **WHEN** water flows toward a dry duct lying on the ground
- **THEN** the duct stays dry, no item drops and the water flows around it

#### Scenario: Lava flows against a duct
- **WHEN** lava flows toward a duct
- **THEN** the duct stays and the lava does not enter the duct's block

#### Scenario: Duct into a lava source
- **WHEN** a player places a duct into a lava source block
- **THEN** no duct is placed and the lava source stays

#### Scenario: Duct into flowing water
- **WHEN** a player places a duct into flowing water
- **THEN** the duct is placed dry and the water stops at it

### Requirement: Redstone setting per line per side
Each item, power and fluid line of a duct SHALL have a redstone setting per side, Ignore redstone, While powered or While unpowered, default Ignore redstone, kept with the block and cycled in that order by the line's redstone button. The settings of the lines on one side SHALL be independent. The data line SHALL have none.

#### Scenario: Kept on reload
- **WHEN** the fluid redstone setting of the east side is While powered and the world is reloaded
- **THEN** the fluid setting of east is still While powered, and the item and power settings of east are Ignore redstone

### Requirement: On/Off per line per side
Each item, power and fluid line of a duct SHALL have an On/Off switch per side, default on, kept with the block and toggled by the line's On/Off button. A side SHALL be active when it is switched on and its redstone setting is met: Ignore redstone always, While powered while the duct is powered, While unpowered while it is not. Switching a side off or on SHALL keep its redstone setting and its flags. The data line SHALL have none.

#### Scenario: Off kept on reload
- **WHEN** the item line's up side is switched off and the world is reloaded
- **THEN** the item line's up side is still off and its other lines are on

#### Scenario: Off keeps the redstone setting
- **WHEN** a side is While unpowered and the player switches it off and on again
- **THEN** it is While unpowered again and runs as before

#### Scenario: Older Control
- **WHEN** a world saved with the older Control is loaded, a side on Off and one on While powered
- **THEN** the first loads switched off with Ignore redstone, the second switched on with While powered

### Requirement: Inactive sides move nothing
A side that is not active SHALL keep its arm, connection, flags, channel, filter and priority, and SHALL move nothing on that line: it SHALL not be pulled from, SHALL not be a source or a sink, SHALL not receive routed items, fluid or energy, and SHALL accept nothing filled or inserted into the duct through it.

#### Scenario: Switch a side off
- **WHEN** chest A touches a side with In and Pull, chest B a side with Out on the same channel, chest A holds 20 cobblestone, and the player switches chest A's item side off
- **THEN** chest A keeps its cobblestone, the arm toward chest A stays, and after switching it on again the cobblestone reaches chest B

#### Scenario: Lines are independent
- **WHEN** a machine's side carries the Item Pipe and the Power Cable, a lever beside the duct powers it, and the item redstone setting is While unpowered while the power setting is Ignore redstone
- **THEN** the machine receives energy and no items

#### Scenario: Off side accepts no pushed fluid
- **WHEN** a pump pushes water into a duct side whose fluid line is switched off and a tank on the same channel has room
- **THEN** the duct accepts no water through that side

### Requirement: Duct redstone
A duct SHALL count as powered while it receives a redstone signal from any neighbour, like a block that a lever or redstone dust powers. The powered state SHALL follow signal changes as they happen and SHALL NOT be found by checking blocks every tick. A duct SHALL not emit redstone.

#### Scenario: Lever on a duct
- **WHEN** chest A touches a side with In and Pull whose item redstone setting is While powered, chest B a side with Out on the same channel, chest A holds cobblestone and a lever on the ground beside the duct is switched on
- **THEN** cobblestone moves to chest B, and once the lever is switched off no more cobblestone moves

#### Scenario: Comparator stops the input
- **WHEN** a comparator reads a full chest and powers the duct, and the item redstone setting of the side feeding that chest is While unpowered
- **THEN** no items are delivered to that chest while it is full

### Requirement: Side setting logic is pure
The In, Out and Pull flags with their rules (Pull only with In, only on the Item Pipe, the data line only connected or not) and the loading of the older single modes, whether a side is active for its On/Off switch, redstone setting and powered state, the redstone cycle order and its storage encoding SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Activity table
- **WHEN** the unit tests check each redstone setting for on and off, powered and unpowered
- **THEN** a switched off side is never active; switched on, Ignore redstone is active in both, While powered only when powered, While unpowered only when unpowered, and the cycle runs Ignore redstone, While powered, While unpowered, Ignore redstone

#### Scenario: Flag tests
- **WHEN** the unit tests set and toggle flags and load the older mode names
- **THEN** Pull never stays without In, a side with neither In nor Out does not connect, and in and out, pull in, push out and disabled load as In and Out, In and Pull, Out and neither

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

### Requirement: Amounts on filter entries
Every item filter slot and every fluid filter slot of a duct side SHALL carry an amount, none by default, in items for item entries and in mB for fluid entries. The amount SHALL be kept with the block and on dismantling, and SHALL be cleared with its slot. An entry without an amount SHALL behave as before. When two entries of one side name the same item or fluid, the first one's amount SHALL count.

#### Scenario: Default
- **WHEN** a player sets cobblestone in a filter slot
- **THEN** the slot has no amount and cobblestone passes as before

#### Scenario: Cleared with the slot
- **WHEN** a slot shows sapling with amount 64 and the player shift right clicks it
- **THEN** the slot is empty, and setting sapling again gives it no amount

### Requirement: Keep amount on giving sides
On a side that gives (an item side with Pull, a fluid side with only In, or a fluid side with In and Out acting as a source), an entry's amount SHALL be a keep amount: the duct SHALL take that item or fluid from the block only while the block holds more than the amount, and SHALL never take it below the amount. The held amount SHALL count everything of that item or fluid the block shows to that side, over all its slots or tanks.

#### Scenario: Keep 64 saplings
- **WHEN** a chest holds 300 oak saplings, its side has In and Pull with the entry "oak sapling, 64", and a composter's side on the same channel has Out
- **THEN** the duct moves 236 saplings and the chest keeps 64

#### Scenario: Overflow to the Sink
- **WHEN** a tank holds 12,000 mB of water, its side has only In with the entry "water, 8,000 mB", and a Sink on the same channel accepts fluid
- **THEN** 4,000 mB move to the Sink and the tank stays at 8,000 mB, and when a pump fills the tank again only the water above 8,000 mB moves

#### Scenario: Below the amount
- **WHEN** the chest holds 40 saplings with the entry "oak sapling, 64"
- **THEN** no sapling leaves the chest

#### Scenario: Counted over all slots
- **WHEN** a chest holds 3 stacks of 64 cobblestone in different slots and the entry of its side with Pull is "cobblestone, 100"
- **THEN** 92 cobblestone leave the chest

### Requirement: Fill up to amount on receiving sides
On a side that receives (an item side with Out receiving routed items, a fluid side with only Out, or a fluid side with In and Out acting as a sink), an entry's amount SHALL be a fill up to amount: the block SHALL receive that item or fluid only until it holds the amount. Further items or fluid SHALL go to other outputs on the channel as if this one were full.

#### Scenario: Regulate a furnace
- **WHEN** a furnace's side has only Out with the entry "coal, 16" and 64 coal arrive on the channel while a chest on a lower priority also receives
- **THEN** the furnace holds 16 coal and the other 48 go to the chest

#### Scenario: Refill as it burns
- **WHEN** the furnace burns 3 of its 16 coal
- **THEN** it receives 3 coal again

#### Scenario: Fill a boiler to a level
- **WHEN** a boiler's side has only Out with the entry "water, 4,000 mB" and a pump supplies water on the channel
- **THEN** the boiler holds at most 4,000 mB of water from the duct

### Requirement: Editing amounts
A filter slot with an amount SHALL show it in its corner, fluids in mB shortened above 1,000. Right clicking a set filter slot SHALL open an amount popup with a number field, step buttons and Clear, labelled "Keep" on a giving side, "Up to" on a receiving side and "Keep / up to" on a side that does both (an item side with Pull and Out, a fluid side with In and Out). Saving an empty field or 0 SHALL remove the amount. The slot's tooltip SHALL name the amount and its meaning.

#### Scenario: Set a keep amount
- **WHEN** the item side has In and Pull, the player right clicks the sapling slot, types 64 and saves
- **THEN** the slot shows 64 and its tooltip reads "Keep 64"

#### Scenario: Fluid amount
- **WHEN** the player sets 8000 on a water slot of a fluid side with only In
- **THEN** the slot shows "8k" and its tooltip reads "Keep 8,000 mB"

#### Scenario: Remove an amount
- **WHEN** the player opens the popup on a slot with amount 64 and presses Clear
- **THEN** the slot shows no amount and still lists its item

### Requirement: Keep logic is pure
How much may be given above a keep amount and how much may be received below a fill up to amount, given the held amount and the offered amount, SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Allowance tests
- **WHEN** the unit tests compute allowances
- **THEN** holding 300 with keep 64 allows 236 of an offer of 1,000; holding 40 with keep 64 allows 0; holding 13 with up to 16 accepts 3 of an offer of 64; no amount allows the whole offer
