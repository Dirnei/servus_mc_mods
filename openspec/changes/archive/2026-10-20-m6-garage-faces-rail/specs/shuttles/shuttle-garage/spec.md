## MODIFIED Requirements

### Requirement: Doorway at the back
The garage's back SHALL show a doorway with a rail running into it, so the block reads as the place shuttles drive into from the aisle behind it. The front SHALL carry the connection lamp, and the carrot SHALL turn the garage. On placement the garage SHALL turn its doorway towards a warehouse rail when one is at hand:
- placed by clicking the side of a warehouse rail, the doorway SHALL face that rail;
- otherwise, when a warehouse rail lies beside it in the direction the player looks, the doorway SHALL face that rail;
- otherwise, when exactly one warehouse rail lies beside it, the doorway SHALL face that rail;
- otherwise the front SHALL face the player who placed it, like other storage blocks.

#### Scenario: Placed from the aisle
- **WHEN** a player standing in the aisle places a garage against the aisle's rail
- **THEN** the doorway faces the rail behind the garage and the lamp faces the player

#### Scenario: Clicked on the side of a rail
- **WHEN** a player looking north clicks the east side of a rail with a garage
- **THEN** the garage stands east of the rail with its doorway facing west, towards the rail, and the rail shows its arm towards the garage

#### Scenario: One rail beside it
- **WHEN** a player looking north places a garage on the floor with a rail only on its west side
- **THEN** the doorway faces west, towards the rail

#### Scenario: Rail in the look direction wins
- **WHEN** a player looking north places a garage on the floor with rails on its north and its west side
- **THEN** the doorway faces north

#### Scenario: Several rails, none in the look direction
- **WHEN** a player looking north places a garage on the floor with rails on its east and its west side
- **THEN** the front faces the player, so the doorway faces north

#### Scenario: No rail beside it
- **WHEN** a player looking north places a garage on the floor with no rail beside it
- **THEN** the front faces the player, so the doorway faces north

#### Scenario: Carrot turns it after placement
- **WHEN** a garage placed with its doorway towards a rail is turned with the carrot
- **THEN** it turns as before and keeps the new facing
