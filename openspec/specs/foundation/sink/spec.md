# foundation/sink Specification

## Purpose
The Sink is a 2x2x2 brick multiblock that voids surplus items, fluids and energy from automation and by hand, at a rate set by the Chimney stacked on one of its top corners, and shows its work with one big spinning fan and chimney smoke.

## Requirements

### Requirement: Forming with the carrot
Clicking a horizontal face of an unformed Sink block with the Configuration Carrot SHALL form a Sink when that block is part of a 2x2x2 cube of unformed Sink blocks whose front side contains the clicked face and all eight blocks share one stone. The clicked face direction SHALL become the front of the whole cube. When no such cube exists, nothing SHALL change and the player SHALL get an action bar message.

#### Scenario: Form a cube
- **WHEN** the player builds eight Sink blocks as a 2x2x2 cube and clicks the north face of an upper front block with the carrot
- **THEN** all eight blocks form one Sink facing north

#### Scenario: Incomplete cube
- **WHEN** the player clicks a Sink block that is part of only seven Sink blocks in a 2x2x2 shape
- **THEN** nothing forms and the action bar says a full 2x2x2 cube of Sink blocks is needed

#### Scenario: Already formed blocks are not reused
- **WHEN** a cube of unformed Sink blocks touches a formed Sink
- **THEN** forming the new cube never takes blocks of the formed Sink

#### Scenario: Mixed stones refused
- **WHEN** the player clicks a full cube of six Stone Bricks Sink blocks and two Deepslate Bricks Sink blocks with the carrot
- **THEN** nothing forms and the action bar says all eight blocks need the same stone and names Stone Bricks and Deepslate Bricks

#### Scenario: Blocks without a stone count as Bricks
- **WHEN** a cube holds seven Bricks Sink blocks and one Sink block given by command without a stone
- **THEN** the carrot forms the Sink

#### Scenario: One stone forms
- **WHEN** the player clicks a full cube of eight Polished Deepslate Sink blocks with the carrot
- **THEN** the Sink forms and shows polished deepslate where it showed bricks

### Requirement: Unformed blocks are casings
An unformed Sink block SHALL accept nothing, void nothing and open no GUI. Right clicking it with an empty hand SHALL tell the player in the action bar to form the Sink with the Configuration Carrot. Right clicking it with an item SHALL let the item act as on any block, so the cube can be built by placing blocks against it.

#### Scenario: Hopper into a casing
- **WHEN** a hopper points into an unformed Sink block
- **THEN** the hopper keeps its items

### Requirement: Breaking unforms
Removing any block of a formed Sink, by breaking, by the sneak Configuration Carrot or otherwise, SHALL unform all remaining blocks of that Sink. The buffer SHALL be voided and the Capacity Upgrades SHALL drop. Every Sink block SHALL drop as one Sink block carrying its stone. The remaining blocks SHALL be formable again once the cube is complete. Formed Sink blocks SHALL not be moved by pistons.

#### Scenario: Break a corner
- **WHEN** the player breaks one block of a formed Sink
- **THEN** the other seven are unformed casings, the fan is gone and the broken block drops a Sink block

#### Scenario: Rebuild
- **WHEN** the player places a Sink block back into the gap and clicks the front with the carrot
- **THEN** the Sink forms again

#### Scenario: Buffer gone, upgrades drop
- **WHEN** a Sink with two Capacity Upgrades holds 40 cobblestone and the player breaks one of its blocks
- **THEN** two Capacity Upgrades drop, no cobblestone drops and a reformed Sink starts empty

#### Scenario: Stone kept on the drop
- **WHEN** the player dismantles a Sandstone Sink block with the sneak carrot
- **THEN** the dropped Sink block carries Sandstone

### Requirement: Sink voids items, fluids and energy
A formed Sink SHALL accept items, any fluid and FE from neighbours through the standard item, fluid and energy capabilities on every outer face except the four front faces, with no configuration. All faces SHALL share one buffer and one energy rate budget. Items and fluid SHALL go into the buffer and FE SHALL be destroyed at once. The Sink SHALL offer nothing for extraction to automation and SHALL never drop what it accepted.

#### Scenario: Hopper into the Sink
- **WHEN** a hopper holding cobblestone points into the side of a Sink with an unlimited rate
- **THEN** the cobblestone leaves the hopper, goes through the buffer and appears nowhere

#### Scenario: Front refuses
- **WHEN** a hopper points into a front face of the Sink
- **THEN** the hopper keeps its items

