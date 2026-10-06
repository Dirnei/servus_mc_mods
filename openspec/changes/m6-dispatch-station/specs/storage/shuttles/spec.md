# Spec Delta

## MODIFIED Requirements

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
