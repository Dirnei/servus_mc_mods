# Spec Delta

## MODIFIED Requirements

### Requirement: The Athanor burns principles
A craft SHALL run only while the Athanor is formed on its hearth, its tier is at least the craft's tier, its tanks hold the craft's principles, the input holds the item and the output has room. The principles and the item SHALL be taken when the craft completes. The Athanor SHALL need no energy and SHALL show a burning flame while it crafts.

#### Scenario: Too low a tier
- **WHEN** a glass bottle lies on an Athanor without a Salt Crystal ring and the Mercury is there
- **THEN** no Bottle o' Enchanting is made

#### Scenario: Not formed
- **WHEN** gunpowder lies on an Athanor with Sulfur that was never formed on its hearth
- **THEN** no Blaze Powder is made

### Requirement: Tier is checked on interaction and slowly
The Athanor SHALL check its tier when it is placed, when a player uses it, when a craft starts, and otherwise at a slow interval (example every 5 seconds). The slow check SHALL also check the hearth and unform the Athanor when it is incomplete. It SHALL never check every tick. The tier SHALL show as the flame colour and in the GUI.

#### Scenario: Ring placed
- **WHEN** a player completes the Salt Crystal ring and right clicks the Athanor
- **THEN** the GUI shows Albedo and the flame turns white

#### Scenario: Hearth brick taken
- **WHEN** a corner brick of the hearth of a formed Athanor is broken
- **THEN** the Athanor is unformed at its next check

## ADDED Requirements

### Requirement: Athanor hearth
The Athanor SHALL stand on a hearth of 3 by 3 vanilla Bricks directly below it, centred under it. Right clicking the Athanor with Paracelsus' Journal SHALL check the hearth and form the Athanor when it is complete, making the player its owner, and SHALL NOT open the book; when a brick is missing it SHALL name it. Breaking the brick right under a formed Athanor SHALL unform it at once.

#### Scenario: Formed
- **WHEN** a player right clicks an Athanor standing on a complete hearth with the Journal
- **THEN** the Athanor forms and the player sees "The Athanor is ready."

#### Scenario: Brick missing
- **WHEN** a player right clicks an Athanor whose hearth misses a brick with the Journal
- **THEN** nothing forms and the player is told that Bricks are needed in the hearth

### Requirement: Athanor layout is pure logic
The hearth cells and the schematic of a stage (the hearth, the Athanor and the blocks of every tier up to it) SHALL be computed in pure Java covered by JUnit.

#### Scenario: Albedo schematic
- **WHEN** the schematic of a tier with eight Salt Crystal Blocks around the Athanor is computed
- **THEN** it has the hearth layer below and the ring around the Athanor in the layer above
