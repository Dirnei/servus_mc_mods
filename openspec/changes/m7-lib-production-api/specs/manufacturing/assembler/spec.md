# Spec Delta

## ADDED Requirements

### Requirement: Assembler reports its production
The Assembler SHALL expose the Lib production capability on every side. Its recipes SHALL be its configured recipes: for each one the recipe id, the template stacks as inputs with their counts added up per item, the recipe result as output, and the assembling time or the machine's work time for a crafting recipe. The active recipe SHALL be the one it is working on.

#### Scenario: Configured gear recipe
- **WHEN** the iron gear recipe is configured and the production capability is queried
- **THEN** one recipe is reported with two iron ingots in, one iron gear out and 20 ticks

#### Scenario: Nothing configured
- **WHEN** no recipe is configured
- **THEN** the capability reports no recipes and no active recipe
