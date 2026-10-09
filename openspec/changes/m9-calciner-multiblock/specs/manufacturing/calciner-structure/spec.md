# Spec Delta

## Purpose

The Calciner is built in the world as a calcining furnace with an alembic: vanilla bricks and custom parts in a fixed layout, formed with Paracelsus' Journal, drawn as one model and worked through its parts instead of a GUI.

## ADDED Requirements

### Requirement: Calciner layout
The Calciner structure SHALL be one block deep, 5 wide and 5 tall, standing on the ground, seen from its front with the Calciner block in the middle of the third row from the bottom. The middle three columns are the furnace: Bricks on both sides of the Ash Pan, the Firebox and the Calciner, the Alembic above the Calciner and the Sulfur Hood above the Alembic. The outer columns are a Mercury tank of three Fluid Tanks on the right, with the Receiver on top, and a Sulfur tank of three Fluid Tanks on the left, with the Sulfur Duct running from the Hood across and down into it. The cell between the Alembic and the Receiver SHALL be air for the spout. Bricks SHALL be vanilla Bricks, tanks the Foundation Fluid Tank.

#### Scenario: Layout from the front
- **WHEN** a player looks at the front of a complete Calciner structure
- **THEN** they see, top to bottom: `D D H . .`, `D . L _ R`, `T B C B T`, `T B F B T`, `T B A B T` (H Hood, L Alembic, R Receiver, D Sulfur Duct, T Fluid Tank, B Bricks, C Calciner, F Firebox, A Ash Pan, `_` air, `.` any block)

#### Scenario: Corners are free
- **WHEN** a torch stands left of the Alembic and stone blocks stand right of the Hood
- **THEN** the structure is still complete

#### Scenario: Spout blocked
- **WHEN** a block stands between the Alembic and the Receiver
- **THEN** the structure does not form, and a formed Calciner unforms when the block is placed there

### Requirement: Calciner parts
Manufacturing SHALL add the Firebox, Ash Pan, Alembic, Receiver, Sulfur Hood and Sulfur Duct as blocks, each crafted at a crafting table from vanilla materials without research. The Calciner block itself SHALL be the furnace mouth.

#### Scenario: Alembic recipe
- **WHEN** the player crafts the Alembic pattern of copper ingots and glass
- **THEN** one Alembic is produced

### Requirement: Forming with the Journal
Right clicking the Calciner block with Paracelsus' Journal SHALL check the structure in each of the four horizontal facings and form it in the first facing that matches; the front is then the side the Calciner faces in that match. A right click with the Journal SHALL NOT open the book. The Configuration Carrot SHALL NOT form, turn or dismantle any block of the structure.

#### Scenario: Formed
- **WHEN** a player right clicks the Calciner of a complete structure with the Journal
- **THEN** the structure forms and the player sees "The Calciner is ready."

#### Scenario: Other parts do not form
- **WHEN** a player right clicks the Firebox of a complete structure with the Journal
- **THEN** nothing forms

### Requirement: Forming reports what is missing
When the structure does not match in any facing, forming SHALL fail and SHALL tell the player the first missing or wrong block of the facing that matched best, by part name and position relative to the Calciner, and SHALL point to the Journal's Calciner page.

#### Scenario: Receiver missing
- **WHEN** a player right clicks the Calciner with the Journal and only the Receiver is missing
- **THEN** nothing forms and the player is told that the Receiver is missing right of the Alembic

### Requirement: Formed Calciner looks like one machine
A formed Calciner SHALL be drawn as one model covering the whole structure instead of its single blocks, with the Alembic's spout reaching the Receiver and the Sulfur Duct joining Hood and tank. The model SHALL show the matter being calcined in the crucible of the mouth, the fire glowing and the Firebox giving light while it burns, smoke rising from the Hood while it works, and the Salt in the Ash Pan and the Mercury in the Receiver by fill. The Fluid Tanks show their own level. The blocks of a formed Calciner SHALL keep their collision and SHALL be mined like bricks.

#### Scenario: Burning
- **WHEN** a formed Calciner burns fuel and calcines
- **THEN** the fire glows, the Firebox lights its surroundings and smoke rises from the Hood

#### Scenario: Matter in the crucible
- **WHEN** cobblestone lies in the input of a formed Calciner
- **THEN** a small cobblestone stands in the crucible of the mouth, and it disappears when the input is empty

### Requirement: Breaking unforms
Breaking any block of a formed Calciner SHALL unform the structure: the broken block SHALL drop as its item (a brick position drops Bricks), and every other block SHALL return to its unformed block in place. Removing a Fluid Tank of the structure or placing a block in the spout's air cell SHALL unform it as well; the tanks keep their fluid. A Calciner that stays in place SHALL keep its tanks, input and fuel while unformed and SHALL NOT work until formed again. A broken Calciner block SHALL drop its input and fuel, like other machines.

