# Spec Delta

## MODIFIED Requirements

### Requirement: Put-away without shuttles
When the warehouse is online and the chosen target is a crate or foreign inventory that no shuttle can carry to, the warehouse SHALL move a buffer stack there at once, at most `put_away_per_tick` stacks per station per tick (example 1). Targets SHALL follow the put-away routing of the zones. A Heavy Duty Rack SHALL never be filled instantly: a stack whose only room is in racks SHALL wait for a shuttle, and when no shuttle can reach them the station SHALL report that the stack needs a shuttle. The buffer SHALL stay untouched while the warehouse is offline.

#### Scenario: Instant put-away
- **WHEN** a station touching the controller receives 64 cobblestone, a crate touches the controller and the warehouse has no garage
- **THEN** within a few ticks the cobblestone sits in the crate and the buffer is empty

#### Scenario: Offline
- **WHEN** the station belongs to a network with no controller and receives cobblestone
- **THEN** the cobblestone stays in the buffer

#### Scenario: Routed to its zone
- **WHEN** a crate is zoned "Ores" with `c:ores` and raw iron arrives at the station
- **THEN** the raw iron lands in that crate

#### Scenario: Only a rack has room
- **WHEN** a station receives bread, the only block with room is a rack and the warehouse has no garage
- **THEN** the bread waits in the buffer, the lamp turns red and the GUI says the bread needs a shuttle
