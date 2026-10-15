# shuttles/shuttles Specification

## Purpose

Shuttles are the warehouse's pickers: small carts that live on the controller, travel the rails to fetch requested items and are drawn moving so the warehouse looks alive.

## Requirements

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
Each shuttle SHALL park on the home rail of its garage, the rail touching that garage. It SHALL travel only along rails and elevator segments at its own speed, the base speed plus its Speed upgrades (in `shuttle.json`, example 0.15 blocks per tick without upgrades). It SHALL change level only by riding a shaft's lift car. All stock of a warehouse sits in Heavy Duty Racks and SHALL be served only by shuttles; a warehouse without a shuttle that reaches its racks serves nothing.

#### Scenario: Round trip
- **WHEN** a shuttle without Speed upgrades is sent to a rack six rails away at 0.15 blocks per tick
- **THEN** it reaches the rail in front of the rack after about 40 ticks, stands there for its stop, and is back at its garage about 40 ticks after leaving the rack

#### Scenario: No home rail
- **WHEN** a garage holds shuttles but no rail touches it
- **THEN** its shuttles stay parked inside, serve nothing, and the summary says they are parked without a rail

#### Scenario: No garage
- **WHEN** a warehouse has racks on a rail and no garage
- **THEN** the racks still join the warehouse and the terminal lists their contents, but nothing is picked or put away

#### Scenario: Rack without a shuttle
- **WHEN** a warehouse has a rack on a rail and no garage
- **THEN** a request for an item in the rack is refused with the reason that it needs a shuttle

### Requirement: Pick jobs
A request at the storage terminal, or a shipment to a data member, SHALL be split into pick jobs of up to one stack each, one per rack and stack. A job SHALL ride a shuttle when the rack's front rail is reached by some garage's home rail and the warehouse has a shuttle that fits the job; otherwise that part SHALL be refused with the reason or left missing. Among the free shuttles whose modules fit the job, the one with the shortest way SHALL take it, drive to the rack's rail, take the stack, drive to the rail of the Packing Station that ships the request and deliver. Pick jobs and put-away jobs SHALL share one queue and SHALL be taken in the order they were made.

#### Scenario: Pick by shuttle
- **WHEN** a player requests a stack of cobblestone and the only rack holding it faces a rail five blocks from a garage
- **THEN** nothing arrives at once, a shuttle drives there and to the Packing Station, and the cobblestone travels on as packets into the terminal's output slots

#### Scenario: Request larger than a stack
- **WHEN** a player requests 100 cobblestone held in one rack
- **THEN** two pick jobs are made, of 64 and 36

#### Scenario: Queued jobs
- **WHEN** three requests are made with one shuttle
- **THEN** the shuttle serves them one after another in the order they were made

#### Scenario: Stock gone
- **WHEN** the rack is emptied by hand before the shuttle arrives
- **THEN** the shuttle returns with nothing and the request shows what is missing

#### Scenario: Mixed queue
- **WHEN** a put-away arrives at a station, then a request is made, with one shuttle that is busy
- **THEN** the shuttle does the put-away first and the pick after it

### Requirement: Delivery
A shuttle SHALL deliver its cargo into the delivery buffer of the block the job names: the outgoing buffer of a Packing Station for a request or shipment, the buffer of an Outbound Station for a feed. It drives to the rail touching that block. When the buffer has no room the shuttle SHALL wait beside it with its cargo until there is room. When the block is gone the cargo SHALL drop where it was. A destination touching no rail SHALL NOT take shuttle deliveries, and requests for it SHALL be refused.

#### Scenario: Player left
- **WHEN** the requesting player logs out before the shuttle arrives
- **THEN** the stack still reaches the terminal's output slots as packets and waits there

#### Scenario: Output full
- **WHEN** the terminal's output slots are full when the packets arrive
- **THEN** the packets wait at the terminal and land as soon as the player takes something out

#### Scenario: Terminal broken
- **WHEN** the terminal is broken while packets of its request are on the line
- **THEN** no item is lost: the packets settle as the data line rules say

#### Scenario: Terminal off the rails
- **WHEN** the only Packing Station on the terminal's data line touches no rail and the player requests an item that sits in a rack
- **THEN** the request is refused with the reason that there is no rail

