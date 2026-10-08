# Spec Delta

## ADDED Requirements

### Requirement: Formed cube look
A formed CPU SHALL show each side of the cube as one 3x3 face: a continuous iron frame along the outer edges and corners of the cube and teal data lights on the inner tiles. Every CPU block SHALL know its position in the cube while formed and forget it when unformed; unformed blocks SHALL show a riveted casing.

#### Scenario: Corner block
- **WHEN** a cube forms
- **THEN** the block at a corner of the cube shows frame on its outer edges and the block in the middle of a side shows only lights

#### Scenario: Unform
- **WHEN** a block of a formed cube is broken
- **THEN** the remaining blocks show the casing again

### Requirement: Active lights
The lights of a formed CPU SHALL run while the CPU has orders and SHALL glow slowly while it has none. The change SHALL follow order changes, not a per tick check.

#### Scenario: Order starts
- **WHEN** a formed CPU without orders takes an order
- **THEN** its blocks switch to running lights, and back to the slow glow when the order is done
