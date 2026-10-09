# Spec Delta

## ADDED Requirements

### Requirement: Calciner schematic
The Calciner entry SHALL show the Calciner structure as a multiblock page: a turnable 3D view of the build with a Visualize button that projects the build into the world standing on the block the player clicks, marking blocks still missing. The entry SHALL show the crafting recipe of every part and SHALL tell the player to form the build by using the Journal on the Calciner.

#### Scenario: Visualize
- **WHEN** a player opens the Calciner entry, presses Visualize and looks at the ground
- **THEN** a ghost of the Calciner structure appears standing on that ground and the player can build into it

#### Scenario: Parts listed
- **WHEN** a player pages through the Calciner entry
- **THEN** they find the recipes of the Calciner, Firebox, Ash Pan, Alembic, Receiver, Sulfur Hood and Sulfur Duct
