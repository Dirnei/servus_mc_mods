## ADDED Requirements

### Requirement: Field Head wood and facing
The Field Head SHALL look like a station: board panels in the wood of its recipe under the station iron frame. Its recipe SHALL take planks, the crafted item SHALL carry the wood picked from them, a placed head SHALL show that wood, and the drop of a broken head SHALL carry it again. The head SHALL have a horizontal facing, and its front SHALL face the player who places it.

#### Scenario: Wood from the recipe
- **WHEN** a player crafts a Field Head with birch planks
- **THEN** the item names birch as its wood, the placed head shows birch boards, and breaking it drops a Field Head with birch

#### Scenario: Front faces the player
- **WHEN** a player looking north places a Field Head
- **THEN** the head's front faces south, toward the player

### Requirement: Ripe share bar
The front of the Field Head SHALL show a ripe share bar in its lower panel: a small dark window that fills from the left with wheat gold in a few steps as the ripe share of the planted plots approaches the head's harvest threshold, and that is full and green while a harvest is due. The bar SHALL be empty when nothing is ripe or the head serves no field. The head SHALL change the bar only when its ripe count, planted count, crop, harvest only mode or threshold changes, never by looking at the field per tick.

#### Scenario: Bar follows the ripe share
- **WHEN** a head with wheat set and a threshold of 50 percent sees one of four planted plots ripe
- **THEN** its bar shows wheat gold, part way filled, and not green

#### Scenario: Bar turns green when due
- **WHEN** the ripe share reaches the head's threshold and a harvest is due
- **THEN** the bar is full and green

#### Scenario: Lowering the threshold
- **WHEN** the threshold is lowered in the GUI to the current ripe share
- **THEN** the bar turns green without any crop changing

### Requirement: Water window
Each side of the Field Head SHALL show a dark glass window in its upper panel, with the head's water drawn as real water (its still texture and the biome's water tint) from the bottom of the window up to the fill share of the buffer. An empty buffer SHALL show the dark glass only. The client SHALL learn the water level in a few steps of the window height (for example eighths), and only when the step changes, not on every mB.

#### Scenario: Water rises in the window
- **WHEN** a pipe fills an empty head to half of its capacity
- **THEN** the windows show water up to half their height

#### Scenario: Small changes stay quiet
- **WHEN** the field uses a few mB and the level stays within the same step
- **THEN** nothing is sent to the clients for the water

### Requirement: Crop icon on top
The top of the Field Head SHALL show a dark inset plate with the field's crop item drawn flat on it. The plate SHALL stay empty when no crop is set or harvest only is on. Players near the head SHALL see a new crop on the plate as soon as it is set.

#### Scenario: Crop shown on top
- **WHEN** a player sets carrots as the field's crop
- **THEN** the head's top shows a carrot on the plate

#### Scenario: Harvest only clears the plate
- **WHEN** harvest only is turned on
- **THEN** the plate is empty

### Requirement: Look steps are pure logic
The mapping from planted count, ripe count, threshold and due state to a bar step, and from water amount and capacity to a window level, SHALL be pure Java without Minecraft imports and covered by JUnit tests. Any water above nothing SHALL show at least the lowest level, and only a full buffer SHALL show the top level.

#### Scenario: Water levels
- **WHEN** a buffer of 8000 mB holds 1 mB, 4000 mB or 8000 mB
- **THEN** the window level is the lowest step, half, and full
