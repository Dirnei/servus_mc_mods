## MODIFIED Requirements

### Requirement: Home rail per garage
The rail behind a garage SHALL be the home of its shuttles: idle shuttles sit inside the garage, a shuttle leaves onto the home rail for a job and returns through it. A garage with no rail behind it SHALL keep its shuttles inside and they SHALL NOT serve jobs. When a garage loses its home rail while shuttles are out, those shuttles SHALL be recalled as the Recall button does; a home rail that only unloads with its chunk SHALL NOT recall them, the garage keeps it as home until the chunk loads again. Shuttles out on a job SHALL keep moving and finish their legs whether or not any garage has a home rail. The shuttles of several garages SHALL serve one shared queue; the free shuttle with the shortest way to the job SHALL take it.

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
