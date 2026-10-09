# Spec Delta

## ADDED Requirements

### Requirement: Shuttle served blocks join only through rails
Every block that shuttles serve SHALL join a warehouse only through a rail on one of the faces where it takes rails, and SHALL NOT join or pass on a warehouse by touching controllers, crates, other storage blocks or each other. The faces are: a Heavy Duty Rack's front (and the bays of its own section), every side of an Inbound Station, Outbound Station and Access Point, and the back of a Shuttle Garage. Blocks served instantly (crates, the controller, foreign inventories) keep linking to touching storage blocks and rails.

#### Scenario: Station pressed against a crate
- **WHEN** an Inbound Station touches a crate of an online warehouse and no rail touches the station
- **THEN** the station is not part of the warehouse

#### Scenario: Crates still link by touch
- **WHEN** a crate touches the controller
- **THEN** the crate is part of the warehouse without a rail
