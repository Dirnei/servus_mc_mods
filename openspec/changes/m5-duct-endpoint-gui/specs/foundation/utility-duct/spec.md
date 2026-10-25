## MODIFIED Requirements

### Requirement: Per side modes per channel
Each line of a duct SHALL have one of the modes in and out, pull in, push out or disabled per side, default in and out, set through the endpoint GUI's mode button for that line and kept with the block. A disabled side SHALL not connect visually or logically, and SHALL stay reachable in the endpoint GUI through the side picker so it can be set to another mode again.

#### Scenario: Set a mode
- **WHEN** the player sets the item line's west side to pull in
- **THEN** the duct keeps that mode after reload and the GUI shows it

#### Scenario: Enable a disabled side again
- **WHEN** the item line's west side toward a chest is disabled, so the duct shows no arm there, and the player uses the carrot on the duct's centre, picks West in the side picker and sets the mode to in and out
- **THEN** the arm toward the chest is back and items reach the chest again

### Requirement: Duct GUI
Using the Configuration Carrot, not sneaking, on any part of a duct (its centre or any arm) SHALL open the endpoint GUI. It SHALL open on the side of the clicked arm when that arm ends at a block other than a duct, and otherwise on the first selectable side in the order down, up, north, south, west, east. A duct without a selectable side SHALL open nothing.

The GUI SHALL show a side picker row of six buttons, one per side. Each button SHALL show the item of the block beside that side (the item the pick block key would give), and the side's name in its tooltip together with the block's name. A side whose neighbour is a block other than a duct SHALL be selectable, whatever the modes of its lines, also when every line on it is disabled. A side toward another duct SHALL show that duct greyed and SHALL NOT be selectable. A side with no block beside it SHALL show an empty frame and SHALL NOT be selectable. The selected side SHALL be highlighted, and picking another side SHALL switch the GUI to that side without closing it, keeping the selected line tab when the duct carries it.

Below the picker the GUI SHALL show the selected side's touched block name, one tab per present line on its left edge showing the line's item, and the page of the selected tab: the line's mode button, its Control button, a channel button showing the side's channel with its colour, icon and name, and a remove button; the Item Pipe page also the filter slots and the priority with minus and plus buttons; the data line page only an on and off button and the remove button. The player inventory SHALL be shown under every page. Filter slots SHALL be hidden and inert on every page but the Item Pipe page. Clicking a filter slot with an empty cursor SHALL open a search popup over the GUI that lists every item with its icon and name, filtered by the typed text, with a "none" entry first; picking an entry SHALL set the slot, "none" SHALL clear it. Bare hand right click SHALL open nothing.

#### Scenario: Configure one side
- **WHEN** the player uses the carrot on the west arm that touches a chest, sets the item mode to pull in and places cobblestone in a filter slot
- **THEN** the duct pulls only cobblestone from the west

#### Scenario: Two lines, two tabs
- **WHEN** the player opens the GUI of a duct with an Item Pipe and a Power Cable
- **THEN** two tabs show, the Item Pipe page is selected, and clicking the Power Cable tab shows its mode, Control button, channel button and remove button without filter slots

#### Scenario: Fluid Pipe tab
- **WHEN** the player opens the GUI of an arm whose duct carries all three lines and that touches a tank
- **THEN** three tabs show, and the Fluid Pipe page shows its mode, Control button, channel button and remove button without filter slots or priority

#### Scenario: Data tab
- **WHEN** the player opens the GUI of an arm toward a Warehouse Controller on a duct with the data line
- **THEN** the data page shows an on and off button and the remove button, without Control button, channel button, filter slots or priority

#### Scenario: Open from the centre
- **WHEN** a duct has a furnace above it and a chest to the north and the player uses the carrot on its centre
- **THEN** the GUI opens on the up side, showing the furnace

#### Scenario: Side picker shows the neighbours
- **WHEN** a duct has a chest to the north, a furnace above, another duct to the east and nothing on the other sides, and the player opens its GUI
- **THEN** the north button shows a chest, the up button a furnace, the east button a greyed duct and the other buttons empty frames; only north and up can be picked

#### Scenario: Switch sides
- **WHEN** the GUI is open on the north side with the Item Pipe tab and the player picks up
- **THEN** the GUI shows the up side's Item Pipe page with the furnace's name and that side's filter, mode and channel

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

## ADDED Requirements

### Requirement: Control per line per side
Each item, power and fluid line of a duct SHALL have a Control per side: Always, While powered, While unpowered or Off, default Always, kept with the block and cycled in that order by the Control button of the line's page. A side SHALL be active when its Control is Always, While powered while the duct is powered, or While unpowered while the duct is not powered; Off SHALL never be active. The Controls of the lines on one side SHALL be independent. The data line SHALL have no Control.

#### Scenario: Kept on reload
- **WHEN** the fluid Control of the east side is While powered and the world is reloaded
- **THEN** the fluid Control of the east side is still While powered and the item and power Controls of that side are Always

### Requirement: Inactive sides move nothing
A side that is not active SHALL keep its arm, connection, mode, channel, filter and priority, and SHALL move nothing on that line: it SHALL not be pulled from, SHALL not be a source or a sink, SHALL not receive routed items, fluid or energy, and SHALL accept nothing filled or inserted into the duct through it.

#### Scenario: Switch a side off
- **WHEN** chest A touches a pull in side, chest B a push out side on the same channel, chest A holds 20 cobblestone, and the player sets the item Control of chest A's side to Off
- **THEN** chest A keeps its cobblestone, the arm toward chest A stays, and after setting the Control back to Always the cobblestone reaches chest B

#### Scenario: Lines are independent
- **WHEN** a machine's side carries the Item Pipe and the Power Cable, a lever beside the duct powers it, and the item Control is While unpowered while the power Control is Always
- **THEN** the machine receives energy and no items

#### Scenario: Off side accepts no pushed fluid
- **WHEN** a pump pushes water into a duct side whose fluid Control is Off and a tank on the same channel has room
- **THEN** the duct accepts no water through that side

### Requirement: Duct redstone
A duct SHALL count as powered while it receives a redstone signal from any neighbour, like a block that a lever or redstone dust powers. The powered state SHALL follow signal changes as they happen and SHALL NOT be found by checking blocks every tick. A duct SHALL not emit redstone.

#### Scenario: Lever on a duct
- **WHEN** chest A touches a pull in side whose item Control is While powered, chest B a push out side on the same channel, chest A holds cobblestone and a lever on the ground beside the duct is switched on
- **THEN** cobblestone moves to chest B, and once the lever is switched off no more cobblestone moves

#### Scenario: Comparator stops the input
- **WHEN** a comparator reads a full chest and powers the duct, and the item Control of the side feeding that chest is While unpowered
- **THEN** no items are delivered to that chest while it is full

### Requirement: Control logic is pure
Whether a Control is active for a powered state, the Control cycle order and its storage encoding SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Activity table
- **WHEN** the unit tests check each Control for powered and unpowered
- **THEN** Always is active in both, While powered only when powered, While unpowered only when unpowered, Off in neither, and the cycle runs Always, While powered, While unpowered, Off, Always
