# Spec Delta

## MODIFIED Requirements

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
