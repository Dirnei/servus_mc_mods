# alchemy/calciner-structure Specification

## Purpose
The Calciner is built in the world as a calcining furnace with an alembic: vanilla stone and custom parts in a fixed layout, formed with Paracelsus' Journal, drawn as one model and worked through its parts instead of a GUI.

## Requirements

### Requirement: Calciner parts
Alchemy SHALL add the Firebox, Ash Pan, Alembic, Receiver, Sulfur Hood and Copper Duct as blocks, each crafted at a crafting table from vanilla materials without research. The Calciner block itself SHALL be the furnace mouth. The stone parts SHALL be crafted from the stone they are drawn in: the Firebox from Polished Deepslate and the Ash Pan from Deepslate Tiles.

#### Scenario: Alembic recipe
- **WHEN** the player crafts the Alembic pattern of copper ingots and glass
- **THEN** one Alembic is produced

#### Scenario: Firebox recipe
- **WHEN** the player crafts the Firebox pattern of Polished Deepslate, iron bars and a furnace
- **THEN** one Firebox is produced

#### Scenario: Ash Pan recipe
- **WHEN** the player crafts the Ash Pan pattern of Deepslate Tiles and an iron ingot
- **THEN** one Ash Pan is produced

#### Scenario: Bricks no longer craft the parts
- **WHEN** the player lays Bricks in the Firebox, Ash Pan or Calciner pattern
- **THEN** nothing is produced

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
A formed Calciner SHALL be drawn as one model covering the whole structure instead of its single blocks: a furnace of Stone Bricks over Polished Deepslate and Deepslate Tiles with the still on top. The Alembic's spout SHALL reach into the side of the Receiver, a copper condenser drum with a tap underneath. The Copper Duct SHALL end in a raised copper collar sunk into the left Stone Bricks. The Receiver SHALL stand on copper legs in a shallow stone basin with a grated drain in the right Stone Bricks. The blocks SHALL keep their collision and SHALL be mined with a pickaxe.

#### Scenario: Burning
- **WHEN** a formed Calciner burns fuel and calcines
- **THEN** the fire glows, the Firebox lights its surroundings, smoke rises from the Hood and Mercury drips from the Receiver's tap into the basin

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
- **WHEN** a formed Calciner makes Mercury
- **THEN** the Mercury goes straight into the Mercury tank, and the Receiver shows no flask and no fill

#### Scenario: Spout meets the condenser
- **WHEN** a player looks at a formed Calciner from the front or the side
- **THEN** the Alembic's spout ends in the side of the Receiver's copper drum and no part of the Receiver floats or is see-through

### Requirement: Breaking unforms
Breaking any block of a formed Calciner SHALL unform the structure. The broken block SHALL drop as its item: a Stone Bricks position drops Stone Bricks and a Deepslate Tiles position drops Deepslate Tiles. Every other block SHALL return to its unformed block in place. Removing a Fluid Tank of the structure SHALL unform it as well, and the tanks keep their fluid. A Calciner that stays in place SHALL keep its buffers, input, fuel and Ignis Perpetuus while unformed and SHALL NOT work until formed again. A broken Calciner block SHALL drop its input, fuel and Ignis Perpetuus. A broken Firebox SHALL drop the Calciner's Ignis Perpetuus.

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

#### Scenario: Calciner with the eternal flame broken
- **WHEN** a player breaks the Calciner block of a formed Calciner holding Ignis Perpetuus
- **THEN** the Ignis Perpetuus drops with the Calciner

#### Scenario: Firebox broken
- **WHEN** a player breaks the Firebox of a formed Calciner holding Ignis Perpetuus
- **THEN** the Firebox and the Ignis Perpetuus drop, and the Calciner holds no Ignis Perpetuus

### Requirement: Matter and fuel go in by hand
On a formed Calciner, using a calcinable item on the Calciner block SHALL put it into the input, using a furnace fuel on the Firebox SHALL put it into the fuel slot unless the Calciner holds Ignis Perpetuus, using Ignis Perpetuus on the Firebox SHALL set it as the eternal flame, using the Calciner with an empty hand while sneaking SHALL give the input back to the player, and using the Firebox with an empty hand while sneaking SHALL give the Ignis Perpetuus back. The Calciner SHALL have no GUI.

#### Scenario: Cobblestone by hand
- **WHEN** a player holding 16 cobblestone uses them on a formed Calciner with an empty input
- **THEN** the 16 cobblestone go into the input

#### Scenario: Take back
- **WHEN** a player sneaks and uses a formed Calciner with an empty hand
- **THEN** the cobblestone in the input goes to the player

#### Scenario: Ignis by hand
- **WHEN** a player uses Ignis Perpetuus on the Firebox of a formed Calciner without one
- **THEN** the Calciner holds the Ignis Perpetuus as its eternal flame

