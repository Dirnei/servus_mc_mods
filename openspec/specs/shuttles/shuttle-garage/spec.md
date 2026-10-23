# shuttles/shuttle-garage Specification

## Purpose

The Shuttle Garage is where a warehouse keeps its shuttles: each garage holds a few, sends them out through its front onto the rail, and a warehouse grows its fleet by building more garages.

## Requirements

### Requirement: Garage block and capacity
The Shuttle Garage SHALL be a block with a horizontal facing that holds up to `garage_capacity` shuttles (example 4, from `data/<ns>/shuttles/warehouse.json`). Right clicking it SHALL open a GUI with one slot per shuttle place and the player inventory. A Shuttle item in a slot is a shuttle of the garage; taking it out removes the shuttle. Only Shuttle items fit the slots, one per slot, with or without modules. A slot SHALL keep its shuttle's modules. The GUI SHALL say how many shuttles the garage has and how many are out.

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

### Requirement: Garage is a warehouse node without stock
The garage SHALL be a member of the warehouse network only through the rail on its back, following the rail only rule for shuttle served blocks: it SHALL NOT link to touching controllers, storage blocks, racks, stations, other garages or elevator segments. It SHALL hold no stock, SHALL NOT make inventories beside it part of the warehouse, and SHALL show the connection lamp like storage blocks, lit only while its back rail links it to an online warehouse. The rail behind it SHALL render a plain arm towards the garage, not a dock piece.

#### Scenario: Chest beside a garage
- **WHEN** a chest with bread touches only a garage of an online warehouse
- **THEN** the terminal does not list the bread

#### Scenario: Rail in front
- **WHEN** a rail lies behind a garage of an online warehouse
- **THEN** the rail shows an arm towards the garage, no dock piece, and the garage shows the online lamp

#### Scenario: Rail at the side
- **WHEN** a rail runs along the side or the front of a garage and nothing else touches it
- **THEN** the rail shows neither an arm nor a dock towards the garage and the garage has no home rail

#### Scenario: Touching the controller is not enough
- **WHEN** a garage touches the controller and no rail lies behind it
- **THEN** the garage does not belong to that warehouse, its lamp is dark and its shuttles serve nothing

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
