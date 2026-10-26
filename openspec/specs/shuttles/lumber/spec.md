# shuttles/lumber Specification

## Purpose
Lumber shuttles tend the tree fields of their warehouse: they drive out over the farm rails, fell grown trees with their leaves, bring the wood to the Field Head and plant new saplings on the field's spots.

## Requirements

### Requirement: Lumber module
The Lumber module SHALL be a craftable tool module that goes into a shuttle through the install slot of the Shuttle Workshop, into its one tool place, replacing a fitted tool. A shuttle with the Lumber module SHALL be field only and SHALL take only tree field jobs: no warehouse picks, no put-aways, no crop field jobs and no fertilize runs. It SHALL be wide, never riding a lift car, and its tooltip SHALL say that it fells trees and does not fit into lifts.

#### Scenario: Fit the Lumber module
- **WHEN** a player puts a Mk1 shuttle into the workshop and a Lumber module into the install slot
- **THEN** the shuttle has the Lumber module as its tool and its tooltip lists it

#### Scenario: Lumber shuttles stay out of the warehouse
- **WHEN** a warehouse has only a Lumber shuttle and a player requests cobblestone from a rack on the rails
- **THEN** the request is refused because it needs a shuttle and the Lumber shuttle stays in its garage

#### Scenario: Lumber shuttles leave crops alone
- **WHEN** a warehouse has only a Lumber shuttle and its crop field has a harvest due
- **THEN** no shuttle drives onto the crop field

#### Scenario: Tooltip
- **WHEN** a player hovers over a Lumber module item
- **THEN** the tooltip says it fells trees and does not fit into lifts

### Requirement: Tree field jobs
At every field check a warehouse SHALL queue a tree field job for a Trees field when a Lumber shuttle reaches it without a lift, no field job for it is queued or running, the head's To storage buffer has a free slot, and either a harvest is due or a sapling is set with an empty spot and saplings in the head's buffer. The job SHALL go to the free Lumber shuttle with the shortest way to a farm rail of the field that has work.

#### Scenario: Grown trees call a Lumber shuttle
- **WHEN** a Trees field with oak saplings set has half of its planted spots grown into trees and the warehouse has a free Lumber shuttle
- **THEN** within a few seconds the Lumber shuttle leaves its garage and drives to the field

#### Scenario: Empty spots need saplings
- **WHEN** a Trees field with oak saplings set has only empty spots and the head's buffer holds no oak saplings
- **THEN** no tree field job is queued

#### Scenario: Harvesters do not fell
- **WHEN** a warehouse has only a Harvester shuttle and its Trees field has a harvest due
- **THEN** no shuttle drives onto the Trees field

### Requirement: Tree field run
On a tree field run the Lumber shuttle SHALL drive to the nearest farm rail of the field whose plots have work, work them, then drive to the next nearest rail with work, until no rail has work it can do or its cargo is full. It SHALL work each tree from the farm rail nearest to the tree's trunk. At a rail where it fells, unfolding SHALL take `unfold_ticks` ticks before the first tree and folding `unfold_ticks` ticks before it leaves (in `field.json`, example 30), neither shortened by Handling. Felling a tree SHALL take a saw part of `fell_ticks` ticks (in `field.json`, example 40), shortened by Handling like a stop and at least one tick, and then a topple part of `topple_ticks` ticks (example 80) that Handling SHALL NOT shorten; planting a spot SHALL take `work_ticks` like a harvester's plot.

#### Scenario: Rail by rail
- **WHEN** a Trees field of six farm rails has grown trees beside the first and the last rail
- **THEN** the Lumber shuttle stops at the first rail, unfolds, fells those trees, folds, drives to the last rail and unfolds again to fell the trees beside it

#### Scenario: Handling speeds up felling
- **WHEN** a Lumber shuttle with two Handling upgrades fells a tree, with `fell_ticks` 40, `topple_ticks` 80, `unfold_ticks` 30 and a Handling factor of 0.8
- **THEN** the saw part takes about 26 ticks instead of 40, while unfolding takes 30 and the topple part 80

### Requirement: Felling a tree
Felling SHALL remove at once the tree standing on the plot: its logs connected to the trunk, diagonally too and down to the ground, but never another tree standing on a spot or square of the field: a log that touches both trees SHALL go to the trunk it grows from, so no trunk, branch or stump of the other tree is cut off and left behind; the natural leaves that none of the remaining logs holds any more; and hanging parts (vines, cocoa, hanging propagules, hanging moss, bee nests) touching a removed block. A bee nest SHALL go with the bees inside it and, like a nest broken without Silk Touch, give nothing. Which blocks count as logs and hanging parts SHALL come from block tags. Leaves placed by a player and other blocks SHALL stay.

#### Scenario: An oak comes down
- **WHEN** a Lumber shuttle fells a grown oak on a spot
- **THEN** its trunk, its branches and its leaves are gone and the plot is empty

#### Scenario: The neighbour keeps its crown
- **WHEN** two oaks stand two plots apart with their leaves touching and the Lumber shuttle fells one
- **THEN** the other oak keeps its trunk and the leaves it holds

#### Scenario: Jungle vines and cocoa
- **WHEN** a Lumber shuttle fells a jungle tree with vines and cocoa pods on its trunk
- **THEN** the vines and the cocoa are removed with it

#### Scenario: A branch into the neighbour
- **WHEN** two oaks stand on neighbouring spots, a branch of one touches the other's trunk and the Lumber shuttle fells the field
- **THEN** both trees come down whole, no log stays on either spot and both spots get a new sapling

#### Scenario: Leaning neighbours
- **WHEN** a field of acacias planted on every spot has grown, their leaning trunks touching each other, and the Lumber shuttle fells them
- **THEN** every acacia comes down and no stump is left on any spot

