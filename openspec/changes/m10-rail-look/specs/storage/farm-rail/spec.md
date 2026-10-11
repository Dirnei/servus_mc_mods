## ADDED Requirements

### Requirement: Farm Rail copper heads
The Farm Rail SHALL have the track look of the Warehouse Rail with rail heads of fresh copper with a light touch of verdigris instead of steel, and sleepers in the wood of its recipe. Its recipe SHALL take copper ingots and planks and SHALL give several farm rails carrying the wood of the planks.

#### Scenario: Crafting
- **WHEN** the player places copper ingots and oak planks in the rail pattern
- **THEN** the result is several farm rails (for example sixteen) carrying oak

#### Scenario: Next to a Warehouse Rail
- **WHEN** a Farm Rail and a Warehouse Rail of the same wood lie in one run
- **THEN** the heads change from copper to steel at the border and the gauge stays the same
