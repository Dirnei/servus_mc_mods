# storage/rail-elevator Specification

## Purpose

The Rail Elevator is a shaft of stacked segments that links Warehouse Rails on different floors into one network and later carries shuttles between them.

## Requirements

### Requirement: Shaft segment block
The Rail Elevator SHALL be a full height block that renders four corner posts with an open middle. Segments SHALL stack on top of each other to form a shaft of any height and SHALL render as one continuous frame with the posts lining up. A segment with no Warehouse Rail on any of its four sides SHALL render only its posts. A segment with at least one rail beside it SHALL render a deck as its floor (see "Rail floors are decks"). The top segment of a shaft SHALL render a closed head.

#### Scenario: Three segments
- **WHEN** three elevator segments are placed on top of each other
- **THEN** they look like one frame from floor to top with the posts lining up

#### Scenario: Plain level
- **WHEN** a segment in the middle of a shaft has no rail on any side
- **THEN** it shows only its four corner posts and no rail or plate floats in its middle

#### Scenario: Rail through the middle
- **WHEN** rails lie on the west and east sides of a segment and the car is elsewhere
- **THEN** the segment shows a deck spanning between its posts with a rail from its west edge through its centre to its east edge

#### Scenario: Closed top
- **WHEN** a shaft is three segments high
- **THEN** the top segment shows a head with a motor beam across the top of its posts, and the lower two do not

#### Scenario: Walk through
- **WHEN** a player walks along a rail into a ground floor segment
- **THEN** the player passes into the middle of the segment and stands there on the rail level

### Requirement: Segments connect vertically and to rails
An elevator segment SHALL connect to elevator segments directly above and below it, and to Warehouse Rails on its four horizontal sides, rendering a rail arm towards each connected rail. Segments SHALL NOT connect sideways to other segments.

#### Scenario: Rail into shaft
- **WHEN** a rail lies beside a segment
- **THEN** the segment shows a rail arm towards that rail and the rail shows an arm towards the segment

#### Scenario: Side by side shafts
- **WHEN** two segments stand beside each other on the same level
- **THEN** they show no connection to each other

### Requirement: Rail levels join through a shaft
Every elevator segment SHALL be a node of the warehouse network. Segments SHALL link to the segments directly above and below them and to rails beside them, so rails on different floors linked by a shaft form one warehouse. A storage block touching a segment SHALL NOT join the warehouse through that segment; only rails and direct contact between storage blocks carry membership.

#### Scenario: Two floors one controller
- **WHEN** a crate stands beside a rail on the ground floor, the rail meets a shaft, and three segments up another rail leads to a controller and a terminal
- **THEN** the warehouse is online and the terminal lists the crate's contents

#### Scenario: Broken segment
- **WHEN** the middle segment of that shaft is broken
- **THEN** the ground floor leaves the controller's network and the crate's items leave the index

#### Scenario: Crate against the shaft
- **WHEN** a crate touches a segment of an online warehouse and no rail or storage block of it
- **THEN** the crate is not part of the warehouse, its items are not listed and its lamp stays dark

### Requirement: Segments hold nothing
An elevator segment SHALL NOT be indexed as a source and SHALL NOT make the inventories beside it part of the warehouse.

#### Scenario: Chest beside a shaft
- **WHEN** a chest with bread touches only elevator segments of an online warehouse
- **THEN** the bread is not in the index

### Requirement: Elevator is obtainable and dismantled
The elevator segment SHALL be craftable from vanilla materials and a Warehouse Rail in a batch, SHALL drop itself when broken, and SHALL be dismantled by sneaking with the Configuration Carrot.

#### Scenario: Crafting
- **WHEN** the player places iron ingots around a rail in the elevator pattern (for example a rail in the middle of a ring of six ingots)
- **THEN** the result is several segments (for example four)

#### Scenario: Sneak dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a segment
- **THEN** the segment drops as an item, and the segments above it lose their support and drop too

### Requirement: Segments need support
An elevator segment SHALL only be placed on a block with a solid top or on another segment. A segment whose support is removed SHALL drop as an item.

#### Scenario: Placed in the air
- **WHEN** the player tries to place a segment against a wall with nothing below it
- **THEN** the segment is not placed

