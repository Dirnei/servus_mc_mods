# alchemy/athanor-rituals Specification

## Purpose
Rituals advance the Athanor to the higher stages of the Great Work: the player places plain blocks around the Athanor and offers one item, and the Athanor turns those blocks into grown stage blocks in an animated ritual.

## Requirements

### Requirement: Rituals are data
A ritual SHALL be a JSON file at `data/<namespace>/servus_alchemy/athanor_ritual/<name>.json` naming the offering item, the tier the Athanor must have, and a list of blocks to turn: an offset from the Athanor (given for an Athanor facing north, turned with its front), the block expected there and the block it becomes. Alchemy SHALL ship the Citrinitas ritual: an Amethyst Block offered to an Albedo Athanor turns the three Principle Vats beside its tanks into vats with grown citrine.

#### Scenario: Shipped Citrinitas ritual
- **WHEN** the shipped data is loaded
- **THEN** it holds a ritual with an Amethyst Block as offering, Albedo as the needed tier and the three vat positions of the Citrinitas tier

### Requirement: Casting a ritual
Using a ritual's offering on a formed Athanor SHALL start that ritual when the Athanor's tier equals the ritual's tier, no craft runs, no other ritual runs, and every block to turn is in place; the offering SHALL be taken from the player's hand. Otherwise the offering SHALL stay in the hand, nothing SHALL start, and the player SHALL be told why, naming a missing block and where it belongs. A ritual SHALL take a fixed time (example 8 seconds) and SHALL turn the blocks when it ends; a block that is no longer in place then SHALL stay as it is. While a ritual runs the Athanor SHALL start no craft.

#### Scenario: Cast
- **WHEN** a player uses an Amethyst Block on a formed Albedo Athanor with an empty Principle Vat beside each tank
- **THEN** the Amethyst Block leaves the hand, the ritual plays, and after it the three vats hold grown citrine and the Athanor is Citrinitas

#### Scenario: Vat missing
- **WHEN** a player uses an Amethyst Block on a formed Albedo Athanor without the vat beside the tank behind it
- **THEN** the player keeps the Amethyst Block and is told that a Principle Vat is needed behind the tank behind the Athanor

#### Scenario: Wrong stage
- **WHEN** a player uses an Amethyst Block on a formed Nigredo Athanor with the three vats in place
- **THEN** the player keeps the Amethyst Block and is told the Athanor must reach Albedo first

#### Scenario: Vat taken during the ritual
- **WHEN** a vat is broken while the ritual runs
- **THEN** the other two vats grow citrine, the broken vat drops as a plain Principle Vat, and the Athanor stays Albedo

### Requirement: The ritual is seen and heard
Every player near the Athanor SHALL see the ritual play in step with the server: the offering rises from the plate, spins and hums while the flame turns the ritual's colour; it bursts into one stream of light per block to turn, each arcing to its block; then each block grows into its result with its own light; at the end a flash, and the flame takes the new tier's colour. For the Citrinitas ritual the flame turns violet, the streams turn gold on their way, the vats fill with glowing gold liquid and a cluster of citrine prisms grows out of each, the tallest as high as the tanks. A player who comes near while a ritual runs SHALL see it from its current point. Sounds SHALL mark the offering, the burst, the growth and the end.

#### Scenario: Watching from afar
- **WHEN** a second player walks up to the Athanor halfway through the ritual
- **THEN** that player sees the clusters growing out of the vats, not the start

### Requirement: Grown vats
A Principle Vat with grown citrine SHALL show a citrine cluster as tall as the tanks, give light, and send up a few glints of particles now and then. It SHALL NOT count as a Principle Vat of a Coagulator. Breaking it SHALL drop a Principle Vat and a few Amethyst Shards (example 2 to 4), and the Athanor SHALL fall back as described in Falling back; the ritual must be cast again. Grown vats SHALL be placed only by the ritual; the grown vat SHALL have no item.

#### Scenario: Broken crystal
- **WHEN** a player breaks one grown vat of a Citrinitas Athanor
- **THEN** a Principle Vat and some Amethyst Shards drop, the other two vats lose their citrine and stand empty, and the Athanor is Albedo

#### Scenario: Grown vat in a Coagulator
- **WHEN** a grown vat stands in a Coagulator's vat row and the player right clicks the Coagulator with the Journal
- **THEN** nothing forms and the player is told a Principle Vat is needed there

