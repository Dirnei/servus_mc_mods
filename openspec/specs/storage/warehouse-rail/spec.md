# storage/warehouse-rail Specification

## Purpose

The Warehouse Rail is the wiring of the mid game warehouse: a floor rail that links storage blocks into one network and later carries shuttles.

## Requirements

### Requirement: Rail stands or hangs
The Warehouse Rail SHALL be a flat block in the bottom of its block space, one block wide, with a selection shape a few pixels high so players walk over it. A rail with a block with a solid top under it SHALL be standing. A rail without one SHALL be hung when one of its horizontal neighbours is an anchor and free otherwise. An anchor SHALL be the front side of a Heavy Duty Rack, any side of a Rail Elevator segment or a standing rail; crates, controllers, terminals, stations and garages SHALL NOT be anchors. Standing and hung rails SHALL hold on their own. A rail SHALL be placeable on any block with a solid top, and in open air only when one of its horizontal neighbours is an anchor or another rail. Hung and free rails SHALL look the same as standing rails.

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

#### Scenario: Crate is no anchor
- **WHEN** a player tries to place a rail in the air beside a crate with nothing else beside it
- **THEN** no rail is placed

### Requirement: Free rails bridge short gaps
Free rails that link to each other SHALL form a gap. The span of a gap SHALL be its number of free rails plus the number of hung rails linked to them. A gap SHALL hold when its span is at most the maximum span (for example 3, set in the warehouse datapack config) and it links to at least two hung rails. A gap that does not hold SHALL collapse: its free rails and the hung rails linked to them drop as items. A gap SHALL be checked again when a rail is placed into it or beside it, when a rail, anchor or floor block it relies on is removed, and when one of its rails loses its floor. Gaps SHALL NOT be checked every tick.

#### Scenario: Bridge between two elevators
- **WHEN** two elevator shafts stand with a three block gap between them and the player fills it with three rails, one at each shaft first and the middle one last
- **THEN** all three rails stay and the shafts are linked through them

#### Scenario: Bridge between two racks
- **WHEN** two Heavy Duty Racks face each other in the air across a three block gap and the player fills it with three rails, ends first
- **THEN** all three rails stay

#### Scenario: Fourth rail collapses the bridge
- **WHEN** a three rail bridge spans two racks and the player places a fourth rail beside its middle rail
- **THEN** all four rails drop as items

#### Scenario: Four block gap cannot be bridged
- **WHEN** two racks face each other across a four block gap with a rail hung on each front and the player places a rail beside one of them
- **THEN** that rail and the hung rail it touches drop as items

#### Scenario: Overhang collapses
- **WHEN** a rail hangs on a rack front and the player places a free rail beside it that touches nothing else
- **THEN** both rails drop as items

#### Scenario: Bridge between floors
- **WHEN** two standing rails end at a drop three blocks wide and the player hangs a rail off each and then fills the middle
- **THEN** the three rails stay

#### Scenario: One rail off a floor
- **WHEN** a standing rail ends at a drop and the player places one rail beyond it
- **THEN** the rail stays, hung off the standing rail

#### Scenario: Rack row to elevator
- **WHEN** a row of rails hangs along an upper rack row and one free rail links its last rail to a rail hung on an elevator segment
- **THEN** every rail stays

#### Scenario: Anchor removed
- **WHEN** a three rail bridge spans two racks and one rack is broken
- **THEN** all three rails drop as items

#### Scenario: Floor removed under a bridge end
- **WHEN** a rail hung on a rack front links through a free rail to a rail hung off a standing rail, and the block under the standing rail is broken
- **THEN** all four rails drop as items

### Requirement: Rails auto connect
A rail SHALL connect to every neighbouring rail, every Rail Elevator segment, every stock holding storage block (crate, shelving rack, warehouse controller, counter terminal), every Inbound Station and the back of every Shuttle Garage on the same level and SHALL render the matching piece: straight, curve, T or cross. The centre SHALL follow the connections: straight along the axis of a straight run or a single connection, a curve when two connected sides meet at a corner, and a `#` of both line pairs when three or four sides connect. Docks SHALL NOT shape the centre: a dock is drawn on its own and the centre follows only the sides that carry rail. Towards a storage block or a station the piece SHALL be a dock, visibly different from the arm towards a rail or segment; towards a garage it SHALL be a plain arm. A Heavy Duty Rack SHALL take a dock only on its front side. Rails SHALL NOT connect up or down. Connections SHALL update when a neighbouring rail, segment, storage block, station or garage is placed, rotated or removed.

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
Every rail SHALL be a node of the warehouse network. Crates, shelving racks, warehouse controllers, counter terminals and inbound stations touching a rail on any side SHALL belong to the same network as that rail, and a chain of connected rails SHALL carry membership along its whole length. Rails SHALL link to each other sideways only; a rail directly above another rail does not link to it.

#### Scenario: Two aisles one controller
- **WHEN** a controller touches a rail, the rail runs ten blocks, and racks stand beside the rail at both ends
- **THEN** all racks and the controller form one online warehouse and the counter terminal lists their contents

#### Scenario: Rail removed
- **WHEN** a rail in the middle of that run is broken
- **THEN** the racks past the gap leave the controller's network and their items leave the index

#### Scenario: Stacked rails
- **WHEN** a rail lies on a block directly above another rail and nothing else joins them
- **THEN** the two rails are in different networks

#### Scenario: Station at the end of a rail
- **WHEN** a controller touches a rail and a Inbound Station stands beside the far end of the rail
- **THEN** the station is in the controller's warehouse and shows the online lamp

### Requirement: Rails hold nothing
A rail SHALL NOT be indexed as a source and SHALL NOT make the inventories beside it part of the warehouse. Only storage blocks pull neighbouring inventories in; a Inbound Station does not.

#### Scenario: Chest beside a rail
- **WHEN** a chest with bread stands beside a rail of an online warehouse and touches no storage block
- **THEN** the bread is not in the index

#### Scenario: Chest beside a rack
- **WHEN** that chest also touches a rack of the same warehouse
- **THEN** the bread is in the index

#### Scenario: Chest beside a station
- **WHEN** a chest with bread touches a Inbound Station and nothing else of the warehouse
- **THEN** the bread is not in the index

### Requirement: Rail is obtainable and dismantled
The rail SHALL be craftable from vanilla materials in a batch, SHALL drop itself when broken, and SHALL be dismantled by sneaking with the Configuration Carrot. When the block under a rail is removed the rail SHALL become hung or free and SHALL drop itself only when it collapses with its gap.

#### Scenario: Crafting
- **WHEN** the player places iron ingots and a stick in the rail pattern (for example two rows of three ingots around a stick)
- **THEN** the result is several rails (for example eight)

#### Scenario: Sneak dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a rail
- **THEN** the rail drops as an item

#### Scenario: Floor removed
- **WHEN** the block under a rail is broken and the rail touches no anchor and no other rail
- **THEN** the rail drops as an item

#### Scenario: Floor removed beside a rack
- **WHEN** the block under a rail in front of a Heavy Duty Rack is broken and no other rail touches it
- **THEN** the rail stays, hung on the rack front
