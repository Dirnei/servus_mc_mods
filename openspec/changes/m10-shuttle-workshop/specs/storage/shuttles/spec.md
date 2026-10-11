## MODIFIED Requirements

### Requirement: Shuttle item and fleet
A Shuttle SHALL be a craftable item. Shuttles SHALL be kept in Shuttle Garages, each up to `garage_capacity` (in `data/<ns>/storage/warehouse.json`, example 4); the fleet of a warehouse is every shuttle of every garage in its network. Each Shuttle item in a garage slot SHALL be one shuttle of the fleet with the modules of that item, so shuttles with different modules are told apart. The Warehouse Controller SHALL NOT take shuttles.

#### Scenario: Add and take back
- **WHEN** a player right clicks a garage twice with shuttles and then sneak right clicks it with an empty hand
- **THEN** the warehouse has one shuttle and the player got one shuttle item back

#### Scenario: Cap
- **WHEN** a garage already holds its capacity of shuttles
- **THEN** another shuttle item is refused and stays in hand

#### Scenario: Controller refuses shuttles
- **WHEN** a player right clicks the Warehouse Controller with a Shuttle item
- **THEN** the shuttle stays in hand and the controller shows its summary

#### Scenario: Mixed garage
- **WHEN** a garage holds a plain shuttle and a shuttle with a Cargo Hold
- **THEN** the warehouse has two shuttles, one that carries one stack and one that carries several

### Requirement: Pick jobs
A request at the storage terminal, or a shipment to a data member, SHALL be split into pick jobs of up to one stack each, one per source block and stack. A job SHALL ride a shuttle when the source touches a rail or segment some garage's home rail reaches and the warehouse has a shuttle; a job for a crate or foreign inventory no shuttle can reach SHALL be served instantly; a job for a rack SHALL only ever ride a shuttle. Among the free shuttles whose modules fit the job, the one with the shortest way SHALL take it, drive to the source rail, take the stack, drive to the rail of the Packing Station that ships the request and deliver. Pick jobs and put-away jobs SHALL share one queue and SHALL be taken in the order they were made.

#### Scenario: Pick by shuttle
- **WHEN** a player requests a stack of cobblestone and the only crate holding it stands beside a rail five blocks from a garage
- **THEN** nothing arrives at once, a shuttle drives there and to the Packing Station, and the cobblestone travels on as packets into the terminal's output slots

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

## ADDED Requirements

### Requirement: Jobs go to shuttles whose modules fit
Every job SHALL name the modules a shuttle needs for it and whether it is field work. Warehouse picks and put-aways SHALL need no module and SHALL NOT be field work. A free shuttle SHALL take a job only when it has every module the job needs and, for a job that is not field work, is not field only. A job that no shuttle of the warehouse fits SHALL be served as if the warehouse had no shuttle; a job that only busy shuttles fit SHALL wait in the queue while later jobs that free shuttles fit go ahead.

#### Scenario: Only field only shuttles
- **WHEN** every shuttle of a warehouse is field only and a player requests cobblestone from a crate beside a rail
- **THEN** the cobblestone is served instantly as without shuttles

#### Scenario: Plain and Cargo Hold shuttles share the queue
- **WHEN** a warehouse has a plain shuttle and a Cargo Hold shuttle and two picks are made
- **THEN** each takes one of them

### Requirement: A Cargo Hold shuttle carries several jobs that share a way
A shuttle that takes a job SHALL also take further queued jobs that share its way, in queue order, until it carries as many jobs as its stack capacity: picks from the same source to the same destination, put-aways from the same station to the same target. Each job SHALL be loaded, delivered and accounted on its own, and a job that is not loaded when its shuttle is recalled SHALL go back to the queue. A plain shuttle SHALL take one job at a time.

#### Scenario: Two stacks in one trip
- **WHEN** a player requests 100 cobblestone held in one rack and the only shuttle has a Cargo Hold
- **THEN** the shuttle takes both pick jobs, of 64 and 36, drives once and delivers all 100

#### Scenario: Plain shuttle takes one
- **WHEN** the same request is served by a plain shuttle
- **THEN** the shuttle takes the job of 64 first and the job of 36 waits in the queue for its next trip

#### Scenario: Put-away in one trip
- **WHEN** an inbound station holds three stacks of cobblestone that go to the same rack and the only shuttle has a Cargo Hold
- **THEN** the shuttle loads all three at the station and puts them into the rack on one trip

#### Scenario: Different ways
- **WHEN** two picks wait, one from rack A and one from rack B, and the only shuttle has a Cargo Hold
- **THEN** the shuttle takes the pick from rack A first and the one from rack B on its next trip
