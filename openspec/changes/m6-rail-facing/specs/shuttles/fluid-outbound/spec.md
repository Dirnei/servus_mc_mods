# Spec Delta

## MODIFIED Requirements

### Requirement: Two blocks high station
The Fluid Outbound SHALL be placed, broken and dismantled like the Fluid Inbound: one item placing a station two blocks high, facing a rail or else the player by the same rule, when both spaces are free and the block below has a solid top, both halves removed together, and one dropped item that keeps the tank, the keep, the side configuration and the wood and brings them back when placed.

#### Scenario: Break keeps the keep
- **WHEN** a Fluid Outbound keeping 8,000 mB of water is broken and placed again
- **THEN** it still keeps 8,000 mB of water and holds what it held

#### Scenario: Placed beside a rail
- **WHEN** a player looking north places a Fluid Outbound with a rail only on the west side of its lower half
- **THEN** its front faces west, towards the rail
