# Spec Delta

## ADDED Requirements

### Requirement: Calciner is no producer
The Calciner SHALL NOT expose the production capability: it makes no item a Crafting CPU could order. It is fed by pipes, hoppers or by hand.

#### Scenario: Not on the order list
- **WHEN** a duct with the data line touches a Calciner
- **THEN** the Calciner is no producer member of the data network

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
