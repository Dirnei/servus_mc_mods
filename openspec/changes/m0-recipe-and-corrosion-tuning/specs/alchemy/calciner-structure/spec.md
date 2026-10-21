## MODIFIED Requirements

### Requirement: Calciner parts
Alchemy SHALL add the Firebox, Ash Pan, Alembic, Receiver, Sulfur Hood and Copper Duct as blocks, each crafted at a crafting table from vanilla materials without research. The Calciner block itself SHALL be the furnace mouth. The stone parts SHALL be crafted from the stone they are drawn in: the Firebox from Polished Deepslate and the Ash Pan from Deepslate Tiles.

#### Scenario: Alembic recipe
- **WHEN** the player crafts the Alembic pattern of copper ingots and glass
- **THEN** one Alembic is produced

#### Scenario: Firebox recipe
- **WHEN** the player crafts the Firebox pattern of Polished Deepslate, iron bars and a furnace
- **THEN** one Firebox is produced

#### Scenario: Ash Pan recipe
- **WHEN** the player crafts the Ash Pan pattern of Deepslate Tiles and an iron ingot
- **THEN** one Ash Pan is produced

#### Scenario: Bricks no longer craft the parts
- **WHEN** the player lays Bricks in the Firebox, Ash Pan or Calciner pattern
- **THEN** nothing is produced
