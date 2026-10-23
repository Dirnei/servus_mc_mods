## MODIFIED Requirements

### Requirement: Home rail per garage
The rail behind a garage SHALL be the home of its shuttles: idle shuttles stand inside the garage, a shuttle leaves through the doorway onto the home rail for a job and returns through it. The step between a garage and its home rail SHALL be usable only by that garage's shuttles. Before a shuttle drives out of its garage it SHALL wait inside for the door time from `warehouse.json` (`door_ticks`, example 10). A garage with no rail behind it SHALL keep its shuttles inside and they SHALL NOT serve jobs. When a garage loses its home rail while shuttles are out, those shuttles SHALL be recalled as the Recall button does. Shuttles out on a job SHALL keep moving and finish their legs whether or not any garage has a home rail. The shuttles of several garages SHALL serve one shared queue; the free shuttle with the shortest way to the job SHALL take it.

#### Scenario: Two garages
- **WHEN** a warehouse has garage A on the west aisle and garage B on the east aisle, each with one shuttle, and a pick is made on the east aisle
- **THEN** garage B's shuttle takes it and returns to B afterwards

#### Scenario: Garage without a rail
- **WHEN** a garage holding two shuttles has no rail behind it
- **THEN** its GUI says the shuttles have no rail and no cart is drawn on any rail

#### Scenario: Rail removed while out
- **WHEN** the rail behind a garage is broken while its shuttle carries cobblestone to a rack
- **THEN** the cobblestone drops at the garage, the shuttle is inside and the GUI says none are out

#### Scenario: Home rail unloads while out
- **WHEN** the home rail of a garage lies in the next chunk, which unloads while the garage's shuttle carries cobblestone to a rack
- **THEN** nothing drops, the shuttle is still out, and it drives home once the chunk loads again

#### Scenario: Leaving through the doorway
- **WHEN** a shuttle of a garage takes a job
- **THEN** it waits the door time inside, then drives out through the doorway onto the home rail

#### Scenario: Job while pulling in
- **WHEN** a new job comes in while a shuttle on its way home is already close enough to its doorway that the shutter opens for it
- **THEN** the shuttle drives into its garage first and takes the next job from there, through the door as usual; a shuttle further from home takes the job at once

#### Scenario: No shortcut through a garage
- **WHEN** two rails are linked only through a garage standing between them
- **THEN** no shuttle of another garage drives through it

### Requirement: Doorway at the back
The garage's back SHALL have an open doorway, 12 by 12 pixels with the rail running in, closed by a roll-up shutter of steel slats. The shutter SHALL roll up into the lintel when a shuttle of the garage is about to drive through the doorway and roll down once it is through, in both directions; it SHALL be drawn by the client from where the garage's shuttles are and SHALL NOT be saved or sent. The front SHALL carry the connection lamp, and the carrot SHALL turn the garage. On placement the garage SHALL turn its doorway towards a warehouse rail when one is at hand:
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

#### Scenario: Shutter opens for a shuttle
- **WHEN** a shuttle of the garage takes a job
- **THEN** the shutter rolls up while the shuttle waits inside, the shuttle drives out, and the shutter rolls down behind it

#### Scenario: Shutter shut at rest
- **WHEN** every shuttle of the garage is inside and idle
- **THEN** the shutter is down