#### Scenario: Feed to a dock
- **WHEN** a feed of 4 ingots is made for an Outbound Station on the rail
- **THEN** the shuttle unloads the ingots into the station's buffer

### Requirement: Shuttles are drawn moving
Every player who can see a garage SHALL see its shuttles that are out on a job on the rails, each drawn with its own look (see `storage/shuttle-looks`), moving smoothly along their paths. A shuttle with an open hold SHALL show its cargo on top; a shuttle with a closed hold SHALL NOT show its cargo. Idle shuttles SHALL sit inside their garage and SHALL NOT be drawn on the rail; a shuttle appears on the garage's home rail when it leaves and disappears into the garage when it is back. A shuttle SHALL show its cargo only on the legs where it carries something.

#### Scenario: Watching a pick
- **WHEN** a player stands in the aisle while a Mk1 shuttle runs a pick
- **THEN** the shuttle drives past along the rail, with the picked item visible in its open crate on its way to the terminal

#### Scenario: Closed hold
- **WHEN** a Mk2 shuttle carries iron ingots past the player
- **THEN** the shuttle shows its closed iron hold and no ingots

#### Scenario: Idle fleet
- **WHEN** a garage holds three idle shuttles
- **THEN** no shuttle is drawn on its rail and the garage's right click says three shuttles, none out

#### Scenario: Watching a put-away
- **WHEN** a Mk1 shuttle carries cobblestone from the station to a rack
- **THEN** the shuttle shows cobblestone on that leg and nothing on the way home

### Requirement: Shuttles ride the lift car between floors
A shuttle whose way leads up or down a shaft SHALL stop on the rail beside the shaft and call the car. It SHALL wait there until the car stands at its floor and its call is first in line. Then it drives onto the car, rides to the floor where its way leaves the shaft, and drives off. A shuttle that waits SHALL be drawn standing still.

#### Scenario: Pick from the upper floor
- **WHEN** a garage is on the ground floor, the car is on the ground floor, and a request needs a rack two floors up
- **THEN** the shuttle drives into the car, rides up, drives to the rack, comes back into the car, rides down and delivers

#### Scenario: Waiting for the car
- **WHEN** a shuttle reaches a shaft whose car is two floors up with another shuttle on board
- **THEN** the shuttle stands still on the rail beside the shaft until the car has delivered the other shuttle and come down for it

#### Scenario: Goal inside the shaft
- **WHEN** a shuttle's goal is a rail level of the shaft itself
- **THEN** it ends its leg on the car at that floor and keeps the car until it drives out

### Requirement: Shuttles cross a shaft on its deck
A shuttle that passes through a segment on one floor without changing level SHALL NOT call the car. It SHALL wait on the rail beside the shaft while the deck there is folded for a moving car, and SHALL cross as soon as the deck is down or the car stands at that floor. The car SHALL NOT leave or pass a floor while a shuttle is crossing it.

#### Scenario: Deck down
- **WHEN** a shuttle's way crosses a shaft from west to east on the ground floor and the car is parked two floors up
- **THEN** the shuttle drives straight through without stopping

#### Scenario: Car in the way
- **WHEN** the car is moving through that floor as the shuttle arrives
- **THEN** the shuttle waits beside the shaft until the car has passed and the deck is down, then crosses

#### Scenario: Car parked at the floor
- **WHEN** the car stands at that floor with no shuttle on board
- **THEN** the shuttle crosses over the car's platform

### Requirement: A broken shaft drops its rider's cargo
When a segment of a shaft is removed, every shuttle riding that shaft's car or waiting for it SHALL lose its cargo the way it would with its garage gone, and SHALL be sent home. If it has no way home, it SHALL be returned to its garage. A job not yet loaded SHALL go back to the queue. Shuttles crossing the shaft on a deck SHALL carry on.

#### Scenario: Segment broken during a ride
- **WHEN** a player breaks a middle segment while the car carries a shuttle with bread
- **THEN** the bread drops at the shuttle's garage, the request shows the bread as missing, and the shuttle returns to its garage

#### Scenario: Empty ride
- **WHEN** the shaft breaks while the car carries an empty shuttle on its way to a source
- **THEN** the pick job goes back to the front of the queue and nothing drops

