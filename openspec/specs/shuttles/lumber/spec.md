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
On a tree field run the Lumber shuttle SHALL drive to the nearest farm rail of the field whose plots have work, work them, then drive to the next nearest rail with work, until no rail has work it can do or its cargo is full. It SHALL work each tree from the farm rail nearest to the tree's trunk. Felling a tree SHALL take `fell_ticks` ticks (in `field.json`, example 40), shortened by Handling like a stop and at least one tick; planting a spot SHALL take `work_ticks` like a harvester's plot.

#### Scenario: Rail by rail
- **WHEN** a Trees field of six farm rails has grown trees beside the first and the last rail
- **THEN** the Lumber shuttle stops at the first rail, fells those trees, drives to the last rail and fells the trees beside it

#### Scenario: Handling speeds up felling
- **WHEN** a Lumber shuttle with two Handling upgrades fells a tree, with `fell_ticks` 40 and a Handling factor of 0.8
- **THEN** the felling takes about 26 ticks instead of 40

### Requirement: Felling a tree
Felling SHALL remove at once the tree standing on the plot: its logs connected to the trunk, diagonally too, but never another trunk standing on the plots' level; the natural leaves that none of the remaining logs holds any more; and hanging parts (vines, cocoa, hanging propagules, hanging moss) touching a removed block. Which blocks count as logs and hanging parts SHALL come from block tags. Leaves placed by a player and other blocks SHALL stay.

#### Scenario: An oak comes down
- **WHEN** a Lumber shuttle fells a grown oak on a spot
- **THEN** its trunk, its branches and its leaves are gone and the plot is empty

#### Scenario: The neighbour keeps its crown
- **WHEN** two oaks stand two plots apart with their leaves touching and the Lumber shuttle fells one
- **THEN** the other oak keeps its trunk and the leaves it holds

#### Scenario: Jungle vines and cocoa
- **WHEN** a Lumber shuttle fells a jungle tree with vines and cocoa pods on its trunk
- **THEN** the vines and the cocoa are removed with it

#### Scenario: Player leaves stay
- **WHEN** a player placed leaves against a grown oak and the oak is felled
- **THEN** the placed leaves stay where they are

### Requirement: Drops of a felled tree
Every block removed by felling SHALL give the drops of its loot table rolled as if broken without a tool: logs give their log, leaves give saplings, sticks and apples by chance. The drops SHALL go into the shuttle's cargo; nothing SHALL drop on the ground and no leaf SHALL be left to decay. Felling SHALL play the wood break sound with break particles at the tree's base.

#### Scenario: Wood into the cargo
- **WHEN** a Lumber shuttle fells an oak of five logs
- **THEN** its cargo holds five oak logs plus whatever saplings, sticks and apples the leaves gave, and no item lies on the ground

#### Scenario: No leaf decay
- **WHEN** an oak is felled
- **THEN** no leaf block of it stays behind to decay

### Requirement: Trees too large to fell
A tree with more than `max_tree_logs` logs (in `field.json`, example 200) or more than `max_tree_blocks` blocks in all (example 1000) SHALL NOT be felled, and logs without natural leaves among the blocks felling would remove SHALL NOT be felled either. The head SHALL stop counting such a plot as ready to fell until a block on it changes, and the field work status SHALL say that a tree is too large to fell.

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
