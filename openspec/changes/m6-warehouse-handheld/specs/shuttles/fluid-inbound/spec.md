# Spec Delta

## MODIFIED Requirements

### Requirement: Put-away by tanker
The buffer tank SHALL be a source of tanker put-away runs by the rules for fluid put-away runs: a run is queued when the tank's free fluid is at least `tanker_min_load` or the tank is full, a Tanker shuttle reaches the station and a fluid store has room. The tanker SHALL stop at the rail in front of the station's lower half. The buffer SHALL stay untouched while the warehouse is offline.

#### Scenario: Pump to rack
- **WHEN** a pump fills a Fluid Inbound with 4,000 mB of water and the warehouse has an empty Fluid Rack shelf and a Tanker shuttle
- **THEN** the tanker stops in front of the station, takes the water and brings it into the rack
