# Spec Delta

## MODIFIED Requirements

### Requirement: Formed Calciner looks like one machine
A formed Calciner SHALL be drawn as one model covering the whole structure instead of its single blocks, with the Alembic's spout reaching the Receiver and the Copper Duct joining Hood and tank. The model SHALL show the matter being calcined in the crucible of the mouth, the fire glowing and the Firebox giving light while it burns, smoke rising from the Hood while it works, the ash heap in the Ash Pan growing with the Ash it holds and the Mercury in the Receiver by fill. The Fluid Tanks show their own level. The blocks of a formed Calciner SHALL keep their collision and SHALL be mined like bricks.

#### Scenario: Burning
- **WHEN** a formed Calciner burns fuel and calcines
- **THEN** the fire glows, the Firebox lights its surroundings and smoke rises from the Hood

#### Scenario: Matter in the crucible
- **WHEN** cobblestone lies in the input of a formed Calciner
- **THEN** a small cobblestone stands in the crucible of the mouth, and it disappears when the input is empty

#### Scenario: Ash heap
- **WHEN** the Ash Pan of a formed Calciner goes from empty to half its room in Ash
- **THEN** a grey ash heap in the Ash Pan rises from nothing to half its height

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
A formed Calciner SHALL offer only Mercury through the fluid capability on the faces of the Receiver and only Sulfur on the faces of the lowest Copper Duct, and SHALL push each into touching fluid handlers outside the furnace: the Mercury down into the Mercury tank, the Sulfur down into the Sulfur tank. The Salt stays in the Ash Pan as Ash. It SHALL never accept fluid. No other block of its own SHALL expose a fluid capability; pipes take Mercury and Sulfur from the tanks.

#### Scenario: No Salt by pipe
- **WHEN** a Fluid Pipe touches the Ash Pan of a working Calciner
- **THEN** the pipe finds no fluid handler there

#### Scenario: Tanks fill
- **WHEN** a formed Calciner holds 400 mB Sulfur and 300 mB Mercury
- **THEN** the Sulfur tank column receives the 400 mB Sulfur and the Mercury tank column the 300 mB Mercury

### Requirement: Buckets at the ports
Using an empty bucket or another empty fluid container on the Receiver of a formed Calciner SHALL fill it with Mercury when the Calciner holds enough. Using a filled container SHALL do nothing. Using an empty hand on the Ash Pan SHALL give the player all the Ash it holds. Mercury and Sulfur are usually bucketed from their tanks.

#### Scenario: Mercury bucket
- **WHEN** a player uses an empty bucket on the Receiver and the Mercury buffer holds 1,200 mB
- **THEN** the player holds a Mercury bucket and 200 mB Mercury remain

#### Scenario: Ash by hand
- **WHEN** a player uses the Ash Pan with an empty hand and it holds 500 mB of Salt
- **THEN** the player receives 5 Ash and the Ash Pan is empty
