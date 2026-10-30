# Spec Delta

## MODIFIED Requirements

### Requirement: Shuttles with the same heading follow with a gap
A shuttle SHALL NOT come closer than the follow gap (`follow_gap` in `data/<ns>/shuttles/warehouse.json`, example 1 block from centre to centre) to a shuttle ahead of it in the same lane. It SHALL brake in time to keep that gap, at its normal braking rate, and drive no faster than the shuttle ahead while it is that close. Shuttles SHALL never be drawn inside each other while they follow.

#### Scenario: Fast shuttle behind a slow one
- **WHEN** a Mk3 shuttle with Speed upgrades drives behind a plain Mk1 shuttle along the same aisle
- **THEN** it catches up, brakes, and follows the Mk1 one block behind at the Mk1's speed until their ways part

#### Scenario: Two shuttles to the same rack
- **WHEN** two shuttles leave for two bays of the same rack shelf, one right after the other
- **THEN** the second drives behind the first and never overlaps it
