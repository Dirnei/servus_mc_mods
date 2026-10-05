# lib/wrench Specification

## Purpose

The wrench is the one tool for configuring Servus blocks. It rotates ordinary blocks and hands the click to blocks that know what a wrench means to them.

## Requirements

### Requirement: Wrench rotates blocks with a facing
Using the wrench on a block that has a horizontal facing or a six way facing property, while not sneaking, SHALL cycle that property to its next value and play a sound.

#### Scenario: Rotating a furnace
- **WHEN** the player right clicks a furnace facing north with the wrench
- **THEN** the furnace now faces the next horizontal direction and a click sound plays

#### Scenario: Sneaking
- **WHEN** the player sneaks and right clicks a furnace with the wrench
- **THEN** the furnace keeps its facing

#### Scenario: Block without facing
- **WHEN** the player right clicks a stone block with the wrench
- **THEN** nothing happens and the block interaction continues as if no wrench was held

### Requirement: Wrenchable blocks handle the click
A block that implements the `Wrenchable` interface SHALL receive the wrench use with the full click context instead of the rotation behavior, regardless of the rotation config option.

#### Scenario: Servus block
- **WHEN** the player right clicks a block implementing `Wrenchable` with the wrench
- **THEN** the block's own handler runs and decides the result, and the block is not rotated by the wrench

### Requirement: Wrench is obtainable and listed
The wrench SHALL be craftable from vanilla materials and SHALL appear in the Servus creative tab, which exists whenever Servus Lib is installed.

#### Scenario: Crafting
- **WHEN** the player places iron ingots and a stick in the wrench pattern (for example an inverted T of three iron ingots over a stick)
- **THEN** the crafting result is one wrench

#### Scenario: Creative tab
- **WHEN** the player opens the creative inventory
- **THEN** a tab named "Servus" exists with the wrench as its icon and lists the wrench
