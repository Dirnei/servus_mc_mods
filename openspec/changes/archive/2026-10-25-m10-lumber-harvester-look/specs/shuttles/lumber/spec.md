## ADDED Requirements

### Requirement: The Lumber shuttle unfolds at the stop
When a Lumber shuttle stops at a rail where it will fell a tree, it SHALL first unfold for `unfold_ticks` ticks (in `field.json`, example 30), not shortened by Handling: the lid doors swing over and down the outside of the side walls, all four walls fold out and down until their tops rest on the ground, and a knuckle-boom crane rises out of the hold with its harvester head folded and upright. It SHALL stay unfolded while it fells and plants at that rail.

#### Scenario: Unfold before the first tree
- **WHEN** a Lumber shuttle stops at a rail with two grown oaks beside it
- **THEN** it unfolds once, then fells the first oak and the second without folding in between

#### Scenario: No unfold to plant
- **WHEN** a Lumber shuttle stops at a rail where it only plants saplings
- **THEN** it plants them without unfolding

### Requirement: The harvester head fells a tree
For each tree, during the saw part, the crane SHALL turn toward the trunk, its telescope run out and its head open, go round the base log nearest the rail at the plot's level, close both links of every roller and knife arm on the log, and swing its chain bar out across the base with sawdust in the colour of the log.

#### Scenario: Grip and cut
- **WHEN** an unfolded Lumber shuttle fells an oak two plots beside its rail
- **THEN** the head goes round the oak's base log, its rollers and knives close on it and the chain bar cuts across the base with sawdust

#### Scenario: Far tree
- **WHEN** an unfolded Lumber shuttle fells a tree four plots beside its rail
- **THEN** the telescope runs out until the head reaches the trunk

### Requirement: The chainsaw sounds while it saws
Players near an unfolded Lumber shuttle SHALL hear its chainsaw at the head's saw: an idle in a loop from the moment the crane starts to rise until it starts to fold, a rev-up into cutting when the bar swings out across a log, and a wind-down when the topple part starts. The idle SHALL pause while the cutting or the wind-down sounds and resume after it. The cutting and the wind-down SHALL play at most once per tree, and every sound of a shuttle SHALL stop once it is no longer drawn.

#### Scenario: Idle, cut, stop, idle
- **WHEN** an unfolded Lumber shuttle saws an oak, the oak topples and the shuttle stays at the rail
- **THEN** a player nearby hears the chainsaw idle while the head reaches and grips, cut while the bar is out, wind down as the head lets go and idle again until the shuttle folds, with the subtitles "Chainsaw idles", "Chainsaw cuts" and "Chainsaw stops"

#### Scenario: Silent when gone
- **WHEN** a player walks out of range of a Lumber shuttle while it cuts
- **THEN** the cutting sound stops

### Requirement: The hold and the crane sound as they fold
Players near a Lumber shuttle SHALL hear it unfold and fold, each step once: the lid doors open, the walls land on the ground, the crane rises, and when folding the crane sinks, the walls rise and the doors shut, with subtitles naming each step.

#### Scenario: Unfold and fold
- **WHEN** a Lumber shuttle unfolds at a rail, fells a tree and folds
- **THEN** a player nearby hears "Hold doors open", "Stabilizers land" and "Crane rises" in that order, and later "Crane folds in", "Stabilizers rise" and "Hold doors shut"

### Requirement: The head lets go before the tree falls
As the topple part starts the head SHALL let go at once, back off a little and wait open while the tree falls and breaks apart, then swing back over the hold with its arms folded, and only then SHALL the shuttle plant the spot.

#### Scenario: Let go, wait, plant
- **WHEN** the oak starts to tip
- **THEN** the head opens and backs off before the oak lands, waits while it breaks apart, swings back over the hold, and the new sapling appears on the spot after that

### Requirement: The Lumber shuttle folds before it drives on
Before a Lumber shuttle that is unfolded leaves its rail, for the next rail, to unload or because its run ends, it SHALL fold for `unfold_ticks` ticks in the reverse order of unfolding: the head and crane sink into the hold, the walls swing up and the doors close. It SHALL NOT move while it folds. A shuttle restored from a save SHALL be folded.