### Requirement: Item ports
A formed Calciner SHALL accept calcinable items through the item capability on every face of the Calciner block and furnace fuel through the item capability on every face of the Firebox, except while it holds Ignis Perpetuus, when the Firebox SHALL take nothing; neither SHALL give items out. It SHALL give Ash, and take nothing, through the item capability on every face of the Ash Pan. The other blocks of the structure SHALL expose no item capability.

#### Scenario: Hopper feeds the mouth
- **WHEN** a hopper points into the front of a formed Calciner's mouth and holds cobblestone
- **THEN** the cobblestone moves into the Calciner's input

#### Scenario: Pipe feeds the fire
- **WHEN** an Item Pipe ends at the back of the Firebox and delivers coal
- **THEN** the coal goes into the fuel slot

#### Scenario: Pipe at the eternal flame
- **WHEN** an Item Pipe ends at the back of the Firebox of a Calciner holding Ignis Perpetuus and offers coal
- **THEN** the Firebox takes no coal

#### Scenario: Hopper under the Ash Pan
- **WHEN** a formed Calciner has calcined cobblestone into 300 mB of Salt and a hopper takes from the Ash Pan
- **THEN** the hopper receives 3 Ash (one Ash for each 100 mB the Purifier makes from one Ash)

### Requirement: Principle ports
A formed Calciner SHALL put the Sulfur of each finished cycle into its own Sulfur tank and the Mercury into its own Mercury tank; no Sulfur or Mercury SHALL stay in the Calciner. The Salt SHALL stay in the Ash Pan as Ash, up to a room counted in Ash (a stack of 64 in the example data), independent of the tanks. The Calciner and every other block of its structure SHALL expose no fluid capability.

#### Scenario: Tanks fill
- **WHEN** a formed Calciner finishes a cycle that gives 100 mB Salt, 40 mB Sulfur and 10 mB Mercury and both tanks have room
- **THEN** the Sulfur tank gains 40 mB Sulfur, the Mercury tank 10 mB Mercury, and the Ash Pan 100 mB of Salt

#### Scenario: Tank full
- **WHEN** the Mercury tank of a formed Calciner is full and the next cycle would make Mercury
- **THEN** the Calciner does not finish the cycle, and no Mercury waits in the Calciner

#### Scenario: A stack of Ash
- **WHEN** a formed Calciner calcines cobblestone until it waits on a full Ash Pan, and the tanks have room
- **THEN** the Ash Pan holds 64 Ash

#### Scenario: Pipe at the Ash Pan
- **WHEN** a Fluid Pipe touches the Ash Pan of a working Calciner
- **THEN** the pipe finds no fluid handler there

#### Scenario: Pipe at the Receiver
- **WHEN** a Fluid Pipe touches the Receiver of a formed Calciner
- **THEN** the pipe finds no fluid handler there

### Requirement: Buckets at the ports
Using an empty hand on the Ash Pan SHALL give the player all the Ash it holds. Using a bucket or another fluid container on the Receiver or any other block of the structure SHALL do nothing; Mercury and Sulfur are bucketed from their tanks.

#### Scenario: Mercury bucket
- **WHEN** a player uses an empty bucket on the Receiver of a working Calciner
- **THEN** the bucket stays empty and the Calciner is unchanged

#### Scenario: Ash by hand
- **WHEN** a player uses the Ash Pan with an empty hand and it holds 500 mB of Salt
- **THEN** the player receives 5 Ash and the Ash Pan is empty

### Requirement: Upkeep at the Firebox
A formed Calciner SHALL report its fuel as upkeep on the Firebox, so a data line touching the Firebox keeps it fuelled.

#### Scenario: Data line fuels
- **WHEN** a Machine Adapter or data line delivers upkeep coal to the Firebox of a formed Calciner
- **THEN** the coal lands in the fuel slot

### Requirement: Calciner in Jade
With Jade installed, looking at any block of a formed Calciner SHALL show the input, the fuel, the progress and the Ash in the Ash Pan out of its room. While the Calciner holds Ignis Perpetuus, Jade SHALL show the eternal flame in place of the fuel. The Fluid Tanks show their own fluid.

#### Scenario: Look at the Receiver
- **WHEN** a player with Jade looks at the Receiver of a Calciner whose Ash Pan holds 1,200 mB of Salt
- **THEN** the tooltip shows 12 / 64 Ash among the Calciner's state

#### Scenario: Eternal flame in Jade
- **WHEN** a player with Jade looks at a formed Calciner holding Ignis Perpetuus
- **THEN** the tooltip shows Ignis Perpetuus as its fire and no fuel line

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
