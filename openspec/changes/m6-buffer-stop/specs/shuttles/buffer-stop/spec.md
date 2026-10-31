## ADDED Requirements

### Requirement: Buffer Stop look
The Buffer Stop SHALL look like a steel buffer stop at the end of a track: a steel sole on the ground, two steel uprights on the lines of the rail heads, stepped steel braces behind them, a beam across the track in the yellow and black dock stripe of the Warehouse Rail, and two steel buffer heads on the front of the beam, one in front of each rail head. In front of the beam it SHALL show a short stub of track with the gauge of the Warehouse Rail: steel heads on sleepers in the wood the stop carries. Every part SHALL be solid geometry in steel textures on every face, and no two faces SHALL flicker against each other. The stop SHALL stay inside its own block.

#### Scenario: Seen from the track
- **WHEN** a player stands on the rail and looks at the stop's front
- **THEN** the rail heads run on into the stop's track stub and end under the beam between the two buffer heads

#### Scenario: Wood of the sleepers
- **WHEN** a Buffer Stop crafted from a Warehouse Rail with spruce sleepers is placed
- **THEN** the sleepers of its track stub show spruce

### Requirement: Buffer Stop is placed by the player
The Buffer Stop SHALL be a block of its own, placed by the player beyond the last rail of a run. It SHALL NOT appear on its own at an open rail end, and it SHALL NOT sit inside a rail block.

#### Scenario: Open rail end stays open
- **WHEN** a player places a run of rails and no Buffer Stop
- **THEN** the last rail ends with its end stub and no stop appears

### Requirement: Buffer Stop faces the rail
The Buffer Stop SHALL turn its front, the side with the buffer heads, to a rail by the rail facing rule, and SHALL face its front to the player when placed while sneaking. Using the Configuration Carrot on it SHALL turn it a quarter clockwise; sneaking with the carrot SHALL dismantle it.

#### Scenario: Placed at the end of a run
- **WHEN** a player looking north places a Buffer Stop with the last rail of a run on its south side
- **THEN** its front faces south, towards the rail

#### Scenario: Turned with the carrot
- **WHEN** a player uses the Configuration Carrot on a Buffer Stop that faces south
- **THEN** it faces west

### Requirement: Buffer Stop stands on a floor
The Buffer Stop SHALL only be placed on a block that can carry a rail standing on it, and SHALL drop itself when that block is removed. It SHALL NOT hold up a rail beside it.

#### Scenario: Floor removed
- **WHEN** the block under a Buffer Stop is broken
- **THEN** the stop drops as an item, keeping its wood

#### Scenario: No anchor for a rail
- **WHEN** a rail in the air touches only a Buffer Stop
- **THEN** the rail is treated as if the stop were not there

### Requirement: Buffer Stop stays outside the network
The Buffer Stop SHALL NOT join a warehouse network, SHALL NOT dock a rail and SHALL NOT be a place a shuttle drives to or through. It SHALL NOT change any route, job or count of the warehouse or the field it stands at.

#### Scenario: Stop at the end of a warehouse rail
- **WHEN** a controller touches a rail, two rails run on from it and a Buffer Stop faces the last one
- **THEN** the warehouse network holds the three rails and nothing at the stop

#### Scenario: Stop at the end of a field
- **WHEN** a Buffer Stop faces the last Farm Rail of a field
- **THEN** the field's rail count and the Field Head's work are the same as without the stop

### Requirement: Buffer Stop is obtainable and dismantled
Three iron ingots over one Warehouse Rail SHALL craft one Buffer Stop that carries the wood of the rail. The stop SHALL drop itself with its wood when broken or dismantled.

#### Scenario: Crafting
- **WHEN** the player places three iron ingots in the top row and a Warehouse Rail with oak sleepers in the middle of the row below
- **THEN** the result is one Buffer Stop with oak sleepers

#### Scenario: Broken
- **WHEN** a player breaks a Buffer Stop with birch sleepers
- **THEN** it drops a Buffer Stop item with birch sleepers
