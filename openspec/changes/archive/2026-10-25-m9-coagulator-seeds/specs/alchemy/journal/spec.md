## MODIFIED Requirements

### Requirement: Chapter recipes stay locked
A coagulation recipe MAY name a `chapter`. A Coagulator SHALL only grow such a recipe from its seed when the team of its owner has finished that chapter, and its GUI SHALL name the needed chapter in the seed's tooltip.

#### Scenario: Locked recipe
- **WHEN** a player puts the seed of a recipe whose chapter their team has not finished into a Coagulator
- **THEN** nothing grows and the seed tooltip names the chapter the recipe needs
