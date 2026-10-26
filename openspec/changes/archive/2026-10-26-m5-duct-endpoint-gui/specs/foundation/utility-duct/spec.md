## MODIFIED Requirements

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

### Requirement: Duct GUI
Using the Configuration Carrot, not sneaking, on any part of a duct (its centre or any arm) SHALL open the endpoint GUI. It SHALL open on the side of the clicked arm when that arm ends at a block other than a duct, and otherwise on the first selectable side in the order down, up, north, south, west, east. A duct without a selectable side SHALL open nothing.

The GUI SHALL show side tabs on top of the page, one fixed place per side in the order down, up, north, south, west, east. A tab SHALL show the item of the block beside that side (the item the pick block key would give), and the side's name in its tooltip together with the block's name. A side whose neighbour is a block other than a duct SHALL have a selectable tab, whatever the flags of its lines, also when no line on it connects. A side toward another duct SHALL show a greyed tab that SHALL NOT be selectable. A side with no block beside it SHALL show no tab, leaving its place free. The selected tab SHALL join the page, and picking another tab SHALL switch the GUI to that side without closing it, keeping the selected line tab when the duct carries it.

The page SHALL show the selected side's name and touched block name, one tab per present line on its left edge showing the line's item, and the page of the selected line. The item, power and fluid pages SHALL show a row of small icon buttons, each explained by its tooltip: In, Out and, on the Item Pipe page only, Pull as toggles that show whether they are on, the redstone setting as one button cycling Ignore redstone (gunpowder), While powered (lit redstone torch) and While unpowered (unlit redstone torch), and an On/Off button. Below the row a channel button SHALL show the side's channel with its colour, icon and name. The Item Pipe page SHALL also show the filter slots and the priority with minus and plus buttons. The data line page SHALL only show an on and off button. Every page SHALL show a small trash can button in its bottom right corner, apart from the settings, whose tooltip says that it takes the line out of the whole duct and drops it, and that On/Off pauses instead. The player inventory SHALL be shown under every page. Filter slots SHALL be hidden and inert on every page but the Item Pipe page. Clicking a filter slot with an empty cursor SHALL open a search popup over the GUI that lists every item with its icon and name, filtered by the typed text, with a "none" entry first; picking an entry SHALL set the slot, "none" SHALL clear it. Bare hand right click SHALL open nothing.

#### Scenario: Configure one side
- **WHEN** the player uses the carrot on the west arm that touches a chest, switches Out off and Pull on for the item line and places cobblestone in a filter slot
- **THEN** the duct pulls only cobblestone from the west

#### Scenario: Two lines, two tabs
- **WHEN** the player opens the GUI of a duct with an Item Pipe and a Power Cable
- **THEN** two line tabs show, the Item Pipe page is selected, and clicking the Power Cable tab shows In, Out, the redstone button, On/Off, the channel button and the trash can, without Pull or filter slots

#### Scenario: Fluid Pipe tab
- **WHEN** the player opens the GUI of an arm whose duct carries all three lines and that touches a tank
- **THEN** three line tabs show, and the Fluid Pipe page shows In, Out, the redstone button, On/Off, the channel button and the trash can, without Pull, filter slots or priority

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

## ADDED Requirements

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
