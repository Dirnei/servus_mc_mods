# storage/crafting-station Specification

## Purpose

The Crafting Station is Storage's own producer: a powerless block that crafts the vanilla recipes a player configured, so a warehouse without Manufacturing can still autocraft.

## Requirements

### Requirement: Crafting
The station SHALL have nine input slots and four output slots. While it has a job with crafts left, it SHALL craft the job's pattern whenever the pattern's items are all present in the inputs and the result and remainders fit the outputs, each craft taking `work_ticks` (example 40) and counting one craft off the job. Finished items and remainders SHALL go to the outputs. Without a job it SHALL craft nothing. No power is needed.

#### Scenario: Sticks from planks
- **WHEN** the station has a sticks job for 2 crafts and four planks are in the inputs
- **THEN** after the work time four sticks are in the outputs and two planks remain, and after the work time again eight sticks and the job has no crafts left

#### Scenario: Outputs full
- **WHEN** the result would not fit the outputs
- **THEN** the craft waits and the inputs are untouched

#### Scenario: No job
- **WHEN** the station has no job and planks lie in its inputs
- **THEN** nothing is crafted

### Requirement: Machine like sides
The station SHALL have a Lib side configuration like a machine: Auto, Input, Output, Both, Pull, Push and Off, all sides Auto by default except the front, which is fixed to Off, shown in the Lib side panel and cycled by the carrot. Through sides that allow insertion the inputs accept only items the current job's pattern uses, and nothing while the station has no job with crafts left; through sides that allow extraction the outputs can be taken. While the station has no job, items in the outputs SHALL be pushed into adjacent inventories through Auto and Push sides, checking regularly; while it holds a job it SHALL NOT push, because the CPU collects the job's outputs. Items the job uses SHALL be pulled from neighbours through Pull sides.

#### Scenario: Packets feed the job
- **WHEN** the station has a sticks job and packets of planks arrive over a data duct on an Auto side
- **THEN** the planks go into the inputs

#### Scenario: No job refuses
- **WHEN** the station has no job and a hopper above offers planks
- **THEN** the planks stay in the hopper

#### Scenario: Unused item refused
- **WHEN** the station has a sticks job and a hopper offers cobblestone
- **THEN** the cobblestone is refused

#### Scenario: Side off
- **WHEN** the top side is Off, the station has a sticks job and a hopper above offers planks
- **THEN** the planks stay in the hopper

#### Scenario: Dock feeds and collects
- **WHEN** the station has a sticks job, an Outbound Station faces it with planks in its buffer and an Inbound Station touches it, and the job ends after its crafts
- **THEN** the planks move into the station, the sticks stay in its outputs while the job is held, and they end up in the Inbound Station's buffer once the job ends

#### Scenario: Job outputs stay for the CPU
- **WHEN** the station holds a sticks job, has crafted sticks and an Access Point touches it on an Auto side
- **THEN** the sticks stay in the outputs until the CPU collects them as packets, and the order finishes

#### Scenario: Front closed
- **WHEN** the station has a sticks job and an Outbound Station pushes planks against the station's front
- **THEN** the planks stay in the outbound buffer

### Requirement: Station reports its production
The station SHALL expose the Lib production capability with no fixed recipes, and the recipe of its job as active while the job has crafts left.

#### Scenario: Idle station
- **WHEN** an idle station's production capability is queried
- **THEN** it reports no recipes and nothing active

#### Scenario: Configured sticks
- **WHEN** the station has a sticks job with crafts left
- **THEN** the sticks recipe is reported as active

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from a crafting table, iron ingots and a chest, SHALL drop itself and its items when broken, and SHALL be dismantled by sneaking with the Configuration Carrot keeping its configuration. The carrot without sneaking SHALL cycle the mode of the clicked side, like on machines.

#### Scenario: Crafting
- **WHEN** the player places a crafting table over a chest framed by iron ingots
- **THEN** a Crafting Station is produced

### Requirement: Station GUI layout
The station GUI SHALL show the nine inputs on the left, the job in the middle, and the four outputs with a progress arrow on the right, above the player inventory and beside the side panel. The job area SHALL show the result of the job's pattern with the number of crafts left, or "idle" without a job.

#### Scenario: Working
- **WHEN** the station crafts sticks with 3 crafts left
- **THEN** the middle shows the sticks icon and 3 left, and the progress arrow fills toward the outputs

#### Scenario: Idle
- **WHEN** the station has no job
- **THEN** the middle says idle

#### Scenario: Editor open
- **WHEN** the station GUI is open
- **THEN** it has no recipe cells, no plus button and no recipe editor

### Requirement: Pattern jobs
A Crafting CPU SHALL hand a Crafting Station a job: one crafting pattern and how many crafts to make. A station SHALL hold at most one job; a second job SHALL be refused while one has crafts left. The CPU SHALL end the job when the step is done or its order is cancelled, and the station is then idle. Jobs SHALL NOT survive a restart, like the orders they belong to. Leftover inputs SHALL stay in the station when a job ends.

#### Scenario: Job set
- **WHEN** a CPU hands an idle station a sticks job for 2 crafts
- **THEN** the station reports sticks as its active recipe and shows 2 crafts left

#### Scenario: Busy station
- **WHEN** a station has a sticks job with crafts left and another CPU hands it a torch job
- **THEN** the torch job is refused and the sticks job stays

#### Scenario: Cancel
- **WHEN** the order of a station's job is cancelled
- **THEN** the station is idle and keeps whatever is in its inputs
