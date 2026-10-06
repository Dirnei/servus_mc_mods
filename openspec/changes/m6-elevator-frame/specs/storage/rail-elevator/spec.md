# Spec Delta

## MODIFIED Requirements

### Requirement: Shaft segment block
The Rail Elevator SHALL be a full height block that renders as a frame of four corner posts with an open middle. The warehouse rail SHALL run through the middle of the segment at floor level, with an arm towards every connected rail. Segments SHALL stack on top of each other to form a shaft of any height, SHALL render as one continuous frame, and players SHALL be able to walk through a segment between its posts.

#### Scenario: Three segments
- **WHEN** three elevator segments are placed on top of each other
- **THEN** they look like one frame from floor to top with the posts lining up

#### Scenario: Walk through
- **WHEN** a player walks along a rail into a ground floor segment
- **THEN** the player passes between the posts and stands in the middle of the segment on the rail

#### Scenario: Rail through the middle
- **WHEN** rails lie on the west and east sides of a segment
- **THEN** the segment shows a rail from its west edge through its centre to its east edge
