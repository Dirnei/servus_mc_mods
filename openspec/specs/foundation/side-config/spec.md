# foundation/side-config Specification

## Purpose

One vocabulary for what a block side does, relative to the block's facing, so machines, ducts and future blocks configure sides the same way.

## Requirements

### Requirement: Side modes
A side SHALL have exactly one mode out of Auto, Input, Output, Both, Pull, Push and Off. Input allows insertion, Output allows extraction, Both allows insertion and extraction, Pull allows insertion and makes the block take from the neighbour, Push allows extraction and makes the block give to the neighbour, Auto allows insertion and extraction and lets the block push, Off allows nothing. Cycling moves through the modes the block supports in its declared order.

#### Scenario: Cycle
- **WHEN** a side in Auto is cycled five times on a block that supports Auto, Input, Output, Both, Off
- **THEN** it passes Input, Output, Both, Off and is Auto again

#### Scenario: Permissions
- **WHEN** a side is Output
- **THEN** it allows extraction and refuses insertion, and the block does not push through it

#### Scenario: Active modes
- **WHEN** a side is Push
- **THEN** it allows extraction, refuses insertion and the block pushes through it; a Pull side allows insertion, refuses extraction and the block pulls through it

### Requirement: Relative sides follow the facing
Sides SHALL be addressed as front, back, left, right, top and bottom relative to the block's facing, so rotating a block keeps its configuration. Left and right SHALL be the left and right of a player who looks at the block's front.

#### Scenario: Facing north
- **WHEN** a block faces north
- **THEN** front is north, back is south, left is east, right is west, top is up, bottom is down

#### Scenario: Facing east
- **WHEN** a block faces east
- **THEN** front is east, back is west, left is south, right is north

### Requirement: Default is Auto
A new side configuration SHALL have every side in the block's declared default, which is Auto for blocks that support it, so blocks behave as before configuration.

#### Scenario: Fresh config
- **WHEN** a side configuration with the default Auto is created
- **THEN** all six sides are Auto and allow both insertion and extraction

### Requirement: Pure Java
The side configuration types SHALL have no Minecraft dependency.

#### Scenario: Unit tests
- **WHEN** the side config tests run under `gradlew :lib:test`
- **THEN** modes, cycling and relative side mapping are verified without the game

### Requirement: Supported modes per block
A side configuration SHALL be created with the list of modes the block supports and the block's default mode, and MAY set single sides to another supported mode at creation. Cycling a side SHALL move to the next supported mode and wrap around; setting an unsupported mode SHALL fall back to the default.

#### Scenario: Station cycle
- **WHEN** a configuration supports Input, Pull and Off with the default Input and a side is cycled three times
- **THEN** it passes Pull, Off and is Input again

#### Scenario: Unsupported mode
- **WHEN** a configuration that supports Push, Output and Off is set to Both on one side
- **THEN** that side is Off

### Requirement: Side panel widget
Lib SHALL provide a client side panel that draws the six relative sides as a 3 by 3 grid left of a container GUI, with a colour per mode, the side's first letter, a tooltip naming side and mode, and a click that sends the side's menu button so the server cycles it. Every block with a side configuration SHALL use this panel and the same colours, so sides look the same on machines and stations.

#### Scenario: Same look
- **WHEN** the player opens an Assembler and then an Inbound Station
- **THEN** both show the same panel left of the GUI, and a Pull side is the same colour on both

#### Scenario: Click cycles
- **WHEN** the player clicks the top cell of an Inbound Station panel
- **THEN** the top side goes from Input to Pull and the cell changes colour at once

### Requirement: Carrot cycles sides
The Configuration Carrot used on a face of a block with a side configuration SHALL cycle that face's mode and tell the player the side and the new mode in the action bar. Sneaking with the carrot SHALL dismantle the block and keep its configuration in the dropped item. The carrot SHALL NOT rotate such blocks.

#### Scenario: Carrot on a machine
- **WHEN** the player uses the carrot on the left face of a Burner Smelter in Auto
- **THEN** the left side is Input and the action bar reads "Left: Input"

#### Scenario: Dismantle keeps sides
- **WHEN** the player sneaks and uses the carrot on a station with the top side set to Pull and places it again
- **THEN** the top side is still Pull

