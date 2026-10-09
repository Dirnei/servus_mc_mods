# Spec Delta

## MODIFIED Requirements

### Requirement: Station is a warehouse node without stock
The station SHALL be a member of the warehouse network only through rails, like an Inbound Station: rails SHALL dock onto it on every side and link it, and it SHALL NOT link to touching controllers, storage blocks, racks, garages, other stations or elevator segments. Its buffer SHALL NOT be indexed and the blocks it pushes into SHALL NOT become warehouse sources through it. It SHALL show the connection lamp, lit only while a rail links it to an online warehouse, and an activity lamp: green while every keep slot is satisfied, orange while deliveries are on their way, red while a keep slot cannot be filled, dark while offline.

#### Scenario: Machine is not stock
- **WHEN** an Assembler holds 20 iron ingots in front of a station of an online warehouse
- **THEN** the terminal does not list the ingots

#### Scenario: Lamp while short
- **WHEN** a keep slot asks for sticks that are not in stock
- **THEN** the activity lamp is red and turns green once the sticks arrived

#### Scenario: Touching the controller is not enough
- **WHEN** an Outbound Station touches the controller and no rail touches the station
- **THEN** the station is offline and its keep slots are not filled
