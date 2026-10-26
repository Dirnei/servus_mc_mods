## MODIFIED Requirements

### Requirement: Duct inspect
Using the Data Potato on an arm of a duct SHALL print a report into the player's chat, one line per present line of the duct: for the item, power and fluid lines the side's flags (In, Out, Pull, or not connected), its redstone setting, marked switched off when its On/Off button is off or paused when the setting is not met, its channel name in its colour, for items and fluids the priority and the number of filter entries, the number of ducts in the connected run of that line, and how many sides on that channel the run takes from (in) and delivers to (out), a side with In and Out counting on both; for the data line the tier, its bandwidth in items per second and the load of the last second. Using it on the duct's centre SHALL report each line's run size and, for the data line, tier and load, and whether the duct is powered, without side details. It SHALL open no GUI and change nothing.

#### Scenario: Item arm
- **WHEN** the clicked arm's item side is Out on "Iron ore" with priority 2 and Ignore redstone, the run has 14 ducts and on "Iron ore" two sides have In and three have Out
- **THEN** the item line's report names Out, Ignore redstone, "Iron ore" in its colour, priority 2, 14 ducts, 2 in and 3 out

#### Scenario: Fluid arm
- **WHEN** the clicked arm's fluid side is only Out with fluid priority 4 and a fluid filter listing lava and water
- **THEN** the fluid line's report names Out, priority 4 and 2 filter entries

#### Scenario: Side blocked by redstone
- **WHEN** the clicked arm's fluid side is While unpowered and a lever powers the duct
- **THEN** the fluid line's report names While unpowered and that the side is paused

#### Scenario: Side switched off
- **WHEN** the clicked arm's fluid side is While unpowered and switched off
- **THEN** the fluid line's report names While unpowered and that the side is switched off

#### Scenario: Data line
- **WHEN** the clicked duct also carries a Data Cable with 24 of 32 items per second used in the last second
- **THEN** the data line's report names the Data Cable tier, 32 items per second and 24 used

#### Scenario: Nothing changes
- **WHEN** the player uses the potato on a duct's arm
- **THEN** no GUI opens and the duct's flags, redstone settings, On/Off, channels and filters are unchanged
