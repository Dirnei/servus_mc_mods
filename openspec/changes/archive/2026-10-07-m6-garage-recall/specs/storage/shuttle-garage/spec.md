# Spec Delta

## ADDED Requirements

### Requirement: Recall and pause
The garage GUI SHALL have a Recall button and an Active / Paused toggle. Recall SHALL bring every shuttle of the garage inside at once: cargo drops at the garage, a job not yet loaded goes back to the front of the queue, a loaded pick counts as missing for its request, and every slot can be emptied afterwards. A paused garage SHALL hand out no shuttles for new jobs; shuttles already out finish their job and park. The pause state SHALL persist with the block and SHALL show in the GUI line and in the warehouse summary.

#### Scenario: Recall
- **WHEN** two shuttles of a garage are out and the player presses Recall
- **THEN** both are inside, their cargo lies at the garage, the GUI says none are out and the shuttle items can be taken

#### Scenario: Paused garage
- **WHEN** garage A is paused, garage B is active, each holds a shuttle, and a pick is made
- **THEN** garage B's shuttle takes it and garage A's shuttle stays inside

#### Scenario: Pause survives a reload
- **WHEN** a paused garage is reloaded with the world
- **THEN** it is still paused

## MODIFIED Requirements

### Requirement: Home rail per garage
The rail behind a garage SHALL be the home of its shuttles: idle shuttles sit inside the garage, a shuttle leaves onto the home rail for a job and returns through it. A garage with no rail behind it SHALL keep its shuttles inside and they SHALL NOT serve jobs. When a garage loses its home rail while shuttles are out, those shuttles SHALL be recalled as the Recall button does. Shuttles out on a job SHALL keep moving and finish their legs whether or not any garage has a home rail. The shuttles of several garages SHALL serve one shared queue; the free shuttle with the shortest way to the job SHALL take it.

#### Scenario: Two garages
- **WHEN** a warehouse has garage A on the west aisle and garage B on the east aisle, each with one shuttle, and a pick is made on the east aisle
- **THEN** garage B's shuttle takes it and returns to B afterwards

#### Scenario: Garage without a rail
- **WHEN** a garage holding two shuttles has no rail behind it
- **THEN** its GUI says the shuttles have no rail and no cart is drawn on any rail

#### Scenario: Rail removed while out
- **WHEN** the rail behind a garage is broken while its shuttle carries cobblestone to a rack
- **THEN** the cobblestone drops at the garage, the shuttle is inside and the GUI says none are out
