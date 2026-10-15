# foundation/duct-appearance Specification

## Purpose

How a Utility Duct looks in the world: its size tells how many lines it carries, its colors tell which lines, and the rings on its arms tell the channel of each endpoint.

## Requirements

### Requirement: Size follows the line count
A duct SHALL render and collide as a centre cube plus arms whose thickness grows with the number of lines it carries: one line is thin (example 4 pixels), two lines thicker (example 6 pixels), three or four lines thickest (example 8 pixels). An arm SHALL be as thick as the lines that connect on that side, not as the centre. The centre and the arms SHALL sit at the duct's resting pose, which may move them away from the middle of the block.

#### Scenario: One line
- **WHEN** a duct carries only an Item Pipe
- **THEN** its centre and arms are thin

#### Scenario: Two lines
- **WHEN** a Power Cable is added to that duct
- **THEN** the centre becomes thicker without replacing the block

#### Scenario: Three lines
- **WHEN** a Fluid Pipe is added to a duct with an Item Pipe and a Power Cable
- **THEN** the centre becomes the thickest size without replacing the block

#### Scenario: Four lines
- **WHEN** a Data Cable is added to a duct with the three other lines
- **THEN** the centre keeps the thickest size and shows four bands

#### Scenario: Mixed arm
- **WHEN** a duct with both lines has a neighbour duct that carries only an Item Pipe
- **THEN** the arm toward that neighbour is thin and the centre stays thick

#### Scenario: Fluid only arm
- **WHEN** a three line duct touches a tank
- **THEN** only the fluid arm reaches the tank and it is thin

#### Scenario: Thicker duct on the floor
- **WHEN** a duct on the floor gets a second line
- **THEN** it becomes thicker and still lies on the floor

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

### Requirement: Arms per line
A side SHALL show an arm for a line when that line is present, its mode on that side is not disabled, and the neighbour is a duct carrying the same line or a block that offers the matching capability on that face.

#### Scenario: Chest beside a two line duct
- **WHEN** a chest touches a duct carrying both lines
- **THEN** only the item arm reaches the chest

#### Scenario: Disabled side
- **WHEN** the item line's side toward the chest is set to disabled
- **THEN** no arm reaches the chest

### Requirement: Channel ring at endpoints
An arm that touches an inventory or machine SHALL carry a thin ring at its outer end in the colour of the channel of that line and side; the data line has no channels and its ring shows the colour of its tier. With several lines on the arm each line's band carries its own ring. Arms toward other ducts SHALL have no ring. When a channel's colour is edited, every loaded ring on that channel SHALL change colour.

#### Scenario: Channel 3 endpoint
- **WHEN** the item line's side toward a chest is on the red channel "Iron ore"
- **THEN** the arm toward the chest ends in a red ring

#### Scenario: Ring follows the change
- **WHEN** the player switches that side to the green channel "Copper" in the GUI
- **THEN** the ring turns green without replacing the block

#### Scenario: Fluid ring
- **WHEN** the fluid line's side toward a tank is on the blue channel "Water"
- **THEN** the fluid arm toward the tank ends in a blue ring

#### Scenario: Ring follows a recolour
- **WHEN** a team member changes "Water" from blue to black
- **THEN** every loaded ring on "Water" turns black and stays visible against the duct

#### Scenario: Data ring
- **WHEN** a Data Cable duct touches a Warehouse Controller
- **THEN** the data arm toward it ends in a white ring

### Requirement: Panels sit next to the duct's parts
A duct with panels SHALL render its lines, arms, bands and rings as without panels, and each panel on its face in addition. An arm toward a hollow panel SHALL show through the hole. A face closed by a solid panel SHALL show no arm and no ring. The hidden duct overlay SHALL NOT draw ducts behind panels; the player sees the panels.

#### Scenario: Hollow panel on a two line duct
- **WHEN** a hollow stone panel covers the east face of a duct carrying both lines with an arm to the east
- **THEN** the two line arm shows through the hole of the stone frame

#### Scenario: Solid panel on an endpoint
- **WHEN** a solid panel covers the face of a duct toward a chest
- **THEN** that face shows the panel and no arm or ring