### Requirement: Side modes shown on the block
While the viewing player holds the Configuration Carrot or wears the goggles, every block with a side configuration SHALL draw on each face a thin frame along its edges in the colour the side panel uses for that side's mode, and a centred glyph: a square for Input and Pull, a diamond for Output and Push, a square with a dot for Both, a cross for Off, with Pull and Push filled and Input and Output outlined. Without the carrot or the goggles no marker SHALL be drawn. Markers SHALL render at full brightness and SHALL cost nothing while nobody configures.

#### Scenario: Configured side from afar
- **WHEN** the top of an Inbound Station is set to Pull and the player looks at it with empty hands
- **THEN** no marker is drawn on any face

#### Scenario: Carrot in hand
- **WHEN** the player holds the carrot and looks at a machine in Auto
- **THEN** every face shows the Auto frame and the Auto glyph, and after setting the left side to Push its frame and glyph change at once

#### Scenario: Plain block
- **WHEN** a machine has every side in Auto and the player holds nothing special
- **THEN** no marker is drawn

### Requirement: Fixed sides
A block MAY fix single relative sides to one mode. A fixed side SHALL keep that mode through cycling, setting, the carrot and loading, SHALL count as default, SHALL be drawn greyed in the side panel, SHALL NOT react to clicks there, and SHALL show no marker on the block even while the carrot or the goggles are in use. A side fixed to Off SHALL offer no item capability, so pipes, hoppers and other mods do not connect to it; a side a player sets to Off SHALL keep its capability.

#### Scenario: Cycling a fixed side
- **WHEN** a machine's front is fixed to Off and the player clicks its cell or uses the carrot on the front
- **THEN** the front stays Off and the cell stays greyed

#### Scenario: Loading
- **WHEN** a saved configuration holds Input for a side the block fixes to Off
- **THEN** the side is Off after loading

#### Scenario: No marker on a fixed side
- **WHEN** the player holds the carrot and looks at the front of a machine
- **THEN** the front shows no frame and no glyph while the other faces show theirs

#### Scenario: No pipe at a fixed front
- **WHEN** an Item Pipe is placed against the front of a machine whose front is fixed to Off
- **THEN** the pipe draws no arm to the front and finds no item handler there

#### Scenario: A side set to Off stays connected
- **WHEN** a player sets a machine's side to Off and an Item Pipe touches that side
- **THEN** the pipe still connects, moves nothing while the side is Off, and works again once the side is set back

### Requirement: Side configuration has layers
A side configuration SHALL consist of an Items layer, a Fluids layer, or both: an Items layer for blocks that hold items and a Fluids layer for blocks that hold fluid tanks. The Items layer SHALL keep the side modes of the requirements above. Each layer SHALL be stored, synced, kept on dismantling and cycled independently of the other.

#### Scenario: Layers are independent
- **WHEN** the player sets the left side of a block's Fluids layer to Push on tank 1
- **THEN** the left side's Items mode stays what it was

#### Scenario: Kept on dismantle
- **WHEN** a block with a configured Fluids layer is dismantled with the sneak carrot and placed again
- **THEN** every Fluids side comes back with its mode and its tank

#### Scenario: Fluids only
- **WHEN** a block holds only fluid tanks
- **THEN** it has a Fluids layer and no Items layer

### Requirement: Fluids layer assigns tanks to sides
In the Fluids layer each side SHALL have a mode (see Fluids layer modes) and a tank choice, All or one of the block's tanks (see Tank choice per side). A side SHALL expose through the fluid capability only the tanks its choice reaches, filling and draining as its mode and the tanks' directions allow. A side on Off SHALL offer a fluid handler that moves nothing, so pipes stay connected. New sides SHALL start at the block's default fluid mode on All, and fixed sides SHALL stay Off with no fluid capability.

#### Scenario: One principle per side
- **WHEN** a block's left side is Output on tank 1 holding Salt and its right side is Output on tank 2 holding Mercury
- **THEN** a pipe on the left drains only Salt and a pipe on the right drains only Mercury

#### Scenario: Default behaves as before
- **WHEN** a block declaring Input as its default fluid mode is placed and nothing is configured
- **THEN** every side but the fixed ones fills any of its tanks and drains none

