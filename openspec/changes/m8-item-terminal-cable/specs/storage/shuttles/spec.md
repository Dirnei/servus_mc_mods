# Spec Delta

## MODIFIED Requirements

### Requirement: Pick jobs
A request at the counter terminal, or a shipment to a data member, SHALL be split into pick jobs of up to one stack each, one per source block and stack. A job SHALL ride a shuttle when the source touches a rail or segment some garage's home rail reaches and the warehouse has a shuttle; a job for a crate or foreign inventory no shuttle can reach SHALL be served instantly; a job for a rack SHALL only ever ride a shuttle. The free shuttle with the shortest way SHALL take a job, drive to the source rail, take the stack, drive to the rail of the Access Point that ships the request and deliver. Pick jobs and put-away jobs SHALL share one queue and SHALL be taken in the order they were made.

#### Scenario: Pick by shuttle
- **WHEN** a player requests a stack of cobblestone and the only crate holding it stands beside a rail five blocks from a garage
- **THEN** nothing arrives at once, a shuttle drives there and to the Access Point, and the cobblestone travels on as packets into the terminal's output slots

#### Scenario: Request larger than a stack
- **WHEN** a player requests 100 cobblestone held in one crate
- **THEN** two pick jobs are made, of 64 and 36

#### Scenario: Queued jobs
- **WHEN** three requests are made with one shuttle
- **THEN** the shuttle serves them one after another in the order they were made

#### Scenario: Stock gone
- **WHEN** the crate is emptied by hand before the shuttle arrives
- **THEN** the shuttle returns with nothing and the request shows what is missing

#### Scenario: Mixed queue
- **WHEN** a put-away arrives at a station, then a request is made, with one shuttle that is busy
- **THEN** the shuttle does the put-away first and the pick after it

### Requirement: Delivery
A shuttle SHALL deliver its cargo into the delivery buffer of the block the job names: the outgoing buffer of an Access Point for a request or shipment, the buffer of an Outbound Station for a feed. It drives to the rail touching that block. When the buffer has no room the shuttle SHALL wait beside it with its cargo until there is room. When the block is gone the cargo SHALL drop where it was. A destination touching no rail SHALL NOT take shuttle deliveries, and requests for it SHALL be served instantly or refused.

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
- **WHEN** the only Access Point on the terminal's data line touches no rail and the player requests an item that only sits in a rack
- **THEN** the request is refused with the reason that there is no rail

#### Scenario: Feed to a dock
- **WHEN** a feed of 4 ingots is made for an Outbound Station on the rail
- **THEN** the shuttle unloads the ingots into the station's buffer
