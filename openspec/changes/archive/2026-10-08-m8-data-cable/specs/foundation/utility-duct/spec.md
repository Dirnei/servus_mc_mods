# Spec Delta

## MODIFIED Requirements

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

### Requirement: Separate channel networks
Ducts SHALL connect per line: an item line network only spans ducts that carry the Item Pipe, an energy line network only ducts that carry the Power Cable, a fluid line network only ducts that carry the Fluid Pipe, and a data network only ducts that carry the data line.

#### Scenario: Gap in a channel
- **WHEN** three ducts in a row carry items but only the outer two carry energy
- **THEN** items flow end to end and energy does not

#### Scenario: Gap in the fluid line
- **WHEN** three ducts in a row carry the Item Pipe but only the outer two the Fluid Pipe
- **THEN** items flow end to end and fluid does not

### Requirement: Crafting
Item Pipes SHALL be crafted from iron ingots, glass and a chest; Power Cables from copper ingots, redstone and glass; Fluid Pipes from iron ingots, glass and a bucket; eight each. Data Cables SHALL be crafted from two copper ingots and a redstone dust, four at a time.

#### Scenario: Recipes
- **WHEN** the player crafts any of the three pipe and cable patterns
- **THEN** eight of the line item are produced

#### Scenario: Data Cable recipe
- **WHEN** the player crafts a copper ingot, a redstone dust and a copper ingot in a row
- **THEN** four Data Cables are produced

### Requirement: Duct GUI
Using the Configuration Carrot, not sneaking, on an arm of a duct that touches an inventory or machine SHALL open the endpoint GUI for that side. It SHALL show the side and the touched block's name, one tab per present line on its left edge showing the line's item, and the page of the selected tab: the line's mode button, the eight channel cells with the current channel framed, and a remove button; the Item Pipe page also the filter slots and the priority with minus and plus buttons; the data line page only an on and off button and the remove button. The player inventory SHALL be shown under every page. Filter slots SHALL be hidden and inert on every page but the Item Pipe page. Using the carrot on the centre or on an arm toward another duct SHALL open nothing. Bare hand right click SHALL open nothing.

#### Scenario: Configure one side
- **WHEN** the player uses the carrot on the west arm that touches a chest, sets the item mode to pull in and places cobblestone in a filter slot
- **THEN** the duct pulls only cobblestone from the west

#### Scenario: Two lines, two tabs
- **WHEN** the player opens the GUI of a duct with an Item Pipe and a Power Cable
- **THEN** two tabs show, the Item Pipe page is selected, and clicking the Power Cable tab shows its mode, channels and remove button without filter slots

#### Scenario: Fluid Pipe tab
- **WHEN** the player opens the GUI of an arm whose duct carries all three lines and that touches a tank
- **THEN** three tabs show, and the Fluid Pipe page shows its mode, channels and remove button without filter slots or priority

#### Scenario: Data tab
- **WHEN** the player opens the GUI of an arm toward a Warehouse Controller on a duct with the data line
- **THEN** the data page shows an on and off button and the remove button, without channels, filter slots or priority

#### Scenario: Duct to duct arm
- **WHEN** the player uses the carrot on an arm that leads to another duct
- **THEN** no GUI opens

#### Scenario: Bare hand
- **WHEN** the player right clicks a duct without the data line with an empty hand
- **THEN** no GUI opens and the click passes through
