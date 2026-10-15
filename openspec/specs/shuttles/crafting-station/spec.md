# shuttles/crafting-station Specification

## Purpose

The Crafting Station is Shuttles' own producer: a powerless block that crafts the vanilla recipes a player configured, so a warehouse without Alchemy can still autocraft.

## Requirements

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

### Requirement: Station reports its production
The station SHALL expose the Lib production capability with no fixed recipes, and the recipe of its job as active while the job has crafts left.

#### Scenario: Idle station
- **WHEN** an idle station's production capability is queried
- **THEN** it reports no recipes and nothing active

#### Scenario: Configured sticks
- **WHEN** the station has a sticks job with crafts left
- **THEN** the sticks recipe is reported as active

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from a crafting table, iron ingots, a chest and planks, recording the wood of the planks, SHALL drop itself with its wood and its items when broken, and SHALL be dismantled by sneaking with the Configuration Carrot keeping its configuration and wood. The carrot without sneaking SHALL cycle the mode of the clicked side, like on machines.

#### Scenario: Crafting
- **WHEN** the player places a crafting table over a chest framed by iron ingots over a row of planks
- **THEN** a Crafting Station carrying the wood of the planks is produced

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

### Requirement: Station online lamp
The Crafting Station SHALL show an online lamp on its front, lit while a formed Crafting CPU or an online warehouse is on its data line and dark otherwise. The lamp SHALL be refreshed about once a second, not by scanning blocks every tick.

#### Scenario: On a line with a CPU
- **WHEN** a Crafting Station touches a data duct whose line has a formed CPU
- **THEN** its lamp is lit within about a second

#### Scenario: Alone
- **WHEN** a Crafting Station touches no data duct
- **THEN** its lamp is dark

### Requirement: Station shows crafting
While the Crafting Station holds a job and crafts, its front SHALL show an animated working face; otherwise the front is still.

#### Scenario: Crafting
- **WHEN** the station crafts a pattern job
- **THEN** its front animates until the job ends

### Requirement: Outputs keep a station busy
A Crafting Station whose output slots still hold items SHALL NOT be idle for jobs of anyone but the owner of its current job: it SHALL refuse a new job, and a Crafting CPU SHALL NOT give it a step but use another free station. Its own current job's outputs SHALL still be collected as usual, and once its outputs are empty it SHALL be idle again. Leftovers without any job SHALL keep it closed until a player or automation takes them out; the station's own push of outputs without a job still applies. The Production Terminal's Machines tab SHALL show such a station as blocked with "outputs not collected" instead of idle.

#### Scenario: Leftovers skip the station
- **WHEN** one of two Crafting Stations on a line holds cobblestone in its outputs and no job, and a sticks order starts
- **THEN** the sticks step runs on the other station, the first one gets no job and the Machines tab shows it blocked

#### Scenario: Own outputs still collected
- **WHEN** a station finishes the crafts of its job and its outputs wait for the CPU
- **THEN** the CPU collects them as before and the station is idle once they are gone

### Requirement: Station sides pull and push
The station SHALL register no item capability, so hoppers, item pipes and machines can neither insert into nor extract from it and pipes show no item arm toward it. Packets SHALL feed its inputs and collect its outputs through its packet inlet, which takes only items the current job's pattern uses and nothing while the station has no job with crafts left. Every side but the front SHALL be Pull, Push or Off, all Push by default; the front is fixed to Off, shown in the Lib side panel and cycled by the carrot. While the station has no job, items in the outputs SHALL be pushed into adjacent inventories through Push sides, checking regularly; while it holds a job it SHALL NOT push, because the CPU collects the job's outputs. Items the job uses SHALL be pulled from neighbours through Pull sides.

#### Scenario: Packets feed the job
- **WHEN** the station has a sticks job and packets of planks arrive over a data duct
- **THEN** the planks go into the inputs

#### Scenario: No job refuses packets
- **WHEN** the station has no job and packets of planks are sent to it
- **THEN** the planks stay at their source

#### Scenario: Unused item refused
- **WHEN** the station has a sticks job and packets of cobblestone are sent to it
- **THEN** the cobblestone stays at its source

#### Scenario: Hopper above the station
- **WHEN** the station has a sticks job and a hopper above it offers planks
- **THEN** the planks stay in the hopper

#### Scenario: Chest feeds and takes leftovers
- **WHEN** the station has a sticks job, its top side is Pull with a chest of planks above it, its back is Push against an empty chest, and the order is cancelled after the first craft
- **THEN** the planks move into the station, the sticks stay in its outputs while the job is held, and they end up in the back chest once the job ends

#### Scenario: Job outputs stay for the CPU
- **WHEN** the station holds a sticks job and has crafted sticks
- **THEN** the sticks stay in the outputs until the CPU collects them as packets, and the order finishes

#### Scenario: Item pipe beside the station
- **WHEN** a duct with the Item Pipe and the data line is placed beside the station
- **THEN** the duct shows a data arm but no item arm toward the station

#### Scenario: Front closed
- **WHEN** the player uses the carrot on the front
- **THEN** the front stays Off

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
