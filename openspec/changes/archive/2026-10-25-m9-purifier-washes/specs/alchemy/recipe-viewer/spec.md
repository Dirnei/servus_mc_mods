## ADDED Requirements

### Requirement: Purification recipes appear in JEI
When JEI is installed, every purification recipe SHALL appear in a "Purification" category showing the item with its count, the water with its mB, the Salt with its mB when the recipe makes any, the residue with its count when the recipe leaves one, and the time in seconds. The Purifier SHALL be listed as the category's machine.

#### Scenario: Look up Kandis
- **WHEN** the player looks up how Kandis is made in JEI
- **THEN** the Purification category shows Sugar and water giving Salt and Kandis, with the time

#### Scenario: Wash without Salt
- **WHEN** the player looks up the uses of Red Concrete Powder in JEI
- **THEN** the Purification category shows it and water giving Red Concrete, with no Salt slot
