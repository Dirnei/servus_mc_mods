## MODIFIED Requirements

### Requirement: Tiers are data
A tier SHALL be a JSON file at `data/<namespace>/servus_alchemy/athanor_tier/<name>.json` with a tier number, a colour and a list of blocks at offsets from the Athanor. The offsets SHALL be given for an Athanor whose front faces north and SHALL turn with the Athanor's front, so a tier fits an Athanor facing any way. The Athanor's tier SHALL be the highest tier whose blocks, and the blocks of every lower tier, are all in place; with none in place it is tier 0. Alchemy SHALL ship three tiers:
- Albedo (tier 1, white): sixteen Salt Crystal Blocks in a 5 by 5 ring one layer below the hearth, centred under the Athanor, so the tanks and the edge of the hearth stand on it.
- Citrinitas (tier 2, yellow): beside each of the three tanks, on its outer side three blocks from the Athanor, a Principle Vat on the hearth level with an Amethyst Block on it.
- Rubedo (tier 3, red): four Crying Obsidian pillars two blocks high on the salt ring, standing on the hearth level: one on each side of the tank behind the Athanor and one behind each side tank.

#### Scenario: Albedo ring
- **WHEN** sixteen Salt Crystal Blocks form the 5 by 5 ring under the hearth of a formed Athanor
- **THEN** the Athanor is tier 1, Albedo

#### Scenario: Broken ring
- **WHEN** one block of the ring is removed
- **THEN** the Athanor is tier 0 again at its next check

#### Scenario: Citrinitas ring
- **WHEN** a Principle Vat with an Amethyst Block on it stands beside each tank of an Albedo Athanor
- **THEN** the Athanor is tier 2, Citrinitas, and its flame is yellow

#### Scenario: Amethyst without salt
- **WHEN** the three vats with amethyst stand in place but a Salt Crystal Block of the ring is missing
- **THEN** the Athanor is tier 0, Nigredo

#### Scenario: Rubedo pillars
- **WHEN** four pillars of two Crying Obsidian stand on the ring beside the back tank and behind the side tanks of a Citrinitas Athanor
- **THEN** the Athanor is tier 3, Rubedo, and its flame is red

#### Scenario: Pillar too short
- **WHEN** one pillar of a Rubedo Athanor loses its top Crying Obsidian
- **THEN** the Athanor is tier 2, Citrinitas, at its next check

#### Scenario: Turned Athanor
- **WHEN** an Athanor faces east and its ring, vats and pillars are built turned with it
- **THEN** it reaches the same tiers as one facing north

### Requirement: Tier is checked on interaction and slowly
The Athanor SHALL check its tier when it is placed, when a player uses it, when a craft starts, and otherwise at a slow interval (example every 5 seconds). The slow check SHALL also check the hearth and unform the Athanor when it is incomplete. It SHALL never check every tick. The tier SHALL show as the flame colour (black Nigredo, white Albedo, yellow Citrinitas, red Rubedo) and, with Jade installed, by its stage name in its tooltip. The Athanor SHALL have no GUI.

#### Scenario: Ring placed
- **WHEN** a player completes the Salt Crystal ring and right clicks the Athanor
- **THEN** the flame turns white and Jade shows Albedo

#### Scenario: Citrinitas in Jade
- **WHEN** a player looks at a formed Citrinitas Athanor with Jade
- **THEN** Jade shows "Stage: Citrinitas"

#### Scenario: Hearth brick taken
- **WHEN** the Brick in front of a formed Athanor is broken
- **THEN** the Athanor is unformed at its next check

### Requirement: Athanor hearth
The Athanor SHALL stand on a hearth of 3 by 3 blocks directly below it, centred under it: vanilla Bricks under the Athanor and in front of it, and a bottom Brick Slab on each of the other seven cells. A Brick Stair SHALL stand on the hearth level in front of the front Brick, as the step. Three Foundation Fluid Tanks SHALL stand two blocks from the Athanor, behind it, to its left and to its right seen from its front, each three blocks high from the hearth level up to one block above the Athanor's level. Each tank SHALL be joined by one Copper Duct on the Athanor's level, between the Athanor and the tank. Right clicking the Athanor with Paracelsus' Journal SHALL check the hearth, the step, the tanks and the ducts and form the Athanor when they are complete, making the player its owner, and SHALL NOT open the book; when a block is missing it SHALL name it. A Copper Duct formed into a Calciner SHALL NOT count. Breaking the Brick right under a formed Athanor or one of its ducts SHALL unform it at once; another missing block SHALL unform it at the next check.

#### Scenario: Duct missing
- **WHEN** a player right clicks an Athanor on a complete hearth with its tanks but without the duct to the tank behind it with the Journal
- **THEN** nothing forms and the player is told that a Copper Duct is needed behind the Athanor

#### Scenario: Duct broken
- **WHEN** a Copper Duct of a formed Athanor is broken
- **THEN** the Athanor unforms at once

