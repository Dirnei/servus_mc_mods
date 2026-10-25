## ADDED Requirements

### Requirement: Felling arm
While a Lumber shuttle fells a tree, its turret SHALL turn toward the trunk, its two part arm SHALL unfold until the felling head reaches the trunk at the plot's level, the head's claws SHALL close on the trunk and its saw disc SHALL spin at the base with sawdust in the colour of the log. Once the tree has come down, the arm SHALL fold back into its resting pose, and only then SHALL the shuttle plant the spot. Players near the shuttle SHALL see this; it SHALL change nothing in the world.

#### Scenario: Reach and saw
- **WHEN** a Lumber shuttle starts to fell an oak two blocks beside its rail
- **THEN** the arm unfolds toward the oak, the claws close on the trunk and the saw spins at its base with sawdust

#### Scenario: Fold back, then plant
- **WHEN** the felled oak has broken apart
- **THEN** the arm folds back onto the shuttle and the new sapling appears on the spot after that

### Requirement: The tree topples away from the rail
When the saw part ends, the server SHALL remove the tree's blocks at once and the players near it SHALL see the whole tree, every removed block in its state, tip over as one piece around the edge of its base that faces away from the shuttle's rail, perpendicular to the rail, outward. The tip SHALL start slowly and speed up like falling until the tree lies flat on the ground. The falling tree MAY pass through terrain and blocks; nothing it passes SHALL change.

#### Scenario: Falls outward
- **WHEN** a Lumber shuttle on a rail running north to south fells an oak on the plot east of the rail
- **THEN** the oak tips toward the east and lies flat with its crown east of its base

#### Scenario: Speeds up
- **WHEN** a player watches the oak fall
- **THEN** it turns little at first and fastest just before it lands

#### Scenario: Through a hill
- **WHEN** a hill of dirt stands where the oak falls
- **THEN** the oak falls through the hill and the dirt is unchanged

### Requirement: The tree breaks apart
Once the falling tree lies flat, its leaves SHALL burst into leaf particles and vanish over a short time, its logs after them, while the items the tree dropped fly in arcs into the shuttle. When the tree is gone nothing of it SHALL stay drawn.

#### Scenario: Items into the shuttle
- **WHEN** a felled oak has landed
- **THEN** its leaves burst into particles, the logs vanish and log, sapling and stick items fly into the shuttle

### Requirement: Fall look from one message
The server SHALL send, once per felled tree and only to players near the tree, one message with the trunk position, the fall direction, the shuttle's rail, every removed block's position and state and the drops. The client SHALL draw the fall from it alone, and the tree's blocks SHALL be gone in the world for every player as soon as the fall starts. A player who comes near after the message SHALL see no fall.

#### Scenario: One message per tree
- **WHEN** a Lumber shuttle fells three trees with a player standing beside the field
- **THEN** the player's client gets three fall messages

#### Scenario: Late arrival
- **WHEN** a player arrives at the field while a felled tree is falling for others
- **THEN** that player sees the spot without the tree and no fall

### Requirement: Fall logic is pure
The fall direction from the rail and the trunk positions, the tip angle over the fall time, and the arm's pose for a reach distance and progress SHALL be pure Java without Minecraft imports and covered by JUnit tests.

#### Scenario: Direction from the rail
- **WHEN** the fall direction is asked for a trunk two blocks east and one block south of a rail running north to south
- **THEN** the answer is east

#### Scenario: Angle grows like falling
- **WHEN** the tip angle is asked at a quarter, half and all of the fall time
- **THEN** the answers are about 6, 23 and 90 degrees

## MODIFIED Requirements

### Requirement: Tree field run
On a tree field run the Lumber shuttle SHALL drive to the nearest farm rail of the field whose plots have work, work them, then drive to the next nearest rail with work, until no rail has work it can do or its cargo is full. It SHALL work each tree from the farm rail nearest to the tree's trunk. Felling a tree SHALL take a saw part of `fell_ticks` ticks (in `field.json`, example 40), shortened by Handling like a stop and at least one tick, and then a topple part of `topple_ticks` ticks (example 80) that Handling SHALL NOT shorten; planting a spot SHALL take `work_ticks` like a harvester's plot.

#### Scenario: Rail by rail
- **WHEN** a Trees field of six farm rails has grown trees beside the first and the last rail
- **THEN** the Lumber shuttle stops at the first rail, fells those trees, drives to the last rail and fells the trees beside it

#### Scenario: Handling speeds up felling
- **WHEN** a Lumber shuttle with two Handling upgrades fells a tree, with `fell_ticks` 40, `topple_ticks` 80 and a Handling factor of 0.8
- **THEN** the saw part takes about 26 ticks instead of 40 and the topple part 80

### Requirement: Drops of a felled tree
Every block removed by felling SHALL give the drops of its loot table rolled as if broken without a tool: logs give their log, leaves give saplings, sticks and apples by chance. The drops SHALL go into the shuttle's cargo; nothing SHALL drop on the ground and no leaf SHALL be left to decay. The felling arm, the toppling tree and its break apart SHALL show the felling (see "Felling arm", "The tree topples away from the rail" and "The tree breaks apart").

#### Scenario: Wood into the cargo
- **WHEN** a Lumber shuttle fells an oak of five logs
- **THEN** its cargo holds five oak logs plus whatever saplings, sticks and apples the leaves gave, and no item lies on the ground

#### Scenario: No leaf decay
- **WHEN** an oak is felled
- **THEN** no leaf block of it stays behind to decay