#### Scenario: Nothing to extract
- **WHEN** a hopper below a Sink that holds cobblestone tries to pull from it
- **THEN** it receives nothing

#### Scenario: Energy and fluid on different blocks
- **WHEN** a Power Cable touches the back of one block and a neighbour fills water into the top of another block of the same Sink
- **THEN** both are accepted, the water lands in the first tank and the Sink's reported energy stays zero

### Requirement: Chimney column
A Chimney block placed straight on top of a top block of a formed Sink, and every Chimney block stacked straight on top of that one, SHALL form a chimney column, whatever stones its blocks have. The Sink's chimney height SHALL be the height of its tallest column. Counting SHALL stop at the first block above that is not a Chimney. The height SHALL update as soon as a Chimney block of any column is placed or removed, also when it is not the lowest one.

#### Scenario: Three high column on a corner
- **WHEN** the player stacks three Chimney blocks on the back right top block of a formed Sink
- **THEN** the Sink reports a chimney height of 3

#### Scenario: Tallest column counts
- **WHEN** one top corner carries two Chimney blocks and another carries four
- **THEN** the Sink reports a chimney height of 4

#### Scenario: Middle block removed
- **WHEN** the second of four Chimney blocks of the only column is broken
- **THEN** the Sink reports a chimney height of 1 and the two upper blocks no longer count

#### Scenario: Mixed stones count
- **WHEN** a column on a formed Sink holds a Bricks, a Tuff Bricks and a Mud Bricks Chimney block
- **THEN** the Sink reports a chimney height of 3

#### Scenario: Chimney without a Sink
- **WHEN** Chimney blocks stand on a stone block or on an unformed Sink block
- **THEN** they do not count for any Sink and are a free standing column whose smoke follows its own settings (see `foundation/chimney`)

### Requirement: Chimney look
Stacked Chimney blocks SHALL join into one column without seams, each block in its own stone, and the top block of a column SHALL show a cap with an open flue.

#### Scenario: Cap moves up
- **WHEN** the player adds a Chimney block on top of a two block column
- **THEN** the cap is shown only on the new top block

#### Scenario: Stones per block
- **WHEN** a column holds a Stone Bricks block under a Bricks block
- **THEN** the lower block shows stone bricks and the upper block shows bricks with the cap

### Requirement: Rate depends on chimney height
For each resource the Sink SHALL void at most the rate configured for its current chimney height: items per second and fluid in mB per second, voided in batches every 20 ticks, and energy in FE per tick, accepted at once. Unused energy capacity SHALL carry over for at most one second, and energy above the rate SHALL be refused and stay with the sender.

#### Scenario: Base rate without a chimney
- **WHEN** the item rate for height 0 is 4 per second and a sender puts 64 items into an empty Sink at once
- **THEN** the Sink holds all 64, voids 4 per batch and is empty after 16 batches

#### Scenario: Taller chimney is faster
- **WHEN** the energy rate is 100 FE per tick at height 0 and 400 FE per tick at height 2 and the player adds two Chimney blocks
- **THEN** the Sink accepts up to 400 FE per tick on average

#### Scenario: Short burst after idle
- **WHEN** the energy rate is 100 FE per tick and the Sink was idle for 5 seconds
- **THEN** a single offer of 2,000 FE is accepted at once and a second offer in the same tick is refused

### Requirement: Unlimited from a configured height
From the configured unlimited height on (example 5 Chimney blocks) every batch SHALL void the whole buffer and the Sink SHALL accept any amount of energy at once. Chimney blocks above that height SHALL change nothing.

#### Scenario: Five chimney blocks
- **WHEN** a Sink has a column of five Chimney blocks and holds 7 full stacks and 8,000 mB of lava
- **THEN** the next batch voids all of it

#### Scenario: Unlimited energy
- **WHEN** a Sink has a column of five Chimney blocks and a sender offers 10,000 FE in one tick
- **THEN** all of it is accepted

### Requirement: Sink balance is data
The rate tables per chimney height for items per second, fluid in mB per second and energy in FE per tick, the unlimited height, the time the Sink stays active after voiding, the slots per row, the tank capacity and the number of Capacity Upgrade slots SHALL be read from datapack JSON. The unlimited height SHALL be the first height that has no entry in the tables.

#### Scenario: Retuned rates
- **WHEN** a datapack sets the item rates to 1, 2 and 3 per second
- **THEN** a Sink with two Chimney blocks voids 3 items per batch and one with three Chimney blocks voids its whole buffer per batch

