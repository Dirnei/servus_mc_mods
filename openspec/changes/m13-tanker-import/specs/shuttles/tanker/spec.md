## Purpose

Tanker shuttles carry fluids over the warehouse rails: they collect fluid from Import gantries and bring it to Storage gantries with room, and only move fluid when it has somewhere to go.

## ADDED Requirements

### Requirement: Tanker module
The Tanker SHALL be a craftable tool module (example: a Fluid Tank, two buckets and iron ingots). It SHALL go into a shuttle through the install slot of the Shuttle Workshop, into the shuttle's one tool place, replacing a fitted tool. A shuttle with a Tanker SHALL take only tanker runs: no warehouse picks, no put-aways and no field runs. A shuttle without a Tanker SHALL NOT take tanker runs. The Tanker SHALL NOT be wide: a Tanker shuttle SHALL ride lift cars like a shuttle without a tool. Its tooltip SHALL say that it carries fluids. A Tanker shuttle SHALL carry up to its tier's fluid capacity of one fluid at a time (`tanker_capacity` per tier in `data/<ns>/shuttles/shuttle.json`, examples 4000, 8000 and 16000 mB for Mk1 to Mk3).

#### Scenario: Install
- **WHEN** a player puts a Tanker into the install slot of a workshop holding a Mk1 shuttle with a Harvester
- **THEN** the shuttle has the Tanker as its tool and the Harvester goes to the player

#### Scenario: No item jobs
- **WHEN** a warehouse whose only free shuttle has a Tanker gets a terminal request
- **THEN** the request waits and the Tanker shuttle does not take it

#### Scenario: Rides the lift
- **WHEN** an Import gantry is on the ground floor and the only Storage gantry with room is on the floor above, reached by an elevator shaft
- **THEN** the Tanker shuttle rides the lift car to bring the fluid up

### Requirement: Fluid put-away runs
Every `fluid_check_interval` ticks (in `data/<ns>/shuttles/warehouse.json`, example 40) an online warehouse SHALL look at each tank of its Import gantries. For a tank, the free fluid SHALL be its amount minus what tanker runs already queued or under way will take from it. The warehouse SHALL queue one tanker run for that tank when no run for it is queued or under way, the free fluid is at least `tanker_min_load` (in `warehouse.json`, example 1000 mB) or the tank is full, the warehouse has a Tanker shuttle that reaches the gantry, and a Storage gantry tank has room for the fluid. The run SHALL take the least of the free fluid, the fluid capacity of the shuttle that takes it, and the room of the chosen target tank.

#### Scenario: Pipe fills, tanker collects
- **WHEN** a pump fills an Import gantry tank with 2000 mB of water and a Storage gantry has an empty tank on Any
- **THEN** a Tanker shuttle drives to the Import gantry, takes the water and brings it into the Storage tank

#### Scenario: Small amounts wait
- **WHEN** an Import gantry tank holds 300 mB of water and is not full
- **THEN** no tanker run is queued for it

### Requirement: Target tank by lock
The target of a fluid put-away SHALL be chosen among the Storage gantry tanks of the warehouse that a Tanker shuttle reaches, in this order: tanks locked to the fluid with room, then tanks on Any holding the fluid with room, then empty tanks on Any. Room SHALL be the tank's capacity minus its amount minus what runs already on their way will put into it. Within one rank the tank with the shortest way from the Import gantry SHALL be chosen.

#### Scenario: Locked tank first
- **WHEN** water arrives and the warehouse has an empty tank on Any and a half full tank locked to water
- **THEN** the water goes to the tank locked to water

#### Scenario: Room counts runs on the way
- **WHEN** a Storage tank locked to water has 4000 mB of room and a run bringing 4000 mB is on its way to it
- **THEN** the next water goes to another tank or waits

### Requirement: No room, no run
When an Import gantry tank holds fluid and no Storage gantry tank of the warehouse has room for that fluid, the warehouse SHALL queue no run for it, the fluid SHALL stay in the Import gantry, and the pipes feeding it SHALL back up once it is full. The Import gantry's activity lamp SHALL be red and its GUI state line SHALL name the fluid that has no room. When room appears, runs SHALL start again by themselves. An Import gantry holding fluid that no Tanker shuttle of the warehouse reaches SHALL show the red lamp as well, and its state line SHALL say that no Tanker shuttle reaches it.

#### Scenario: Backed up import
- **WHEN** every Storage tank is locked to water or full and an Import gantry holds lava
- **THEN** no tanker drives, the Import gantry's lamp is red and its GUI says lava has no room

### Requirement: Tanker run
On a tanker run the shuttle SHALL drive to the rail under the source gantry, stop there for `pump_ticks` (in `gantry.json`, example 40) shortened by Handling like other stops, and take the run's fluid out of the source tank, as much as it still holds up to the run's amount. It SHALL then drive to the rail under the target gantry, stop the same way and put the fluid into the target tank. Fluid that does not fit there SHALL go to the next Storage tank with room by the target order; when none has room, the rest SHALL go back into the source tank as far as it has room and what still does not fit SHALL be lost. Then the shuttle SHALL go home. The warehouse's fluid stock SHALL follow every transfer.

#### Scenario: Partial pick
- **WHEN** a run for 4000 mB arrives and the source tank holds only 2500 mB because an Export pipe drained it meanwhile
- **THEN** the tanker takes 2500 mB and brings them to the target

#### Scenario: Target filled by hand meanwhile
- **WHEN** the target tank was filled by bucket while the tanker was on its way and only half of the cargo fits
- **THEN** the other half goes to the next Storage tank with room

### Requirement: Tanker runs that cannot go on
When the source gantry is removed or leaves Import mode before the tanker took its fluid, the tanker SHALL go home empty. When the target gantry is removed or leaves Storage mode during a run, the tanker SHALL bring its cargo to another Storage tank by the target order and otherwise back to the source as in a tanker run. When the rail it stands on or drives over is removed, its garage loses the way home, its garage is removed or the player recalls the garage's shuttles, it SHALL be back in its garage and its fluid cargo SHALL be lost. A run that ends in one of these ways SHALL NOT be queued again by itself; the next check MAY queue a new one.

#### Scenario: Recall with cargo
- **WHEN** a Tanker shuttle carrying 3000 mB of water is recalled
- **THEN** it is back in its garage and carries nothing

### Requirement: Spout and stream
While a Tanker shuttle stops under a gantry to take or put fluid, the gantry's spout SHALL lower onto the tanker's filler cap and a stream of the fluid SHALL run between them, flowing up into the gantry when the gantry takes fluid and down into the tanker when it gives fluid; when the stop ends the spout SHALL rise back. The gantry's activity lamp SHALL be orange while a run for it is queued or under way and green when it has nothing to do. Clients SHALL learn the stop's start and end, not every tick of it.

#### Scenario: Unloading at a Storage gantry
- **WHEN** a tanker stops under a Storage gantry to put water into it
- **THEN** the spout lowers onto the tanker, a water stream runs up into the gantry and the spout rises when the tanker drives on

### Requirement: Tanker look
A Tanker shuttle SHALL show a steel tank lying along its body with two bands and a copper filler cap on top, staying within the shuttle body's width and below the top of the rail's block, so it fits under the gantry spout and on a lift car. A Tanker shuttle carrying fluid SHALL show a sight glass on each side filled to its share. Every tool look rule of the shuttles (steel and iron textures on every face, no shared planes where boxes overlap) SHALL hold.

#### Scenario: Tanker on a lift car
- **WHEN** a Tanker shuttle stands on a lift car
- **THEN** no part of it reaches outside the car's posts or above its block