#### Scenario: Brick broken
- **WHEN** a player breaks a brick of a formed Calciner
- **THEN** one Bricks item drops, the other blocks stand as Bricks and unformed parts again, and the Calciner stops

#### Scenario: Tank removed
- **WHEN** a player breaks a Fluid Tank of the Mercury column
- **THEN** the Calciner unforms

#### Scenario: Calciner broken
- **WHEN** a player breaks the Calciner block of a formed Calciner holding Salt, cobblestone and coal
- **THEN** the structure unforms, the Calciner drops as an item together with the cobblestone and the coal, and the Salt is lost

### Requirement: Matter and fuel go in by hand
On a formed Calciner, using a calcinable item on the Calciner block SHALL put it into the input, using a furnace fuel on the Firebox SHALL put it into the fuel slot, and using the Calciner with an empty hand while sneaking SHALL give the input back to the player. The Calciner SHALL have no GUI.

#### Scenario: Cobblestone by hand
- **WHEN** a player holding 16 cobblestone uses them on a formed Calciner with an empty input
- **THEN** the 16 cobblestone go into the input

#### Scenario: Take back
- **WHEN** a player sneaks and uses a formed Calciner with an empty hand
- **THEN** the cobblestone in the input goes to the player

### Requirement: Item ports
A formed Calciner SHALL accept calcinable items through the item capability on every face of the Calciner block and furnace fuel through the item capability on every face of the Firebox. Neither SHALL give items out. The other blocks of the structure SHALL expose no item capability.

#### Scenario: Hopper feeds the mouth
- **WHEN** a hopper points into the front of a formed Calciner's mouth and holds cobblestone
- **THEN** the cobblestone moves into the Calciner's input

#### Scenario: Pipe feeds the fire
- **WHEN** an Item Pipe ends at the back of the Firebox and delivers coal
- **THEN** the coal goes into the fuel slot

### Requirement: Principle ports
A formed Calciner SHALL offer only Salt through the fluid capability on the faces of the Ash Pan, only Mercury on the faces of the Receiver and only Sulfur on the faces of the lowest Sulfur Duct, and SHALL push each into touching fluid handlers outside the furnace: the Mercury down into the Mercury tank, the Sulfur down into the Sulfur tank, the Salt into whatever touches the Ash Pan. It SHALL never accept fluid. No other block of its own SHALL expose a fluid capability; pipes take Mercury and Sulfur from the tanks.

#### Scenario: Pipe at the Ash Pan
- **WHEN** a Fluid Pipe touches the Ash Pan of a working Calciner and leads to a Fluid Tank
- **THEN** only Salt flows into that tank

#### Scenario: Tanks fill
- **WHEN** a formed Calciner holds 400 mB Sulfur and 300 mB Mercury
- **THEN** the Sulfur tank column receives the 400 mB Sulfur and the Mercury tank column the 300 mB Mercury

### Requirement: Buckets at the ports
Using an empty bucket or another empty fluid container on the Ash Pan or the Receiver of a formed Calciner SHALL fill it with that port's principle when the Calciner holds enough. Using a filled container SHALL do nothing. Mercury and Sulfur are usually bucketed from their tanks.

#### Scenario: Mercury bucket
- **WHEN** a player uses an empty bucket on the Receiver and the Mercury buffer holds 1,200 mB
- **THEN** the player holds a Mercury bucket and 200 mB Mercury remain

### Requirement: Upkeep at the Firebox
A formed Calciner SHALL report its fuel as upkeep on the Firebox, so a data line touching the Firebox keeps it fuelled.

#### Scenario: Data line fuels
- **WHEN** a Machine Adapter or data line delivers upkeep coal to the Firebox of a formed Calciner
- **THEN** the coal lands in the fuel slot

### Requirement: Calciner in Jade
With Jade installed, looking at any block of a formed Calciner SHALL show the input, the fuel, the progress and the three principle amounts with capacity.

#### Scenario: Look at the Receiver
- **WHEN** a player with Jade looks at the Receiver of a Calciner holding 300 mB Mercury
- **THEN** the tooltip shows 300 / 4000 mB Mercury among the Calciner's state

### Requirement: Calciner layout is pure logic
The layout SHALL be described as positions relative to the Calciner per facing, with the part at each position and the Calciner position from any part position. This mapping SHALL be pure Java covered by JUnit, and the Journal's multiblock schematic SHALL be generated from it.

#### Scenario: Round trip
- **WHEN** the layout gives the world position of the Receiver for a Calciner facing east, and the Calciner position is computed back from it
- **THEN** it is the original Calciner position
