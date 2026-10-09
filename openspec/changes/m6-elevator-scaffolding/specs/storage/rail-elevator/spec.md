# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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

## REMOVED Requirements

### Requirement: Sneak placement builds a shaft up
**Reason**: A plain click now builds the shaft up too, so placing against a side face of a segment is gone.
**Migration**: See "Clicks build a shaft up", which keeps the sneak behaviour.