#### Scenario: Bigger tanks
- **WHEN** a datapack sets the tank capacity to 16,000 mB
- **THEN** each open tank of the Sink holds 16,000 mB

### Requirement: Active state with fan and smoke
A formed Sink SHALL be active while it voided anything within the configured active time (example 2 seconds) and idle otherwise. Its front SHALL show one fan spanning all four front blocks behind a grate. An active Sink SHALL spin the fan, faster with a taller chimney, and SHALL emit smoke from the top of its tallest chimney column, or from the top of the cube without a chimney. An idle Sink SHALL show a still fan and no smoke and SHALL cost no work per tick.

#### Scenario: Smoke at the top
- **WHEN** a Sink with a column of three Chimney blocks voids items
- **THEN** its fan spins and smoke rises from above the third Chimney block

#### Scenario: Goes idle
- **WHEN** the buffer is empty and nothing reaches the Sink for longer than the active time
- **THEN** the fan stops and the smoke ends

### Requirement: Buckets empty into the Sink
Using a filled fluid container on any block of a formed Sink SHALL empty it into the Sink's tanks like any other sender and leave the empty container in the player's hand. When the tanks have no room for the whole content, the container SHALL stay full.

#### Scenario: Lava bucket
- **WHEN** the player uses a lava bucket on a formed Sink with an empty tank
- **THEN** the tank holds 1,000 mB of lava and the player holds an empty bucket

#### Scenario: No room
- **WHEN** the only open tank holds water and the player uses a lava bucket on the Sink
- **THEN** the player keeps the full lava bucket

### Requirement: Crafting
Sink blocks and Chimney blocks SHALL be hand crafted from any stone of `#servus_foundation:stones` without research: Sink blocks from stone and iron, Chimney blocks from stone alone. The result SHALL carry the stone of the grid as defined in `foundation/materials`.

#### Scenario: Craft both
- **WHEN** the player has Stone Bricks and iron bars in a crafting table
- **THEN** they can craft Stone Bricks Sink blocks and Stone Bricks Chimney blocks without any research

#### Scenario: Bricks still work
- **WHEN** the player crafts Sink blocks from Bricks and iron bars
- **THEN** the result carries Bricks and looks like the Sink always did

### Requirement: Buffer of slots and tanks
A formed Sink SHALL hold a buffer of item slots in rows and of fluid tanks: one open row (example 7 slots) and one open tank (example 8,000 mB), plus one open row and one open tank for every Capacity Upgrade it holds. Each tank SHALL hold one fluid at a time. Items and fluids that arrive SHALL go into the open slots and tanks: first onto matching stacks and into tanks with the same fluid, then into empty ones. What does not fit SHALL be refused and stay with the sender.

#### Scenario: Hopper fills the buffer
- **WHEN** a hopper offers cobblestone to a Sink without upgrades and with an empty buffer
- **THEN** the cobblestone goes into the first slot and the hopper is not refused while the open row has room

#### Scenario: Full buffer refuses
- **WHEN** all 7 open slots hold full stacks of different items and a sender offers dirt
- **THEN** the dirt is refused and stays with the sender

#### Scenario: Two fluids queue
- **WHEN** a Sink with one Capacity Upgrade receives water and then lava
- **THEN** the water is in the first tank and the lava in the second

#### Scenario: Second fluid without a free tank
- **WHEN** a Sink without upgrades holds water and a sender offers lava
- **THEN** the lava is refused

### Requirement: Voiding in batches
While its buffer holds anything, a formed Sink SHALL void one batch every 20 ticks: up to the item rate of its chimney height in items, taken from the first slot on, and up to the fluid rate in mB, drained from the first tank on. Voided items and fluid SHALL be gone. A Sink with an empty buffer SHALL do no work for its buffer.

#### Scenario: Four items per batch
- **WHEN** the item rate for height 0 is 4 per second and a Sink without a chimney holds 64 cobblestone in its first slot
- **THEN** 4 cobblestone are voided every 20 ticks and the slot holds 60 after the first batch

#### Scenario: Batch spans slots
- **WHEN** the item rate is 8 per second and the first slot holds 3 dirt and the second 64 gravel
- **THEN** one batch voids the 3 dirt and 5 gravel

#### Scenario: Fluid batch
- **WHEN** the fluid rate for height 0 is 1,000 mB per second and the first tank holds 8,000 mB of water
- **THEN** one batch leaves 7,000 mB of water

