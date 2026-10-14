# Spec Delta

## MODIFIED Requirements

### Requirement: Ash
Alchemy SHALL add the item Ash, which the Calciner's Ash Pan gives. It SHALL have no crafting recipe of its own.

#### Scenario: Ash from the Calciner
- **WHEN** a Calciner has made Salt and a player empties its Ash Pan
- **THEN** the player holds Ash

### Requirement: Caput Mortuum
Alchemy SHALL add the item Caput Mortuum, the residue the Purifier leaves after washing Ash. It SHALL have no use in this change; a later stage turns it into carbon and diamond.

#### Scenario: Residue
- **WHEN** a Purifier finishes washing 4 Ash
- **THEN** one Caput Mortuum lies in its Residue Crock

### Requirement: Purification recipes are data
A purification recipe SHALL be a datapack recipe of type `servus_alchemy:purification` with one counted item ingredient, the mB of water it needs, the mB of Salt it makes, an optional residue item with count and a time in ticks. The Salt one item makes in the first recipe that takes Ash SHALL also be the Salt the Ash Pan collects per Ash.

#### Scenario: Shipped recipe
- **WHEN** the shipped data holds a recipe of 4 Ash and 400 mB water for 400 mB Salt and one Caput Mortuum in 160 ticks
- **THEN** a formed Purifier with 4 Ash and 400 mB water makes 400 mB Salt and one Caput Mortuum in 8 seconds
