## MODIFIED Requirements

### Requirement: Tiers are data
A tier SHALL be a JSON file at `data/<namespace>/servus_alchemy/athanor_tier/<name>.json` with a tier number, a colour and a list of blocks at offsets from the Athanor; a block MAY name block state properties it must have, each with one or more accepted values. The offsets SHALL be given for an Athanor whose front faces north and SHALL turn with the Athanor's front, so a tier fits an Athanor facing any way. The Athanor's tier SHALL be the highest tier whose blocks, and the blocks of every lower tier, are all in place; with none in place it is tier 0. Alchemy SHALL ship three tiers:
- Albedo (tier 1, white): sixteen Salt Crystal Blocks in a 5 by 5 ring one layer below the hearth, centred under the Athanor, so the tanks and the edge of the hearth stand on it.
- Citrinitas (tier 2, yellow): beside each of the three tanks, on its outer side three blocks from the Athanor, a Principle Vat with grown crystal, citrine or orange, on the hearth level.
- Rubedo (tier 3, red): the three vats with orange crystal, and four Rubedo Obelisks on the salt ring, standing on the hearth level: one on each side of the tank behind the Athanor and one behind each side tank.

#### Scenario: Albedo ring
- **WHEN** sixteen Salt Crystal Blocks form the 5 by 5 ring under the hearth of a formed Athanor
- **THEN** the Athanor is tier 1, Albedo

#### Scenario: Broken ring
- **WHEN** one block of the ring is removed
- **THEN** the Athanor is tier 0 again at its next check

#### Scenario: Citrinitas ring
- **WHEN** a Principle Vat with grown citrine stands beside each tank of an Albedo Athanor
- **THEN** the Athanor is tier 2, Citrinitas, and its flame is yellow

#### Scenario: Plain vats
- **WHEN** three empty Principle Vats stand beside the tanks of an Albedo Athanor
- **THEN** the Athanor stays tier 1, Albedo

#### Scenario: Amethyst without salt
- **WHEN** the three vats with grown crystal stand in place but a Salt Crystal Block of the ring is missing
- **THEN** the Athanor is tier 0, Nigredo

#### Scenario: Rubedo pillars
- **WHEN** four Rubedo Obelisks stand on the ring beside the back tank and behind the side tanks of an Athanor whose vats hold orange crystal
- **THEN** the Athanor is tier 3, Rubedo, and its flame is red

#### Scenario: Pillar too short
- **WHEN** one obelisk of a Rubedo Athanor is broken
- **THEN** the Athanor is tier 2, Citrinitas, at its next check

#### Scenario: Turned Athanor
- **WHEN** an Athanor faces east and its ring, vats and obelisks are grown turned with it
- **THEN** it reaches the same tiers as one facing north
