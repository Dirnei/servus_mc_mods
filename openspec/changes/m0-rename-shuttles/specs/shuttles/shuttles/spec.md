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
