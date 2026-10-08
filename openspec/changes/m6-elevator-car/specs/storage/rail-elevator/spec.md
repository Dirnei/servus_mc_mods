# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

### Requirement: Players pass and climb through segments
An elevator segment SHALL NOT block players or other entities. It SHALL still show its outline and be breakable when targeted. A player inside a segment SHALL climb it like a ladder: holding jump moves up, sneaking holds the player in place, and otherwise the player slides down slowly without fall damage.

#### Scenario: Walk past the posts
- **WHEN** a player walks along a rail into a ground floor segment and keeps walking
- **THEN** the player passes through the segment, posts included, without stopping

#### Scenario: Climb up
- **WHEN** a player stands in the lowest segment of a three segment shaft and holds jump
- **THEN** the player rises through the shaft and can step out onto a rail beside the top segment

#### Scenario: Step into an upper floor shaft
- **WHEN** a player walks off an upper floor rail into the shaft and presses nothing
- **THEN** the player slides down the shaft slowly and takes no fall damage at the bottom

#### Scenario: Break the bottom
- **WHEN** a player breaks the lowest segment of a shaft
- **THEN** that segment and every segment above it drop as items

### Requirement: Rail floors are decks
A segment with at least one rail beside it SHALL have a deck that covers the shaft floor. Its rail SHALL lie at the same height as the Warehouse Rail outside and join every connected rail; the deck plate under it SHALL sit just below the segment. The deck SHALL fold away only on edges without a rail, so a folded deck never stands in the way of a shuttle: two flaps on the two free opposite edges, one flap on a free edge when no opposite pair is free, and four quarter plates pulled into the corner posts when rails lie on all four sides.

#### Scenario: Straight run
- **WHEN** rails lie west and east of a segment
- **THEN** the deck hangs on axles along the north and south edges and its rail joins both outside rails at the same height

#### Scenario: Corner
- **WHEN** rails lie north and east of a segment
- **THEN** the deck is one flap hinged on a free edge (south or west) and its rail turns from the north edge to the east edge

#### Scenario: Dead end entry
- **WHEN** a single rail lies south of a segment
- **THEN** the deck hangs on axles along the west and east edges and its rail runs from the south edge to the centre

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
