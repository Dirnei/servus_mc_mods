# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
- **THEN** it stays in the middle of its block and the floor run steps up into it

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
Two connected ducts SHALL draw their shared arm at one position where they meet at the shared face. When a duct sits elsewhere than that position, its arm SHALL make a short step up or down from the duct to the face, so the run stays joined. The position at the face SHALL be the one of the duct that rests on a floor or is held in the middle by a panel, else the one of a draped duct.

#### Scenario: Off the edge of a floor
- **WHEN** a run on a floor continues into open air past the edge of the floor and the next duct stays in the middle
- **THEN** the run steps between floor height and the middle with no gap

#### Scenario: Duct beside a facade duct
- **WHEN** a duct on the floor connects to a duct with a facade panel
- **THEN** the arm steps so both meet at the middle of the shared face

### Requirement: Endpoint arms end centred
An arm that touches an inventory or machine SHALL end in the middle of that face with its channel ring there, as without a resting pose. When the duct rests elsewhere, the arm SHALL step from the duct to the middle of the face.

#### Scenario: Floor duct into a furnace
- **WHEN** a duct lying on the floor touches a furnace to its east
- **THEN** the arm steps up and enters the middle of the furnace face, and its ring is centred on that face

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
A duct's outline, collision and the parts the Configuration Carrot can click SHALL match where the duct is drawn, including steps. The data load overlay of the goggles SHALL be drawn around the duct's centre where it is drawn.

#### Scenario: Carrot on a floor duct arm
- **WHEN** the player uses the carrot on the arm of a floor duct that steps up into a chest
- **THEN** the endpoint GUI for that side opens, whether the click hits the low part, the step or the centred end

#### Scenario: Walking over a floor duct
- **WHEN** a player walks across a thin duct lying on the floor
- **THEN** the player collides with the duct where it is drawn, low on the floor

#### Scenario: Goggles on a floor duct
- **WHEN** a player with goggles looks at a data duct lying on the floor
- **THEN** the load colour box surrounds the duct low on the floor
