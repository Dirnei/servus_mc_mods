## ADDED Requirements

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
