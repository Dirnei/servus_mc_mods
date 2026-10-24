## ADDED Requirements

### Requirement: Calcination shows what a recipe really burns
The input slot of a calcination recipe in JEI SHALL show only the items the Calciner burns with that recipe; items that a more specific recipe takes SHALL appear only in that recipe. A recipe that takes no item at all, because more specific recipes take every one of its items, SHALL NOT be listed.

#### Scenario: Nether Wart
- **WHEN** the player looks up the uses of Nether Wart in JEI
- **THEN** the Calcination category shows the Nether Wart recipe and not the Crops recipe

#### Scenario: Crops recipe
- **WHEN** the player looks at the Crops calcination recipe in JEI
- **THEN** its input slot cycles through wheat, carrots and the other crops but never shows Nether Wart