### Requirement: Ducts lie on the floor
A duct whose block below has a full sturdy top face SHALL lie on it: its centre and its horizontal arms SHALL sit as low in the block as their thickness allows, a hair above the floor so no faces share a plane. A duct on the floor SHALL stay in the middle of its block across both horizontal axes. Ducts SHALL NOT count as floor, and partial blocks such as bottom slabs SHALL NOT count either. A block the duct has an arm into SHALL NOT count as floor. A duct with an arm up SHALL lie on the floor like any other duct, so a floor run turning up bends cleanly and its arm up rises from the low duct, unless it also has a horizontal arm into an inventory or machine: then it SHALL stay in the middle, so the run goes straight up from the connection. A duct with only an arm down may still lie on the floor.

#### Scenario: Run across a stone floor
- **WHEN** a player lays a straight run of Item Pipe ducts on a stone floor
- **THEN** every duct of the run lies on the floor and the run is straight

#### Scenario: Floor removed
- **WHEN** the stone block under a duct lying on it is broken
- **THEN** that duct moves back to the middle of its block without replacing the block

#### Scenario: Above a bottom slab
- **WHEN** a duct stands above a bottom slab
- **THEN** it stays in the middle of its block

#### Scenario: On another duct
- **WHEN** a duct stands directly above another duct
- **THEN** it does not lie on the duct below

#### Scenario: On a machine it feeds
- **WHEN** a duct stands on a furnace and has an arm down into it
- **THEN** it stays in the middle of its block and the arm reaches the middle of the furnace top

#### Scenario: Horizontal run beside a wall
- **WHEN** a horizontal run on a floor passes along a wall
- **THEN** it lies on the floor in the middle of the block across the run, not against the wall

#### Scenario: Floor run turning up
- **WHEN** a run of ducts lies on a stone floor and its last duct has an arm west along the floor and an arm up
- **THEN** that duct lies on the floor like the rest of the run, so the run has no step in it, and its arm up rises straight from the low duct

#### Scenario: Climb above a bend
- **WHEN** a duct with an arm up and an arm down stands above a duct where a floor run turns up
- **THEN** it stays in the middle of its block, and the vertical arm between the two ducts is one straight arm in the middle across the block, with no gap and no step

#### Scenario: Machine and floor run on one duct turning up
- **WHEN** a duct above a stone floor has an arm west along the floor, an arm east into a furnace and an arm up
- **THEN** it stays in the middle of its block with straight arms, and the duct of the floor run beside it steps up in its own block to meet it in the middle

#### Scenario: Machine on the floor, pipe straight up
- **WHEN** a duct above a stone floor has an arm east into a furnace standing on the floor and an arm up
- **THEN** it stays in the middle of its block, its arm runs straight into the middle of the furnace face, and the run goes straight up

### Requirement: Unsupported ducts drape over edges
A duct with nothing under it SHALL take the height that its connected neighbour ducts beside it ask for, when they rest on a floor or are held in the middle themselves and agree, so a run over the edge of a step or a short gap stays level. Neighbours above or below it SHALL NOT ask. A duct with an arm up and a horizontal arm into an inventory or machine SHALL NOT drape. Neighbours that disagree SHALL leave it in the middle. Only neighbours one block away SHALL count, with one exception: a duct at the top or bottom of a climb (an arm up or down to a duct) with no such neighbour SHALL take the height of the draped neighbour ducts beside it, as long as they agree, so the climb meets a draped run at the run's height. No duct SHALL take its height from such a climb end. Draping SHALL NOT move a duct sideways.

#### Scenario: Over the edge of a step
- **WHEN** a run lies on a floor, climbs one block up beside the east wall of a step, and continues east on top of the step
- **THEN** the duct at the foot of the climb lies on the floor, so the floor run has no step in it, its arm up rises straight from the low duct, and the duct at the top of the climb (with only an arm down) lies as low as the run on top of the step, so the run on the step has no step in it

#### Scenario: Climb, one duct in the air, then onto a block
- **WHEN** a floor run climbs one block, continues east with one duct in open air, and then onto a block, where the next duct lies on the block
- **THEN** the duct in the air drapes to the height of the duct on the block, the duct at the top of the climb takes that height too, and the climb meets the run low with no hump and no step

#### Scenario: Up beside a machine in the air
- **WHEN** a duct in open air has an arm east into a furnace, an arm up, and an arm west to a duct lying on a floor
- **THEN** it does not drape and stays in the middle of its block

#### Scenario: Neighbours disagree
- **WHEN** a duct in open air connects to a duct lying on a floor on one side and to a duct with a facade panel on the other side
- **THEN** it stays in the middle of its block

#### Scenario: Long span
- **WHEN** a run crosses a three block gap of open air between two floors
- **THEN** the first and last duct over the gap take the floor height and the middle duct stays in the middle of its block

