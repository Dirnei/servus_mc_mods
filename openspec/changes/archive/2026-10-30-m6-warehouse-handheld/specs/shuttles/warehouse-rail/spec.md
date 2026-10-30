# Spec Delta

## MODIFIED Requirements

### Requirement: Only rails join blocks to a warehouse
Every block that shuttles serve SHALL join a warehouse only through a rail on one of the faces where it takes rails, and SHALL NOT join or pass on a warehouse by touching controllers, other storage blocks or each other. The faces are: a Heavy Duty Rack's front (and the bays of its own shelf), the front of a Fluid Rack shelf's port bay (and the bays of its own shelf), every side of an Inbound Station, Outbound Station and Packing Station, the front of a Fluid Inbound's and a Fluid Outbound's lower half, and the back of a Shuttle Garage. The Warehouse Controller SHALL link to touching rails on any side and to a touching controller; no other block SHALL join a warehouse by touching it.

#### Scenario: Station pressed against the controller
- **WHEN** an Inbound Station touches the controller of an online warehouse and no rail touches the station
- **THEN** the station is not part of the warehouse

#### Scenario: Chest touching the controller
- **WHEN** a chest touches the controller
- **THEN** the chest is not part of the warehouse and its items are not listed

#### Scenario: Gantry over a rail
- **WHEN** a Fluid Inbound has a rail of one warehouse in front of its lower half and a rail of another warehouse touching its side
- **THEN** the station belongs only to the warehouse of the rail in front of it
