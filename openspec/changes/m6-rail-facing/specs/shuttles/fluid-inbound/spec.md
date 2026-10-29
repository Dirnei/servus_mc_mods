# Spec Delta

## MODIFIED Requirements

### Requirement: Two blocks high station
The Fluid Inbound SHALL be one item that places a station two blocks high with a horizontal facing chosen by the shared rail facing rule (see `shuttles/rail-facing`) from the rails beside its lower half, so its front faces a rail or else the player, when both spaces are free and the block below has a solid top; otherwise nothing SHALL be placed and the player SHALL be told why.

#### Scenario: Placed on the floor
- **WHEN** a player places a Fluid Inbound on a stone floor with two free blocks of room
- **THEN** a station two blocks high stands there with its front toward the player

#### Scenario: Placed beside a rail
- **WHEN** a player looking north places a Fluid Inbound with a rail only on the west side of its lower half
- **THEN** its front faces west, towards the rail

#### Scenario: No room
- **WHEN** a block stands where the upper half would go
- **THEN** nothing is placed and the player is told there is no room