### Requirement: Jobs go to shuttles whose modules fit
Every job SHALL name the modules a shuttle needs for it and whether it is field work. Warehouse picks and put-aways SHALL need no module and SHALL NOT be field work. A free shuttle SHALL take a job only when it has every module the job needs and, for a job that is not field work, is not field only. A warehouse job that no shuttle of the warehouse fits SHALL be refused, or wait at its station, with the reason that it needs a shuttle; a field job that no shuttle fits, or that no free fitting shuttle can drive to, SHALL be dropped, because field work needs a shuttle. A job that only busy shuttles fit SHALL wait in the queue while later jobs that free shuttles fit go ahead.

#### Scenario: Only field only shuttles
- **WHEN** every shuttle of a warehouse is field only and a player requests cobblestone from a rack on the rails
- **THEN** the request is refused with the reason that it needs a shuttle

#### Scenario: Shuttles of different tiers share the queue
- **WHEN** a warehouse has a Mk1 shuttle and a Mk2 shuttle and two picks from different racks are made
- **THEN** each takes one of them

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

### Requirement: Shuttles speed up and brake
A shuttle SHALL start every leg from standing, speed up by its acceleration each tick until it reaches its top speed, and brake at the same rate so it comes to a stop at the end of its leg and before a lift ride, never past it. The acceleration SHALL come from the shuttle stats file as a base plus a share per Speed upgrade (examples `accel` 0.01 and `accel_per_upgrade` 0.0025 blocks per tick per tick), so a faster shuttle accelerates harder but still needs a longer run to reach its top speed. A shuttle stopped by a gate (waiting for the car or a deck) SHALL start again from standing. Clients SHALL draw the same speeding up and braking.

#### Scenario: Plain shuttle starts
- **WHEN** a shuttle without Speed upgrades leaves its garage
- **THEN** it reaches its top speed after about 15 ticks and about one block

#### Scenario: Fast shuttle needs a longer run
- **WHEN** a Mk3 shuttle with eight Speed upgrades leaves its garage
- **THEN** it accelerates harder than a plain shuttle and reaches its top speed after about four blocks

#### Scenario: Braking for the end of the leg
- **WHEN** a shuttle at top speed nears the rail beside the rack it picks from
- **THEN** it slows down and stands still on that rail without overshooting it

### Requirement: The lift car speeds up and brakes
The lift car SHALL start each trip from standing, speed up by the lift acceleration each tick up to the lift speed, and brake at the same rate so it stops exactly on its floor. The acceleration SHALL come from the warehouse stats file (example `lift_accel` 0.005 blocks per tick per tick). A car held by a crossing deck SHALL start again from standing. Clients SHALL draw the same speeding up and braking.

#### Scenario: Car trip
- **WHEN** the car carries a shuttle two floors up
- **THEN** it starts slowly, reaches its full speed, and slows down before it stops on the second floor

### Requirement: Shuttles pay FE per block
A shuttle SHALL pay `shuttle_fe_per_block` (example 20, `storage/power.json`) from its warehouse's controller for every block of rail or farm rail it starts to drive, whatever its job: picks, put-aways, deliveries, field runs and the way home. A ride on the lift car SHALL cost the same per block of height, paid in full when the car takes the shuttle. Standing, loading, unloading, field work at a plot and waiting SHALL cost nothing.

#### Scenario: Six rails out and back
- **WHEN** a shuttle drives to a rack six rails away and back with 20 FE per block
- **THEN** the controller pays 240 FE for the trip

#### Scenario: Lift ride
- **WHEN** a shuttle rides the lift car up 4 floors of one block each
- **THEN** the controller pays 80 FE when the car takes it

### Requirement: Shuttles stop without power
A shuttle that cannot pay for its next block SHALL stop at the start of that block, keep its cargo and its jobs, and drive on once the block is paid; a shuttle beside a shaft that cannot pay its ride SHALL wait there. A ride already paid SHALL finish. While the controller is in the no power state, no shuttle SHALL leave its garage for a new job; field runs SHALL not start either. Shuttles stopped for power SHALL be drawn standing on the rail.

#### Scenario: Power lost mid trip
- **WHEN** a shuttle carrying iron is three blocks from a Packing Station and the controller's buffer empties
- **THEN** the shuttle stops at the next block with the iron, and after FE arrives it drives on and unloads the iron

#### Scenario: No new jobs
- **WHEN** a request needs a rack and the controller is in the no power state
- **THEN** no shuttle leaves its garage and the job waits until the state ends
