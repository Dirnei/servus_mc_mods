## MODIFIED Requirements

### Requirement: Shuttle item and fleet
A shuttle SHALL be a Shuttle Mk1, Mk2 or Mk3 item; Mk1 is craftable. Shuttles SHALL be kept in Shuttle Garages, each up to `garage_capacity` (in `data/<ns>/storage/warehouse.json`, example 4); the fleet of a warehouse is every shuttle of every garage in its network. Each shuttle item in a garage slot SHALL be one shuttle of the fleet with the tier, upgrades and tool of that item, so shuttles that differ are told apart. The Warehouse Controller SHALL NOT take shuttles.

#### Scenario: Add and take back
- **WHEN** a player right clicks a garage twice with shuttles and then sneak right clicks it with an empty hand
- **THEN** the warehouse has one shuttle and the player got one shuttle item back

#### Scenario: Cap
- **WHEN** a garage already holds its capacity of shuttles
- **THEN** another shuttle item is refused and stays in hand

#### Scenario: Controller refuses shuttles
- **WHEN** a player right clicks the Warehouse Controller with a shuttle item
- **THEN** the shuttle stays in hand and the controller shows its summary

#### Scenario: Mixed garage
- **WHEN** a garage holds a Mk1 shuttle and a Mk3 shuttle
- **THEN** the warehouse has two shuttles, one that carries two stacks and one that carries eight

### Requirement: Shuttles move on the rail graph
Each shuttle SHALL park on the home rail of its garage, the rail touching that garage. It SHALL travel only along rails and elevator segments at its own speed, the base speed plus its Speed upgrades (in `shuttle.json`, example 0.15 blocks per tick without upgrades). It SHALL change level only by riding a shaft's lift car. Stock in crates and foreign inventories that no shuttle can reach SHALL be served instantly. Stock in Heavy Duty Racks SHALL be served only by shuttles.

#### Scenario: Round trip
- **WHEN** a shuttle without Speed upgrades is sent to a crate six rails away at 0.15 blocks per tick
- **THEN** it reaches the rail beside the crate after about 40 ticks, stands there for its stop, and is back at its garage about 40 ticks after leaving the crate

#### Scenario: No home rail
- **WHEN** a garage holds shuttles but no rail touches it
- **THEN** its shuttles stay parked inside, serve nothing, and the summary says they are parked without a rail

#### Scenario: No garage
- **WHEN** a warehouse has crates on a rail and no garage
- **THEN** requests for the crates' items are served instantly

#### Scenario: Rack without a shuttle
- **WHEN** a warehouse has a rack on a rail and no garage
- **THEN** a request for an item that only sits in the rack is refused with the reason that it needs a shuttle

### Requirement: Jobs go to shuttles whose modules fit
Every job SHALL name the modules a shuttle needs for it and whether it is field work. Warehouse picks and put-aways SHALL need no module and SHALL NOT be field work. A free shuttle SHALL take a job only when it has every module the job needs and, for a job that is not field work, is not field only. A warehouse job that no shuttle of the warehouse fits SHALL be served as if the warehouse had no shuttle; a field job that no shuttle fits, or that no free fitting shuttle can drive to, SHALL be dropped, because field work needs a shuttle. A job that only busy shuttles fit SHALL wait in the queue while later jobs that free shuttles fit go ahead.

#### Scenario: Only field only shuttles
- **WHEN** every shuttle of a warehouse is field only and a player requests cobblestone from a crate beside a rail
- **THEN** the cobblestone is served instantly as without shuttles

#### Scenario: Shuttles of different tiers share the queue
- **WHEN** a warehouse has a Mk1 shuttle and a Mk2 shuttle and two picks from different racks are made
- **THEN** each takes one of them

## REMOVED Requirements

### Requirement: A Cargo Hold shuttle carries several jobs that share a way
**Reason**: Every shuttle now carries several stacks by its tier; the requirement moves to "A shuttle carries several jobs that share a way".
**Migration**: None.

## ADDED Requirements

### Requirement: A shuttle carries several jobs that share a way
A shuttle that takes a job SHALL also take further queued jobs that share its way, in queue order, until it carries as many jobs as its tier's stacks: picks from the same source to the same destination, put-aways from the same station to the same target. Each job SHALL be loaded, delivered and accounted on its own, and a job that is not loaded when its shuttle is recalled SHALL go back to the queue.

#### Scenario: Two stacks in one trip
- **WHEN** a player requests 100 cobblestone held in one rack and the only shuttle is a Mk1 with two stacks
- **THEN** the shuttle takes both pick jobs, of 64 and 36, drives once and delivers all 100

#### Scenario: More than the tier holds
- **WHEN** a player requests 200 cobblestone held in one rack and the only shuttle is a Mk1 with two stacks
- **THEN** the shuttle takes the jobs of 64 and 64 first and the rest wait in the queue for its next trip

#### Scenario: Put-away in one trip
- **WHEN** an inbound station holds three stacks of cobblestone that go to the same rack and the only shuttle is a Mk2 with four stacks
- **THEN** the shuttle loads all three at the station and puts them into the rack on one trip

#### Scenario: Different ways
- **WHEN** two picks wait, one from rack A and one from rack B, and the only shuttle is a Mk2
- **THEN** the shuttle takes the pick from rack A first and the one from rack B on its next trip

### Requirement: Shuttles pause at a lift handover
A shuttle that rides a lift car SHALL drive into the car, stand still for a short handover time, and only then be carried; when the car reaches its floor, the shuttle SHALL stand still for the same time before it drives off. The time SHALL come from the warehouse stats file (example `lift_handover_ticks` 10). A shuttle SHALL never be drawn moving up or down a shaft by itself: between syncs a client SHALL not move it past the start of a vertical step, so only the car moves it vertically.

#### Scenario: Ride with pauses
- **WHEN** a shuttle with full Speed rides the car from the ground floor to the second floor
- **THEN** it drives into the car, stands still, rides up with the car, stands still at the second floor and then drives off, and it is never drawn above the car
