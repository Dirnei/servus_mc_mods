# Spec Delta

## MODIFIED Requirements

### Requirement: Rails auto connect
A rail SHALL connect to every neighbouring rail, every Rail Elevator segment, every stock holding storage block (crate, shelving rack, warehouse controller), every Inbound Station and the back of every Shuttle Garage on the same level and SHALL render the matching piece: straight, curve, T or cross. The centre SHALL follow the connections: straight along the axis of a straight run or a single connection, a curve when two connected sides meet at a corner, and a `#` of both line pairs when three or four sides connect. Docks SHALL NOT shape the centre: a dock is drawn on its own and the centre follows only the sides that carry rail. Towards a storage block or a station the piece SHALL be a dock, visibly different from the arm towards a rail or segment; towards a garage it SHALL be a plain arm. A Heavy Duty Rack SHALL take a dock only on its front side. Rails SHALL NOT connect up or down. Connections SHALL update when a neighbouring rail, segment, storage block, station or garage is placed, rotated or removed.

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
- **WHEN** a rail runs north to south with a rack on its east side
- **THEN** the rail shows a dock piece towards the rack and its centre stays straight north to south

#### Scenario: Different level
- **WHEN** a rail is placed directly above another rail
- **THEN** the two rails are not connected to each other

#### Scenario: Rail meets a shaft
- **WHEN** a rail is placed beside an elevator segment
- **THEN** the rail shows an arm towards the segment

#### Scenario: Rail docks onto a rack
- **WHEN** a rack is placed beside a rail
- **THEN** the rail shows a dock piece towards the rack that looks different from an arm towards a rail, and the dock disappears when the rack is broken

#### Scenario: Rail behind a heavy duty rack
- **WHEN** a rail runs behind a Heavy Duty Rack
- **THEN** the rail shows no dock towards it, and placing the rack again so that it faces the rail makes the dock appear

#### Scenario: Rail docks onto a station
- **WHEN** a Inbound Station is placed beside a rail, on any of its sides
- **THEN** the rail shows a dock piece towards the station

#### Scenario: Rail runs into a garage
- **WHEN** a garage is placed with its back to a rail
- **THEN** the rail shows a plain arm towards the garage, and turning the garage with the carrot removes it

### Requirement: Storage blocks join through rails
Every rail SHALL be a node of the warehouse network. Crates, shelving racks, warehouse controllers and inbound stations touching a rail on any side SHALL belong to the same network as that rail, and a chain of connected rails SHALL carry membership along its whole length. Rails SHALL link to each other sideways only; a rail directly above another rail does not link to it.

#### Scenario: Two aisles one controller
- **WHEN** a controller touches a rail, the rail runs ten blocks, and racks stand beside the rail at both ends
- **THEN** all racks and the controller form one online warehouse and a counter terminal on the data line lists their contents

#### Scenario: Rail removed
- **WHEN** a rail in the middle of that run is broken
- **THEN** the racks past the gap leave the controller's network and their items leave the index

#### Scenario: Stacked rails
- **WHEN** a rail lies on a block directly above another rail and nothing else joins them
- **THEN** the two rails are in different networks

#### Scenario: Station at the end of a rail
- **WHEN** a controller touches a rail and a Inbound Station stands beside the far end of the rail
- **THEN** the station is in the controller's warehouse and shows the online lamp
