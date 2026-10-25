## MODIFIED Requirements

### Requirement: Duct inspect
Using the Data Potato on an arm of a duct SHALL print a report into the player's chat, one line per present line of the duct: for the item, power and fluid lines the side's mode, its Control and whether the side is active now, its channel name in its colour, for items the priority, the number of ducts in the connected run of that line, and how many sides on that channel the run takes from (in) and delivers to (out), an in and out side counting on both; for the data line the tier, its bandwidth in items per second and the load of the last second. Using it on the duct's centre SHALL report each line's run size and, for the data line, tier and load, and whether the duct is powered, without side details. It SHALL open no GUI and change nothing.

#### Scenario: Item arm
- **WHEN** the clicked arm's item side is on "Iron ore" in push out with priority 2 and Control Always, the run has 14 ducts and on "Iron ore" two sides pull in and three push out
- **THEN** the item line's report names Push, Always, "Iron ore" in its colour, priority 2, 14 ducts, 2 in and 3 out

#### Scenario: Side blocked by redstone
- **WHEN** the clicked arm's fluid side has Control While unpowered and a lever powers the duct
- **THEN** the fluid line's report names While unpowered and that the side is paused

#### Scenario: Data line
- **WHEN** the clicked duct also carries a Data Cable with 24 of 32 items per second used in the last second
- **THEN** the data line's report names the Data Cable tier, 32 items per second and 24 used

#### Scenario: Nothing changes
- **WHEN** the player uses the potato on a duct's arm
- **THEN** no GUI opens and the duct's modes, Controls, channels and filters are unchanged
