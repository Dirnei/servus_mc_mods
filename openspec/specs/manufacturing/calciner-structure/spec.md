# manufacturing/calciner-structure Specification

## Purpose
The Calciner is built in the world as a calcining furnace with an alembic: vanilla stone and custom parts in a fixed layout, formed with Paracelsus' Journal, drawn as one model and worked through its parts instead of a GUI.

## Requirements

### Requirement: Calciner parts
Manufacturing SHALL add the Firebox, Ash Pan, Alembic, Receiver, Sulfur Hood and Copper Duct as blocks, each crafted at a crafting table from vanilla materials without research. The Calciner block itself SHALL be the furnace mouth.

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
A formed Calciner SHALL be drawn as one model covering the whole structure instead of its single blocks: a furnace of Stone Bricks over Polished Deepslate and Deepslate Tiles with the still on top. The Alembic's spout SHALL reach the Receiver. The Copper Duct SHALL end in a raised copper collar sunk into the left Stone Bricks. The Receiver SHALL stand on legs over a shallow stone basin with a grated drain in the right Stone Bricks. The blocks SHALL keep their collision and SHALL be mined with a pickaxe.

#### Scenario: Burning
- **WHEN** a formed Calciner burns fuel and calcines
- **THEN** the fire glows, the Firebox lights its surroundings, smoke rises from the Hood and Mercury drips from the Receiver into the basin

#### Scenario: Idle
- **WHEN** a formed Calciner has no fuel or nothing to calcine
- **THEN** no smoke rises and no Mercury drips

#### Scenario: Matter in the crucible
- **WHEN** cobblestone lies in the input of a formed Calciner
- **THEN** a small cobblestone stands in the crucible of the mouth, and it disappears when the input is empty

#### Scenario: Ash heap
- **WHEN** the Ash Pan of a formed Calciner goes from empty to half its room in Ash
- **THEN** a grey ash heap in the Ash Pan rises from nothing to half its height

#### Scenario: Mercury in the basin
- **WHEN** the Mercury tank of a formed Calciner holds Mercury
- **THEN** a Mercury puddle lies in the basin under the Receiver, and it is gone when the tank is empty

#### Scenario: Mercury in the flask
- **WHEN** the Calciner holds Mercury that has not reached its tank yet
- **THEN** the Receiver's flask shows it by fill

### Requirement: Breaking unforms
Breaking any block of a formed Calciner SHALL unform the structure. The broken block SHALL drop as its item: a Stone Bricks position drops Stone Bricks and a Deepslate Tiles position drops Deepslate Tiles. Every other block SHALL return to its unformed block in place. Removing a Fluid Tank of the structure SHALL unform it as well, and the tanks keep their fluid. A Calciner that stays in place SHALL keep its buffers, input and fuel while unformed and SHALL NOT work until formed again. A broken Calciner block SHALL drop its input and fuel.

#### Scenario: Brick broken
- **WHEN** a player breaks the Stone Bricks left of a formed Calciner
- **THEN** one Stone Bricks item drops, the other blocks stand as Stone Bricks, Deepslate Tiles and unformed parts again, and the Calciner stops

#### Scenario: Deepslate broken
- **WHEN** a player breaks the Deepslate Tiles right of the Ash Pan of a formed Calciner
- **THEN** one Deepslate Tiles item drops and the Calciner unforms

#### Scenario: Tank removed
- **WHEN** a player breaks the Mercury tank of a formed Calciner
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
A formed Calciner SHALL accept calcinable items through the item capability on every face of the Calciner block and furnace fuel through the item capability on every face of the Firebox; neither SHALL give items out. It SHALL give Ash, and take nothing, through the item capability on every face of the Ash Pan. The other blocks of the structure SHALL expose no item capability.

#### Scenario: Hopper feeds the mouth
- **WHEN** a hopper points into the front of a formed Calciner's mouth and holds cobblestone
- **THEN** the cobblestone moves into the Calciner's input

