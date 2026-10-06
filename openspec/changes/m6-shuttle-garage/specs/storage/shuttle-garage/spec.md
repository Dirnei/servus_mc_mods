# Spec Delta

## Purpose

The Shuttle Garage is where a warehouse keeps its shuttles: each garage holds a few, parks them on the rail in front of it, and a warehouse grows its fleet by building more garages.

## ADDED Requirements

### Requirement: Garage block and capacity
The Shuttle Garage SHALL be a block with a horizontal facing that holds up to `garage_capacity` shuttles (example 4, from `data/<ns>/storage/warehouse.json`). Right clicking it with a Shuttle item SHALL add one shuttle up to the capacity. Sneak right clicking it with an empty hand SHALL take one shuttle back as an item. Right clicking it with an empty hand SHALL show how many shuttles it holds and how many are out.

#### Scenario: Add and take back
- **WHEN** a player right clicks the garage twice with shuttles and then sneak right clicks it with an empty hand
- **THEN** the garage holds one shuttle and the player got one shuttle item back

#### Scenario: Full garage
- **WHEN** the garage already holds its capacity
- **THEN** another shuttle item is refused, stays in hand, and a message says to build another garage

#### Scenario: More garages, more shuttles
- **WHEN** a warehouse has two garages with four shuttles each
- **THEN** the warehouse has eight shuttles

### Requirement: Garage is a warehouse node without stock
The garage SHALL be a member of the warehouse network like a storage block: it SHALL link to touching storage blocks, stations and garages, rails SHALL dock onto it on any side, and it SHALL NOT link to elevator segments. It SHALL hold no stock, SHALL NOT make inventories beside it part of the warehouse, and SHALL show the connection lamp like storage blocks.

#### Scenario: Chest beside a garage
- **WHEN** a chest with bread touches only a garage of an online warehouse
- **THEN** the terminal does not list the bread

#### Scenario: Garage on a rail
- **WHEN** a garage is placed beside a rail of an online warehouse
- **THEN** the rail shows a dock towards the garage and the garage shows the online lamp

### Requirement: Home rail per garage
The rail touching a garage SHALL be the home of its shuttles: they SHALL park there when idle and return there after every job. A garage touching no rail SHALL keep its shuttles parked inside and they SHALL NOT serve jobs. The shuttles of several garages SHALL serve one shared queue; the free shuttle with the shortest way to the job SHALL take it.

#### Scenario: Two garages
- **WHEN** a warehouse has garage A on the west aisle and garage B on the east aisle, each with one shuttle, and a pick is made on the east aisle
- **THEN** garage B's shuttle takes it and returns to B's rail afterwards

#### Scenario: Garage without a rail
- **WHEN** a garage holding two shuttles touches no rail
- **THEN** its summary says the shuttles are parked without a rail and no cart is drawn on any rail

### Requirement: Breaking a garage
Breaking or dismantling a garage SHALL drop its shuttles as items at the garage. Shuttles of that garage that are out on a job SHALL drop their cargo at the garage and their job SHALL go back to the queue.

#### Scenario: Broken mid pick
- **WHEN** a garage holding three shuttles is broken while one of them carries cobblestone
- **THEN** three shuttle items and the cobblestone drop at the garage and the pick is queued again

### Requirement: Garage is obtainable and dismantled
The garage SHALL be craftable from vanilla materials and a rail, SHALL drop itself when broken, SHALL be dismantled by sneaking with the Configuration Carrot, and the carrot without sneaking SHALL turn its facing.

#### Scenario: Crafting
- **WHEN** the player places iron ingots, a rail and planks in the garage pattern (for example a rail between ingots over a row of planks)
- **THEN** a Shuttle Garage is produced

#### Scenario: Dismantle
- **WHEN** the player sneaks and uses the carrot on a garage holding two shuttles
- **THEN** the garage and two shuttle items drop
