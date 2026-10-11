## ADDED Requirements

### Requirement: Further material swaps
Besides the wood, a wood block SHALL be able to swap further textures of its model per block: its block entity names, in its model data, textures of its model and the textures to show in their place. Quads with a named texture SHALL be drawn with the replacement at the same spot; every other quad SHALL stay as it is. Items SHALL show the model without these swaps. Foundation SHALL NOT need to know the blocks or textures that use this.

#### Scenario: Weathered rail head
- **WHEN** a farm rail's block entity names its fresh head texture with the weathered one as replacement
- **THEN** the rail is drawn with weathered heads and its sleepers keep its wood
