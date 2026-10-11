## MODIFIED Requirements

### Requirement: Garage block and capacity
The Shuttle Garage SHALL be a block with a horizontal facing that holds up to `garage_capacity` shuttles (example 4, from `data/<ns>/storage/warehouse.json`). Right clicking it SHALL open a GUI with one slot per shuttle place and the player inventory. A Shuttle item in a slot is a shuttle of the garage; taking it out removes the shuttle. Only Shuttle items fit the slots, one per slot, with or without modules. A slot SHALL keep its shuttle's modules. The GUI SHALL say how many shuttles the garage has and how many are out.

#### Scenario: Add and take back
- **WHEN** a player puts two Shuttle items into the garage slots and then takes one out again
- **THEN** the garage holds one shuttle and the player has the other item back

#### Scenario: Full garage
- **WHEN** every slot holds a shuttle
- **THEN** another Shuttle item cannot be placed and the player builds another garage for it

#### Scenario: More garages, more shuttles
- **WHEN** a warehouse has two garages with four shuttles each
- **THEN** the warehouse has eight shuttles

#### Scenario: Shuttle out on a job
- **WHEN** one of three shuttles is out on a job
- **THEN** the GUI says three shuttles, one out, and the slot of that shuttle cannot be emptied until it is back, while the other two can

#### Scenario: Slot of the shuttle that left
- **WHEN** a garage has only its second slot filled, that shuttle goes out on a job and the player then puts a shuttle into the first slot
- **THEN** the second slot stays locked and the first slot can be emptied
