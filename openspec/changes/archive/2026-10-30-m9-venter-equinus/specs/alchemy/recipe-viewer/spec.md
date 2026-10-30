# Spec Delta

## ADDED Requirements

### Requirement: Putrefaction recipes appear in JEI
When JEI is installed, every putrefaction recipe SHALL appear in a "Putrefaction" category showing the rot with its count, the result with its count, the time in seconds in a full bed of fresh straw and in an empty bed, and the shipped bedding items. The Cucurbit SHALL be listed as the category's machine, with the Dung Bed and the Bed Hatch beside it.

#### Scenario: Look up Humus
- **WHEN** the player looks up how Humus is made in JEI
- **THEN** the Putrefaction category shows the putrefiable items cycling with count 8, 1 Humus, 60 s warm and 240 s cold

#### Scenario: Uses of Rotten Flesh
- **WHEN** the player looks up the uses of Rotten Flesh in JEI
- **THEN** the Putrefaction category is among them next to the Purification category