### Requirement: Centred ducts
A duct SHALL stay in the middle of its block on both horizontal axes, whatever blocks stand beside it, so a vertical run is always in the middle of its blocks across the run. Up and down, a duct SHALL stay in the middle where no floor or draping moves it. A duct with any facade panel SHALL stay in the middle on every axis. A duct SHALL NOT move up toward a ceiling.

#### Scenario: In open air
- **WHEN** a run hangs in open air with no blocks around
- **THEN** it sits in the middle of its blocks as before

#### Scenario: Pipe up a wall
- **WHEN** a player places a vertical run of ducts against the east side of a stone wall
- **THEN** every duct of the run stays in the middle of its block and the run is straight

#### Scenario: Climb in a corner
- **WHEN** a vertical run stands in a corner with stone to the north and to the west
- **THEN** the run stays in the middle of its blocks

#### Scenario: Under a ceiling
- **WHEN** a duct has stone above it and air below
- **THEN** it stays in the middle of its block

#### Scenario: Facade on a floor duct
- **WHEN** a player puts a facade panel on a duct lying on the floor
- **THEN** that duct moves to the middle of its block and its arms through hollow panels pass through the holes

### Requirement: Arms step between positions
Two connected ducts SHALL draw their shared arm at one position where they meet at the shared face. When a duct sits elsewhere than that position, its centre SHALL grow up or down until it reaches the arm at that position, and the arm SHALL run straight from the centre to the face, so the run stays joined and the bend happens in the middle of the block, never near its edge. The position at the face SHALL be the one of a duct that stays in the middle because it has an arm up and an arm into an inventory or machine, so that such a junction keeps straight arms and its neighbours step; else the one of the duct that rests on a floor or is held in the middle by a panel, else the one of a draped duct.

#### Scenario: Off the edge of a floor
- **WHEN** a run on a floor continues into open air past the edge of the floor and the next duct stays in the middle
- **THEN** the run steps between floor height and the middle with no gap, the vertical part in the middle of the stepping duct's block

#### Scenario: Duct beside a facade duct
- **WHEN** a duct on the floor connects to a duct with a facade panel
- **THEN** the arm steps so both meet at the middle of the shared face

### Requirement: Endpoint arms end centred
An arm that touches an inventory or machine SHALL end in the middle of that face with its channel ring there, as without a resting pose. When the duct rests elsewhere, its centre SHALL grow to the middle and the arm SHALL run straight into the middle of the face.

#### Scenario: Floor duct into a furnace
- **WHEN** a duct lying on the floor touches a furnace to its east
- **THEN** the duct's centre rises from the floor to the middle of its block, and the arm runs straight into the middle of the furnace face with its ring centred on that face

#### Scenario: Centred duct into a chest
- **WHEN** a duct in the middle of its block touches a chest
- **THEN** the arm runs straight into the middle of the chest face as before

### Requirement: Pose follows the world
The resting pose SHALL be worked out when a duct is placed, when it gains or loses an arm or a panel, and when a block next to it or next to a connected neighbour duct changes, never by checking blocks every tick. Every player SHALL see the same pose, including after the chunk reloads.

#### Scenario: Floor placed later
- **WHEN** a player places a stone block under a duct hanging in open air
- **THEN** the duct moves down onto the new floor without being replaced

#### Scenario: Neighbour of a draped duct changes
- **WHEN** the floor under the duct that a draped duct takes its height from is broken
- **THEN** both that duct and the draped duct move back to the middle

#### Scenario: Reload
- **WHEN** the player leaves and rejoins the world
- **THEN** every duct shows the same pose as before

### Requirement: Outline and clicks follow the pose
A duct's outline, collision and the parts the Configuration Carrot can click SHALL match where the duct is drawn, including a centre that grew. The data load overlay of the goggles SHALL be drawn around the duct's centre where it is drawn.

#### Scenario: Carrot on a floor duct arm
- **WHEN** the player uses the carrot on the arm of a floor duct that steps up into a chest
- **THEN** the endpoint GUI for that side opens wherever the click hits the arm

#### Scenario: Walking over a floor duct
- **WHEN** a player walks across a thin duct lying on the floor
- **THEN** the player collides with the duct where it is drawn, low on the floor

#### Scenario: Goggles on a floor duct
- **WHEN** a player with goggles looks at a data duct lying on the floor
- **THEN** the load colour box surrounds the duct low on the floor

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
