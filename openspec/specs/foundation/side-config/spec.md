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
A side configuration SHALL consist of an Items layer and, for blocks that hold fluid tanks, a Fluids layer. The Items layer SHALL keep the side modes of the requirements above. Each layer SHALL be stored, synced, kept on dismantling and cycled independently of the other.

#### Scenario: Layers are independent
- **WHEN** the player sets the left side of a Calciner's Fluids layer to Salt
- **THEN** the left side's Items mode stays what it was

#### Scenario: Kept on dismantle
- **WHEN** a Calciner with a configured Fluids layer is dismantled with the sneak carrot and placed again
- **THEN** both layers come back as they were

### Requirement: Fluids layer assigns tanks to sides
In the Fluids layer each side SHALL be Off, All, or one of the block's declared tanks. A side set to a tank SHALL expose only that tank through the fluid capability, in the direction the block declares for it (fill for an input tank, drain for an output tank). A side set to All SHALL expose every tank as the block did before layers existed. A side set to Off SHALL expose no fluid capability. New sides SHALL default to All, and fixed sides SHALL stay Off.

#### Scenario: One principle per side
- **WHEN** the Calciner's left side is set to Salt and its right side to Mercury
- **THEN** a pipe on the left drains only Salt and a pipe on the right drains only Mercury

#### Scenario: Default behaves as before
- **WHEN** a Calciner is placed and nothing is configured
- **THEN** every side but the front offers all three principles

### Requirement: Output tanks push on their sides
A block SHALL push the contents of an output tank into a touching fluid handler on every side set to that tank or to All, at the block's push interval. A block SHALL never push into a side set to an input tank or to Off.

#### Scenario: Tank beside a Salt side
- **WHEN** a Fluid Tank touches the Calciner's side set to Salt and the Calciner makes Salt and Mercury
- **THEN** only Salt flows into the tank

### Requirement: Pure layer model
The layer types, the tank assignment, cycling and their byte encoding SHALL have no Minecraft dependency and SHALL be covered by JUnit tests.

#### Scenario: Cycle through tanks
- **WHEN** a Fluids layer side of a block with tanks Salt, Sulfur and Mercury is cycled from Off
- **THEN** it moves to All, Salt, Sulfur, Mercury and back to Off

### Requirement: Side panel tabs
The side panel SHALL show one tab per layer the block has, above its 3 by 3 grid, and SHALL show the grid of the selected tab. Fluids layer cells SHALL be coloured with the assigned tank's fluid colour, with distinct colours for All and Off, and their tooltip SHALL name the side and the assignment. A click on a cell SHALL cycle that side in the selected layer. A block with only the Items layer SHALL show no tabs.

#### Scenario: Switch to fluids
- **WHEN** the player clicks the Fluids tab of a Calciner GUI and then the left cell
- **THEN** the left side's Fluids assignment cycles and the tooltip reads "Left: Salt" or the next assignment

### Requirement: Carrot layer
The Configuration Carrot SHALL carry a layer, Items by default. Using the carrot on the air SHALL switch it to the next layer and tell the player the new layer in the action bar. Using the carrot on a block face SHALL cycle that face in the carrot's layer; on a block without that layer it SHALL cycle the Items layer. The carrot's tooltip SHALL name its layer.

#### Scenario: Configure a fluid side by hand
- **WHEN** the player right clicks the air with the carrot, then right clicks the Calciner's top face
- **THEN** the action bar first shows "Layer: Fluids", then "Top: Salt" or the next assignment

### Requirement: Markers show the carrot's layer
While the player holds the carrot, side markers SHALL draw the layer the carrot is set to; Fluids layer frames SHALL use the assigned tank's fluid colour. While the player only wears the goggles, markers SHALL draw the Items layer.

#### Scenario: Fluid markers
- **WHEN** the player holds a carrot set to Fluids and looks at a configured Calciner
- **THEN** the Salt side shows a frame in the Salt colour and the Off sides show none