#### Scenario: Empties and stops
- **WHEN** the last item of the buffer is voided
- **THEN** no further batch runs until something arrives again

### Requirement: Capacity Upgrade slots on the Sink
A formed Sink SHALL have capacity upgrade slots (example 3, in `sink.json`) in its GUI. Taking a Capacity Upgrade out SHALL be refused while the last open row or the last open tank holds anything, and the GUI SHALL say why. The upgrades SHALL be kept when the world is saved.

#### Scenario: Upgrade opens a row and a tank
- **WHEN** the player puts one Capacity Upgrade into the Sink
- **THEN** the GUI shows two open rows and two open tanks and senders can fill them

#### Scenario: Removal refused while the last row holds items
- **WHEN** a Sink with one Capacity Upgrade holds cobblestone in its second row and the player takes the upgrade out
- **THEN** the upgrade stays in its slot and the GUI says the row and tank must be empty first

#### Scenario: Removal after emptying
- **WHEN** the second row and second tank are empty and the player takes the only upgrade out
- **THEN** the upgrade comes out and the second row and tank are locked again

### Requirement: Taking items back by hand
The player SHALL be able to take items out of the open slots in the GUI, including by shift click into the inventory, and put items in by hand. Automation SHALL NOT be able to extract items or fluid from the Sink.

#### Scenario: Rescue a stack
- **WHEN** a pipe sent diamonds into the Sink and the player takes them out of the GUI before their batch
- **THEN** the player holds the diamonds and they are no longer in the buffer

#### Scenario: Player puts a stack in
- **WHEN** the player shift clicks 64 cobblestone from the inventory into the GUI of a Sink with a free open slot
- **THEN** the cobblestone moves into the buffer and is voided in batches

### Requirement: Buffer is kept on save
The buffer contents and the Capacity Upgrades SHALL be kept when the world is saved and loaded, and a Sink that still holds something after loading SHALL go on voiding in batches.

#### Scenario: Reload with a full buffer
- **WHEN** a Sink holds 30 cobblestone and the world is saved and loaded again
- **THEN** it still holds the cobblestone and voids it in batches

### Requirement: Buffer GUI
Right clicking any block of a formed Sink with an empty hand or an item that is neither a fluid container nor a Chimney block SHALL open a GUI with the rows of slots, each row's tank beside it, the upgrade slots, the chimney height and the rates for items per second, fluid in mB per second and energy in FE per tick, or Unlimited. Rows and tanks that are not open SHALL be shown closed.

#### Scenario: Chimney placed without sneaking
- **WHEN** the player right clicks the top of a formed Sink with a Chimney block
- **THEN** the Chimney block is placed and no GUI opens

#### Scenario: Locked rows shown
- **WHEN** the player opens the GUI of a Sink without upgrades
- **THEN** the first row and first tank are open and the other three rows and tanks are shown closed

#### Scenario: Rates shown
- **WHEN** the player opens the GUI of a Sink with a column of five Chimney blocks
- **THEN** it shows height 5 and Unlimited for all three resources

### Requirement: Next batch shown
The GUI SHALL show how far the next batch is while the buffer holds anything, and SHALL mark the slots whose items the next batch will void, at least in part.

#### Scenario: Marked slots
- **WHEN** the item rate is 4 per second and the first slot holds 2 dirt and the second 64 gravel
- **THEN** the first two slots are marked and the rest are not

### Requirement: Batch planning is pure logic
How much of each slot and tank one batch takes, and whether a Capacity Upgrade may come out, SHALL be computed by pure Java without Minecraft classes and be covered by unit tests.

#### Scenario: Unit test of a batch
- **WHEN** a unit test plans a batch of 8 over slot counts 3, 64 and 10
- **THEN** it takes 3, 5 and 0

### Requirement: Sink blocks keep their stone
A Sink block SHALL keep the stone of its item when placed, formed and unformed, SHALL show it on every face that was bricks (unformed casing and formed parts, the iron frame and grate unchanged), and SHALL show Bricks when it has no stone. The item tooltip SHALL name the stone. Rates, buffer, fan and smoke of a formed Sink SHALL NOT depend on its stone.

#### Scenario: Unformed casing in its stone
- **WHEN** the player places a Granite Sink block
- **THEN** the casing shows granite inside its iron frame

#### Scenario: Same rate for every stone
- **WHEN** a Bricks Sink and a Quartz Sink each have a column of two Chimney blocks
- **THEN** both report the same rates
