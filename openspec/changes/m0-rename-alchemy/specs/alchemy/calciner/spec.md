# Spec Delta

## MODIFIED Requirements

### Requirement: Calcination recipes are data
A calcination recipe SHALL be a datapack recipe of type `servus_alchemy:calcination` with one ingredient (an item or a tag), the mB of Salt, Sulfur and Mercury it gives (each may be 0) and a time in ticks. Any matching item SHALL be accepted; an item without a recipe SHALL be refused.

#### Scenario: Cobblestone
- **WHEN** the shipped data holds a recipe for `#c:cobblestones` giving 100 mB Salt and 10 mB Mercury in 80 ticks
- **THEN** a Calciner turns one cobblestone into 100 mB Salt and 10 mB Mercury in 4 seconds

#### Scenario: Unknown item
- **WHEN** a hopper offers a diamond and no calcination recipe matches diamonds
- **THEN** the Calciner takes nothing
