# Spec Delta

## MODIFIED Requirements

### Requirement: The Athanor burns principles
A craft SHALL run only while the Athanor is formed, its tier is at least the craft's tier, the tanks of its build hold the craft's principles, the input holds the item and the output has room. The principles and the item SHALL be taken when the craft completes. The Athanor SHALL need no energy and SHALL show a burning flame while it crafts.

#### Scenario: Too low a tier
- **WHEN** a glass bottle lies on an Athanor without a Salt Crystal ring and the Mercury is there
- **THEN** no Bottle o' Enchanting is made

#### Scenario: Not formed
- **WHEN** gunpowder lies on an Athanor with Sulfur that was never formed on its hearth
- **THEN** no Blaze Powder is made

### Requirement: Tier is checked on interaction and slowly
The Athanor SHALL check its tier when it is placed, when a player uses it, when a craft starts, and otherwise at a slow interval (example every 5 seconds). The slow check SHALL also check the hearth and unform the Athanor when it is incomplete. It SHALL never check every tick. The tier SHALL show as the flame colour and, with Jade installed, in its tooltip. The Athanor SHALL have no GUI.

#### Scenario: Ring placed
- **WHEN** a player completes the Salt Crystal ring and right clicks the Athanor
- **THEN** the flame turns white and Jade shows Albedo

#### Scenario: Hearth brick taken
- **WHEN** a corner brick of the hearth of a formed Athanor is broken
- **THEN** the Athanor is unformed at its next check

### Requirement: Principles by pipe or bucket
The Athanor SHALL hold no principles itself and SHALL offer no fluid capability. It SHALL read the principles from the Fluid Tanks of its build, whatever principle each tank holds, and drain them when a craft completes. Pipes and buckets SHALL fill the tanks. A bucket on the Athanor SHALL do nothing.

#### Scenario: Bucket of Sulfur
- **WHEN** a player right clicks a tank of a formed Athanor with a Sulfur bucket
- **THEN** that tank gains 1,000 mB Sulfur, the player holds an empty bucket, and the Athanor can burn it

#### Scenario: Sulfur from a tank
- **WHEN** Sulfur is piped into the tank left of a formed Athanor and gunpowder lies on it
- **THEN** Blaze Powder is made and the Sulfur in that tank goes down

#### Scenario: Any tank
- **WHEN** the Sulfur is in the tank behind the Athanor instead
- **THEN** the craft runs the same

### Requirement: Tiers are data
A tier SHALL be a JSON file at `data/<namespace>/servus_manufacturing/athanor_tier/<name>.json` with a tier number, a colour and a list of blocks at offsets from the Athanor. The Athanor's tier SHALL be the highest tier whose blocks, and the blocks of every lower tier, are all in place; with none in place it is tier 0. The shipped Albedo tier SHALL be twelve Salt Crystal Blocks in the hearth layer, filling the corners of the 5 by 5 square around the diamond hearth, so nothing stands next to the Athanor on its own level.

#### Scenario: Albedo ring
- **WHEN** twelve Salt Crystal Blocks fill the hearth layer of a formed Athanor out to a 5 by 5 square
- **THEN** the Athanor is tier 1, Albedo

#### Scenario: Broken ring
- **WHEN** one block of the ring is removed
- **THEN** the Athanor is tier 0 again at its next check

### Requirement: Salt Crystal Block
Manufacturing SHALL add a Salt Crystal Block. It SHALL be grown only on the Athanor, from one Salt Crystal and Salt from the tanks of its build (example 1,000 mB) at tier 0. It SHALL NOT be crafted from Salt Crystals at a crafting table and SHALL NOT be crafted back into Salt Crystals.

#### Scenario: Block recipe
- **WHEN** a Salt Crystal lies on a formed Athanor whose tanks hold the Salt
- **THEN** one Salt Crystal Block is made

#### Scenario: Not at the table
- **WHEN** a player puts nine Salt Crystals into a crafting table
- **THEN** no Salt Crystal Block is offered

### Requirement: Athanor production
The Athanor SHALL expose the production capability with one recipe per craft its current tier allows: the item in, the result out, the time; principles are expected in the tanks of its build.

#### Scenario: Tier filters crafts
- **WHEN** the production capability of a tier 0 Athanor is queried
- **THEN** it reports Blaze Powder but not the Bottle o' Enchanting

### Requirement: The item rests on top
The item to burn SHALL be drawn lying on top of the Athanor, and the result SHALL lie there once it is made and no item waits. A player SHALL put an item there by using it on the Athanor, take the result with an empty hand, and take the item back with an empty hand while sneaking. Hoppers and Item Pipes SHALL put items in and take results out on every side but the top.

#### Scenario: Place gunpowder
- **WHEN** a player uses gunpowder on the Athanor
- **THEN** the gunpowder lies on top of the Athanor

#### Scenario: Take the result
- **WHEN** Blaze Powder was made and the player uses the Athanor with an empty hand
- **THEN** the player receives the Blaze Powder

## ADDED Requirements

### Requirement: Athanor hearth
The Athanor SHALL stand on a hearth of 13 vanilla Bricks in a diamond directly below it, centred under it and reaching two blocks out, with three Foundation Fluid Tanks two blocks from it on its own level, standing on the tips of the diamond: behind it, to its left and to its right, seen from its front. Right clicking the Athanor with Paracelsus' Journal SHALL check the hearth and the tanks and form the Athanor when they are complete, making the player its owner, and SHALL NOT open the book; when a brick or a tank is missing it SHALL name it. Breaking the brick right under a formed Athanor SHALL unform it at once; a missing tank SHALL unform it at the next check.

#### Scenario: Formed
- **WHEN** a player right clicks an Athanor standing on a complete hearth with the Journal
- **THEN** the Athanor forms and the player sees "The Athanor is ready."

#### Scenario: Brick missing
- **WHEN** a player right clicks an Athanor whose hearth misses a brick with the Journal
- **THEN** nothing forms and the player is told that Bricks are needed in the hearth

#### Scenario: Tank missing
- **WHEN** a player right clicks an Athanor on a complete hearth without a Fluid Tank to its left with the Journal
- **THEN** nothing forms and the player is told that a Fluid Tank is needed left of the Athanor

### Requirement: Athanor layout is pure logic
The hearth cells, the tank positions for each facing and the schematic of a stage (the hearth, the Athanor, its tanks and the blocks of every tier up to it) SHALL be computed in pure Java covered by JUnit.

#### Scenario: Albedo schematic
- **WHEN** the schematic of the Albedo tier is computed
- **THEN** its lower layer is the diamond hearth with the twelve Salt Crystal Blocks around it, a 5 by 5 square, and its upper layer holds only the Athanor and its tanks

### Requirement: Formed Athanor look
A formed Athanor SHALL be drawn as a brick tower furnace about one and a half blocks tall, turned by its front, with an iron fire door showing the flame in the tier's colour and an iron plate on top where the item rests. Copper pipes SHALL run from the tower into each of its three tanks; when a tank is stacked higher, the pipe SHALL enter its side. Unformed, the Athanor SHALL keep its plain block.

#### Scenario: Formed look
- **WHEN** a player forms an Athanor
- **THEN** it shows as a brick tower with a fire door and three copper pipes running into its tanks

#### Scenario: Unformed look
- **WHEN** a tank of a formed Athanor is taken away and the Athanor unforms
- **THEN** the tower and the pipes give way to the plain block
