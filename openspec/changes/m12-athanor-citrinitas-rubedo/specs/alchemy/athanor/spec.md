## MODIFIED Requirements

### Requirement: Tiers are data
A tier SHALL be a JSON file at `data/<namespace>/servus_alchemy/athanor_tier/<name>.json` with a tier number, a colour and a list of blocks at offsets from the Athanor. The Athanor's tier SHALL be the highest tier whose blocks, and the blocks of every lower tier, are all in place; with none in place it is tier 0. Alchemy SHALL ship three tiers:
- Albedo (tier 1, white): twelve Salt Crystal Blocks in the hearth layer, filling the corners of the 5 by 5 square around the diamond hearth, so nothing stands next to the Athanor on its own level.
- Citrinitas (tier 2, yellow): twelve Amethyst Blocks standing on the twelve Salt Crystal Blocks, on the Athanor's own level.
- Rubedo (tier 3, red): eight Crying Obsidian as four pillars two blocks high on the four corner Amethyst Blocks.

#### Scenario: Albedo ring
- **WHEN** twelve Salt Crystal Blocks fill the hearth layer of a formed Athanor out to a 5 by 5 square
- **THEN** the Athanor is tier 1, Albedo

#### Scenario: Broken ring
- **WHEN** one block of the ring is removed
- **THEN** the Athanor is tier 0 again at its next check

#### Scenario: Citrinitas ring
- **WHEN** twelve Amethyst Blocks stand on the twelve Salt Crystal Blocks of an Albedo Athanor
- **THEN** the Athanor is tier 2, Citrinitas, and its flame is yellow

#### Scenario: Amethyst without salt
- **WHEN** the twelve Amethyst Blocks stand in place but the Salt Crystal Blocks below them are missing
- **THEN** the Athanor is tier 0, Nigredo

#### Scenario: Rubedo pillars
- **WHEN** four pillars of two Crying Obsidian stand on the corner Amethyst Blocks of a Citrinitas Athanor
- **THEN** the Athanor is tier 3, Rubedo, and its flame is red

#### Scenario: Pillar too short
- **WHEN** one pillar of a Rubedo Athanor loses its top Crying Obsidian
- **THEN** the Athanor is tier 2, Citrinitas, at its next check

### Requirement: Tier is checked on interaction and slowly
The Athanor SHALL check its tier when it is placed, when a player uses it, when a craft starts, and otherwise at a slow interval (example every 5 seconds). The slow check SHALL also check the hearth and unform the Athanor when it is incomplete. It SHALL never check every tick. The tier SHALL show as the flame colour (black Nigredo, white Albedo, yellow Citrinitas, red Rubedo) and, with Jade installed, by its stage name in its tooltip. The Athanor SHALL have no GUI.

#### Scenario: Ring placed
- **WHEN** a player completes the Salt Crystal ring and right clicks the Athanor
- **THEN** the flame turns white and Jade shows Albedo

#### Scenario: Citrinitas in Jade
- **WHEN** a player looks at a formed Citrinitas Athanor with Jade
- **THEN** Jade shows "Stage: Citrinitas"

#### Scenario: Hearth brick taken
- **WHEN** a corner brick of the hearth of a formed Athanor is broken
- **THEN** the Athanor is unformed at its next check

### Requirement: Athanor layout is pure logic
The hearth cells, the tank positions for each facing and the schematic of a stage (the hearth, the Athanor, its tanks and the blocks of every tier up to it) SHALL be computed in pure Java covered by JUnit.

#### Scenario: Albedo schematic
- **WHEN** the schematic of the Albedo tier is computed
- **THEN** its lower layer is the diamond hearth with the twelve Salt Crystal Blocks around it, a 5 by 5 square, and its upper layer holds only the Athanor and its tanks

#### Scenario: Rubedo schematic
- **WHEN** the schematic of the Rubedo tier is computed
- **THEN** it has four layers: the hearth with the salt ring, the Athanor level with its tanks and the Amethyst ring, the duct level with the lower Crying Obsidian on the corners, and a top layer with only the four upper Crying Obsidian
