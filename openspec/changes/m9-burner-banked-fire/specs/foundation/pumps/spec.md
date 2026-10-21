## MODIFIED Requirements

### Requirement: Pumps water into a buffer
A working pump SHALL add `fluid_per_cycle` mB of water to its internal buffer of `fluid_capacity` mB once per `work_ticks`, and SHALL pause without spending fuel or energy while the buffer cannot take a full cycle. Once its buffer cannot take a full cycle, the pump SHALL stay off until the buffer holds less than its `restart_below` fraction of `fluid_capacity` (example 0.5), then run until the buffer is full again.

#### Scenario: Buffer fills
- **WHEN** a pump with nothing attached runs on a pond
- **THEN** its buffer fills up to `fluid_capacity` and the pump then pauses

#### Scenario: Room again
- **WHEN** a pipe drains the full buffer to below half
- **THEN** the pump resumes

#### Scenario: Small draw
- **WHEN** a pipe takes a little water from the full buffer, so that it holds more than half
- **THEN** the pump stays off and unlit

### Requirement: Burner Pump burns fuel
The Burner Pump SHALL burn any item vanilla furnaces accept as fuel, consuming one item when the previous burn has ended and work is available, like a furnace. Its burn SHALL count down only on ticks it pumps; while it pauses, a burn in progress SHALL keep its remaining time.

#### Scenario: Coal
- **WHEN** a Burner Pump on a pond has one coal and an empty buffer
- **THEN** it pumps for the coal's furnace burn time, counted over the ticks it pumps, even when a full buffer pauses it in between

#### Scenario: Full buffer keeps the burn
- **WHEN** the buffer of a burning Burner Pump is full
- **THEN** the remaining burn time stays the same until the pump runs again

#### Scenario: No fuel
- **WHEN** the fuel slot is empty and the burn has ended
- **THEN** progress stops and resumes when fuel is added

### Requirement: Working engine moves and smokes
While the Burner Pump is lit, its beam SHALL rock about its pivot with a steady stroke, the pump rod SHALL rise and fall with the beam's end, and smoke SHALL rise from the top of the chimney. While it is not lit, the beam and rod SHALL stand still and no smoke SHALL rise. Each time the pump rod of a working Burner Pump or Electric Pump reaches its lowest point, a small water splash SHALL rise at the socket where the rod dips. The stroke speed, the amount of smoke and the splash are look only and do not depend on balance data.

#### Scenario: Working
- **WHEN** a Burner Pump on a pond burns coal and fills its buffer
- **THEN** the beam rocks, the rod moves up and down, smoke rises from the chimney and water splashes at the socket once per stroke

#### Scenario: Buffer full
- **WHEN** the buffer is full and the pump pauses
- **THEN** the pump goes unlit right away with its burn kept: the beam stops, the smoke stops and no more splashes rise

#### Scenario: No source
- **WHEN** a Burner Pump with coal stands on dirt
- **THEN** the beam does not move, no smoke rises and nothing splashes
