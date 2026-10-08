# foundation/utility-duct Specification

## Purpose

The Utility Duct is Foundation's one pipe: channels for items and energy in the same block, configured per side.

## Requirements

### Requirement: Channels are placed as items
An Item Pipe, Power Cable or Fluid Pipe item SHALL place a Utility Duct carrying that line, or add the line to a clicked duct that lacks it. Clicking a duct that already carries the line SHALL place a new duct against the clicked face like any block. Sneaking SHALL use the placement assist with the run axis: the run and its connections are the ducts and arms of the held line, and otherwise the direction is the clicked face. When the spot beside the clicked duct is free, sneaking places there as before. A duct SHALL drop its line items when broken or dismantled, and removing the last line SHALL remove the duct.

#### Scenario: Add a second channel
- **WHEN** a player uses a Power Cable on a duct that carries only an Item Pipe
- **THEN** the duct carries both and one Power Cable is consumed

#### Scenario: Add the fluid line
- **WHEN** a player uses a Fluid Pipe on a duct that carries an Item Pipe and a Power Cable
- **THEN** the duct carries all three lines and one Fluid Pipe is consumed

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
- **WHEN** a command refers to `servus_logistics:item_channel` or `servus_logistics:energy_channel`
- **THEN** no such item exists; the items are `servus_logistics:item_pipe`, `servus_logistics:power_cable` and `servus_logistics:fluid_pipe`

### Requirement: Per side modes per channel
Each line of a duct SHALL have one of the modes in and out, pull in, push out or disabled per side, default in and out, set through the endpoint GUI's mode button for that line and kept with the block. A disabled side SHALL not connect visually or logically.

#### Scenario: Set a mode
- **WHEN** the player sets the item line's west side to pull in
- **THEN** the duct keeps that mode after reload and the GUI shows it

### Requirement: Item channel behaviour
Items pushed into a duct on a side whose item mode allows input SHALL be routed round robin to inventories on sides whose item mode allows output and whose item channel equals the inserting side's channel, never back to the inserting inventory, within the per tick budget. Every `pull_interval` ticks each pull in side SHALL extract up to `items_per_pull` items from its inventory and route them on its own channel.

#### Scenario: Pull from a chest into another
- **WHEN** chest A touches a duct's pull in side, chest B touches a push out side on the same channel and chest A holds 20 cobblestone
- **THEN** within a few seconds chest B holds the 20 cobblestone

#### Scenario: Disabled side
- **WHEN** chest B's side is disabled
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
Ducts SHALL connect per line: an item line network only spans ducts that carry the Item Pipe, an energy line network only ducts that carry the Power Cable, a fluid line network only ducts that carry the Fluid Pipe.

#### Scenario: Gap in a channel
- **WHEN** three ducts in a row carry items but only the outer two carry energy
- **THEN** items flow end to end and energy does not

#### Scenario: Gap in the fluid line
- **WHEN** three ducts in a row carry the Item Pipe but only the outer two the Fluid Pipe
- **THEN** items flow end to end and fluid does not

### Requirement: Crafting
Item Pipes SHALL be crafted from iron ingots, glass and a chest; Power Cables from copper ingots, redstone and glass; Fluid Pipes from iron ingots, glass and a bucket; eight each.

#### Scenario: Recipes
- **WHEN** the player crafts any of the three patterns
- **THEN** eight of the line item are produced

### Requirement: Per side item filters
Each side of a duct's item line SHALL have a whitelist of up to nine ghost items, set in the endpoint GUI from the cursor or by shift clicking an inventory item, cleared by clicking with an empty cursor. An empty list passes everything. Items pulled in from a side and items pushed out to a side SHALL match that side's list.

#### Scenario: Sorting pull
- **WHEN** a chest holding cobblestone and dirt touches a pull in side whose filter lists cobblestone
- **THEN** only the cobblestone leaves the chest

#### Scenario: Sorting delivery
- **WHEN** chest A's side lists iron ingots and chest B's side has no filter and iron and copper ingots arrive
- **THEN** chest A receives only iron ingots and chest B receives the copper ingots

### Requirement: Per side priorities
Each side of a duct's item line SHALL have a priority from -9 to 9, default 0, set in the endpoint GUI. Routing SHALL offer an item to all outputs of the highest priority on its channel first, round robin among them, and only move to lower priorities when none of them accepts.

#### Scenario: Preferred output
- **WHEN** two chests touch push out sides with priorities 5 and 0 and 20 items arrive
- **THEN** all 20 land in the priority 5 chest

#### Scenario: Overflow
- **WHEN** the priority 5 chest is full
- **THEN** further items go to the priority 0 chest

