# Spec Delta

## MODIFIED Requirements

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

### Requirement: Calciner in Jade
With Jade installed, looking at any block of a formed Calciner SHALL show the input, the fuel, the progress and the Ash in the Ash Pan out of its room. While the Calciner holds Ignis Perpetuus, Jade SHALL show the eternal flame in place of the fuel. The Fluid Tanks show their own fluid.

#### Scenario: Look at the Receiver
- **WHEN** a player with Jade looks at the Receiver of a Calciner whose Ash Pan holds 1,200 mB of Salt
- **THEN** the tooltip shows 12 / 64 Ash among the Calciner's state

#### Scenario: Eternal flame in Jade
- **WHEN** a player with Jade looks at a formed Calciner holding Ignis Perpetuus
- **THEN** the tooltip shows Ignis Perpetuus as its fire and no fuel line
