# Spec Delta

## Purpose

The Sink is a 2x2x2 brick multiblock that voids surplus items, fluids and energy from automation and by hand, at a rate set by the Chimney stacked on one of its top corners, and shows its work with one big spinning fan and chimney smoke.

## ADDED Requirements

### Requirement: Forming with the carrot
Clicking a horizontal face of an unformed Sink block with the Configuration Carrot SHALL form a Sink when that block is part of a 2x2x2 cube of unformed Sink blocks whose front side contains the clicked face. The clicked face direction SHALL become the front of the whole cube. When no such cube exists, nothing SHALL change and the player SHALL get an action bar message.

#### Scenario: Form a cube
- **WHEN** the player builds eight Sink blocks as a 2x2x2 cube and clicks the north face of an upper front block with the carrot
- **THEN** all eight blocks form one Sink facing north

#### Scenario: Incomplete cube
- **WHEN** the player clicks a Sink block that is part of only seven Sink blocks in a 2x2x2 shape
- **THEN** nothing forms and the action bar says a full 2x2x2 cube of Sink blocks is needed

#### Scenario: Already formed blocks are not reused
- **WHEN** a cube of unformed Sink blocks touches a formed Sink
- **THEN** forming the new cube never takes blocks of the formed Sink

### Requirement: Unformed blocks are casings
An unformed Sink block SHALL accept nothing, void nothing and open no GUI. Right clicking it SHALL tell the player in the action bar to form the Sink with the Configuration Carrot.

#### Scenario: Hopper into a casing
- **WHEN** a hopper points into an unformed Sink block
- **THEN** the hopper keeps its items

### Requirement: Breaking unforms
Removing any block of a formed Sink, by breaking, by the sneak Configuration Carrot or otherwise, SHALL unform all remaining blocks of that Sink. Every Sink block SHALL drop as one plain Sink block. The remaining blocks SHALL be formable again once the cube is complete. Formed Sink blocks SHALL not be moved by pistons.

#### Scenario: Break a corner
- **WHEN** the player breaks one block of a formed Sink
- **THEN** the other seven are unformed casings, the fan is gone and the broken block drops a Sink block

#### Scenario: Rebuild
- **WHEN** the player places a Sink block back into the gap and clicks the front with the carrot
- **THEN** the Sink forms again

### Requirement: Sink voids items, fluids and energy
A formed Sink SHALL accept items, any fluid and FE from neighbours through the standard item, fluid and energy capabilities on every outer face except the four front faces, with no configuration. All faces SHALL share one rate budget per resource. The Sink SHALL destroy everything it accepts, SHALL never hold, give back or drop anything it accepted, and SHALL offer nothing for extraction.

#### Scenario: Hopper into the Sink
- **WHEN** a hopper holding cobblestone points into the side of a Sink with an unlimited rate
- **THEN** the cobblestone leaves the hopper and appears nowhere

#### Scenario: Front refuses
- **WHEN** a hopper points into a front face of the Sink
- **THEN** the hopper keeps its items

#### Scenario: Nothing to extract
- **WHEN** a hopper below the Sink tries to pull from it
- **THEN** it receives nothing

#### Scenario: Energy and fluid on different blocks
- **WHEN** a Power Cable touches the back of one block and a neighbour fills water into the top of another block of the same Sink
- **THEN** both are accepted within the rate and the Sink's reported energy and fluid stay zero

### Requirement: Chimney column
A Chimney block placed straight on top of a top block of a formed Sink, and every Chimney block stacked straight on top of that one, SHALL form a chimney column. The Sink's chimney height SHALL be the height of its tallest column. Counting SHALL stop at the first block above that is not a Chimney. The height SHALL update as soon as a Chimney block of any column is placed or removed, also when it is not the lowest one.

#### Scenario: Three high column on a corner
- **WHEN** the player stacks three Chimney blocks on the back right top block of a formed Sink
- **THEN** the Sink reports a chimney height of 3

#### Scenario: Tallest column counts
- **WHEN** one top corner carries two Chimney blocks and another carries four
- **THEN** the Sink reports a chimney height of 4

#### Scenario: Middle block removed
- **WHEN** the second of four Chimney blocks of the only column is broken
- **THEN** the Sink reports a chimney height of 1 and the two upper blocks no longer count

