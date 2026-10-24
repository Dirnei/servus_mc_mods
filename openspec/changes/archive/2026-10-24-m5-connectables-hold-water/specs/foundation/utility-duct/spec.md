# Spec Delta

## ADDED Requirements

### Requirement: Ducts stand in water
A Utility Duct SHALL be waterloggable. Placed into a water source it SHALL keep the water. A water bucket used on a dry duct SHALL waterlog it, and an empty bucket used on a waterlogged duct SHALL take the water out. Breaking a waterlogged duct SHALL leave a water source. Water SHALL flow out of a waterlogged duct as from any source. The duct's lines SHALL not take or give the water it stands in.

#### Scenario: Duct into a pond
- **WHEN** a player places a duct into a water source block of a pond
- **THEN** the duct is placed waterlogged and the pond has no gap

#### Scenario: Bucket on a dry duct
- **WHEN** a player uses a water bucket on a dry duct
- **THEN** the duct is waterlogged and the player holds an empty bucket

#### Scenario: Empty bucket on a waterlogged duct
- **WHEN** a player uses an empty bucket on a waterlogged duct
- **THEN** the duct is dry and the player holds a water bucket

#### Scenario: Breaking a waterlogged duct
- **WHEN** a waterlogged duct is broken
- **THEN** a water source stays where the duct was

#### Scenario: Fluid pipe in water
- **WHEN** a waterlogged duct carries a Fluid Pipe whose side toward a tank pushes out on channel 1 and no source is on channel 1
- **THEN** the tank receives no water

### Requirement: Ducts hold back other fluids
Flowing water and lava SHALL stop at a Utility Duct like at a solid wall: they SHALL neither enter the duct's block nor wash the duct away. A duct SHALL never hold lava. Placing a duct into a lava source SHALL fail without a message and the lava SHALL stay. Placing a duct into flowing water or lava SHALL place it dry, and the duct SHALL then dam the flow.

#### Scenario: Water flows against a dry duct
- **WHEN** water flows toward a dry duct lying on the ground
- **THEN** the duct stays dry, no item drops and the water flows around it

#### Scenario: Lava flows against a duct
- **WHEN** lava flows toward a duct
- **THEN** the duct stays and the lava does not enter the duct's block

#### Scenario: Duct into a lava source
- **WHEN** a player places a duct into a lava source block
- **THEN** no duct is placed and the lava source stays

#### Scenario: Duct into flowing water
- **WHEN** a player places a duct into flowing water
- **THEN** the duct is placed dry and the water stops at it
