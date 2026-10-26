# foundation/data-potato Specification

## Purpose
The Data Potato is Foundation's investigation tool: held, it shows analysis views such as the data duct load; used on a duct or Fluid Tank, it reports what that block is doing.

## Requirements

### Requirement: Data Potato item
The Data Potato SHALL be a Foundation item that stacks to 1, crafted from a potato, a Data Cable and redstone, and shown in the Foundation creative tab.

#### Scenario: Craft
- **WHEN** the player crafts a potato, a Data Cable and a redstone dust in the potato's pattern
- **THEN** one Data Potato is produced

### Requirement: Potato reveals analysis views
While a player holds the Data Potato in either hand, the player SHALL see the analysis views (the data duct load overlay) and see-through facade panels, but no side markers.

#### Scenario: Potato in hand
- **WHEN** a player without goggles holds the Data Potato near busy data ducts and a machine
- **THEN** the data ducts show their load colours, and the machine shows no side markers

#### Scenario: Carrot in hand
- **WHEN** a player without goggles holds the Configuration Carrot near the same ducts
- **THEN** the machine shows its side markers and the data ducts show no load colours

### Requirement: Duct inspect
Using the Data Potato on an arm of a duct SHALL print a report into the player's chat, one line per present line of the duct: for the item, power and fluid lines the side's flags (In, Out, Pull, or not connected), its redstone setting, marked switched off when its On/Off button is off or paused when the setting is not met, its channel name in its colour, for items and fluids the priority, the number of filter entries and how many of them carry an amount, the number of ducts in the connected run of that line, and how many sides on that channel the run takes from (in) and delivers to (out), a side with In and Out counting on both; for the data line the tier, its bandwidth in items per second and the load of the last second. Using it on the duct's centre SHALL report each line's run size and, for the data line, tier and load, and whether the duct is powered, without side details. It SHALL open no GUI and change nothing.

#### Scenario: Item arm
- **WHEN** the clicked arm's item side is Out on "Iron ore" with priority 2 and Ignore redstone, the run has 14 ducts and on "Iron ore" two sides have In and three have Out
- **THEN** the item line's report names Out, Ignore redstone, "Iron ore" in its colour, priority 2, 14 ducts, 2 in and 3 out

#### Scenario: Fluid arm
- **WHEN** the clicked arm's fluid side is only Out with fluid priority 4 and a fluid filter listing lava and water
- **THEN** the fluid line's report names Out, priority 4 and 2 filter entries

#### Scenario: Entries with amounts
- **WHEN** the clicked arm's item side has In and Pull with the entries "oak sapling, 64" and "stick"
- **THEN** the item line's report names 2 filter entries, 1 with an amount

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

### Requirement: Tank inspect
Using the Data Potato on a Fluid Tank SHALL print into chat the whole tank's fluid, amount and capacity in mB and its size in blocks.

#### Scenario: Tank report
- **WHEN** the player uses the potato on a 3 by 2 by 3 tank holding 12,000 mB of water
- **THEN** the chat shows water, 12,000 mB of its capacity and the size 3 x 2 x 3

#### Scenario: Empty tank
- **WHEN** the player uses the potato on an empty single block tank
- **THEN** the chat shows that it is empty, its capacity and the size 1 x 1 x 1

### Requirement: Endpoint tally is pure
Counting the in and out sides of one channel SHALL be plain Java without Minecraft classes and covered by unit tests.

#### Scenario: Unit tested
- **WHEN** the unit tests run
- **THEN** they cover in and out, pull in, push out and disabled sides, and sides on other channels being ignored

### Requirement: Blocks of other mods give potato reports
Any Servus block SHALL be able to answer the Data Potato with its own chat report, also when it lives in a mod that Foundation does not know. Using the potato on such a block SHALL print that report and SHALL NOT open its GUI or change it. Using the potato on a named machine SHALL print its label first, also when its block gives no report of its own, and SHALL NOT open its GUI. Using the potato on a block that gives no report and is no named machine SHALL do what any other item does there.

#### Scenario: Report from another mod
- **WHEN** Servus: Shuttles is installed and a player uses the Data Potato on a Heavy Duty Rack
- **THEN** the rack's report is printed into the player's chat and no GUI opens

#### Scenario: Block without a report
- **WHEN** a player uses the Data Potato on a vanilla chest
- **THEN** no report is printed and the potato acts like a plain item

#### Scenario: Named machine without a report
- **WHEN** a player uses the Data Potato on Shuttle Garage "Main Garage (Garage #1)"
- **THEN** chat shows "Main Garage (Garage #1)" and the garage GUI does not open
