## MODIFIED Requirements

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

### Requirement: Jobs go to shuttles whose modules fit
Every job SHALL name the modules a shuttle needs for it and whether it is field work. Warehouse picks and put-aways SHALL need no module and SHALL NOT be field work. A free shuttle SHALL take a job only when it has every module the job needs and, for a job that is not field work, is not field only. A warehouse job that no shuttle of the warehouse fits SHALL be refused, or wait at its station, with the reason that it needs a shuttle; a field job that no shuttle fits, or that no free fitting shuttle can drive to, SHALL be dropped, because field work needs a shuttle. A job that only busy shuttles fit SHALL wait in the queue while later jobs that free shuttles fit go ahead.

#### Scenario: Only field only shuttles
- **WHEN** every shuttle of a warehouse is field only and a player requests cobblestone from a rack on the rails
- **THEN** the request is refused with the reason that it needs a shuttle

#### Scenario: Shuttles of different tiers share the queue
- **WHEN** a warehouse has a Mk1 shuttle and a Mk2 shuttle and two picks from different racks are made
- **THEN** each takes one of them
