# Spec Delta

## Purpose

The Calciner is built in the world as a calcining furnace with an alembic: vanilla bricks and custom parts in a fixed layout, formed with Paracelsus' Journal, drawn as one model and worked through its parts instead of a GUI.

## ADDED Requirements

### Requirement: Calciner layout
The Calciner structure SHALL be one block deep, 3 wide and 5 tall, seen from its front with the Calciner block in the middle row: Sulfur Hood above the Alembic, Alembic above the Calciner with the Receiver on its right, Bricks left and right of the Calciner, Firebox below it with Bricks on both sides, and Ash Pan below the Firebox with Bricks on both sides. The two corners beside the Hood and the corner left of the Alembic SHALL be free. Bricks SHALL be vanilla Bricks.

#### Scenario: Layout from the front
- **WHEN** a player looks at the front of a complete Calciner structure
- **THEN** they see, top to bottom: `. H .`, `. L R`, `B C B`, `B F B`, `B A B` (H Hood, L Alembic, R Receiver, B Bricks, C Calciner, F Firebox, A Ash Pan, `.` any block)

#### Scenario: Corners are free
- **WHEN** a torch stands left of the Alembic and stone blocks stand beside the Hood
- **THEN** the structure is still complete

### Requirement: Calciner parts
Manufacturing SHALL add the Firebox, Ash Pan, Alembic, Receiver and Sulfur Hood as blocks, each crafted at a crafting table from vanilla materials without research. The Calciner block itself SHALL be the furnace mouth.

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
A formed Calciner SHALL be drawn as one model covering the whole structure instead of its single blocks. The model SHALL show the fire glowing and the Firebox giving light while it burns, smoke rising from the Hood while it works, and the fill of Salt in the Ash Pan, Mercury in the Receiver and Sulfur in the Hood. The blocks of a formed Calciner SHALL keep their collision and SHALL be mined like bricks.

#### Scenario: Burning
- **WHEN** a formed Calciner burns fuel and calcines
- **THEN** the fire glows, the Firebox lights its surroundings and smoke rises from the Hood

#### Scenario: Receiver fills
- **WHEN** the Mercury tank of a formed Calciner goes from empty to half full
- **THEN** the liquid in the Receiver rises from empty to half its height

### Requirement: Breaking unforms
Breaking any block of a formed Calciner SHALL unform the structure: the broken block SHALL drop as its item (a brick position drops Bricks), and every other block SHALL return to its unformed block in place. A Calciner that stays in place SHALL keep its tanks, input and fuel while unformed and SHALL NOT work until formed again. A broken Calciner block SHALL drop its input and fuel, like other machines.

#### Scenario: Brick broken
- **WHEN** a player breaks a brick of a formed Calciner
- **THEN** one Bricks item drops, the other blocks stand as Bricks and unformed parts again, and the Calciner stops

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
A formed Calciner SHALL offer only Salt through the fluid capability on the faces of the Ash Pan, only Mercury on the faces of the Receiver and only Sulfur on the faces of the Hood, SHALL push each into touching fluid handlers on those faces, and SHALL never accept fluid. No other block of the structure SHALL expose a fluid capability.

#### Scenario: Pipe at the Ash Pan
- **WHEN** a Fluid Pipe touches the Ash Pan of a working Calciner and leads to a Fluid Tank
- **THEN** only Salt flows into that tank

#### Scenario: Tank on the Hood
- **WHEN** a Fluid Tank sits on top of the Hood
- **THEN** Sulfur flows into it and nothing else

### Requirement: Buckets at the ports
Using an empty bucket or another empty fluid container on the Ash Pan, the Receiver or the Hood of a formed Calciner SHALL fill it with that port's principle when the tank holds enough. Using a filled container SHALL do nothing.

#### Scenario: Mercury bucket
- **WHEN** a player uses an empty bucket on the Receiver and the Mercury tank holds 1,200 mB
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
