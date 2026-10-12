## MODIFIED Requirements

### Requirement: Rails auto connect
A rail SHALL connect to every neighbouring rail, every Rail Elevator segment, the front of every Heavy Duty Rack, every Warehouse Controller, every Inbound Station, Outbound Station and Packing Station and the back of every Shuttle Garage on the same level and SHALL render the matching piece: straight, curve, T or cross. The centre SHALL follow the connections: straight along the axis of a straight run or a single connection, a curve when two connected sides meet at a corner, and a `#` of both line pairs when three or four sides connect. Docks SHALL NOT shape the centre: a dock is drawn on its own and the centre follows only the sides that carry rail. Towards a rack, a controller or a station the piece SHALL be a dock, visibly different from the arm towards a rail or segment; towards a garage it SHALL be a plain arm. Rails SHALL NOT connect up or down. Connections SHALL update when a neighbouring rail, segment, rack, controller, station or garage is placed, rotated or removed.

#### Scenario: Straight run
- **WHEN** three rails are placed in a line
- **THEN** the middle rail shows a straight piece and the end rails each show one arm

#### Scenario: Crossing
- **WHEN** a rail has rails on all four horizontal sides
- **THEN** it renders a cross whose centre shows both line pairs like a `#`

#### Scenario: East west run
- **WHEN** three rails are placed in a line from east to west
- **THEN** the middle rail's lines run east to west through the centre without crossing lines

#### Scenario: Corner
- **WHEN** a rail has rails on its north and east sides only
- **THEN** its centre shows a curve joining the north arm to the east arm

#### Scenario: Rail runs past a rack
- **WHEN** a rail runs north to south with a rack on its east side facing it
- **THEN** the rail shows a dock piece towards the rack and its centre stays straight north to south

#### Scenario: Different level
- **WHEN** a rail is placed directly above another rail
- **THEN** the two rails are not connected to each other

#### Scenario: Rail meets a shaft
- **WHEN** a rail is placed beside an elevator segment
- **THEN** the rail shows an arm towards the segment

#### Scenario: Rail docks onto a rack
- **WHEN** a rack is placed beside a rail, facing it
- **THEN** the rail shows a dock piece towards the rack that looks different from an arm towards a rail, and the dock disappears when the rack is broken

#### Scenario: Rail behind a heavy duty rack
- **WHEN** a rail runs behind a Heavy Duty Rack
- **THEN** the rail shows no dock towards it, and placing the rack again so that it faces the rail makes the dock appear

#### Scenario: Rail docks onto a station
- **WHEN** an Inbound Station is placed beside a rail, on any of its sides
- **THEN** the rail shows a dock piece towards the station

#### Scenario: Rail runs into a garage
- **WHEN** a garage is placed with its back to a rail
- **THEN** the rail shows a plain arm towards the garage, and turning the garage with the carrot removes it

### Requirement: Storage blocks join through rails
Every rail SHALL be a node of the warehouse network. Heavy Duty Racks facing a rail, and warehouse controllers and stations touching a rail on any side, SHALL belong to the same network as that rail, and a chain of connected rails SHALL carry membership along its whole length. Rails SHALL link to each other sideways only; a rail directly above another rail does not link to it.

#### Scenario: Two aisles one controller
- **WHEN** a controller touches a rail, the rail runs ten blocks, and racks face the rail at both ends
- **THEN** all racks and the controller form one online warehouse and a storage terminal on the data line lists their contents

#### Scenario: Rail removed
- **WHEN** a rail in the middle of that run is broken
- **THEN** the racks past the gap leave the controller's network and their items leave the index

#### Scenario: Stacked rails
- **WHEN** a rail lies on a block directly above another rail and nothing else joins them
- **THEN** the two rails are in different networks

#### Scenario: Station at the end of a rail
- **WHEN** a controller touches a rail and an Inbound Station stands beside the far end of the rail
- **THEN** the station is in the controller's warehouse and shows the online lamp

## ADDED Requirements

### Requirement: Only rails join blocks to a warehouse
Every block that shuttles serve SHALL join a warehouse only through a rail on one of the faces where it takes rails, and SHALL NOT join or pass on a warehouse by touching controllers, other storage blocks or each other. The faces are: a Heavy Duty Rack's front (and the bays of its own section), every side of an Inbound Station, Outbound Station and Packing Station, and the back of a Shuttle Garage. The Warehouse Controller SHALL link to touching rails on any side and to a touching controller; no other block SHALL join a warehouse by touching it.

#### Scenario: Station pressed against the controller
- **WHEN** an Inbound Station touches the controller of an online warehouse and no rail touches the station
- **THEN** the station is not part of the warehouse

#### Scenario: Chest touching the controller
- **WHEN** a chest touches the controller
- **THEN** the chest is not part of the warehouse and its items are not listed

### Requirement: Rails stand or hang
The Warehouse Rail SHALL be a flat block in the bottom of its block space, one block wide, with a selection shape a few pixels high so players walk over it. A rail with a block with a solid top under it SHALL be standing. A rail without one SHALL be hung when one of its horizontal neighbours is an anchor and free otherwise. An anchor SHALL be the front side of a Heavy Duty Rack, any side of a Rail Elevator segment or a standing rail; controllers, terminals, stations and garages SHALL NOT be anchors. Standing and hung rails SHALL hold on their own. A rail SHALL be placeable on any block with a solid top, and in open air only when one of its horizontal neighbours is an anchor or another rail. Hung and free rails SHALL look the same as standing rails.

#### Scenario: Walk over
- **WHEN** a player walks along an aisle paved with rails
- **THEN** the player is not blocked and does not climb

#### Scenario: Placement on a floor
- **WHEN** a player places a rail on a stone floor
- **THEN** the rail is placed with its flat side on the floor

#### Scenario: Hung on a rack front
- **WHEN** a player clicks the front of a Heavy Duty Rack on the second row of a stack, with air below the space in front of it
- **THEN** a rail is placed in front of the rack and stays there

#### Scenario: Row along an upper rack row
- **WHEN** five Heavy Duty Racks stand in a row on the second level and the player hangs a rail in front of each
- **THEN** all five rails stay and link to each other

#### Scenario: Open air
- **WHEN** a player tries to place a rail in the air with no rail, rack front or elevator segment beside it
- **THEN** no rail is placed

#### Scenario: Rack side is no anchor
- **WHEN** a player tries to place a rail in the air beside the side of a Heavy Duty Rack with nothing else beside it
- **THEN** no rail is placed

#### Scenario: Controller is no anchor
- **WHEN** a player tries to place a rail in the air beside a Warehouse Controller with nothing else beside it
- **THEN** no rail is placed

## REMOVED Requirements

### Requirement: Shuttle served blocks join only through rails
**Reason**: Crates and foreign inventories linked by touch; they are gone, and only the controller still links to touching rails.
**Migration**: See "Only rails join blocks to a warehouse".

### Requirement: Rail stands or hangs
**Reason**: The anchor rules named crates, which are removed.
**Migration**: See "Rails stand or hang"; the anchors are unchanged otherwise.