#### Scenario: Floor removed
- **WHEN** the block under the lowest segment of a three segment shaft is broken
- **THEN** all three segments drop as items

### Requirement: Players pass and climb through segments
An elevator segment SHALL NOT block players or other entities that are inside it or walk into it from the side. It SHALL still show its outline and be breakable when targeted. A player inside a segment SHALL climb it like a ladder: holding jump moves up, and otherwise the player slides down slowly without fall damage. Sneaking SHALL let the player sink down through the shaft instead of holding them in place.

#### Scenario: Walk past the posts
- **WHEN** a player walks along a rail into a ground floor segment and keeps walking
- **THEN** the player passes through the segment, posts included, without stopping

#### Scenario: Climb up
- **WHEN** a player stands in the lowest segment of a three segment shaft and holds jump
- **THEN** the player rises through the shaft and can step out onto a rail beside the top segment

#### Scenario: Step into an upper floor shaft
- **WHEN** a player walks off an upper floor rail into the shaft and presses nothing
- **THEN** the player lands on the top of the segment below that floor and takes no fall damage

#### Scenario: Sneak down
- **WHEN** a player stands on a level of a shaft and holds sneak
- **THEN** the player sinks down through the segments, one level after another, while sneak is held

#### Scenario: Break the bottom
- **WHEN** a player breaks the lowest segment of a shaft
- **THEN** that segment and every segment above it drop as items

### Requirement: Rail floors are decks
A segment with at least one rail beside it SHALL have a deck that covers the shaft floor. Its rail SHALL lie at the same height as the Warehouse Rail outside, join every connected rail and look like an outside rail with the same connections: straight, curve or `#` crossing, with the lines of every arm running along that arm; the deck plate under it SHALL sit just below the segment. The deck SHALL fold away only on edges without a rail, so a folded deck never stands in the way of a shuttle: two flaps on the two free opposite edges, one flap on a free edge when no opposite pair is free, and four quarter plates pulled into the corner posts when rails lie on all four sides. The lowest segment of a shaft SHALL have no deck: its rail lies on the floor below like an outside rail and is covered by the car between the posts while the car stands there; the rail outside the posts stays, so it meets the car's rail without a gap.

#### Scenario: Straight run
- **WHEN** rails lie west and east of a segment
- **THEN** the deck hangs on axles along the north and south edges and its rail joins both outside rails at the same height

#### Scenario: Corner
- **WHEN** rails lie north and east of a segment
- **THEN** the deck is one flap hinged on a free edge (south or west) and its rail turns from the north edge to the east edge

#### Scenario: Dead end entry
- **WHEN** a single rail lies south of a segment
- **THEN** the deck hangs on axles along the west and east edges and its rail runs from the south edge to the centre

#### Scenario: Ground floor
- **WHEN** rails lie west and east of the lowest segment of a shaft and the car is up
- **THEN** a plain rail lies on the floor through the segment with no flaps and nothing flickers against the floor, its lines running west to east without a crossing

#### Scenario: Car parked on the ground floor
- **WHEN** rails lie west and east of the lowest segment and the car stands there
- **THEN** the car's rail and the floor rail outside the posts form one unbroken rail from the west edge to the east edge

#### Scenario: Crossing
- **WHEN** rails lie on all four sides of a segment and the car comes to that floor
- **THEN** the deck splits into four quarters that pull back into the corner posts and leave all four rail entries clear

### Requirement: One lift car per shaft
Every shaft SHALL have exactly one lift car. The car SHALL be drawn as a platform at rail height with a sleeve around each corner post, and SHALL move only along its shaft at `lift_speed` (in `data/<ns>/storage/warehouse.json`, for example 0.1 blocks per tick). The car SHALL stay at the last floor it served and SHALL start at the lowest segment when the shaft is built or the world is loaded.

#### Scenario: New shaft
- **WHEN** a player builds a shaft of four segments
- **THEN** the car stands in the lowest segment

#### Scenario: Ride time
- **WHEN** the car moves three segments up at 0.1 blocks per tick
- **THEN** it reaches the target floor after about 30 ticks

#### Scenario: Parks where it stopped
- **WHEN** the car has carried a shuttle to the top floor and no shuttle calls it
- **THEN** it stays at the top floor

