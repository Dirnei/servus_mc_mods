## Purpose

Rituals advance the Athanor to the higher stages of the Great Work: the player places plain blocks around the Athanor and offers one item, and the Athanor turns those blocks into grown stage blocks in an animated ritual.

## ADDED Requirements

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
A Principle Vat with grown citrine SHALL show a citrine cluster as tall as the tanks, give light, and send up a few glints of particles now and then. It SHALL NOT count as a Principle Vat of a Coagulator. Breaking it SHALL drop a Principle Vat and a few Amethyst Shards (example 2 to 4), so the Athanor falls back a stage at its next check and the ritual must be cast again. Grown vats SHALL be placed only by the ritual; the grown vat SHALL have no item.

#### Scenario: Broken crystal
- **WHEN** a player breaks one grown vat of a Citrinitas Athanor
- **THEN** a Principle Vat and some Amethyst Shards drop and the Athanor is Albedo at its next check

#### Scenario: Grown vat in a Coagulator
- **WHEN** a grown vat stands in a Coagulator's vat row and the player right clicks the Coagulator with the Journal
- **THEN** nothing forms and the player is told a Principle Vat is needed there
