## MODIFIED Requirements

### Requirement: Home rail per garage
The rail behind a garage SHALL be the home of its shuttles: idle shuttles sit inside the garage, a shuttle leaves onto the home rail for a job and returns through it. A garage with no rail behind it SHALL keep its shuttles inside and they SHALL NOT serve jobs. When a garage loses its home rail while shuttles are out, those shuttles SHALL be recalled as the Recall button does; a home rail that only unloads with its chunk SHALL NOT recall them, the garage keeps it as home until the chunk loads again. Shuttles out on a job SHALL keep moving and finish their legs whether or not any garage has a home rail. The shuttles of several garages SHALL serve one shared queue; the free shuttle with the cheapest way to the job by travel cost SHALL take it.

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

## ADDED Requirements

### Requirement: Shuttles park cargo that has nowhere to go
A shuttle holding cargo that no rack has room for, or that it cannot reach, SHALL drive into its garage and park there with the cargo. While parked it SHALL take no other job and SHALL NOT be drawn on the rail. The cargo SHALL be saved with the garage and SHALL drop only when the garage is broken; Recall SHALL drop it at the garage like any other cargo.

#### Scenario: Parked over a restart
- **WHEN** the server restarts while a shuttle is parked with 44 cobblestone
- **THEN** after the restart the shuttle is still parked with its 44 cobblestone and nothing dropped

### Requirement: The garage shows parked cargo
The garage's lamp SHALL turn red while any of its shuttles is parked with cargo, and its GUI SHALL name, for each such shuttle, its label, the item and amount it holds and the reason (no room, no way).

#### Scenario: Warehouse full
- **WHEN** a shuttle arrives at its rack with 64 cobblestone, the rack takes 20 and no other rack has room
- **THEN** the shuttle drives into its garage with 44 cobblestone, the garage lamp turns red and its GUI reads that Shuttle #3 holds 44 Cobblestone with no room

### Requirement: Parked shuttles try again
A parked shuttle SHALL try again when room may have appeared (a rack joins, a zone changes, a lock is released, stock leaves a rack) and every few seconds, and SHALL drive out to the rack that takes its cargo.

#### Scenario: Room appears
- **WHEN** a new rack joins the warehouse while a shuttle is parked with 44 cobblestone
- **THEN** the shuttle drives out, puts the 44 cobblestone into the new rack and the garage lamp is no longer red