### Requirement: Output tanks push on their sides
A block SHALL, at its push interval, push from the tanks a Push or Auto side reaches into the touching fluid handler on that side, and SHALL pull into the tanks a Pull side reaches from the touching fluid handler on that side, as far as the tanks take it. A block SHALL never push through a side in another mode, never pull through a side that is not Pull, and SHALL lose no fluid between the neighbour and its tanks.

#### Scenario: Tank beside a Salt side
- **WHEN** a Fluid Tank touches a block's side set to Push on tank 2 and the block holds Mercury in tank 1 and Salt in tank 2
- **THEN** only Salt flows into the Fluid Tank

#### Scenario: Pull fills only what fits
- **WHEN** a Pull side touches a Fluid Tank with 10,000 mB of water and the reached tank has room for 3,000 mB
- **THEN** the block takes 3,000 mB and the Fluid Tank keeps 7,000 mB

### Requirement: Pure layer model
The layer types, the mode and tank choice per side, cycling, the tank pick for a fill or a drain, and their byte encoding SHALL have no Minecraft dependency and SHALL be covered by JUnit tests.

#### Scenario: Cycle through tanks
- **WHEN** the tank choice of a Fluids side of a block with three tanks is cycled from All
- **THEN** it moves to tank 1, tank 2, tank 3 and back to All, and the side's mode stays what it was

#### Scenario: Cycle through modes
- **WHEN** a Fluids side of a block supporting Input, Pull and Off is cycled from Input
- **THEN** it moves to Pull, Off and back to Input, and the tank choice stays what it was

### Requirement: Side panel tabs
The side panel SHALL show one tab per layer when the block has both layers, above its 3 by 3 grid, and SHALL show the grid of the selected tab. A block with one layer SHALL show that layer's grid and no tabs. A Fluids cell SHALL be drawn in its mode's colour, with a small chip in its corner showing the tank choice: the letter A for All, otherwise the tank's label colour. Its tooltip SHALL name the side, the mode and the tank. A left click on a cell SHALL cycle that side's mode in the shown layer; a right click on a Fluids cell SHALL cycle its tank choice.

#### Scenario: Switch to fluids
- **WHEN** the player clicks the Fluids tab of a GUI of a block with both layers and then left clicks the left cell
- **THEN** the left side's Fluids mode cycles and the tooltip reads "Left: Pull, All" or the next mode

#### Scenario: Right click picks the tank
- **WHEN** the player right clicks the back cell of a block with only a Fluids layer whose back is Push on All, and tank 1 holds water
- **THEN** the back cell keeps the Push colour, its chip turns to the water colour and the tooltip reads "Back: Push, Water"

### Requirement: Carrot layer
The Configuration Carrot SHALL carry a layer, Items by default. Using the carrot on the air SHALL switch it to the next layer and tell the player the new layer in the action bar. Using the carrot on a block face SHALL cycle that face's mode in the carrot's layer and tell the player the side, the mode and, for the Fluids layer, the tank; on a block without that layer it SHALL cycle the layer the block has. The carrot SHALL NOT change a tank choice. The carrot's tooltip SHALL name its layer.

#### Scenario: Configure a fluid side by hand
- **WHEN** the player right clicks the air with the carrot, then right clicks the top face of a block with both layers whose top is Input on tank 1 holding water
- **THEN** the action bar first shows "Layer: Fluids", then "Top: Pull, Water"

#### Scenario: Fluids only block with an Items carrot
- **WHEN** the player uses a carrot set to Items on a face of a block with only a Fluids layer
- **THEN** that face's Fluids mode cycles

### Requirement: Markers show the carrot's layer
While the player holds the carrot, side markers SHALL draw the layer the carrot is set to, or the block's only layer. Fluids markers SHALL use the glyph of the side's mode and a frame in the colour of the side's tank label, or the All colour on All. Fixed sides SHALL show no marker. While the player only wears the goggles, markers SHALL draw the Items layer, or the Fluids layer on a block without an Items layer.

#### Scenario: Fluid markers
- **WHEN** the player holds a carrot set to Fluids and looks at a block whose back is Push on a tank holding lava
- **THEN** the back shows a lava coloured frame with the filled diamond of Push

