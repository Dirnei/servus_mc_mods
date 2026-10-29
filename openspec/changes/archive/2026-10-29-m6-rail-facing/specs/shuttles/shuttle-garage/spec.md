# Spec Delta

## MODIFIED Requirements

### Requirement: Doorway at the back
The garage's back SHALL have an open doorway, 12 by 12 pixels with the rail running in, closed by a roll-up shutter of steel slats. The shutter SHALL roll up into the lintel when a shuttle of the garage is about to drive through the doorway and roll down once it is through, in both directions; it SHALL be drawn by the client from where the garage's shuttles are and SHALL NOT be saved or sent. The front SHALL carry the connection lamp, and the carrot SHALL turn the garage. On placement the garage SHALL turn its doorway towards a warehouse rail when one is at hand:
- placed by clicking the side of a warehouse rail, the doorway SHALL face that rail;
- otherwise, when a warehouse rail lies beside it in the direction the player looks, the doorway SHALL face that rail;
- otherwise, when exactly one warehouse rail lies beside it, the doorway SHALL face that rail;
- otherwise the front SHALL face the player who placed it, like other storage blocks.

This is the shared rail facing rule (see `shuttles/rail-facing`); a garage placed while sneaking SHALL skip it and face its front to the player.

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

#### Scenario: Sneaking skips the rail
- **WHEN** a player looking north sneaks and places a garage on the floor with a rail only on its west side
- **THEN** the front faces the player, so the doorway faces north

#### Scenario: Carrot turns it after placement
- **WHEN** a garage placed with its doorway towards a rail is turned with the carrot
- **THEN** it turns as before and keeps the new facing

#### Scenario: Shutter opens for a shuttle
- **WHEN** a shuttle of the garage takes a job
- **THEN** the shutter rolls up while the shuttle waits inside, the shuttle drives out, and the shutter rolls down behind it

#### Scenario: Shutter shut at rest
- **WHEN** every shuttle of the garage is inside and idle
- **THEN** the shutter is down
