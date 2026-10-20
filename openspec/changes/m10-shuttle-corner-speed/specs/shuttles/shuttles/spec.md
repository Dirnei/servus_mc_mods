## MODIFIED Requirements

### Requirement: Shuttles speed up and brake
A shuttle SHALL start every leg from standing, speed up by its acceleration each tick until it reaches its top speed, and brake at the same rate so it comes to a stop at the end of its leg and before a lift ride, never past it. The acceleration SHALL come from the shuttle stats file as a base plus a share per Speed upgrade (examples `accel` 0.01 and `accel_per_upgrade` 0.0025 blocks per tick per tick), so a faster shuttle accelerates harder but still needs a longer run to reach its top speed. A shuttle stopped by a gate (waiting for the car or a deck) SHALL start again from standing.

Where its path turns (a corner: the way into a rail and the way out of it point in different horizontal directions), a shuttle SHALL drive no faster than its corner speed from half a block before the corner to half a block after it, braking at its normal rate ahead of the corner so it reaches that speed in time, and speeding up again after it. The corner speed SHALL be an absolute speed from the shuttle stats file plus a small share per Speed upgrade (examples `corner` 0.1 and `corner_per_upgrade` 0.005 blocks per tick), at most the shuttle's top speed. Passing a junction straight through is not a corner. Clients SHALL draw the same speeding up, braking and slowing at corners.

#### Scenario: Plain shuttle starts
- **WHEN** a shuttle without Speed upgrades leaves its garage
- **THEN** it reaches its top speed after about 15 ticks and about one block

#### Scenario: Fast shuttle needs a longer run
- **WHEN** a Mk3 shuttle with eight Speed upgrades leaves its garage
- **THEN** it accelerates harder than a plain shuttle and reaches its top speed after about four blocks

#### Scenario: Braking for the end of the leg
- **WHEN** a shuttle at top speed nears the rail beside the rack it picks from
- **THEN** it slows down and stands still on that rail without overshooting it

#### Scenario: Fast shuttle slows for a corner
- **WHEN** a Mk3 shuttle with eight Speed upgrades at top speed reaches a corner of its path
- **THEN** it drives through the corner at no more than its corner speed (about 0.14 blocks per tick with the example numbers), then speeds up again on the straight after it

#### Scenario: Corner speed barely grows with upgrades
- **WHEN** a plain shuttle and a Mk3 shuttle with eight Speed upgrades take the same corner
- **THEN** the fast shuttle is only a little faster through the corner, although its top speed is about three times higher

#### Scenario: Straight through a junction
- **WHEN** a shuttle at top speed passes a junction rail without turning
- **THEN** it keeps its speed

#### Scenario: Corner right after the start
- **WHEN** a shuttle starts a leg one block before a corner
- **THEN** it speeds up only as far as it can still slow to its corner speed at the corner

#### Scenario: Drawn the same on clients
- **WHEN** a player watches a fast shuttle take a corner
- **THEN** the drawn shuttle slows down into the corner and speeds up after it, without jumping when the next sync arrives
