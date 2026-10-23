## MODIFIED Requirements

### Requirement: Shuttles move on the rail graph
Each shuttle SHALL park on the home rail of its garage, the rail touching that garage. It SHALL travel only along rails and elevator segments at its own speed, the base speed plus its Speed upgrades (in `shuttle.json`, example 0.15 blocks per tick without upgrades), and SHALL take the cheapest way by travel cost (see `shuttles/travel-cost`). It SHALL change level only by riding a shaft's lift car. All stock of a warehouse sits in Heavy Duty Racks and SHALL be served only by shuttles; a warehouse without a shuttle that reaches its racks serves nothing.

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

#### Scenario: Cheapest way
- **WHEN** a rack can be reached by a straight aisle or by an equally long way with corners
- **THEN** the shuttle drives the straight aisle

### Requirement: Pick jobs
A request at the storage terminal, or a shipment to a data member, SHALL be split into pick jobs of up to one stack each, taken from the bays holding the item, the cheapest bay by travel cost to the destination first. Only items in slots no other job has locked SHALL count. Each pick SHALL lock the bay slots it takes from and a free slot of its destination buffer (see `shuttles/slot-locks`). A job SHALL ride a shuttle when the rack's front rail is reached by some garage's home rail and the warehouse has a shuttle that fits the job; otherwise that part SHALL be refused with the reason or left missing. Among the free shuttles whose modules fit the job, the one with the cheapest way SHALL take it, drive to the rack's rail, take the locked items, drive to the rail of the Packing Station that ships the request and deliver. A pick whose destination slot is not locked yet SHALL wait in the queue while later jobs go ahead. Pick jobs and put-away jobs SHALL share one queue and SHALL be taken in the order they were made.

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
- **WHEN** the rack is broken before the shuttle arrives and no other rack holds the item
- **THEN** the shuttle returns with nothing and the request shows what is missing

#### Scenario: Cheapest bay
- **WHEN** two racks hold cobblestone, one 3 and one 12 rail blocks from the Packing Station, and 64 are requested
- **THEN** the pick takes them from the nearer rack

#### Scenario: Picks do not count each other's items twice
- **WHEN** a rack holds 64 cobblestone and two requests of 64 are made one after the other
- **THEN** the first gets the 64 and the second is short by 64

#### Scenario: Mixed queue
- **WHEN** a put-away arrives at a station, then a request is made, with one shuttle that is busy
- **THEN** the shuttle does the put-away first and the pick after it

### Requirement: Delivery
A shuttle SHALL deliver its cargo into the delivery buffer of the block the job names, into the slot locked for it: the outgoing buffer of a Packing Station for a request or shipment, the buffer of an Outbound Station for a feed. It drives to the rail touching that block. Because the slot was locked before the shuttle was sent, the delivery SHALL always fit and the shuttle SHALL NOT wait beside the destination. When the destination is gone or no longer takes deliveries, the cargo SHALL be put away into storage by the put-away routing, or parked in the shuttle's garage when no rack has room, and SHALL count as missing for the request. A destination touching no rail SHALL NOT take shuttle deliveries, and requests for it SHALL be refused.

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

#### Scenario: Destination broken on the way
- **WHEN** the Outbound Station a shuttle brings 16 sticks to is broken while it drives
- **THEN** the shuttle puts the sticks into a rack and nothing drops
