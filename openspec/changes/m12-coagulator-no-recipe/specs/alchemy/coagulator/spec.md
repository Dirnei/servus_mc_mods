## ADDED Requirements

### Requirement: The player can stop the Coagulator
The Coagulator's recipe popup SHALL offer a "No recipe" entry before the recipes. Picking it SHALL clear the selection, so the Coagulator does nothing until a recipe is picked again. While nothing is selected the popup SHALL mark the "No recipe" entry and the select button SHALL say so. Items already in the output slot stay there.

#### Scenario: Stop a running Coagulator
- **WHEN** a Coagulator makes Salt Crystals and the player picks "No recipe"
- **THEN** it stops making Salt Crystals, its principles stay in the tanks, and the select button reads "No recipe"

#### Scenario: Nothing selected is marked
- **WHEN** the player opens the popup of a Coagulator without a recipe
- **THEN** the "No recipe" entry is highlighted
