# Spec Delta

## Purpose

Placement assist lets a player extend a straight run of a Servus block from anywhere on the run. While sneaking, the block lands at the first free spot past the end of the run, and a ghost with an arrow shows that spot first.

## ADDED Requirements

### Requirement: Sneak click extends a run
When a sneaking player uses an assisted item on a block that the item accepts as the start of a run, the item SHALL pick a direction and walk from the clicked block through every following block of the run in that direction. The item's block SHALL be placed at the first block past the run instead of against the clicked face. Each assisted item SHALL define which blocks start a run, which blocks continue it, and how the direction is chosen.

#### Scenario: Extend from the middle
- **WHEN** five blocks of a run lie in a line from west to east and a sneaking player clicks the second one with the direction east
- **THEN** the new block is placed east of the fifth block

#### Scenario: Run of one
- **WHEN** a single block of a run stands alone and a sneaking player clicks it with the direction north
- **THEN** the new block is placed directly north of it

### Requirement: Unsneaked placement is unchanged
An assisted item used without sneaking, or used on a block that does not start a run, SHALL place exactly as it did without the assist.

#### Scenario: Normal click
- **WHEN** a player who is not sneaking uses an assisted item on the east face of a block of its run
- **THEN** the block is placed against that face as before

#### Scenario: Sneak on a foreign block
- **WHEN** a sneaking player uses an assisted item on the top of a stone block
- **THEN** the block is placed on top of the stone as before

### Requirement: Walk stops at the end or fails
The walk SHALL stop at the first block that is not part of the run. If that block can be replaced by the placed block (air, for example), it SHALL be the target. If it cannot be replaced, or the walk passes the maximum reach before reaching it, there SHALL be no target. The click SHALL then place nothing and consume no item.

#### Scenario: Run ends at a wall
- **WHEN** a run ends directly against a stone block and a sneaking player clicks the run towards the stone
- **THEN** nothing is placed and the held stack keeps its count

#### Scenario: Run longer than the reach
- **WHEN** the maximum reach is 16 and a sneaking player clicks the first block of a straight run of 20 towards its far end
- **THEN** nothing is placed

### Requirement: Reach comes from the Lib config
The maximum number of blocks the walk may cover SHALL be the `placementAssistReach` option of the Lib common config, with a default of 16. The value is the largest distance in blocks from the clicked block to the target.

#### Scenario: Default reach
- **WHEN** the config file has no edits, and a sneaking player clicks the first block of a straight run of 16 towards its far end with free space beyond it
- **THEN** the block is placed past the end of the run, 16 blocks from the clicked block

#### Scenario: Short reach
- **WHEN** `placementAssistReach` is 4 and a sneaking player clicks the first block of a run of 5 towards its far end
- **THEN** nothing is placed

### Requirement: Ghost and arrow preview
While a player sneaks, holds an assisted item in the main hand and looks at a block of its run, the client SHALL draw a translucent ghost of the block that would be placed at the target, and an arrow on it pointing in the walk direction. Nothing SHALL be drawn when there is no target, when the player stops sneaking, or when the player looks elsewhere.

#### Scenario: Preview appears
- **WHEN** a player holding an assisted item sneaks and looks at a block of a run with a free target
- **THEN** a ghost block with an arrow is drawn at the target

#### Scenario: Preview follows the direction
- **WHEN** the player keeps sneaking and moves the crosshair so that the chosen direction changes
- **THEN** the ghost and arrow move to the target in the new direction

#### Scenario: No preview without sneaking
- **WHEN** the same player stops sneaking
- **THEN** no ghost and no arrow are drawn

#### Scenario: No preview when blocked
- **WHEN** a sneaking player looks at a run that ends against a stone block in the chosen direction
- **THEN** no ghost and no arrow are drawn

### Requirement: Server decides the target
The server SHALL compute the target again from the click and SHALL place only at its own target. A preview that differs from the server's result SHALL never place a block anywhere other than the server's target.

#### Scenario: Block appears where the server walked
- **WHEN** another player places a block at the previewed target in the same tick as a sneaking player clicks
- **THEN** the click either places at the next free spot the server finds within reach or places nothing

### Requirement: Placed block follows its own rules
A block placed by the assist SHALL go through the same placement checks, state choice and follow-up behaviour as a block placed against a face by hand. The assist SHALL NOT keep a block that its own rules would reject or drop.

#### Scenario: Normal follow-up
- **WHEN** the assist places a block whose own rules drop it when it is unsupported
- **THEN** it drops exactly as a hand-placed block in the same spot would
