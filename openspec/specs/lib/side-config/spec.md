# lib/side-config Specification

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
Every block with a side configuration SHALL draw a thin frame along the edges of each face whose mode is not the block's default, in the same colour the side panel uses for that mode. While the viewing player holds the Configuration Carrot or wears the goggles, every face SHALL show its frame, default sides included, and a centred glyph: an arrow pointing in for Input and Pull, an arrow pointing out for Output and Push, a double arrow for Both, a cross for Off, with Pull and Push filled and Input and Output outlined. Markers SHALL render at full brightness and SHALL cost nothing while every side is default and no carrot or goggles are in use.

#### Scenario: Configured side from afar
- **WHEN** the top of an Inbound Station is set to Pull and the player looks at it with empty hands
- **THEN** the top face shows a frame in the Pull colour and the other faces show nothing

#### Scenario: Carrot in hand
- **WHEN** the player holds the carrot and looks at a machine in Auto
- **THEN** every face shows the Auto frame and the Auto glyph, and after setting the left side to Push its frame and glyph change at once

#### Scenario: Plain block
- **WHEN** a machine has every side in Auto and the player holds nothing special
- **THEN** no marker is drawn
