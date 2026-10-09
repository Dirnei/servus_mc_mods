# Spec Delta

## MODIFIED Requirements

### Requirement: Connected look
Blocks of a formed tank SHALL hide their frame edges toward each other so the tank looks like one glass box with a frame around its outside. A block SHALL show its frame toward blocks of another tank, and a block working on its own SHALL show its full frame. The bottom of a tank SHALL be a solid copper plate instead of glass, visible from below and from inside, shown only on blocks with no tank block of the same tank below them.

#### Scenario: Formed tank
- **WHEN** four blocks form a 2 by 2 tank
- **THEN** no frame shows between the blocks, only around the outside of the box

#### Scenario: Block on top
- **WHEN** a single block stands on top of a formed 2 by 2 tank
- **THEN** a frame shows between the single block and the tank below it

#### Scenario: Copper floor
- **WHEN** a player looks down into an empty tank, or up at it from below
- **THEN** they see a copper floor, not the blocks beneath the tank

#### Scenario: Two layers
- **WHEN** a 2 by 2 tank is two layers high
- **THEN** only the lower layer shows the copper floor and no floor shows between the layers

### Requirement: Crafting
The Fluid Tank SHALL be crafted from iron ingots, glass and a copper ingot.

#### Scenario: Recipe
- **WHEN** the player crafts iron ingots in the corners, glass on the top, left and right edges and a copper ingot in the bottom middle around an empty centre
- **THEN** one Fluid Tank is produced
