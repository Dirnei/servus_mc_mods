## ADDED Requirements

### Requirement: Shuttles pay FE per block
A shuttle SHALL pay `shuttle_fe_per_block` (example 20, `storage/power.json`) from its warehouse's controller for every block of rail or farm rail it starts to drive, whatever its job: picks, put-aways, deliveries, field runs and the way home. A ride on the lift car SHALL cost the same per block of height, paid in full when the car takes the shuttle. Standing, loading, unloading, field work at a plot and waiting SHALL cost nothing.

#### Scenario: Six rails out and back
- **WHEN** a shuttle drives to a rack six rails away and back with 20 FE per block
- **THEN** the controller pays 240 FE for the trip

#### Scenario: Lift ride
- **WHEN** a shuttle rides the lift car up 4 floors of one block each
- **THEN** the controller pays 80 FE when the car takes it

### Requirement: Shuttles stop without power
A shuttle that cannot pay for its next block SHALL stop at the start of that block, keep its cargo and its jobs, and drive on once the block is paid; a shuttle beside a shaft that cannot pay its ride SHALL wait there. A ride already paid SHALL finish. While the controller is in the no power state, no shuttle SHALL leave its garage for a new job; field runs SHALL not start either. Shuttles stopped for power SHALL be drawn standing on the rail.

#### Scenario: Power lost mid trip
- **WHEN** a shuttle carrying iron is three blocks from a Packing Station and the controller's buffer empties
- **THEN** the shuttle stops at the next block with the iron, and after FE arrives it drives on and unloads the iron

#### Scenario: No new jobs
- **WHEN** a request needs a rack and the controller is in the no power state
- **THEN** no shuttle leaves its garage and the job waits until the state ends
