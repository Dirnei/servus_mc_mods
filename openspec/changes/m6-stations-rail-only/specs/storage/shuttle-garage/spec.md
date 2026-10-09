# Spec Delta

## MODIFIED Requirements

### Requirement: Garage is a warehouse node without stock
The garage SHALL be a member of the warehouse network only through the rail on its back, following the rail only rule for shuttle served blocks: it SHALL NOT link to touching controllers, storage blocks, racks, stations, other garages or elevator segments. It SHALL hold no stock, SHALL NOT make inventories beside it part of the warehouse, and SHALL show the connection lamp like storage blocks, lit only while its back rail links it to an online warehouse. The rail behind it SHALL render a plain arm towards the garage, not a dock piece.

#### Scenario: Chest beside a garage
- **WHEN** a chest with bread touches only a garage of an online warehouse
- **THEN** the terminal does not list the bread

#### Scenario: Rail in front
- **WHEN** a rail lies behind a garage of an online warehouse
- **THEN** the rail shows an arm towards the garage, no dock piece, and the garage shows the online lamp

#### Scenario: Rail at the side
- **WHEN** a rail runs along the side or the front of a garage and nothing else touches it
- **THEN** the rail shows neither an arm nor a dock towards the garage and the garage has no home rail

#### Scenario: Touching the controller is not enough
- **WHEN** a garage touches the controller and no rail lies behind it
- **THEN** the garage does not belong to that warehouse, its lamp is dark and its shuttles serve nothing
