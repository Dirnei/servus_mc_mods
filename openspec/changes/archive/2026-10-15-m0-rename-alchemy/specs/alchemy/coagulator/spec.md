# Spec Delta

## MODIFIED Requirements

### Requirement: Coagulation recipes are data
A coagulation recipe SHALL be a datapack recipe of type `servus_alchemy:coagulation` with the mB of Salt, Sulfur and Mercury it needs (each may be 0), at most one item ingredient with a count, one result item with a count and a time in ticks.

#### Scenario: Salt Crystal
- **WHEN** the shipped data holds a recipe of 250 mB Salt for one Salt Crystal in 40 ticks
- **THEN** a Coagulator with that recipe selected and 250 mB Salt makes one Salt Crystal in 2 seconds

### Requirement: First alchemical materials
Alchemy SHALL add the items Salt Crystal, Sulfur and Quicksilver, made only in the Coagulator, as the first materials later alchemy builds on.

#### Scenario: Sulfur
- **WHEN** a Coagulator runs the shipped Sulfur recipe
- **THEN** it makes the Sulfur item from Sulfur principle