#### Scenario: Goggles on a fluids only block
- **WHEN** the player wears the goggles and looks at a block with only a Fluids layer whose left side is Pull
- **THEN** the left face shows the Pull glyph

### Requirement: Fluids layer modes
Each side of the Fluids layer SHALL have a mode out of the modes the block supports for fluids, with the meaning the Side modes requirement gives them for items: Input fills, Output drains, Both does both, Pull fills and makes the block take from the neighbour, Push drains and makes the block give to the neighbour, Auto fills, drains and pushes, Off does nothing. Each block SHALL declare its fluid modes and its default fluid mode.

#### Scenario: Pull side takes from a pipe tank
- **WHEN** a block's left Fluids side is Pull on All and a Fluid Tank holding water touches that side
- **THEN** the block takes water from the tank at its push interval, and a pipe on that side can fill it but not drain it

#### Scenario: Output side waits for the neighbour
- **WHEN** a block's back Fluids side is Output and a duct on pull touches it
- **THEN** the duct drains the block, and the block itself pushes nothing through that side

#### Scenario: Off side stays connected
- **WHEN** a player sets a Fluids side to Off and a duct with a fluid line touches it
- **THEN** the duct still connects, moves nothing while the side is Off, and works again once the side is set back

### Requirement: Tank choice per side
Besides its mode, each Fluids side SHALL reach either All tanks or exactly one of the block's tanks. A side on one tank SHALL fill, drain, pull and push only that tank. A side on All SHALL reach every tank. A fixed Fluids side SHALL be Off and SHALL offer no fluid capability.

#### Scenario: One tank per side
- **WHEN** a block with a water tank and a lava tank has its back on Push with the water tank and its left on Push with the lava tank, and a Fluid Tank touches each side
- **THEN** only water flows out of the back and only lava out of the left

#### Scenario: Fixed front
- **WHEN** a block fixes its front Fluids side and a duct with a fluid line is placed against the front
- **THEN** the duct finds no fluid endpoint there

### Requirement: No tank choice with one tank
A block with only one tank SHALL offer no tank choice: its Fluids sides SHALL reach that tank, the side panel SHALL show no tank chip on its cells, a right click on a Fluids cell SHALL do nothing, and the tooltip and the carrot SHALL name the side and the mode only.

#### Scenario: One tank, no choice
- **WHEN** a player opens the GUI of a block with one tank whose back is Push and right clicks the back cell
- **THEN** the back cell shows no tank chip, nothing changes, and the tooltip reads "Back: Push"

### Requirement: Tank direction limits a side
A block MAY declare a tank fill only or drain only. A side SHALL then allow on that tank only what both its mode and the tank allow: a side on Both reaching a drain only tank SHALL drain it and refuse to fill it, and a Push side SHALL never push from a fill only tank.

#### Scenario: Push skips an input tank
- **WHEN** a side is Push on All and the block has a fill only tank holding water and a drain only tank holding lava
- **THEN** only lava is pushed through that side

### Requirement: Fill and drain order on All
A side on All SHALL fill a tank that already holds the offered fluid before an empty tank, tanks of the same rank in tank order, and SHALL never put a second fluid into a tank. A side on All SHALL drain and push from the tanks in tank order, skipping empty tanks, and one push SHALL move fluid of every tank it reaches when the neighbour takes it.

#### Scenario: Holding tank first
- **WHEN** tank 1 is empty, tank 2 holds water, and water arrives through a side on All
- **THEN** the water goes into tank 2 until it is full and only then into tank 1

#### Scenario: Push from two tanks
- **WHEN** a side on Push and All touches a duct that takes any fluid, and the block holds water in tank 1 and lava in tank 2
- **THEN** both water and lava leave through that side

### Requirement: Tank labels follow the tank
Each tank SHALL have a name and a colour that the side panel, its tooltips, the action bar and the markers use. A block MAY give a tank a fixed label. Otherwise the label SHALL be the fluid the tank holds, else the fluid the block sets it to keep or take, else "Tank" with its number in a neutral colour. A label SHALL update on the client when the fluid behind it changes.

#### Scenario: Label follows content
- **WHEN** an empty, unset tank 2 of a block is filled with lava while its GUI is open
- **THEN** the tank 2 chip in the side panel turns from the neutral colour to the lava colour and the tooltip names Lava
