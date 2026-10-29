# Spec Delta

## MODIFIED Requirements

### Requirement: Field Head wood and facing
The Field Head SHALL look like a station: board panels in the wood of its recipe under the station iron frame. Its recipe SHALL take planks, the crafted item SHALL carry the wood picked from them, a placed head SHALL show that wood, and the drop of a broken head SHALL carry it again. The head SHALL have a horizontal facing. On placement its front, with the ripe share bar, SHALL turn to a Farm Rail by the shared rail facing rule (see `shuttles/rail-facing`), and SHALL face the player when no Farm Rail decides it or the player sneaks. The facing SHALL NOT change which Farm Rail the head serves: it serves one on any side as before.

#### Scenario: Wood from the recipe
- **WHEN** a player crafts a Field Head with birch planks
- **THEN** the item names birch as its wood, the placed head shows birch boards, and breaking it drops a Field Head with birch

#### Scenario: Front faces the player
- **WHEN** a player looking north places a Field Head with no Farm Rail beside it
- **THEN** the head's front faces south, toward the player

#### Scenario: Front faces the farm rail
- **WHEN** a player looking north places a Field Head just south of a Farm Rail
- **THEN** the head's front and its ripe share bar face north, toward the rail

#### Scenario: Sneak placed head still serves
- **WHEN** a player sneaks and places a Field Head with its back to a Farm Rail
- **THEN** the front faces the player and the head serves the field of that rail
