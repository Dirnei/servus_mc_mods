## MODIFIED Requirements

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

### Requirement: Crafting
Sink blocks and Chimney blocks SHALL be hand crafted from any stone of `#servus_foundation:stones` without research: Sink blocks from stone and iron, Chimney blocks from stone alone. The result SHALL carry the stone of the grid as defined in `foundation/materials`.

#### Scenario: Craft both
- **WHEN** the player has Stone Bricks and iron bars in a crafting table
- **THEN** they can craft Stone Bricks Sink blocks and Stone Bricks Chimney blocks without any research

#### Scenario: Bricks still work
- **WHEN** the player crafts Sink blocks from Bricks and iron bars
- **THEN** the result carries Bricks and looks like the Sink always did

## ADDED Requirements

### Requirement: Sink blocks keep their stone
A Sink block SHALL keep the stone of its item when placed, formed and unformed, SHALL show it on every face that was bricks (unformed casing and formed parts, the iron frame and grate unchanged), and SHALL show Bricks when it has no stone. The item tooltip SHALL name the stone. Rates, buffer, fan and smoke of a formed Sink SHALL NOT depend on its stone.

#### Scenario: Unformed casing in its stone
- **WHEN** the player places a Granite Sink block
- **THEN** the casing shows granite inside its iron frame

#### Scenario: Same rate for every stone
- **WHEN** a Bricks Sink and a Quartz Sink each have a column of two Chimney blocks
- **THEN** both report the same rates
