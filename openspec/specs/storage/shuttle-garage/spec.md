# storage/shuttle-garage Specification

## Purpose

The Shuttle Garage is where a warehouse keeps its shuttles: each garage holds a few, sends them out through its front onto the rail, and a warehouse grows its fleet by building more garages.

## Requirements

### Requirement: Garage block and capacity
The Shuttle Garage SHALL be a block with a horizontal facing that holds up to `garage_capacity` shuttles (example 4, from `data/<ns>/storage/warehouse.json`). Right clicking it SHALL open a GUI with one slot per shuttle place and the player inventory. A Shuttle item in a slot is a shuttle of the garage; taking it out removes the shuttle. Only Shuttle items fit the slots, one per slot. The GUI SHALL say how many shuttles the garage has and how many are out.

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
- **THEN** the GUI says three shuttles, one out, and the first slot cannot be emptied until it is back

### Requirement: Garage is a warehouse node without stock
The garage SHALL be a member of the warehouse network like a storage block: it SHALL link to touching storage blocks, stations and garages, it SHALL take a rail only on its front, and it SHALL NOT link to elevator segments. It SHALL hold no stock, SHALL NOT make inventories beside it part of the warehouse, and SHALL show the connection lamp like storage blocks.

#### Scenario: Chest beside a garage
- **WHEN** a chest with bread touches only a garage of an online warehouse
- **THEN** the terminal does not list the bread

#### Scenario: Rail in front
- **WHEN** a garage faces a rail of an online warehouse
- **THEN** the rail shows a dock towards the garage and the garage shows the online lamp

#### Scenario: Rail at the side
- **WHEN** a rail runs along the side or the back of a garage and nothing else touches it
- **THEN** the rail shows no dock and the garage has no home rail

### Requirement: Front with a rail
The garage's front SHALL show a doorway with a rail running into it, so the block reads as the place shuttles drive into. The front SHALL face the player who placed it, like other storage blocks, and the carrot SHALL turn it.

#### Scenario: Placed facing the aisle
- **WHEN** a player places a garage while looking at it from the aisle
- **THEN** its doorway and rail face the aisle

### Requirement: Home rail per garage
The rail in front of a garage SHALL be the home of its shuttles: idle shuttles sit inside the garage, a shuttle leaves onto the home rail for a job and returns through it. A garage with no rail in front SHALL keep its shuttles inside and they SHALL NOT serve jobs. The shuttles of several garages SHALL serve one shared queue; the free shuttle with the shortest way to the job SHALL take it.

#### Scenario: Two garages
- **WHEN** a warehouse has garage A on the west aisle and garage B on the east aisle, each with one shuttle, and a pick is made on the east aisle
- **THEN** garage B's shuttle takes it and returns to B afterwards

#### Scenario: Garage without a rail
- **WHEN** a garage holding two shuttles has no rail in front
- **THEN** its GUI says the shuttles have no rail and no cart is drawn on any rail

### Requirement: Garage is protected while it holds shuttles
A garage with at least one shuttle, inside or out, SHALL NOT be broken by a player and SHALL NOT be dismantled with the Configuration Carrot; the player SHALL be told to take the shuttles out first. An empty garage breaks and dismantles normally. When a garage is destroyed anyway, for example by an explosion, its shuttles SHALL drop as items at the garage, shuttles out on a job SHALL drop their cargo there and their jobs SHALL go back to the queue.

#### Scenario: Break attempt
- **WHEN** a player tries to break a garage holding two shuttles
- **THEN** the block stays and a message says to take the shuttles out first

#### Scenario: Carrot on an occupied garage
- **WHEN** a player sneaks and uses the carrot on a garage holding a shuttle
- **THEN** the garage stays and the same message shows

#### Scenario: Empty garage
- **WHEN** a player breaks a garage with empty slots
- **THEN** it drops as an item

#### Scenario: Blown up mid pick
- **WHEN** a garage holding three shuttles explodes while one of them carries cobblestone
- **THEN** three shuttle items and the cobblestone drop at the garage and the pick is queued again

### Requirement: Garage is obtainable
The garage SHALL be craftable from vanilla materials and a rail, and SHALL drop itself when broken while empty.

#### Scenario: Crafting
- **WHEN** the player places iron ingots, a rail and planks in the garage pattern (for example a rail between ingots over a row of planks)
- **THEN** a Shuttle Garage is produced
