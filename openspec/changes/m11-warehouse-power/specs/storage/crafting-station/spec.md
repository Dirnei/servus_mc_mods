## ADDED Requirements

### Requirement: Station pays FE while it works
For every tick a Crafting Station works on a craft of its job it SHALL pay `crafting_station_fe_per_tick` (example 5, `storage/power.json`) from the controller of the first warehouse its data line reaches, the one with the lowest controller position. A station that is idle, waits for inputs or waits for room in its outputs SHALL pay nothing. A tick that cannot be paid SHALL not advance the craft: the station pauses with its progress kept, says so in its GUI and on its front, and goes on once the tick is paid. A station whose line reaches no online warehouse SHALL not craft.

#### Scenario: One craft
- **WHEN** a station crafts sticks with a work time of 40 ticks and 5 FE per tick
- **THEN** the controller pays 200 FE for the craft

#### Scenario: Power lost mid craft
- **WHEN** the controller's buffer empties 30 ticks into a craft of 40
- **THEN** the craft stops at 30 of 40, the station's GUI says it waits for power, and after FE arrives the craft finishes after 10 more ticks

#### Scenario: Waiting for inputs is free
- **WHEN** a station holds a job but its inputs have not arrived
- **THEN** the controller pays nothing for it

## MODIFIED Requirements

### Requirement: Crafting
The station SHALL have nine input slots and four output slots. While it has a job with crafts left, it SHALL craft the job's pattern whenever the pattern's items are all present in the inputs and the result and remainders fit the outputs, each craft taking `work_ticks` (example 40) of work and counting one craft off the job. Finished items and remainders SHALL go to the outputs. Without a job it SHALL craft nothing. Its work SHALL be paid with FE from its warehouse's controller per working tick.

#### Scenario: Sticks from planks
- **WHEN** the station has a sticks job for 2 crafts, four planks are in the inputs and its warehouse has FE
- **THEN** after the work time four sticks are in the outputs and two planks remain, and after the work time again eight sticks and the job has no crafts left

#### Scenario: Outputs full
- **WHEN** the result would not fit the outputs
- **THEN** the craft waits and the inputs are untouched

#### Scenario: No job
- **WHEN** the station has no job and planks lie in its inputs
- **THEN** nothing is crafted