### Requirement: Falling back
When a block of the Athanor's build is gone, the Athanor SHALL fall back to the highest tier whose blocks, and those of every lower tier, are still in place, or unform when the hearth, a tank or a duct is gone. Every block a ritual grew for a tier above the one it falls back to SHALL turn back into what that ritual started from, highest tier first, without dropping anything; only the broken block drops its items. A broken ritual-grown block SHALL make the Athanor fall back at once; another missing block SHALL make it fall back at its next check.

#### Scenario: Vat broken at Citrinitas
- **WHEN** a player breaks one grown vat of a Citrinitas Athanor
- **THEN** the other two vats turn back into empty Principle Vats at once and the Athanor is Albedo

#### Scenario: Salt broken at Citrinitas
- **WHEN** a Salt Crystal Block of the ring under a Citrinitas Athanor is broken
- **THEN** at the Athanor's next check the three vats turn back into empty Principle Vats and the Athanor is Nigredo

#### Scenario: Hearth broken
- **WHEN** a hearth slab of a Citrinitas Athanor is broken
- **THEN** at the Athanor's next check it unforms and the three vats turn back into empty Principle Vats

### Requirement: Rubedo ritual
Alchemy SHALL ship the Rubedo ritual: a Ghast Tear offered to a Citrinitas Athanor turns the Obsidian on the four pillar cells (on the salt ring beside the tank behind the Athanor and behind each side tank, on the hearth level, with air above each) into Rubedo Obelisks, and the citrine in the three vats into orange crystal. A turn MAY accept several states of its block, so a vat already orange turns again when the ritual is cast after a broken obelisk.

#### Scenario: Cast
- **WHEN** a player uses a Ghast Tear on a formed Citrinitas Athanor with Obsidian on the four pillar cells and air above them
- **THEN** the tear leaves the hand, the ritual plays, and after it four Rubedo Obelisks stand there, the vats hold orange crystal and the Athanor is Rubedo

#### Scenario: Blocked above
- **WHEN** a torch stands above one of the Obsidian blocks and a player uses a Ghast Tear on the Athanor
- **THEN** the player keeps the tear and is told that the space above the Obsidian behind the left tank must be free

#### Scenario: Recast after a broken obelisk
- **WHEN** one obelisk of a Rubedo Athanor was broken, the player puts Obsidian back and uses a Ghast Tear on the Athanor
- **THEN** the ritual plays again, four obelisks rise and the Athanor is Rubedo

### Requirement: Rubedo ritual look
The Rubedo ritual SHALL play like every ritual: the Ghast Tear rises from the plate while the flame turns deep red and the citrine clusters pulse; it splits into four glowing drops with trails that arc onto the Obsidian; glowing veins crack through each Obsidian block while the citrine in the vats turns orange; an obelisk rises out of each block with an overshoot and a shard pops free above its tip; a red flash ends it.

#### Scenario: Watching
- **WHEN** a player watches the Rubedo ritual to its end
- **THEN** they see the drops land on the four Obsidian blocks, the veins, the obelisks rising, and the vat crystals change from yellow to orange

### Requirement: Rubedo Obelisk
The Rubedo Obelisk SHALL be a block two blocks tall: a dark red faceted obelisk with a glowing core, a small shard floating and turning above its tip, bands of light running up it and embers rising around it. It SHALL give light, have no item and be placed only by the Rubedo ritual. Breaking either half SHALL remove both and drop one Obsidian, and the Athanor SHALL fall back at once: the other obelisks turn back into Obsidian with the space above cleared, the orange vats turn back to citrine, and the Athanor is Citrinitas; the ritual must be cast again.

#### Scenario: Broken obelisk
- **WHEN** a player breaks the upper half of an obelisk of a Rubedo Athanor
- **THEN** both halves are gone, one Obsidian drops, the other three obelisks turn back into Obsidian, the vats show citrine again, and the Athanor is Citrinitas

#### Scenario: Idle look
- **WHEN** a player looks at a Rubedo Athanor at rest
- **THEN** light bands run up the four obelisks, their shards turn and embers rise around them

### Requirement: Orange vats
A Principle Vat whose citrine turned orange in the Rubedo ritual SHALL look like a grown vat in orange, give light and glint, count for Citrinitas and Rubedo, and break like a citrine vat: a Principle Vat and a few Amethyst Shards drop and the Athanor falls back to Albedo at once, the other vats standing empty and the obelisks turned back into Obsidian.

#### Scenario: Broken orange vat
- **WHEN** a player breaks an orange vat of a Rubedo Athanor
- **THEN** a Principle Vat and some Amethyst Shards drop, the other two vats stand empty, the four obelisks are Obsidian again, and the Athanor is Albedo
