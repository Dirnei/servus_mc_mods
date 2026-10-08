# Spec Delta

## MODIFIED Requirements

### Requirement: Pattern jobs
A Crafting CPU SHALL hand a Crafting Station a job: one crafting pattern and how many crafts to make. A station SHALL hold at most one job; a second job SHALL be refused while one has crafts left. The CPU SHALL end the job when the step is done or its order is cancelled, and the station is then idle. A job SHALL be saved with the station, with its crafts left and the CPU step it belongs to, and SHALL survive a restart like the order. A station whose job belongs to a step that no longer exists on a loaded CPU SHALL end the job by itself. Leftover inputs SHALL stay in the station when a job ends.

#### Scenario: Job set
- **WHEN** a CPU hands an idle station a sticks job for 2 crafts
- **THEN** the station reports sticks as its active recipe and shows 2 crafts left

#### Scenario: Busy station
- **WHEN** a station has a sticks job with crafts left and another CPU hands it a torch job
- **THEN** the torch job is refused and the sticks job stays

#### Scenario: Cancel
- **WHEN** the order of a station's job is cancelled
- **THEN** the station is idle and keeps whatever is in its inputs

#### Scenario: Job survives a restart
- **WHEN** a station has a sticks job with 2 crafts left and the server restarts
- **THEN** the station still has the job with 2 crafts left and its CPU still counts it

#### Scenario: Orphaned job
- **WHEN** a station holds a job whose CPU is loaded but no longer has that order
- **THEN** the station ends the job and is idle
