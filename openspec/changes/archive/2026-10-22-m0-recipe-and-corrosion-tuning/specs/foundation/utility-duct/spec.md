## MODIFIED Requirements

### Requirement: Crafting
Item Pipes SHALL be crafted from copper ingots, iron ingots, glass and a chest; Power Cables from copper ingots, redstone and glass; Fluid Pipes from copper ingots, iron ingots, glass and a bucket; eight each. Data Cables SHALL be crafted from two copper ingots and a redstone dust, four at a time.

#### Scenario: Recipes
- **WHEN** the player crafts any of the three pipe and cable patterns
- **THEN** eight of the line item are produced

#### Scenario: Copper and iron pipes
- **WHEN** the player lays copper ingots in the four corners, iron ingots in the middle of the top and bottom rows, glass left and right and a chest in the centre
- **THEN** eight Item Pipes are produced, and with a bucket in the centre eight Fluid Pipes

#### Scenario: Data Cable recipe
- **WHEN** the player crafts a copper ingot, a redstone dust and a copper ingot in a row
- **THEN** four Data Cables are produced
