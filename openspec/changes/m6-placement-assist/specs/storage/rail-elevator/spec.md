# Spec Delta

## ADDED Requirements

### Requirement: Sneak placement builds a shaft up
The elevator item SHALL use the placement assist. A sneak click on any face of a segment SHALL start the walk, the run SHALL be the segments of the shaft and the direction SHALL always be up, so the new segment lands on top of the shaft. The placed segment SHALL follow the normal support rules.

#### Scenario: Build up from below
- **WHEN** a shaft is five segments high and a sneaking player clicks the side of its lowest segment while holding an elevator
- **THEN** a sixth segment is placed on top of the shaft

#### Scenario: Blocked top
- **WHEN** a stone block sits directly on top of a shaft and a sneaking player clicks a segment of it
- **THEN** nothing is placed and the held stack keeps its count

#### Scenario: Plain click
- **WHEN** a player who is not sneaking clicks the side of a segment while holding an elevator
- **THEN** the segment is placed against that face as before, and stays only if the support rules allow it