#### Scenario: Bee nest
- **WHEN** an oak with a bee nest on its trunk is felled
- **THEN** the nest and its bees are gone and nothing of the nest is in the cargo

#### Scenario: Player leaves stay
- **WHEN** a player placed leaves against a grown oak and the oak is felled
- **THEN** the placed leaves stay where they are

### Requirement: Drops of a felled tree
Every block removed by felling SHALL give the drops of its loot table rolled as if broken without a tool: logs give their log, leaves give saplings, sticks and apples by chance. The drops SHALL go into the shuttle's cargo; nothing SHALL drop on the ground and no leaf SHALL be left to decay. The harvester head, the toppling tree and its break apart SHALL show the felling (see "The harvester head fells a tree", "The head lets go before the tree falls", "The tree topples away from the rail" and "The tree breaks apart").

#### Scenario: Wood into the cargo
- **WHEN** a Lumber shuttle fells an oak of five logs
- **THEN** its cargo holds five oak logs plus whatever saplings, sticks and apples the leaves gave, and no item lies on the ground

#### Scenario: No leaf decay
- **WHEN** an oak is felled
- **THEN** no leaf block of it stays behind to decay

### Requirement: Trees too large to fell
A tree with more than `max_tree_logs` logs (in `field.json`, example 200) or more than `max_tree_blocks` blocks in all (example 1000) SHALL NOT be felled, and logs without natural leaves among the blocks felling would remove SHALL NOT be felled either, unless they touch another tree that holds the leaves. The head SHALL stop counting such a plot as ready to fell until a block on it changes, and the field work status SHALL say that a tree is too large to fell.

#### Scenario: Giant tree left standing
- **WHEN** `max_tree_logs` is 20 and a grown giant spruce of 80 logs stands on a square
- **THEN** the Lumber shuttle leaves it standing and the field work status says a tree is too large to fell

#### Scenario: Log pillar
- **WHEN** a player stacks three oak logs on a spot
- **THEN** the Lumber shuttle does not fell them

### Requirement: Replanting spots
After felling, and on every empty spot it works, the Lumber shuttle SHALL plant the field's sapling when the field has one set and the shuttle carries it; a sapling from the felled tree's drops SHALL be used first. A 2x2 field SHALL plant a square only when the shuttle carries four saplings, and then all four at once. In fell only mode nothing SHALL be planted. Plots that are not spots SHALL never be planted.

#### Scenario: Replant from the drops
- **WHEN** a Lumber shuttle fells an oak whose leaves gave two oak saplings, on a field with oak saplings set
- **THEN** the spot holds a new oak sapling and one oak sapling rides in the cargo

#### Scenario: Square needs four
- **WHEN** a Lumber shuttle carries three dark oak saplings at an empty square of a 2x2 dark oak field
- **THEN** it plants none of them and the square stays empty

#### Scenario: Fell only
- **WHEN** a Trees field is in fell only mode and a grown birch is felled
- **THEN** the spot stays empty

### Requirement: Saplings from the head
To plant, the Lumber shuttle SHALL take the sapling from its cargo. Spare saplings SHALL first fill the head's seed buffer, and missing saplings SHALL come from that buffer, never from racks, the same way a harvester keeps, unloads and fetches seeds (see `shuttles/harvester`), counting four saplings per square in a 2x2 field.

#### Scenario: Saplings from the buffer
- **WHEN** a Trees field with birch saplings set has three empty spots and the head's buffer holds ten birch saplings
- **THEN** the Lumber shuttle locks three saplings in the buffer, takes them at the head and plants the three spots

### Requirement: Lumber cargo
A Lumber shuttle SHALL carry up to the stacks of its tier, each stack one item type, and SHALL unload at the head like a harvester: the drops of the next tree are rolled before it is felled, and when they do not fit the shuttle SHALL unload at the head first; a tree whose drops do not fit even into an empty cargo SHALL be felled anyway, the rest riding along to be unloaded before it works on.

#### Scenario: Full cargo unloads at the head
- **WHEN** a Mk1 Lumber shuttle fells oaks until two stacks are aboard and grown trees remain
- **THEN** it drives to the farm rail beside the head, unloads into the To storage buffer and returns to fell the rest

#### Scenario: A big tree at once
- **WHEN** a Mk1 Lumber shuttle with an empty cargo fells a giant spruce whose drops fill more than two stacks
- **THEN** the whole tree comes down, the shuttle unloads at the head before it fells the next tree, and nothing drops on the ground

### Requirement: Tree runs that cannot go on
A tree field run SHALL end like a harvester's field run when the head is removed or no longer serves the field, when a rail it stands on or drives over is removed, when its garage loses the way home, is removed or is recalled (see `shuttles/harvester`, "Runs that cannot go on"). A field turned to Crops during a run SHALL end the run once the shuttle has unloaded at the head.

#### Scenario: Head broken mid run
- **WHEN** the player breaks the Field Head while the Lumber shuttle fells trees with logs in its cargo
- **THEN** the shuttle stops, goes home and the logs lie on the ground at the garage

#### Scenario: Kind changed
- **WHEN** the player switches the field to Crops while a Lumber shuttle works it with logs aboard
- **THEN** the shuttle unloads the logs at the head and goes home

### Requirement: Tree logic is pure
Which plots are spots and squares, which blocks a felled tree takes from a snapshot of block kinds, the size limits, and what a Lumber shuttle does on one plot SHALL be pure Java without Minecraft imports and covered by JUnit tests.

#### Scenario: Leaves another trunk holds
- **WHEN** the tree logic gets a snapshot of two trunks whose leaves touch and fells one
- **THEN** it returns the felled trunk's logs and the leaves farther than leaf reach from the other trunk, and none of the leaves the other trunk holds

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