#### Scenario: Formed
- **WHEN** a player right clicks an Athanor standing on a complete hearth with the Journal
- **THEN** the Athanor forms and the player sees "The Athanor is ready."

#### Scenario: Brick missing
- **WHEN** a player right clicks an Athanor whose hearth misses the Brick in front of it with the Journal
- **THEN** nothing forms and the player is told that Bricks are needed in the hearth

#### Scenario: Tank missing
- **WHEN** a player right clicks an Athanor on a complete hearth without a Fluid Tank to its left with the Journal
- **THEN** nothing forms and the player is told that a Fluid Tank is needed left of the Athanor

#### Scenario: Slab missing
- **WHEN** a player right clicks an Athanor whose hearth misses a slab with the Journal
- **THEN** nothing forms and the player is told that Brick Slabs are needed in the hearth

#### Scenario: Step missing
- **WHEN** a player right clicks an Athanor whose hearth has no Brick Stair in front with the Journal
- **THEN** nothing forms and the player is told that a Brick Stair is needed in front of the hearth

#### Scenario: Tank too short
- **WHEN** a player right clicks an Athanor on a complete hearth whose left tank is only two blocks high with the Journal
- **THEN** nothing forms and the player is told that a Fluid Tank is needed left of the Athanor

### Requirement: Athanor crafting
The Athanor SHALL be crafted at a crafting table from Stone Bricks, a blast furnace and a cauldron without research. Its hearth stays vanilla Bricks, Brick Slabs and a Brick Stair.

#### Scenario: Recipe
- **WHEN** the player crafts the Athanor pattern of Stone Bricks around a cauldron and a blast furnace
- **THEN** one Athanor is produced

#### Scenario: Bricks craft nothing
- **WHEN** the player lays vanilla Bricks in the Athanor pattern
- **THEN** nothing is produced

### Requirement: Athanor layout is pure logic
The hearth cells, the step, the tank and duct positions for each facing, turning tier blocks with the front, and the schematic of a stage (the hearth, the Athanor, its tanks, its ducts and the blocks of every tier up to it) SHALL be computed in pure Java covered by JUnit.

#### Scenario: Nigredo schematic
- **WHEN** the schematic of tier 0 is computed
- **THEN** its layers are the hearth with the step and the tank bottoms, the Athanor level with the Athanor, the three ducts and the tanks, and the tank tops

#### Scenario: Albedo schematic
- **WHEN** the schematic of the Albedo tier is computed
- **THEN** it has a bottom layer with only the sixteen Salt Crystal Blocks in a 5 by 5 ring, below the Nigredo layers

#### Scenario: Rubedo schematic
- **WHEN** the schematic of the Rubedo tier is computed
- **THEN** the hearth layer and the Athanor level each also hold the three vats or Amethyst Blocks and the four Crying Obsidian beside the back tank and behind the side tanks

#### Scenario: Turned tier block
- **WHEN** a tier block one block left and two blocks behind a north facing Athanor is turned for an Athanor facing east
- **THEN** it lies one block left and two blocks behind that Athanor, seen from its front

### Requirement: Formed Athanor look
A formed Athanor SHALL be drawn as a brick tower furnace about one and a half blocks tall, turned by its front: a brick plinth, steel posts on the four corners, two copper bands, a black iron fire door in a steel frame with a small window where a flame flickers in the tier's colour, copper flanges on the Athanor's level where its ducts meet it, a stepped brick and copper dome with a vent on each side glowing in the tier's colour, and a steel plate on top where the item rests. No two faces of the model SHALL share a plane where they overlap. Its Copper Ducts SHALL be drawn as one straight copper pipe from a flange of the tower into the side of each tank. The ducts of a formed Athanor SHALL collide like their pipe, and the formed Athanor's collision SHALL follow the tower up to its plate, so a player steps from the hearth onto a pipe and from the pipe onto the plate. A formed Athanor SHALL be targeted two blocks tall, its own block and the space above where the item lies, for the outline, Jade and use; breaking it there breaks the Athanor. Unformed, the Athanor SHALL keep its plain block and the ducts their straight pipe.

#### Scenario: Formed look
- **WHEN** a player forms an Athanor
- **THEN** it shows as a brick tower with a black iron fire door and its Copper Ducts as three straight pipes running into the sides of its tanks

#### Scenario: Flame in the stage colour
- **WHEN** a formed Athanor reaches Citrinitas
- **THEN** the flame behind the door window and the four dome vents glow yellow

#### Scenario: Looking at the item on top
- **WHEN** a player looks at the item lying on the plate of a formed Athanor
- **THEN** the Athanor is targeted, its outline two blocks tall, Jade shows its tooltip and using it acts on the Athanor

#### Scenario: Standing on it
- **WHEN** a player steps from the hearth onto a pipe of a formed Athanor and walks along it to the tower
- **THEN** the player stands on its steel plate, about one and a half blocks above its floor

#### Scenario: Unformed look
- **WHEN** a tank of a formed Athanor is taken away and the Athanor unforms
- **THEN** the tower and the pipes give way to the plain block
