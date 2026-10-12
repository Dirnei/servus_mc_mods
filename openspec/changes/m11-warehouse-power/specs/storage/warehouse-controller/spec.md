## ADDED Requirements

### Requirement: The warehouse runs on the controller's FE
Every FE cost of a warehouse SHALL be paid from its controller's FE buffer: wireless transfers, shuttles driving, waking and Crafting Stations working. No other warehouse block SHALL take or hold FE, and nothing SHALL cost FE while idle: storing items, racks, rails, garages, stations, terminals and a CPU without running steps draw nothing. A draw SHALL be paid in full or not at all.

#### Scenario: One connection
- **WHEN** a warehouse with shuttles and a Crafting Station gets FE only through a power duct on its controller
- **THEN** its shuttles drive and the station crafts, and the controller's FE drops by what they used

#### Scenario: Idle warehouse
- **WHEN** a warehouse with 5,000 FE in its controller does nothing for a minute
- **THEN** its controller still holds 5,000 FE

#### Scenario: Draw larger than the buffer
- **WHEN** a shuttle wants to start a block costing 20 FE and the controller holds 15 FE
- **THEN** no FE is taken and the shuttle waits

### Requirement: No power state
A controller SHALL be in the no power state while its buffer is empty or a draw was refused during the last second. In that state no new shuttle job SHALL start, shuttles on a trip SHALL wait at the next block, Crafting Stations SHALL pause, no wake SHALL start and a running wake SHALL end. The state SHALL end by itself once FE arrives and the waiting draws are paid; automation then resumes where it stopped. Stock SHALL stay visible and racks, crates and their GUIs SHALL stay usable by hand.

#### Scenario: Generator runs out
- **WHEN** the only Coal Generator feeding a busy warehouse runs out of coal and the controller's buffer empties
- **THEN** the controller enters the no power state, shuttles stop at the next block and no new job starts

#### Scenario: Power returns
- **WHEN** coal goes back into the generator
- **THEN** the no power state ends, the stopped shuttles drive on with their cargo and waiting jobs start

#### Scenario: Take by hand
- **WHEN** a warehouse is in the no power state and the player opens a Heavy Duty Rack holding iron
- **THEN** the rack's GUI opens and the player takes the iron out

### Requirement: Controller shows power
The controller GUI SHALL show, next to the FE stored and the capacity, the FE used during the last second, and in the no power state a line in red saying the warehouse has no power and its automation waits. The warehouse summary SHALL say when it has no power.

#### Scenario: Use shown
- **WHEN** two shuttles drive at 3 blocks per second each with 20 FE per block
- **THEN** the controller GUI shows about 120 FE used per second

#### Scenario: No power line
- **WHEN** the controller is in the no power state and a player opens its GUI
- **THEN** the GUI shows the no power line in red

## MODIFIED Requirements

### Requirement: Over capacity is an error
A controller with at least one Chunk Loader Upgrade whose site spans more chunks than its capacity SHALL be in an error state: its GUI SHALL say the site is over capacity in red and its activity lamp SHALL be red. A controller in the no power state SHALL show the red lamp as well, and its GUI SHALL say which of the two applies. Otherwise the lamp SHALL be green while online. A controller without upgrades SHALL show no capacity error.

#### Scenario: Over capacity
- **WHEN** a controller with one upgrade of 9 chunks spans 10 chunks
- **THEN** its lamp is red and the GUI says 10 of 9 chunks, over capacity

#### Scenario: Back within capacity
- **WHEN** a second upgrade goes into that controller
- **THEN** the lamp turns green and the GUI says 10 of 18 chunks

#### Scenario: No upgrade
- **WHEN** a controller without upgrades and with FE spans 3 chunks
- **THEN** its lamp is green

#### Scenario: No power
- **WHEN** a controller within capacity holds no FE
- **THEN** its lamp is red and the GUI says the warehouse has no power

### Requirement: Controller shows the wake
While its site is awake by a wake, the controller's activity lamp SHALL be orange, unless the site is over capacity or in the no power state (red). The controller GUI SHALL show a wake line: awake for a transfer, staying awake for the grace time with the seconds left, or waiting to wake with the reason; nothing while the site is not being woken.

#### Scenario: Awake
- **WHEN** a site with FE is woken and a player opens its controller GUI
- **THEN** the lamp is orange and the GUI says the site is awake

#### Scenario: Waiting
- **WHEN** a wake waits because the team limit is reached and a player opens the controller GUI
- **THEN** the GUI says the site is waiting to wake because the team limit is reached