#### Scenario: Pipe feeds the fire
- **WHEN** an Item Pipe ends at the back of the Firebox and delivers coal
- **THEN** the coal goes into the fuel slot

#### Scenario: Hopper under the Ash Pan
- **WHEN** a formed Calciner has calcined cobblestone into 300 mB of Salt and a hopper takes from the Ash Pan
- **THEN** the hopper receives 3 Ash (one Ash for each 100 mB the Purifier makes from one Ash)

### Requirement: Principle ports
A formed Calciner SHALL fill its own Sulfur tank with Sulfur and its own Mercury tank with Mercury as long as they have room. A principle that does not fit SHALL wait in the Calciner. The Receiver SHALL offer only Mercury and the Copper Duct above the Stone Bricks only Sulfur through the fluid capability, drain only. The Calciner SHALL never accept fluid, and no other block of its own SHALL expose a fluid capability. The Salt stays in the Ash Pan as Ash.

#### Scenario: Tanks fill
- **WHEN** a formed Calciner holds 400 mB Sulfur and 300 mB Mercury and both tanks are empty
- **THEN** the Sulfur tank receives the 400 mB Sulfur and the Mercury tank the 300 mB Mercury

#### Scenario: Tank full
- **WHEN** the Mercury tank of a formed Calciner is full and the Calciner makes Mercury
- **THEN** the Mercury stays in the Calciner and shows in the Receiver's flask until the tank has room

#### Scenario: Pipe at the Ash Pan
- **WHEN** a Fluid Pipe touches the Ash Pan of a working Calciner
- **THEN** the pipe finds no fluid handler there

#### Scenario: Pipe at the Receiver
- **WHEN** a Fluid Pipe touches the Receiver of a formed Calciner holding Mercury that has not reached its tank
- **THEN** the pipe can drain Mercury there and cannot fill anything into it

### Requirement: Buckets at the ports
Using an empty bucket or another empty fluid container on the Receiver of a formed Calciner SHALL fill it with Mercury when the Calciner holds enough. Using a filled container SHALL do nothing. Using an empty hand on the Ash Pan SHALL give the player all the Ash it holds. Mercury and Sulfur are usually bucketed from their tanks.

#### Scenario: Mercury bucket
- **WHEN** a player uses an empty bucket on the Receiver and the Mercury buffer holds 1,200 mB
- **THEN** the player holds a Mercury bucket and 200 mB Mercury remain

#### Scenario: Ash by hand
- **WHEN** a player uses the Ash Pan with an empty hand and it holds 500 mB of Salt
- **THEN** the player receives 5 Ash and the Ash Pan is empty

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

### Requirement: Compact Calciner layout
The Calciner structure SHALL be one block deep, 3 wide and 5 tall, standing on the ground, seen from its front with the Calciner block in the middle of the third row from the bottom. The middle column is, bottom to top, the Ash Pan, the Firebox, the Calciner, the Alembic and the Sulfur Hood. Deepslate Tiles SHALL stand on both sides of the Ash Pan, a Fluid Tank on both sides of the Firebox (Sulfur left, Mercury right) and Stone Bricks on both sides of the Calciner. The Receiver SHALL stand right of the Alembic, and the Copper Duct SHALL run from left of the Hood down to the left Stone Bricks. Tanks are the Foundation Fluid Tank.

#### Scenario: Layout from the front
- **WHEN** a player looks at the front of a complete Calciner structure
- **THEN** they see, top to bottom: `D H .`, `D L R`, `S C S`, `T F T`, `E A E` (H Hood, L Alembic, R Receiver, D Copper Duct, S Stone Bricks, C Calciner, T Fluid Tank, F Firebox, E Deepslate Tiles, A Ash Pan, `.` any block)

#### Scenario: Corner is free
- **WHEN** a torch stands right of the Hood
- **THEN** the structure is still complete

#### Scenario: Old bricks do not count
- **WHEN** vanilla Bricks stand right of the Calciner instead of Stone Bricks
- **THEN** the structure does not form and the player is told that Stone Bricks are missing right of the Calciner
