# Spec Delta

## MODIFIED Requirements

### Requirement: Access Point joins rails and the data line
The Access Point SHALL be a block that joins the warehouse only through rails on its sides, like a station, so shuttles can reach it; it SHALL NOT link to touching controllers, storage blocks, racks, garages or stations. It SHALL be a link member of the data line it touches, SHALL hold no stock of the warehouse and SHALL show the online lamp like other warehouse blocks.

#### Scenario: Both networks
- **WHEN** an Access Point stands beside a rail of a warehouse and a duct with the data line touches it
- **THEN** it belongs to that warehouse and is a link member of that data network

#### Scenario: Not stock
- **WHEN** items wait in an Access Point's buffers
- **THEN** the warehouse stock does not count them

#### Scenario: Touching the controller is not enough
- **WHEN** an Access Point touches the controller and no rail touches it
- **THEN** it does not belong to that warehouse and its lamp is dark
