# storage/shuttles Specification

## Purpose

Shuttles are the warehouse's pickers: small carts that live on the controller, travel the rails to fetch requested items and are drawn moving so the warehouse looks alive.

## Requirements

### Requirement: Shuttle item and fleet
A Shuttle SHALL be a craftable item. Right clicking a Warehouse Controller with it SHALL add one shuttle to that warehouse up to a cap (`max_shuttles` in `data/<ns>/storage/warehouse.json`, example 8). Sneak right clicking the controller with an empty hand SHALL take one shuttle back as an item. Dismantling or breaking the controller SHALL drop its shuttles as items.

#### Scenario: Add and take back
- **WHEN** a player right clicks the controller twice with shuttles and then sneak right clicks it with an empty hand
- **THEN** the warehouse has one shuttle and the player got one shuttle item back

#### Scenario: Cap
- **WHEN** the warehouse already has the maximum number of shuttles
- **THEN** another shuttle item is refused and stays in hand

### Requirement: Shuttles move on the rail graph
Shuttles SHALL park on the home rail, a rail touching the controller, and SHALL travel only along rails and elevator segments at a speed from `warehouse.json` (`shuttle_speed`, example 0.25 blocks per tick). A warehouse whose controller touches no rail SHALL keep its shuttles parked and serve every pull instantly.

#### Scenario: Round trip
- **WHEN** a shuttle is sent to a crate eight rails away at a quarter block per tick
- **THEN** it reaches the rail beside the crate after about 32 ticks and is back home after about 64

#### Scenario: No home rail
- **WHEN** the controller has shuttles but no rail touches it
- **THEN** pulls are instant and the summary says the shuttles are parked without a rail

### Requirement: Pick jobs
A pull at the counter terminal SHALL become a pick job when the warehouse has at least one shuttle, the controller has a home rail, and a storage block holding the item touches a rail or segment the home rail reaches. The nearest free shuttle SHALL take the job, drive to that rail, take up to the requested stack from the block, drive home and deliver. Pick jobs and put-away jobs SHALL share one queue: jobs SHALL queue when every shuttle is busy and SHALL be taken in the order they were made, whatever their kind.

#### Scenario: Pick by shuttle
- **WHEN** a player shift clicks cobblestone at the terminal and the only crate holding it stands beside a rail five blocks from the controller
- **THEN** nothing arrives at once, a shuttle drives there and back, and the cobblestone lands in the player's inventory when it returns

#### Scenario: Queued jobs
- **WHEN** three pulls are made with one shuttle
- **THEN** the shuttle serves them one after another in the order they were made

#### Scenario: Stock gone
- **WHEN** the crate is emptied by hand before the shuttle arrives
- **THEN** the shuttle returns with nothing and the player gets nothing

#### Scenario: Mixed queue
- **WHEN** a put-away arrives at a station, then a pull is made, with one shuttle that is busy
- **THEN** the shuttle does the put-away first and the pick after it

### Requirement: Delivery
On return the shuttle SHALL put the stack into the inventory of the player who made the pull. When that player is no longer in the world or the stack does not fit, the rest SHALL drop at the terminal the pull was made from.

#### Scenario: Player left
- **WHEN** the requesting player logs out before the shuttle returns
- **THEN** the stack drops at the terminal

### Requirement: Shuttles are drawn moving
Every player who can see the controller SHALL see its shuttles as small carts on the rails, moving smoothly along their paths with their cargo shown on top, and parked on the home rail when idle. A shuttle on a put-away SHALL show its cargo between the station and the target and ride empty otherwise.

#### Scenario: Watching a pick
- **WHEN** a player stands in the aisle while a pick runs
- **THEN** a cart drives past along the rail, with the picked item visible on its way back

#### Scenario: Idle fleet
- **WHEN** three shuttles are idle
- **THEN** three carts sit on the home rail

#### Scenario: Watching a put-away
- **WHEN** a shuttle carries cobblestone from the station to a crate
- **THEN** the cart shows cobblestone on that leg and nothing on the way home