#### Scenario: Shaft grows
- **WHEN** a segment is added on top of a shaft
- **THEN** the shaft still has one car, at the floor where it was

### Requirement: The car serves calls in order
A shuttle SHALL call the car to its floor when it needs to change level, and the car SHALL serve calls in the order they were made. The car SHALL carry one shuttle per trip: a shuttle boards at its floor, the car takes it to the floor it leaves the shaft at, the shuttle drives off, and then the car serves the next call.

#### Scenario: Two callers
- **WHEN** a shuttle on the ground floor calls the car and then a shuttle on the top floor calls it, while the car is in the middle
- **THEN** the car goes down first, carries the first shuttle to its floor, then goes to the top floor for the second

#### Scenario: Car already there
- **WHEN** a shuttle arrives at a shaft whose car stands at its floor and no other call is waiting
- **THEN** it boards at once

### Requirement: Decks fold away for the car
A deck's flaps SHALL fold up against the shaft edges while the car is at that floor or moving within about one segment of it, and SHALL fold down again when the car is farther away. While a deck is folded, the car's platform SHALL carry the rail at the floor where the car stands. A deck's position SHALL depend only on where the car is.

#### Scenario: Car passes a floor
- **WHEN** the car moves up through a middle floor that has rails
- **THEN** that floor's flaps fold up before the car reaches it and fold down after it has passed

#### Scenario: Parked car
- **WHEN** the car stands at the ground floor and the floor above has rails
- **THEN** the ground floor flaps are up with the car's rail joining the outside rails, and the floor above shows its deck down

### Requirement: Every player sees the car where it is
The car's position and motion SHALL be sent to every player near the shaft, and its shuttle and cargo SHALL be drawn on the car while it rides. A player who arrives later SHALL see the car at its current floor.

#### Scenario: Watching a ride
- **WHEN** two players watch the car carry a shuttle with cobblestone up a shaft
- **THEN** both see the car, the shuttle and the cobblestone rise together and the decks fold as it passes

#### Scenario: Joining later
- **WHEN** a player comes into range after the car parked at the top floor
- **THEN** that player sees the car at the top floor

### Requirement: Segments can be stood on
An entity whose feet are at or above the top of a segment SHALL stand on that segment, like on vanilla scaffolding, unless it is descending (a sneaking player). The walkable part SHALL be a thin plate at the top of the segment, so every level of a shaft can be stood on, while an entity inside the segment or entering it from the side is not blocked.

#### Scenario: Stand on a level
- **WHEN** a player stands on top of the second segment of a shaft and presses nothing
- **THEN** the player stays standing at that level

#### Scenario: Descending passes
- **WHEN** the same player holds sneak
- **THEN** the segment does not hold the player and they sink into it

### Requirement: Clicks build a shaft up
The elevator item SHALL use the placement assist for sneak clicks: a sneak click on any face of a segment SHALL walk up the shaft and place the new segment on top. A click without sneaking with an elevator on any face of a segment SHALL place the new segment on the first free spot above the shaft, walking up through every segment of the column, like scaffolding. The walk SHALL have no reach limit. When the first block above the shaft cannot be replaced, or the spot is above the build height, nothing SHALL be placed and the held stack SHALL keep its count. The placed segment SHALL follow the normal support rules.

#### Scenario: Click the lowest segment
- **WHEN** a shaft is four segments high and a player who is not sneaking clicks the side of its lowest segment while holding an elevator
- **THEN** a fifth segment is placed on top of the shaft and nothing is placed beside the clicked segment

#### Scenario: Plain click with a blocked top
- **WHEN** stone sits directly on top of a two segment shaft and a player who is not sneaking clicks a segment while holding an elevator
- **THEN** nothing is placed and the held stack keeps its count

#### Scenario: Sneak click builds up
- **WHEN** a shaft is five segments high and a sneaking player clicks the side of its lowest segment while holding an elevator
- **THEN** a sixth segment is placed on top of the shaft

#### Scenario: Sneak click with a blocked top
- **WHEN** a stone block sits directly on top of a shaft and a sneaking player clicks a segment of it
- **THEN** nothing is placed and the held stack keeps its count

#### Scenario: Click on the ground
- **WHEN** a player clicks the top of a stone block that has no segment while holding an elevator
- **THEN** a segment is placed on the stone as with any block
