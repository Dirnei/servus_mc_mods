## ADDED Requirements

### Requirement: Capacity Upgrade item
Foundation SHALL provide a Capacity Upgrade item crafted from vanilla materials (example: four iron ingots, four glass and a bucket in the middle), stacking to 16, with a tooltip saying that it gives a block more room. It SHALL NOT count as a speed upgrade: it SHALL NOT be accepted by upgrade slots that multiply a rate, and it SHALL NOT change any rate.

#### Scenario: Crafting
- **WHEN** a player crafts iron ingots, glass and a bucket in the Capacity Upgrade pattern
- **THEN** the result is one Capacity Upgrade

#### Scenario: Not a speed upgrade
- **WHEN** a player puts a Capacity Upgrade into the upgrade slots of a machine with speed upgrade slots
- **THEN** the slot refuses it

### Requirement: Capacity upgrade slots
A block MAY have capacity upgrade slots. Each SHALL take exactly one Capacity Upgrade and SHALL refuse every other item, and the block SHALL be able to read how many Capacity Upgrades its slots hold. How much room one upgrade adds SHALL be set by the block that owns the slots, from its datapack data. Capacity Upgrades in slots SHALL drop when the block is broken unless the block keeps them on its dropped item.

#### Scenario: Only Capacity Upgrades
- **WHEN** a player offers a Speed Upgrade and then a Capacity Upgrade to a capacity upgrade slot
- **THEN** the slot refuses the Speed Upgrade and takes the Capacity Upgrade

#### Scenario: One per slot
- **WHEN** a player puts a stack of four Capacity Upgrades into an empty capacity upgrade slot
- **THEN** the slot holds one and three stay with the player

#### Scenario: Count
- **WHEN** a block's capacity upgrade slots hold two Capacity Upgrades
- **THEN** the block reads a count of two
