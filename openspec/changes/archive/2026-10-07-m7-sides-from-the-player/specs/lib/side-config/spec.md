# Spec Delta

## MODIFIED Requirements

### Requirement: Relative sides follow the facing
Sides SHALL be addressed as front, back, left, right, top and bottom relative to the block's facing, so rotating a block keeps its configuration. Left and right SHALL be the left and right of a player who looks at the block's front.

#### Scenario: Facing north
- **WHEN** a block faces north
- **THEN** front is north, back is south, left is east, right is west, top is up, bottom is down

#### Scenario: Facing east
- **WHEN** a block faces east
- **THEN** front is east, back is west, left is south, right is north
