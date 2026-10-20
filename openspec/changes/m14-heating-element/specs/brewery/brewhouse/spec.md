## ADDED Requirements

### Requirement: Heating Element in the Brewhouse
A formed Brewhouse SHALL take one Foundation Heating Element into its firebox and SHALL then heat the way a Tiled Stove with an element does: it burns no fuel and refuses new fuel, takes FE into a buffer, draws FE while the Brew Kettle or a kiln tray has work and the buffer holds enough, preheats to a threshold before the kettle brews and the kiln dries, and cools slowly without power or work. A right click on any formed brick with the element SHALL install it; a sneaking right click with an empty hand on a brick SHALL take it out; it SHALL drop when the Brewhouse is broken or unformed. The Brewhouse's heating numbers SHALL come from its own datapack JSON (examples: 40 FE per tick, an 8000 FE buffer, 15 s to heat up, 80 s to cool down, hot from 60 percent).

#### Scenario: Brewing on FE
- **WHEN** a Brewhouse with an element holds a full brewing recipe and FE arrives every tick through its port
- **THEN** it preheats, then brews the wort, and uses no fuel

#### Scenario: Dropped on unform
- **WHEN** a brick of a Brewhouse with an element is broken
- **THEN** the element drops as an item

#### Scenario: Taking it out
- **WHEN** a sneaking player with an empty hand right clicks a brick of a Brewhouse with an element
- **THEN** the element goes into the player's inventory and the firebox burns fuel again

### Requirement: Brewhouse port and thermometer
With a Heating Element installed, the Brewhouse SHALL show a small flat iron power port on the right end face (seen from the front, the kiln side) of its back kiln brick and SHALL take FE through the standard energy capability on that face only; a round brass thermometer between the two fire doors whose needle shows the temperature as on the Tiled Stove; and heating coils over the slits of both doors, warm orange while hot and dull while not, with a softer glow and no flames. Without the element none of them SHALL show and no face SHALL take FE.

#### Scenario: Only the port takes FE
- **WHEN** something offers FE to the front of the kettle bricks and to the port face of a Brewhouse with an element
- **THEN** only the port face takes it

#### Scenario: Wood fire unchanged
- **WHEN** a Brewhouse without an element burns Coal
- **THEN** it shows its fire doors, no thermometer and no port, and takes no FE
