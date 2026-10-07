## MODIFIED Requirements

### Requirement: Size follows the line count
A duct SHALL render and collide as a centre cube plus arms whose thickness grows with the number of lines it carries: one line is thin (example 4 pixels), two lines thicker (example 6 pixels), three lines thickest (example 8 pixels). An arm SHALL be as thick as the lines that connect on that side, not as the centre.

#### Scenario: One line
- **WHEN** a duct carries only an Item Pipe
- **THEN** its centre and arms are thin

#### Scenario: Two lines
- **WHEN** a Power Cable is added to that duct
- **THEN** the centre becomes thicker without replacing the block

#### Scenario: Three lines
- **WHEN** a Fluid Pipe is added to a duct with an Item Pipe and a Power Cable
- **THEN** the centre becomes the thickest size without replacing the block

#### Scenario: Mixed arm
- **WHEN** a duct with both lines has a neighbour duct that carries only an Item Pipe
- **THEN** the arm toward that neighbour is thin and the centre stays thick

#### Scenario: Fluid only arm
- **WHEN** a three line duct touches a tank
- **THEN** only the fluid arm reaches the tank and it is thin

### Requirement: Each line has a color
The Item Pipe, the Power Cable and the Fluid Pipe SHALL each have their own color on the block. A duct with one line SHALL show that line's color. A duct with two or three lines SHALL show every line's color as bands running along the duct and its arms, so the player can tell from a distance which lines the block carries. The centre's bands SHALL run along the axis of its arms when every arm lies on one axis. A centre whose arms lie on more than one axis SHALL be a uniform cube in a neutral duct colour, so bends, branches and crossings read as one block.

#### Scenario: Item only
- **WHEN** a duct carries only an Item Pipe
- **THEN** the duct shows the item color only

#### Scenario: Both lines
- **WHEN** a duct carries the Item Pipe and the Power Cable
- **THEN** both colors are visible on the centre and on every arm that carries both

#### Scenario: Three lines
- **WHEN** a duct carries all three lines in a straight run
- **THEN** the item, energy and fluid colors are visible as three bands on the centre and on every arm that carries all three

#### Scenario: Item and fluid
- **WHEN** a duct carries only the Item Pipe and the Fluid Pipe
- **THEN** it has the two line size and shows the item and fluid colors

#### Scenario: East west run
- **WHEN** a two line duct has arms to the east and west only
- **THEN** the bands run through the centre without a break, the same as on a north south run

#### Scenario: Crossing
- **WHEN** a two line duct has arms to the north and to the east
- **THEN** its centre is a uniform cube and the bands start on the arms

#### Scenario: Three line crossing
- **WHEN** a three line duct has arms to the north and to the east
- **THEN** its centre is a uniform cube in the three line size and the bands start on the arms

### Requirement: Channel ring at endpoints
An arm that touches an inventory or machine SHALL carry a thin ring at its outer end in the channel color of that line and side. With several lines on the arm each line's band carries its own ring. Arms toward other ducts SHALL have no ring.

#### Scenario: Channel 3 endpoint
- **WHEN** the item line's side toward a chest is on channel 3
- **THEN** the arm toward the chest ends in a ring of channel 3's color

#### Scenario: Ring follows the change
- **WHEN** the player switches that side to channel 5 in the GUI
- **THEN** the ring changes color without replacing the block

#### Scenario: Fluid ring
- **WHEN** the fluid line's side toward a tank is on channel 2
- **THEN** the fluid arm toward the tank ends in a ring of channel 2's color
