## ADDED Requirements

### Requirement: Segment placed on a rail
Using an elevator item on a Warehouse Rail, sneaking or not, SHALL put a segment in that rail's place when the segment can stand there. The segment SHALL remember the rail it replaced, with its wood, also when the world is saved. When that segment is removed for any reason (broken, dismantled with the Configuration Carrot, or dropped for lack of support), the remembered rail SHALL be put back in its place, unless another block already fills the space. A segment placed anywhere else SHALL remember no rail. Farm Rails SHALL NOT be replaced this way.

#### Scenario: Placed on a rail
- **WHEN** a player uses an elevator on a Warehouse Rail in the middle of a straight run
- **THEN** a segment stands where the rail was, the rails on both sides connect to it and no rail item drops

#### Scenario: Rail comes back
- **WHEN** that segment is broken
- **THEN** the segment drops as an item and a Warehouse Rail with the same wood lies where it stands, connected to the rails beside it

#### Scenario: Shaft on a rail falls
- **WHEN** the lowest segment of a three segment shaft that was placed on a rail is broken
- **THEN** all three segments drop and the rail is back on the floor

#### Scenario: Placed on bare ground
- **WHEN** a segment placed on bare stone is broken
- **THEN** it drops as an item and nothing is left in its place

#### Scenario: Building up from a rail segment
- **WHEN** a player clicks a segment that replaced a rail while holding an elevator
- **THEN** a new segment is placed on top of the shaft as before

#### Scenario: Farm rail stays
- **WHEN** a player uses an elevator on a Farm Rail
- **THEN** the Farm Rail stays and the segment is placed as on any other block, if it can stand there

### Requirement: Segments show their wood
Every elevator segment SHALL have a wood, as Warehouse Rails do, and the sleepers of every rail the shaft draws at that segment (its floor rail, its deck and the car's rail while the car stands there) SHALL show it. A segment placed on a Warehouse Rail SHALL take that rail's wood. An elevator crafted from a rail SHALL carry the wood of that rail. A broken or dismantled segment SHALL drop an elevator item that keeps its wood, the item SHALL show it, and placing the item SHALL give the segment that wood. Items of different woods SHALL NOT stack. A segment without a wood SHALL show oak.

#### Scenario: Placed on a spruce rail
- **WHEN** a player uses an elevator on a Warehouse Rail with spruce sleepers
- **THEN** the segment's floor rail has spruce sleepers

#### Scenario: Crafted from a birch rail
- **WHEN** a player crafts elevators from iron ingots and a Warehouse Rail with birch sleepers
- **THEN** the elevators carry birch, show birch sleepers as items and place segments with birch sleepers

#### Scenario: Wood on the drop
- **WHEN** a segment with dark oak wood is broken
- **THEN** it drops an elevator item with dark oak wood that does not stack with an oak elevator item

#### Scenario: Shaft of mixed woods
- **WHEN** a shaft has a spruce segment at the bottom and a birch segment above it, each with a rail beside it
- **THEN** the bottom floor rail shows spruce sleepers and the upper deck rail shows birch sleepers
