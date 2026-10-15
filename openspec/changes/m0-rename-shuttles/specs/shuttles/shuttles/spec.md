# Spec Delta

## MODIFIED Requirements

### Requirement: Shuttle item and fleet
A shuttle SHALL be a Shuttle Mk1, Mk2 or Mk3 item; Mk1 is craftable. Shuttles SHALL be kept in Shuttle Garages, each up to `garage_capacity` (in `data/<ns>/shuttles/warehouse.json`, example 4); the fleet of a warehouse is every shuttle of every garage in its network. Each shuttle item in a garage slot SHALL be one shuttle of the fleet with the tier, upgrades and tool of that item, so shuttles that differ are told apart. The Warehouse Controller SHALL NOT take shuttles.

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

### Requirement: Shuttles pay FE per block
A shuttle SHALL pay `shuttle_fe_per_block` (example 20, `shuttles/power.json`) from its warehouse's controller for every block of rail or farm rail it starts to drive, whatever its job: picks, put-aways, deliveries, field runs and the way home. A ride on the lift car SHALL cost the same per block of height, paid in full when the car takes the shuttle. Standing, loading, unloading, field work at a plot and waiting SHALL cost nothing.

#### Scenario: Six rails out and back
- **WHEN** a shuttle drives to a rack six rails away and back with 20 FE per block
- **THEN** the controller pays 240 FE for the trip

#### Scenario: Lift ride
- **WHEN** a shuttle rides the lift car up 4 floors of one block each
- **THEN** the controller pays 80 FE when the car takes it

### Requirement: Shuttles are drawn moving
Every player who can see a garage SHALL see its shuttles that are out on a job on the rails, each drawn with its own look (see `shuttles/shuttle-looks`), moving smoothly along their paths. A shuttle with an open hold SHALL show its cargo on top; a shuttle with a closed hold SHALL NOT show its cargo. Idle shuttles SHALL sit inside their garage and SHALL NOT be drawn on the rail; a shuttle appears on the garage's home rail when it leaves and disappears into the garage when it is back. A shuttle SHALL show its cargo only on the legs where it carries something.

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
