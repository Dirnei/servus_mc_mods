## MODIFIED Requirements

### Requirement: Coagulation recipes appear in JEI
When JEI is installed, every coagulation recipe SHALL appear in a "Coagulation" category showing the principles with their mB, the item ingredient if any, the seed items that grow it, each saying that it is used up per cycle and what it gives, the result and the time in seconds. A recipe that grows only from its own result SHALL show the result with one more, the seed that comes back. The Coagulator SHALL be listed as the category's machine.

#### Scenario: Look up Gunpowder
- **WHEN** the player looks up how gunpowder is made in JEI
- **THEN** the Coagulation category shows the gunpowder recipe with its principles, gunpowder as its seed, two gunpowder as its result and the time

#### Scenario: Uses of Kandis
- **WHEN** the player looks up the uses of Kandis in JEI
- **THEN** the Coagulation category shows the Salt Crystal recipe with Kandis among its seeds
