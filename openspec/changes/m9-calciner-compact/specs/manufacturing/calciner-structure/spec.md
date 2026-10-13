## ADDED Requirements

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

## MODIFIED Requirements

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

## REMOVED Requirements

### Requirement: Calciner layout
**Reason**: The 5 by 5 brick build with tank columns and a spout air cell is replaced by the 3 by 5 stone and deepslate build.
**Migration**: See "Compact Calciner layout". Rebuild existing Calciners; the project is unreleased.