#### Scenario: Fold, then drive
- **WHEN** an unfolded Lumber shuttle has felled the last tree at its rail and the next tree stands at another rail
- **THEN** it folds, and only then drives to the other rail

#### Scenario: Full cargo
- **WHEN** an unfolded Lumber shuttle's cargo fills up
- **THEN** it folds before it drives off to unload

## MODIFIED Requirements

### Requirement: Tree field run
On a tree field run the Lumber shuttle SHALL drive to the nearest farm rail of the field whose plots have work, work them, then drive to the next nearest rail with work, until no rail has work it can do or its cargo is full. It SHALL work each tree from the farm rail nearest to the tree's trunk. At a rail where it fells, unfolding SHALL take `unfold_ticks` ticks before the first tree and folding `unfold_ticks` ticks before it leaves (in `field.json`, example 30), neither shortened by Handling. Felling a tree SHALL take a saw part of `fell_ticks` ticks (in `field.json`, example 40), shortened by Handling like a stop and at least one tick, and then a topple part of `topple_ticks` ticks (example 80) that Handling SHALL NOT shorten; planting a spot SHALL take `work_ticks` like a harvester's plot.

#### Scenario: Rail by rail
- **WHEN** a Trees field of six farm rails has grown trees beside the first and the last rail
- **THEN** the Lumber shuttle stops at the first rail, unfolds, fells those trees, folds, drives to the last rail and unfolds again to fell the trees beside it

#### Scenario: Handling speeds up felling
- **WHEN** a Lumber shuttle with two Handling upgrades fells a tree, with `fell_ticks` 40, `topple_ticks` 80, `unfold_ticks` 30 and a Handling factor of 0.8
- **THEN** the saw part takes about 26 ticks instead of 40, while unfolding takes 30 and the topple part 80

### Requirement: Drops of a felled tree
Every block removed by felling SHALL give the drops of its loot table rolled as if broken without a tool: logs give their log, leaves give saplings, sticks and apples by chance. The drops SHALL go into the shuttle's cargo; nothing SHALL drop on the ground and no leaf SHALL be left to decay. The harvester head, the toppling tree and its break apart SHALL show the felling (see "The harvester head fells a tree", "The head lets go before the tree falls", "The tree topples away from the rail" and "The tree breaks apart").

#### Scenario: Wood into the cargo
- **WHEN** a Lumber shuttle fells an oak of five logs
- **THEN** its cargo holds five oak logs plus whatever saplings, sticks and apples the leaves gave, and no item lies on the ground

#### Scenario: No leaf decay
- **WHEN** an oak is felled
- **THEN** no leaf block of it stays behind to decay

### Requirement: Fall logic is pure
The fall direction from the rail and the trunk positions, the tip angle over the fall time, and the pose of the Lumber hold, the crane and the harvester head for a trunk position, a phase and its progress SHALL be pure Java without Minecraft imports and covered by JUnit tests.

#### Scenario: Direction from the rail
- **WHEN** the fall direction is asked for a trunk two blocks east and one block south of a rail running north to south
- **THEN** the answer is east

#### Scenario: Angle grows like falling
- **WHEN** the tip angle is asked at a quarter, half and all of the fall time
- **THEN** the answers are about 6, 23 and 90 degrees

#### Scenario: Head at the trunk
- **WHEN** the pose is asked for the grip moment of the saw part for trunks one to four plots beside the rail
- **THEN** the head's grip centre lies on each trunk's axis at the cut height, with every arm closed

#### Scenario: Folded at rest
- **WHEN** the pose is asked at the start of unfolding and at the end of folding
- **THEN** the doors are shut, the walls up and every crane and head part inside the hold

## REMOVED Requirements

### Requirement: Felling arm
**Reason**: The thin turret arm with its felling head read as flimsy in the play check; the harvester crane replaces it.
**Migration**: None; see "The Lumber shuttle unfolds at the stop", "The harvester head fells a tree", "The head lets go before the tree falls" and "The Lumber shuttle folds before it drives on".
