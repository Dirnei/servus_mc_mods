# Spec Delta

## Purpose

Engineer Goggles are the shared "show me the hidden things" item of the Servus family.

## ADDED Requirements

### Requirement: Head slot item
Engineer Goggles SHALL be craftable from leather, iron ingots and glass and SHALL equip to the head slot on use, like a helmet without armour.

#### Scenario: Equip
- **WHEN** the player right clicks with the goggles in hand
- **THEN** they move to the head slot

### Requirement: Reveal rule
Content mods SHALL treat a player as revealing hidden things when the goggles are worn or a wrench is held in either hand, through one Lib helper.

#### Scenario: Wrench in hand
- **WHEN** a player without goggles holds the wrench
- **THEN** the reveal rule is true
