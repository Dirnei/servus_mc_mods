# Spec Delta

## Purpose

How a Utility Duct looks in the world: its size tells how many lines it carries, its colors tell which lines, and the rings on its arms tell the channel of each endpoint.

## ADDED Requirements

### Requirement: Size follows the line count
A duct SHALL render and collide as a centre cube plus arms whose thickness grows with the number of lines it carries: one line is thin (example 4 pixels), two lines thicker (example 6 pixels). An arm SHALL be as thick as the lines that connect on that side, not as the centre.

#### Scenario: One line
- **WHEN** a duct carries only an Item Pipe
- **THEN** its centre and arms are thin

#### Scenario: Two lines
- **WHEN** a Power Cable is added to that duct
- **THEN** the centre becomes thicker without replacing the block

#### Scenario: Mixed arm
- **WHEN** a duct with both lines has a neighbour duct that carries only an Item Pipe
- **THEN** the arm toward that neighbour is thin and the centre stays thick

### Requirement: Each line has a color
The Item Pipe and the Power Cable SHALL each have their own color on the block. A duct with one line SHALL show that line's color. A duct with two lines SHALL show both colors as bands running along the duct and its arms, so the player can tell from a distance which lines the block carries.

#### Scenario: Item only
- **WHEN** a duct carries only an Item Pipe
- **THEN** the duct shows the item color only

#### Scenario: Both lines
- **WHEN** a duct carries both lines
- **THEN** both colors are visible on the centre and on every arm that carries both

### Requirement: Arms per line
A side SHALL show an arm for a line when that line is present, its mode on that side is not disabled, and the neighbour is a duct carrying the same line or a block that offers the matching capability on that face.

#### Scenario: Chest beside a two line duct
- **WHEN** a chest touches a duct carrying both lines
- **THEN** only the item arm reaches the chest

#### Scenario: Disabled side
- **WHEN** the item line's side toward the chest is set to disabled
- **THEN** no arm reaches the chest

### Requirement: Channel ring at endpoints
An arm that touches an inventory or machine SHALL carry a thin ring at its outer end in the channel color of that line and side. With two lines on the arm each line's band carries its own ring. Arms toward other ducts SHALL have no ring.

#### Scenario: Channel 3 endpoint
- **WHEN** the item line's side toward a chest is on channel 3
- **THEN** the arm toward the chest ends in a ring of channel 3's color

#### Scenario: Ring follows the change
- **WHEN** the player switches that side to channel 5 in the GUI
- **THEN** the ring changes color without replacing the block

### Requirement: Facades cover everything
A duct with a facade SHALL render as the facade block regardless of lines and channels, and SHALL show its lines, arms and rings again when the facade comes off. The hidden duct overlay SHALL draw the arms of the facaded duct.

#### Scenario: Facade on a two line duct
- **WHEN** a stone facade is applied to a duct carrying both lines
- **THEN** it renders as a full stone block; after sneak carrot the two line look returns
