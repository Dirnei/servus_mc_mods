# Spec Delta

## ADDED Requirements

### Requirement: Calciner is a producer
The Calciner SHALL expose the production capability: one recipe per calcination recipe, with one of the first matching item of its ingredient in and no item out (the principles go to its tanks, not to storage), and the recipe time as ticks; the recipe of the item in its input slot SHALL be reported as active.

#### Scenario: Recipes listed
- **WHEN** the production capability of a Calciner is queried
- **THEN** it reports one recipe per calcination recipe with the recipe time

### Requirement: Coagulator is a producer
The Coagulator SHALL expose the production capability with exactly its selected recipe: the item ingredient (if any) in, the result out and the recipe time as ticks. Principles are not part of the reported inputs; they reach the Coagulator by pipe.

#### Scenario: Order Salt Crystals
- **WHEN** a Crafting CPU plans 4 Salt Crystals on a line with a Coagulator making Salt Crystals and enough Salt piped into it
- **THEN** the step runs on the Coagulator and the 4 crystals are collected into storage

## REMOVED Requirements

### Requirement: Burner machines keep fuel as upkeep
**Reason**: Only Foundation's Burner Pump and Coal Generator keep fuel now; the requirement moved to `foundation/upkeep`.
**Migration**: See `foundation/upkeep`.

### Requirement: Upkeep stats
**Reason**: Moved to `foundation/upkeep` with the burner machines that use it.
**Migration**: See `foundation/upkeep`.

### Requirement: Burner Smelter is a producer
**Reason**: The Burner Smelter is removed by the alchemy rethink.
**Migration**: None.

### Requirement: Crusher is a producer
**Reason**: The Crusher is removed by the alchemy rethink.
**Migration**: None.
