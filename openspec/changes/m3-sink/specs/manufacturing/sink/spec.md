# Spec Delta

## Purpose

The Sink voids surplus items, fluids and energy from automation and by hand, at a rate set by the brick Chimney stacked on top of it, and shows its work with a spinning fan and chimney smoke.

## ADDED Requirements

### Requirement: Sink voids items, fluids and energy
The Sink SHALL accept items, any fluid and FE from neighbours through the standard item, fluid and energy capabilities and SHALL destroy everything it accepts. It SHALL never hold, give back or drop anything it accepted, and SHALL offer nothing for extraction.

#### Scenario: Hopper into the Sink
- **WHEN** a hopper holding cobblestone points into a Sink with an unlimited rate
- **THEN** the cobblestone leaves the hopper and appears nowhere

#### Scenario: Nothing to extract
- **WHEN** a hopper below the Sink tries to pull from it
- **THEN** it receives nothing

#### Scenario: Energy and fluid
- **WHEN** a Power Cable delivers FE and a neighbour fills water into the Sink
- **THEN** both are accepted within the rate and the Sink's reported energy and fluid stay zero

#### Scenario: Breaking keeps nothing
- **WHEN** a Sink that voided items, fluid and energy is broken
- **THEN** it drops only itself

### Requirement: Chimney column
A Chimney block placed straight on top of a Sink, and every Chimney block stacked straight on top of that one, SHALL count toward the Sink's chimney height. Counting SHALL stop at the first block above that is not a Chimney. The height SHALL update as soon as a Chimney block of the column is placed or removed, also when it is not the lowest one.

#### Scenario: Three high column
- **WHEN** the player stacks three Chimney blocks on a Sink
- **THEN** the Sink reports a chimney height of 3

#### Scenario: Middle block removed
- **WHEN** the second of four Chimney blocks above a Sink is broken
- **THEN** the Sink reports a chimney height of 1 and the two upper blocks no longer count

#### Scenario: Chimney without a Sink
- **WHEN** Chimney blocks stand on a stone block
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
- **WHEN** a Sink has five Chimney blocks and a sender offers 10,000 FE and a full stack in one tick
- **THEN** all of it is accepted

### Requirement: Sink balance is data
The rate tables per chimney height for items, fluid and energy, the unlimited height and the time the Sink stays active after voiding SHALL be read from datapack JSON. The unlimited height SHALL be the first height that has no entry in the tables.

#### Scenario: Retuned rates
- **WHEN** a datapack sets the item rates to 1, 2 and 3 per second
- **THEN** a Sink with two Chimney blocks accepts 3 items per second and one with three Chimney blocks accepts any amount

### Requirement: Active state with fan and smoke
The Sink SHALL be active while it voided anything within the configured active time (example 2 seconds) and idle otherwise. An active Sink SHALL spin the fan behind its front grate, faster with a taller chimney, and SHALL emit smoke from the top of its chimney column, or from its own top without a chimney. An idle Sink SHALL show a still fan and no smoke. An idle Sink SHALL cost no work per tick.

#### Scenario: Smoke at the top
- **WHEN** a Sink with three Chimney blocks voids items
- **THEN** its fan spins and smoke rises from above the third Chimney block

#### Scenario: Goes idle
- **WHEN** nothing reaches the Sink for longer than the active time
- **THEN** the fan stops and the smoke ends

### Requirement: Trash slot GUI
Right clicking the Sink with an empty hand or a non fluid item SHALL open a GUI with one trash slot, the chimney height, the current rate for items, fluid and energy (or Unlimited) and the shared side panel. Any stack the player puts into the trash slot, including by shift click from the inventory, SHALL be voided at once regardless of the rate.

#### Scenario: Throw away a stack
- **WHEN** the player shift clicks 64 cobblestone in the Sink GUI of a Sink without a chimney
- **THEN** all 64 are gone at once and the trash slot is empty again

#### Scenario: Rates shown
- **WHEN** the player opens the GUI of a Sink with five Chimney blocks
- **THEN** it shows height 5 and Unlimited for all three resources

### Requirement: Buckets empty into the Sink
Using a filled fluid container on the Sink SHALL empty it into the Sink, regardless of the rate, and leave the empty container in the player's hand.

#### Scenario: Lava bucket
- **WHEN** the player uses a lava bucket on the Sink
- **THEN** the lava is voided and the player holds an empty bucket

### Requirement: Sink sides
The Sink SHALL support the side modes Input and Off, default Input, configured in its GUI and cycled by the Configuration Carrot like machines. Its front, where the fan is, SHALL be fixed Off. An Off side SHALL refuse items, fluid and energy.

#### Scenario: Front refuses
- **WHEN** a hopper points into the Sink's front
- **THEN** the hopper keeps its items

#### Scenario: Side turned Off
- **WHEN** the player sets the left side to Off and a Power Cable touches the left side
- **THEN** the Sink accepts no energy from it

### Requirement: Placing, crafting and dismantling
The Sink SHALL face the player when placed. Sink and Chimney SHALL be hand crafted from bricks and iron without research. A Sink dismantled with the sneak Configuration Carrot SHALL drop as an item that restores its side configuration.

#### Scenario: Craft both
- **WHEN** the player has bricks and iron in a crafting table
- **THEN** they can craft a Sink and Chimney blocks without any research

#### Scenario: Dismantle a configured Sink
- **WHEN** the player sneak carrots a Sink with its left side Off and places it again
- **THEN** the new Sink has its left side Off