#### Scenario: Chimney without a Sink
- **WHEN** Chimney blocks stand on a stone block or on an unformed Sink block
- **THEN** they are decorative only and emit no smoke

### Requirement: Chimney look
Stacked Chimney blocks SHALL join into one brick column without seams, and the top block of a column SHALL show a cap with an open flue.

#### Scenario: Cap moves up
- **WHEN** the player adds a Chimney block on top of a two block column
- **THEN** the cap is shown only on the new top block

### Requirement: Rate depends on chimney height
For each resource the Sink SHALL accept at most the rate configured for its current chimney height: items per second, fluid in mB per tick and energy in FE per tick. Unused capacity SHALL carry over for at most one second. What exceeds the rate SHALL be refused and stay with the sender.

#### Scenario: Base rate without a chimney
- **WHEN** the item rate for height 0 is 4 per second and a sender offers 64 items to an idle Sink at once
- **THEN** the Sink accepts 4 items and the sender keeps 60

#### Scenario: Taller chimney is faster
- **WHEN** the energy rate is 100 FE per tick at height 0 and 400 FE per tick at height 2 and the player adds two Chimney blocks
- **THEN** the Sink accepts up to 400 FE per tick on average

#### Scenario: Short burst after idle
- **WHEN** the fluid rate is 50 mB per tick and the Sink was idle for 5 seconds
- **THEN** a single fill of 1,000 mB is accepted at once and a second fill in the same tick is refused

### Requirement: Unlimited from a configured height
From the configured unlimited height on (example 5 Chimney blocks) the Sink SHALL accept any amount of every resource at once. Chimney blocks above that height SHALL change nothing.

#### Scenario: Five chimney blocks
- **WHEN** a Sink has a column of five Chimney blocks and a sender offers 10,000 FE and a full stack in one tick
- **THEN** all of it is accepted

### Requirement: Sink balance is data
The rate tables per chimney height for items, fluid and energy, the unlimited height and the time the Sink stays active after voiding SHALL be read from datapack JSON. The unlimited height SHALL be the first height that has no entry in the tables.

#### Scenario: Retuned rates
- **WHEN** a datapack sets the item rates to 1, 2 and 3 per second
- **THEN** a Sink with two Chimney blocks accepts 3 items per second and one with three Chimney blocks accepts any amount

### Requirement: Active state with fan and smoke
A formed Sink SHALL be active while it voided anything within the configured active time (example 2 seconds) and idle otherwise. Its front SHALL show one fan spanning all four front blocks behind a grate. An active Sink SHALL spin the fan, faster with a taller chimney, and SHALL emit smoke from the top of its tallest chimney column, or from the top of the cube without a chimney. An idle Sink SHALL show a still fan and no smoke and SHALL cost no work per tick.

#### Scenario: Smoke at the top
- **WHEN** a Sink with a column of three Chimney blocks voids items
- **THEN** its fan spins and smoke rises from above the third Chimney block

#### Scenario: Goes idle
- **WHEN** nothing reaches the Sink for longer than the active time
- **THEN** the fan stops and the smoke ends

### Requirement: Trash slot GUI
Right clicking any block of a formed Sink with an empty hand or a non fluid item SHALL open a GUI with one trash slot, the chimney height and the current rate for items, fluid and energy (or Unlimited). Any stack the player puts into the trash slot, including by shift click from the inventory, SHALL be voided at once regardless of the rate.

#### Scenario: Throw away a stack
- **WHEN** the player shift clicks 64 cobblestone in the GUI of a Sink without a chimney
- **THEN** all 64 are gone at once and the trash slot is empty again

#### Scenario: Rates shown
- **WHEN** the player opens the GUI of a Sink with a column of five Chimney blocks
- **THEN** it shows height 5 and Unlimited for all three resources

### Requirement: Buckets empty into the Sink
Using a filled fluid container on any block of a formed Sink SHALL empty it into the Sink, regardless of the rate, and leave the empty container in the player's hand.

#### Scenario: Lava bucket
- **WHEN** the player uses a lava bucket on a formed Sink
- **THEN** the lava is voided and the player holds an empty bucket

### Requirement: Crafting
Sink blocks and Chimney blocks SHALL be hand crafted from bricks and iron without research.

#### Scenario: Craft both
- **WHEN** the player has bricks and iron in a crafting table
- **THEN** they can craft Sink blocks and Chimney blocks without any research
