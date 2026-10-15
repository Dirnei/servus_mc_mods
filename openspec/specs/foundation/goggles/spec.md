# foundation/goggles Specification

## Purpose

Engineer Goggles are the shared "show me the hidden things" item of the Servus family.

## Requirements

### Requirement: Head slot item
Engineer Goggles SHALL be craftable from leather, iron ingots and glass and SHALL equip to the head slot on use, like a helmet without armour.

#### Scenario: Equip
- **WHEN** the player right clicks with the goggles in hand
- **THEN** they move to the head slot

### Requirement: Reveal rule
Content mods SHALL use three Foundation reveal rules: a player is configuring when the goggles are worn or a Configuration Carrot is held in either hand; a player is analysing when the goggles are worn or a Data Potato is held in either hand; a player sees through panels when either rule is true. Side markers SHALL follow the configuring rule and analysis views such as the data load overlay the analysing rule.

#### Scenario: Wrench in hand
- **WHEN** a player without goggles holds the Configuration Carrot
- **THEN** the configuring rule is true and the analysing rule is false

#### Scenario: Goggles worn
- **WHEN** a player wears the goggles and holds nothing
- **THEN** the configuring and the analysing rule are both true

#### Scenario: Neither
- **WHEN** a player wears no goggles and holds a pickaxe
- **THEN** both rules are false

#### Scenario: Potato in hand
- **WHEN** a player without goggles holds the Data Potato
- **THEN** the analysing rule is true and the configuring rule is false
