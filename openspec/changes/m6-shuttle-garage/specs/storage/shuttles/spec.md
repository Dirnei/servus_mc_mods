# Spec Delta

## MODIFIED Requirements

### Requirement: Shuttle item and fleet
A Shuttle SHALL be a craftable item. Shuttles SHALL be kept in Shuttle Garages, each up to `garage_capacity` (in `data/<ns>/storage/warehouse.json`, example 4); the fleet of a warehouse is every shuttle of every garage in its network. The Warehouse Controller SHALL NOT take shuttles.

#### Scenario: Add and take back
- **WHEN** a player right clicks a garage twice with shuttles and then sneak right clicks it with an empty hand
- **THEN** the warehouse has one shuttle and the player got one shuttle item back

#### Scenario: Cap
- **WHEN** a garage already holds its capacity of shuttles
- **THEN** another shuttle item is refused and stays in hand

#### Scenario: Controller refuses shuttles
- **WHEN** a player right clicks the Warehouse Controller with a Shuttle item
- **THEN** the shuttle stays in hand and the controller shows its summary

### Requirement: Shuttles move on the rail graph
Each shuttle SHALL park on the home rail of its garage, the rail touching that garage, and SHALL travel only along rails and elevator segments at a speed from `warehouse.json` (`shuttle_speed`, example 0.25 blocks per tick). Stock in crates and foreign inventories that no shuttle can reach SHALL be served instantly; stock in Heavy Duty Racks SHALL be served only by shuttles.

#### Scenario: Round trip
- **WHEN** a shuttle is sent to a crate eight rails away at a quarter block per tick
- **THEN** it reaches the rail beside the crate after about 32 ticks and is back at its garage after about 64

#### Scenario: No home rail
- **WHEN** a garage holds shuttles but no rail touches it
- **THEN** its shuttles stay parked inside, serve nothing, and the summary says they are parked without a rail

#### Scenario: No garage
- **WHEN** a warehouse has crates on a rail and no garage
- **THEN** requests for the crates' items are served instantly

#### Scenario: Rack without a shuttle
- **WHEN** a warehouse has a rack on a rail and no garage
- **THEN** a request for an item that only sits in the rack is refused with the reason that it needs a shuttle

### Requirement: Pick jobs
A request at the counter terminal SHALL be split into pick jobs of up to one stack each, one per source block and stack. A job SHALL ride a shuttle when the source touches a rail or segment some garage's home rail reaches and the warehouse has a shuttle; a job for a crate or foreign inventory no shuttle can reach SHALL be served instantly; a job for a rack SHALL only ever ride a shuttle. The free shuttle with the shortest way SHALL take a job, drive to the source rail, take the stack, drive to the terminal's rail and deliver. Pick jobs and put-away jobs SHALL share one queue and SHALL be taken in the order they were made.

#### Scenario: Pick by shuttle
- **WHEN** a player requests a stack of cobblestone and the only crate holding it stands beside a rail five blocks from a garage
- **THEN** nothing arrives at once, a shuttle drives there and to the terminal, and the cobblestone lands in the terminal's output slots

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
A shuttle SHALL deliver its cargo into the output slots of the terminal the request was made at, driving to the rail touching that terminal. When the output slots have no room the shuttle SHALL wait beside the terminal with its cargo until there is room. When the terminal is gone the cargo SHALL drop where the terminal was. A terminal touching no rail SHALL NOT take shuttle deliveries, and requests at it SHALL be served instantly or refused.

#### Scenario: Player left
- **WHEN** the requesting player logs out before the shuttle arrives
- **THEN** the stack still lands in the terminal's output slots and waits there

#### Scenario: Output full
- **WHEN** the output slots are full when the shuttle arrives
- **THEN** the cart waits beside the terminal and unloads as soon as the player takes something out

#### Scenario: Terminal broken
- **WHEN** the terminal is broken while a shuttle carries its order
- **THEN** the stack drops where the terminal stood

#### Scenario: Terminal off the rails
- **WHEN** a terminal touches no rail and the player requests an item that only sits in a rack
- **THEN** the request is refused with the reason that the terminal has no rail

### Requirement: Shuttles are drawn moving
Every player who can see a garage SHALL see its shuttles as small carts on the rails, moving smoothly along their paths with their cargo shown on top, and parked on their garage's home rail when idle. A shuttle SHALL show its cargo only on the legs where it carries something.

#### Scenario: Watching a pick
- **WHEN** a player stands in the aisle while a pick runs
- **THEN** a cart drives past along the rail, with the picked item visible on its way to the terminal

#### Scenario: Idle fleet
- **WHEN** a garage holds three idle shuttles
- **THEN** three carts sit on that garage's home rail

#### Scenario: Watching a put-away
- **WHEN** a shuttle carries cobblestone from the station to a crate
- **THEN** the cart shows cobblestone on that leg and nothing on the way home
