# Spec Delta

## ADDED Requirements

### Requirement: Doorway at the back
The garage's back SHALL show a doorway with a rail running into it, so the block reads as the place shuttles drive into from the aisle behind it. The front SHALL face the player who placed it, like other storage blocks, SHALL carry the connection lamp, and the carrot SHALL turn the garage.

#### Scenario: Placed from the aisle
- **WHEN** a player standing in the aisle places a garage against the aisle's rail
- **THEN** the doorway faces the rail behind the garage and the lamp faces the player

## MODIFIED Requirements

### Requirement: Garage is a warehouse node without stock
The garage SHALL be a member of the warehouse network like a storage block: it SHALL link to touching storage blocks, stations and garages, it SHALL take a rail only on its back, and it SHALL NOT link to elevator segments. It SHALL hold no stock, SHALL NOT make inventories beside it part of the warehouse, and SHALL show the connection lamp like storage blocks. The rail behind it SHALL render a plain arm towards the garage, not a dock piece.

#### Scenario: Chest beside a garage
- **WHEN** a chest with bread touches only a garage of an online warehouse
- **THEN** the terminal does not list the bread

#### Scenario: Rail in front
- **WHEN** a rail lies behind a garage of an online warehouse
- **THEN** the rail shows an arm towards the garage, no dock piece, and the garage shows the online lamp

#### Scenario: Rail at the side
- **WHEN** a rail runs along the side or the front of a garage and nothing else touches it
- **THEN** the rail shows neither an arm nor a dock towards the garage and the garage has no home rail

### Requirement: Home rail per garage
The rail behind a garage SHALL be the home of its shuttles: idle shuttles sit inside the garage, a shuttle leaves onto the home rail for a job and returns through it. A garage with no rail behind it SHALL keep its shuttles inside and they SHALL NOT serve jobs. The shuttles of several garages SHALL serve one shared queue; the free shuttle with the shortest way to the job SHALL take it.

#### Scenario: Two garages
- **WHEN** a warehouse has garage A on the west aisle and garage B on the east aisle, each with one shuttle, and a pick is made on the east aisle
- **THEN** garage B's shuttle takes it and returns to B afterwards

#### Scenario: Garage without a rail
- **WHEN** a garage holding two shuttles has no rail behind it
- **THEN** its GUI says the shuttles have no rail and no cart is drawn on any rail

## REMOVED Requirements

### Requirement: Front with a rail
**Reason**: The doorway and the rail move to the back; replaced by "Doorway at the back".
**Migration**: None, nothing is released.
