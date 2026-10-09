# Spec Delta

## ADDED Requirements

### Requirement: Calcination recipes appear in JEI
When JEI is installed, every calcination recipe SHALL appear in a "Calcination" category showing the ingredient, the mB of each principle it gives and the time in seconds. The Calciner SHALL be listed as the category's machine.

#### Scenario: Look up Salt
- **WHEN** the player looks up the uses of cobblestone in JEI
- **THEN** the Calcination category shows cobblestone giving Salt and Mercury with their amounts and the time

### Requirement: Coagulation recipes appear in JEI
When JEI is installed, every coagulation recipe SHALL appear in a "Coagulation" category showing the principles with their mB, the item ingredient if any, the result and the time in seconds. The Coagulator SHALL be listed as the category's machine.

#### Scenario: Look up Gunpowder
- **WHEN** the player looks up how gunpowder is made in JEI
- **THEN** the Coagulation category shows the gunpowder recipe with its principles and time

## REMOVED Requirements

### Requirement: Assembling recipes appear in JEI
**Reason**: The Assembler and assembling recipes are removed by the alchemy rethink.
**Migration**: None.
