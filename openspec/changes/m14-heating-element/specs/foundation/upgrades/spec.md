## ADDED Requirements

### Requirement: Heating Element item
Foundation SHALL provide a Heating Element item crafted from vanilla materials (example: six copper ingots, two redstone and one iron ingot), stacking to 16, with a tooltip saying that it lets a firebox run on FE. It SHALL be its own kind of upgrade: it SHALL NOT count as a speed upgrade and SHALL NOT be a Capacity Upgrade, so upgrade slots and capacity upgrade slots SHALL refuse it and it SHALL NOT change any rate. Which blocks take it and what it does there SHALL be up to the mod that owns the block.

#### Scenario: Crafting
- **WHEN** a player crafts copper ingots, redstone and an iron ingot in the Heating Element pattern
- **THEN** the result is one Heating Element

#### Scenario: Not a speed upgrade
- **WHEN** a player puts a Heating Element into the upgrade slots of a machine with speed upgrade slots
- **THEN** the slot refuses it

#### Scenario: Not a Capacity Upgrade
- **WHEN** a player offers a Heating Element to a capacity upgrade slot
- **THEN** the slot refuses it
