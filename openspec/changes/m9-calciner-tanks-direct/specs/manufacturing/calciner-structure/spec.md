# Spec Delta

## MODIFIED Requirements

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

### Requirement: Calciner in Jade
With Jade installed, looking at any block of a formed Calciner SHALL show the input, the fuel, the progress and the Ash in the Ash Pan out of its room. The Fluid Tanks show their own fluid.

#### Scenario: Look at the Receiver
- **WHEN** a player with Jade looks at the Receiver of a Calciner whose Ash Pan holds 1,200 mB of Salt
- **THEN** the tooltip shows 12 / 64 Ash among the Calciner's state