### Requirement: Duct GUI
Using the Configuration Carrot, not sneaking, on an arm of a duct that touches an inventory or machine SHALL open the endpoint GUI for that side. It SHALL show the side and the touched block's name, one tab per present line on its left edge showing the line's item, and the page of the selected tab: the line's mode button, the eight channel cells with the current channel framed, and a remove button; the Item Pipe page also the filter slots and the priority with minus and plus buttons. The player inventory SHALL be shown under every page. Filter slots SHALL be hidden and inert on the Power Cable and Fluid Pipe pages. Using the carrot on the centre or on an arm toward another duct SHALL open nothing. Bare hand right click SHALL open nothing.

#### Scenario: Configure one side
- **WHEN** the player uses the carrot on the west arm that touches a chest, sets the item mode to pull in and places cobblestone in a filter slot
- **THEN** the duct pulls only cobblestone from the west

#### Scenario: Two lines, two tabs
- **WHEN** the player opens the GUI of a duct with an Item Pipe and a Power Cable
- **THEN** two tabs show, the Item Pipe page is selected, and clicking the Power Cable tab shows its mode, channels and remove button without filter slots

#### Scenario: Fluid Pipe tab
- **WHEN** the player opens the GUI of an arm whose duct carries all three lines and that touches a tank
- **THEN** three tabs show, and the Fluid Pipe page shows its mode, channels and remove button without filter slots or priority

#### Scenario: Duct to duct arm
- **WHEN** the player uses the carrot on an arm that leads to another duct
- **THEN** no GUI opens

#### Scenario: Bare hand
- **WHEN** the player right clicks a duct with an empty hand
- **THEN** no GUI opens and the click passes through

### Requirement: Eight channels per line per side
Each side of each line SHALL be on one of eight channels, default channel 1, chosen in the endpoint GUI and kept with the block. Each channel has a fixed color shared by every duct. Channels of the item, energy and fluid lines on the same side are independent.

#### Scenario: Select a channel
- **WHEN** the player opens the endpoint GUI of the east side and picks channel 3 for the item line
- **THEN** the item line's east side stays on channel 3 after reload and the energy and fluid lines' east sides stay on channel 1

#### Scenario: Channel colors
- **WHEN** the player looks at the channel selector
- **THEN** eight buttons appear, each in its channel color, with the current channel marked

### Requirement: Fluid line behaviour
Every tick a fluid line network SHALL, per channel, drain fluid from source blocks and fill it into sink blocks on the same channel. The pipe SHALL have no rate limit of its own: as much moves as the sources give and the sinks accept in that tick, sharing short supply fairly among the sinks. A pull in side SHALL make its block a source and a push out side its block a sink. Any fluid SHALL be moved, and a sink SHALL only receive fluid it accepts.

#### Scenario: Pump to tank
- **WHEN** a pump touches a duct's pull in side, a tank touches a push out side of a duct further along the run, both on channel 1, and the pump holds water
- **THEN** the tank's water rises every tick while the pump's buffer falls by the same amount

#### Scenario: No pipe limit
- **WHEN** a full 64,000 mB tank is a source and an empty tank of the same size a sink on channel 1
- **THEN** the whole content moves in one tick, as far as both tanks allow

#### Scenario: Two sinks share
- **WHEN** one source on channel 1 can give 100 mB in a tick and two empty tanks on channel 1 accept fluid
- **THEN** each tank receives 50 mB that tick

#### Scenario: Different channel
- **WHEN** the pump's side is on channel 1 and the tank's side on channel 2
- **THEN** no fluid moves

#### Scenario: Mixed fluids
- **WHEN** a tank of water and a tank of lava are sources on channel 1 and an empty tank and a water tank are sinks on channel 1
- **THEN** the water tank only receives water, and the empty tank receives one fluid and from then on only that fluid

### Requirement: In and out sides for fluids
On a side whose fluid mode is in and out, a block that accepts the fluid offered on its channel SHALL be a sink, and a block that gives fluid but accepts none SHALL be a source. So a pump drains into the pipe and a tank fills from it with the default mode, and fluid never runs back and forth between two tanks.

#### Scenario: Default settings
- **WHEN** a pump and a tank touch the two ends of a fluid duct run with every side left on in and out
- **THEN** water flows from the pump into the tank

#### Scenario: Emptying a tank
- **WHEN** tank A touches a pull in side and tank B an in and out side on the same channel
- **THEN** fluid flows from tank A to tank B and never back

#### Scenario: Two tanks on default sides
- **WHEN** a full tank A and an empty tank B of the same size touch in and out sides on the same channel
- **THEN** A's content moves into B and stays there; B keeps acting as a sink until the side's configuration changes

### Requirement: Fluid pushed into a duct
A duct carrying the Fluid Pipe SHALL accept fluid filled into it on a side whose fluid mode allows input and SHALL pass it straight on to sinks on that side's channel, keeping nothing itself. With no sink that accepts the fluid it SHALL accept nothing.

#### Scenario: Foreign pump pushes
- **WHEN** another mod's pump fills water into a duct side on channel 1 and a tank on channel 1 has room
- **THEN** the tank receives the water

#### Scenario: Nowhere to go
- **WHEN** every sink on that channel is full
- **THEN** the duct accepts no fluid
