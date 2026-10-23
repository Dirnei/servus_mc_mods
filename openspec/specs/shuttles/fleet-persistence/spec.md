# shuttles/fleet-persistence Specification

## Purpose
Fleet persistence keeps the work of a warehouse's shuttles across restarts and chunk loading: every job has a stable id, and queued jobs, shuttles on the way with their cargo, held deliveries and station claims come back where they were, so no item vanishes and no stack is planned twice.

## Requirements

### Requirement: Every job has a stable id
Every shuttle job (pick, put-away, field, fertilize, supply and tanker run) SHALL get an id when it is made. Ids SHALL count up per world, whichever dimension the warehouse stands in, and no id SHALL be given twice in one world, also across restarts. A job SHALL keep its id while it is queued, while a shuttle carries it and after a reload.

#### Scenario: Two jobs
- **WHEN** two put-away jobs are made one after the other
- **THEN** they have different ids and the second id is higher

#### Scenario: Id after a restart
- **WHEN** the last job of a world got id 41, the world is saved and loaded again and a new job is made
- **THEN** the new job's id is above 41

### Requirement: Shuttles on the way survive a reload
A shuttle out on a job SHALL be saved with its garage: its leg, its path and place on it, its jobs with their cargo items or fluid, and its field, fertilize or supply run with what that run carries. When the garage loads again its shuttles SHALL come back with the same jobs and cargo and go on along their paths. A field shuttle SHALL go on with its run from its rail, putting its cargo away first. A rider boards the car again where it got on.

#### Scenario: Restart mid trip
- **WHEN** a shuttle carries 40 cobblestone from an Inbound Station to a rack and the server restarts while it is halfway
- **THEN** after the restart the shuttle drives on from halfway and the 40 cobblestone land in the rack

#### Scenario: Pick on the way to the Packing Station
- **WHEN** a shuttle carries a picked stack of iron ingots for a shipment and its garage reloads
- **THEN** the ingots reach the Packing Station and the shipment is credited as before

#### Scenario: Harvester with cargo
- **WHEN** a harvester carries wheat on a field run and its garage reloads
- **THEN** the harvester puts the wheat away and goes on with its run

### Requirement: Queued jobs survive a reload
Jobs waiting for a shuttle SHALL be saved with the Warehouse Controller and SHALL be queued again, in their order, when the controller loads. A job that is already queued or on a shuttle SHALL NOT be queued twice. No queued job SHALL be handed to a shuttle in the first second after a restore, so the rest of the site can load first.

#### Scenario: Controller reload with a queue
- **WHEN** three pick jobs wait for the only shuttle and the controller's chunk is saved and loaded again
- **THEN** the three jobs are queued again in the same order and the shuttle serves them

### Requirement: Held deliveries survive a reload
Cargo a shuttle could not unload into a full destination and that the warehouse holds for it SHALL be saved with that destination and SHALL be held again when the destination loads. When the destination is broken, held cargo SHALL drop at its position.

#### Scenario: Packing Station full
- **WHEN** 20 planks are held for a full Packing Station and the station's chunk reloads
- **THEN** the planks are still held and land in the station once it has room

#### Scenario: Destination broken
- **WHEN** 20 planks are held for a Packing Station and the station is broken
- **THEN** the 20 planks drop at its position

### Requirement: Station claims survive a reload
An Inbound Station buffer slot claimed by a put-away job SHALL stay claimed by that job across a reload, so the stack is not planned a second time. While the warehouse is online and every chunk of its site is loaded, a claim whose job is neither queued nor on a shuttle at two checks in a row SHALL be released and its stack planned again. Checks run every few seconds.

#### Scenario: Claim kept
- **WHEN** a put-away job for a station slot is queued and the station and controller reload
- **THEN** the slot stays claimed, no second job is made for it, and the queued job puts the stack away

#### Scenario: Claim without a job
- **WHEN** a station loads with a slot claimed by a job that no longer exists
- **THEN** after a few seconds the claim is released and the stack is put away by a new job

### Requirement: A restore that cannot go on ends in the garage
A restored shuttle whose path is no longer whole SHALL look for a new way from where it stands to the goal of its leg, else drive home. Without a way home, or off the rails, it SHALL be put back into its garage as a recall does: its cargo drops at the garage, jobs not loaded yet are queued again and a loaded pick counts as missing. Field shuttles end their run the same way. No cargo SHALL vanish.

#### Scenario: Rack gone
- **WHEN** a shuttle carrying cobblestone to a rack is saved, the rack's rail is broken while the chunk is unloaded and the garage loads again
- **THEN** the shuttle finds another way or goes home, and without either the cobblestone drops at its garage

### Requirement: Shuttles stop at the edge of loaded chunks
A shuttle SHALL NOT drive into, arrive in or deliver to a chunk that is not loaded; it SHALL wait where it is until the chunk loads. A restored shuttle SHALL wait until every chunk of the rest of its path is loaded and then a second more before it moves. Cargo held for a destination whose chunk is not loaded SHALL wait for it.

#### Scenario: Rails into an unloaded chunk
- **WHEN** a shuttle's path leads into a chunk that unloads before the shuttle gets there
- **THEN** the shuttle stops before that chunk and drives on once it loads

### Requirement: Shuttles sleep with their garage
When a garage's chunk unloads, its shuttles SHALL be saved with it and leave the rails; nothing drops and their jobs are not queued again. The same SHALL hold when a garage changes network without being broken, for example when a split or a link change moves it: its shuttles go on in the new network.

#### Scenario: Garage chunk unloads
- **WHEN** the chunk of a garage unloads while one of its shuttles carries cobblestone
- **THEN** no cobblestone drops, the shuttle is not drawn, and when the chunk loads the shuttle carries on

### Requirement: Old saves load
A world saved without fleet state, such as one from Shuttles 0.3.0, SHALL load without errors: garages hold their shuttles inside, the queue is empty, nothing is held and every station slot is free.

#### Scenario: World from 0.3.0
- **WHEN** a world saved by Shuttles 0.3.0 with a garage, a controller and an Inbound Station holding bread loads
- **THEN** it loads without errors, the shuttles are inside the garage and the bread is put away by a new job
