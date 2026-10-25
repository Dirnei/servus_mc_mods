# Spec Delta

## ADDED Requirements

### Requirement: Citrine tower
Grown citrine SHALL be a stepped tower of upright blocks out of the vat, the lowest narrower than the vat's opening and each narrower than the one below (example 8, 6 and 4 pixels wide), ending in a stepped tip, about as high as the Athanor's tanks. Small buds SHALL grow on the tower's sides and on its steps, each a few upright boxes with stepped pixel tips whose side shards tilt only in 22.5 degree steps.

#### Scenario: Grown look
- **WHEN** a player looks at a grown vat
- **THEN** a stepped tower of three narrowing blocks with a stepped tip stands in it, about as high as the tanks, with box buds on it

### Requirement: Citrine buds
A bud on a side of the tower SHALL lean upward only, never down or sideways (example up to 38 degrees); a bud on top of a step SHALL stand straight. Every bud SHALL have its own lean, turn on its axis and size (example up to 40 percent bigger or smaller), the same each time for the same vat, and two vats at different places SHALL differ. A side of the tower SHALL grow no buds where the block beside the vat at that height is not air.

#### Scenario: Next to a tank
- **WHEN** a player looks at a grown vat with a tank on its east side and air on the other sides
- **THEN** the tower shows buds on its north, south and west sides and on its steps, and no bud reaches into the tank

#### Scenario: Buds lean up
- **WHEN** a player looks at a grown vat from any side
- **THEN** every bud on the tower's sides points away from the tower and tips upward, and none points down

#### Scenario: Same vat, same tower
- **WHEN** a player leaves and comes back to a grown vat
- **THEN** its buds stand at the same places with the same leans and sizes as before

### Requirement: Crystal colours
Grown crystal SHALL be shaded with a ramp of its colours from shadow to highlight, never darkened toward brown: clear yellows for citrine, oranges for the crystal the Rubedo ritual turned. The Rubedo ritual SHALL shift the ramp from the yellows to the oranges.

#### Scenario: Clear yellow
- **WHEN** a player looks at the shaded side of a grown citrine tower
- **THEN** it reads as a deeper golden yellow, not brown

#### Scenario: Turning orange
- **WHEN** the Rubedo ritual turns a citrine vat
- **THEN** the tower's yellows shift to oranges, shadows and highlights alike

## MODIFIED Requirements

### Requirement: The ritual is seen and heard
Every player near the Athanor SHALL see the ritual play in step with the server: the offering rises from the plate, spins and hums while the flame turns the ritual's colour; it bursts into one stream of light per block to turn, each arcing to its block; then each block grows into its result with its own light; at the end a flash, and the flame takes the new tier's colour. For the Citrinitas ritual the flame turns violet, the streams turn gold on their way, the vats fill with glowing gold liquid and a citrine tower grows out of each, block by block, after which its buds sprout through four sizes from small to full. A player who comes near while a ritual runs SHALL see it from its current point. Sounds SHALL mark the offering, the burst, the growth and the end.

#### Scenario: Watching from afar
- **WHEN** a second player walks up to the Athanor halfway through the ritual
- **THEN** that player sees the towers growing out of the vats, not the start

#### Scenario: Tower before buds
- **WHEN** a player watches the Citrinitas ritual
- **THEN** each tower rises block by block first, and only then do the buds appear and grow in steps to their full size

### Requirement: Grown vats
A Principle Vat with grown citrine SHALL show a citrine tower as described in Citrine tower, give light, and send up a few glints of particles now and then. It SHALL NOT count as a Principle Vat of a Coagulator. Breaking it SHALL drop a Principle Vat and a few Amethyst Shards (example 2 to 4), and the Athanor SHALL fall back as described in Falling back; the ritual must be cast again. Grown vats SHALL be placed only by the ritual; the grown vat SHALL have no item.

#### Scenario: Broken crystal
- **WHEN** a player breaks one grown vat of a Citrinitas Athanor
- **THEN** a Principle Vat and some Amethyst Shards drop, the other two vats lose their citrine and stand empty, and the Athanor is Albedo

#### Scenario: Grown vat in a Coagulator
- **WHEN** a grown vat stands in a Coagulator's vat row and the player right clicks the Coagulator with the Journal
- **THEN** nothing forms and the player is told a Principle Vat is needed there
