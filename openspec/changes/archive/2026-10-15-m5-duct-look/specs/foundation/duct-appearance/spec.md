# Spec Delta

## MODIFIED Requirements

### Requirement: Each line has a color
The Item Pipe, the Power Cable, the Fluid Pipe and the data line SHALL each have their own look on the block, drawn as a hose or cable: the Item Pipe as an orange corrugated hose, the Power Cable as ribbed red rubber, the Fluid Pipe as a blue hose with a spiral wrap and a brass sleeve, and the data line as a braid in the colours of its tier (examples: white for the Data Cable, white and gray for the Duplex Data Cable, black with a cyan tracer for the Fast Data Cable). A duct with one line SHALL show that line's look. A duct with two to four lines SHALL show every line's look as bands side by side running along the duct and its arms, so the player can tell from a distance which lines the block carries. The centre's bands SHALL run along the axis of its arms when every arm lies on one axis and the centre does not bend between two heights. A centre whose arms lie on more than one axis, or that bends between two heights, SHALL be a fitting of moulded black rubber with a steel cap, whatever number of lines it carries, so bends, branches and crossings read as one block.

#### Scenario: Item only
- **WHEN** a duct carries only an Item Pipe
- **THEN** the duct shows the orange corrugated hose only

#### Scenario: Both lines
- **WHEN** a duct carries the Item Pipe and the Power Cable
- **THEN** the orange hose and the red cable are visible side by side on the centre and on every arm that carries both

#### Scenario: Three lines
- **WHEN** a duct carries all three lines in a straight run
- **THEN** the item, power and fluid looks are visible as three bands on the centre and on every arm that carries all three

#### Scenario: Data only
- **WHEN** a duct carries only a Data Cable line
- **THEN** the duct shows the white braid only

#### Scenario: Item and fluid
- **WHEN** a duct carries only the Item Pipe and the Fluid Pipe
- **THEN** it has the two line size and shows the item and fluid looks

#### Scenario: East west run
- **WHEN** a two line duct has arms to the east and west only
- **THEN** the bands run through the centre without a break, the same as on a north south run

#### Scenario: Crossing
- **WHEN** a two line duct has arms to the north and to the east
- **THEN** its centre is a black rubber fitting and the bands start on the arms

#### Scenario: Three line crossing
- **WHEN** a three line duct has arms to the north and to the east
- **THEN** its centre is a black rubber fitting in the three line size and the bands start on the arms

#### Scenario: Single line bend
- **WHEN** an Item Pipe on a floor turns up into a climb
- **THEN** the duct at the bend is a black rubber fitting in the one line size, and the orange hose starts on its arms

#### Scenario: Bend between two heights
- **WHEN** a two line duct on a floor meets a centred duct and bends up in the middle of its block
- **THEN** its centre is a black rubber fitting and its arms show the two bands

### Requirement: Channel ring at endpoints
An arm that touches an inventory or machine SHALL carry a thin ring at its outer end in the channel color of that line and side; the data line has no channels and its ring shows the colour of its tier. With several lines on the arm each line's band carries its own ring. Arms toward other ducts SHALL have no ring.

#### Scenario: Channel 3 endpoint
- **WHEN** the item line's side toward a chest is on channel 3
- **THEN** the arm toward the chest ends in a ring of channel 3's color

#### Scenario: Ring follows the change
- **WHEN** the player switches that side to channel 5 in the GUI
- **THEN** the ring changes color without replacing the block

#### Scenario: Fluid ring
- **WHEN** the fluid line's side toward a tank is on channel 2
- **THEN** the fluid arm toward the tank ends in a ring of channel 2's color

#### Scenario: Data ring
- **WHEN** a Data Cable duct touches a Warehouse Controller
- **THEN** the data arm toward it ends in a white ring

## ADDED Requirements

### Requirement: Textures run along the duct
On every side face of an arm, and of a centre whose arms lie on one axis, the hose or cable pattern SHALL run along the arm: ribs, corrugations, spiral wraps and braids SHALL cross the arm, and bands SHALL run along it. The pattern SHALL look the same on all four sides of an arm, never mirrored and never turned. It SHALL keep its texel size whatever the length of the arm, and SHALL continue without a seam from one block to the next along a straight run, including where an arm meets the centre. A face that shows a thinner or thicker duct SHALL show the matching cut of the same pattern rather than a squeezed one.

#### Scenario: Ribs across a run
- **WHEN** a player looks at a straight Power Cable run along the east west axis from the north, the south, above and below
- **THEN** the red ribs cross the cable on every side, at the same spacing

#### Scenario: Seamless run
- **WHEN** an Item Pipe runs straight through five blocks
- **THEN** the corrugation continues across every block border and every arm to centre joint without a jump

#### Scenario: Short arm not squeezed
- **WHEN** a thick duct has a thin arm toward a chest
- **THEN** the thin arm shows its hose at the same texel size as a long arm

#### Scenario: Bands in order from every side
- **WHEN** a player walks around a straight two line duct of Item Pipe and Power Cable
- **THEN** the orange and red bands keep their order on every side face, and also on a duct whose centre bends between two heights

### Requirement: Line items show their cross-section
Each line item (Item Pipe, Fluid Pipe, Power Cable and the three data cable tiers) SHALL have an icon of a short stub of that line seen from its end: a rim, a liner in the line's colour and a core that shows what the line carries (examples: an item, water, copper, one to three data cores by tier), so the colour of the line on the block can be told from the item. The docs site SHALL show the same icons.

#### Scenario: Pipe icons tell the lines apart
- **WHEN** a player holds an Item Pipe, a Fluid Pipe and a Power Cable in the hotbar
- **THEN** each icon shows its line's colour at the rim and its own core

#### Scenario: Data tiers tell apart
- **WHEN** a player holds a Data Cable, a Duplex Data Cable and a Fast Data Cable
- **THEN** the icons show one, two and three data cores in the colours of their tiers
