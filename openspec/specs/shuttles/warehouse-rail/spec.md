# shuttles/warehouse-rail Specification

## Purpose

The Warehouse Rail is the wiring of the mid game warehouse: a floor rail that links storage blocks into one network and later carries shuttles.

## Requirements

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
- **WHEN** the player places iron ingots and planks in the rail pattern (for example two columns of three ingots with a plank between them)
- **THEN** the result is several rails (for example sixteen) carrying the wood of the planks

#### Scenario: Sneak dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a rail
- **THEN** the rail drops as an item

#### Scenario: Floor removed
- **WHEN** the block under a rail is broken and the rail touches no anchor and no other rail
- **THEN** the rail drops as an item

#### Scenario: Floor removed beside a rack
- **WHEN** the block under a rail in front of a Heavy Duty Rack is broken and no other rail touches it
- **THEN** the rail stays, hung on the rack front

### Requirement: Sneak placement extends a rail run
The rail item SHALL use the placement assist with the run axis. Its run SHALL be the rails in a straight line from the clicked rail, and its run connections the rails and elevator segments beside it. Otherwise a top or bottom face hit SHALL pick the horizontal side whose edge is nearest to the hit point, and a side face hit that face. The placed rail SHALL follow the normal stand, hang and gap rules, with no special handling for rails placed this way.

#### Scenario: Quick straight on a floor
- **WHEN** four rails lie on a floor from west to east and a sneaking player clicks the top of the first rail near its east edge while holding a rail
- **THEN** a rail is placed east of the fourth rail

#### Scenario: Direction from the nearest edge
- **WHEN** a sneaking player clicks the top of a lone rail near its north edge
- **THEN** the ghost and the placed rail are north of it

#### Scenario: Middle of a straight run
- **WHEN** rails lie on a floor from west to east and a sneaking player clicks the top of the second rail near its north edge and east of its centre
- **THEN** a rail is placed past the east end of the line, not north of the clicked rail

#### Scenario: Side face hit
- **WHEN** a sneaking player clicks the south side face of a lone rail
- **THEN** the rail is placed past the run to the south

#### Scenario: Run passes a junction
- **WHEN** a straight line of rails eastwards has a branch to the north at its second rail and a sneaking player clicks the first rail towards the east
- **THEN** the walk passes the junction and the rail is placed past the last rail of the line

#### Scenario: Bridge by extending both ends
- **WHEN** two Heavy Duty Racks face each other across a three block gap, a rail hangs on each front, and a sneaking player clicks one of them towards the other
- **THEN** the rail is placed in the middle of the gap and all three rails stay

#### Scenario: Overhang still collapses
- **WHEN** a rail hangs on a rack front with nothing else around and a sneaking player clicks it away from the rack
- **THEN** a rail is placed beyond it and both rails drop as items, as they would when placed by hand

#### Scenario: Normal click still turns
- **WHEN** a player who is not sneaking clicks the north side face of a rail while holding a rail
- **THEN** a rail is placed against that face as before

### Requirement: Rail track look
A rail SHALL look like a wide gauge track: two raised rail heads, each 2 pixels wide, at 3..5 and 11..13 pixels across the travel direction and one pixel above the sleepers, and four separate sleepers per block, each 2 pixels long along the travel direction with gaps between them and 14 pixels wide, so they stick out past the heads. The Warehouse Rail's heads SHALL be light steel. The whole rail SHALL stay at most 2 pixels high.

#### Scenario: Straight run from above
- **WHEN** a player looks down on three rails in a line
- **THEN** two steel heads run unbroken along the whole run and the ground shows between the sleepers

#### Scenario: Shuttle on the rail
- **WHEN** a shuttle drives along a straight rail
- **THEN** its wheels roll on the two heads

### Requirement: Rail pieces follow the gauge
Every piece of a rail SHALL keep the gauge of the straight track: a straight run, the end of a run that stops at the far side of the centre, a curve whose heads are stepped arcs from one side's heads to the other side's heads with sleepers along the arc, a `#` crossing where both pairs of heads cross over a sleeper plate, and a dock piece that ends the heads at a striped buffer beam as wide as the sleepers. A rail with no rail beside it SHALL show a short piece of track along the axis of its dock, if any.

#### Scenario: Curve
- **WHEN** a rail has rails on its north and east sides only
- **THEN** both heads bend in a stepped arc from the north edge to the east edge and meet the heads of both neighbours without a step

#### Scenario: Crossing
- **WHEN** a rail has rails on all four sides
- **THEN** both pairs of heads run across the block over a sleeper plate

#### Scenario: Dock
- **WHEN** a rail runs north into an Inbound Station
- **THEN** the heads end at a striped buffer beam in front of the station

### Requirement: Rail sleepers show the recipe's wood
The rail recipe SHALL take planks, and the crafted rails SHALL carry the wood of those planks. A placed rail SHALL show its sleepers in that wood and SHALL drop a rail carrying it again when broken or dismantled. A rail without a wood SHALL show oak. The item SHALL show a short piece of track with sleepers in its wood, and its tooltip SHALL name the wood. Rails of different woods do not stack.

#### Scenario: Spruce rails
- **WHEN** a player crafts rails with spruce planks and places them
- **THEN** the sleepers are spruce and a broken rail drops a spruce rail

#### Scenario: Sneak placement keeps the wood
- **WHEN** a player sneak places a run of birch rails
- **THEN** every rail of the run shows birch sleepers

### Requirement: Only rails join blocks to a warehouse
Every block that shuttles serve SHALL join a warehouse only through a rail on one of the faces where it takes rails, and SHALL NOT join or pass on a warehouse by touching controllers, other storage blocks or each other. The faces are: a Heavy Duty Rack's front (and the bays of its own section), every side of an Inbound Station, Outbound Station and Packing Station, the back of a Shuttle Garage, and the inner faces of a Fluid Gantry's legs toward the rail it stands over. The Warehouse Controller SHALL link to touching rails on any side and to a touching controller; no other block SHALL join a warehouse by touching it.

#### Scenario: Station pressed against the controller
- **WHEN** an Inbound Station touches the controller of an online warehouse and no rail touches the station
- **THEN** the station is not part of the warehouse

#### Scenario: Chest touching the controller
- **WHEN** a chest touches the controller
- **THEN** the chest is not part of the warehouse and its items are not listed

#### Scenario: Gantry over a rail
- **WHEN** a Fluid Gantry stands over a rail of a warehouse and its outer leg face touches another warehouse's rail
- **THEN** the gantry belongs only to the warehouse of the rail between its legs

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
