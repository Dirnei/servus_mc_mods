# Spec Delta

## ADDED Requirements

### Requirement: Speed upgrades
A Packing Station SHALL have `upgrade_slots` upgrade slots (example 2, datapack JSON) shown in its GUI, the only slots of that GUI the player can change. Its conversion rate in both directions SHALL be its base `items_per_second` multiplied by its upgrades, and the GUI SHALL show the current rate.

#### Scenario: Doubled
- **WHEN** a Packing Station with a base rate of 8 holds one Speed Upgrade of multiplier 2
- **THEN** it converts about 16 items per second in each direction and its GUI shows 16/s
