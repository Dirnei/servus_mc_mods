# Spec Delta

## ADDED Requirements

### Requirement: Indicator lights
The Pattern Storage front SHALL show a 3x3 grid of lights with one lit light per stored pattern. When `patterns_per_block` is not 9, the lit count SHALL be the fill level scaled to 9, rounded up while any pattern is stored. The lights SHALL update when a pattern is saved or deleted and when the block is placed with patterns.

#### Scenario: Five patterns
- **WHEN** a Pattern Storage with room for 9 holds 5 patterns
- **THEN** 5 of its 9 lights are lit

#### Scenario: Other capacity
- **WHEN** `patterns_per_block` is 18 and the block holds 1 pattern
- **THEN** 1 light is lit
