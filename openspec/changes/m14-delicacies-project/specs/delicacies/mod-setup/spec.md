## Purpose

Defines how Servus: Delicacies, the alcohol free Bavarian kitchen of the family, presents itself in the game: its identity, its creative tab and its language.

## ADDED Requirements

### Requirement: Delicacies identity
Servus: Delicacies SHALL load with the mod id `servus_delicacies` and the display name "Servus: Delicacies", and SHALL declare `servus_foundation` as its only required Servus dependency.

#### Scenario: Mod list
- **WHEN** a game starts with `servus_foundation` and `servus_delicacies`
- **THEN** the Mods screen lists "Servus: Delicacies" with its version and the license "MIT"

#### Scenario: Without Foundation
- **WHEN** a game starts with `servus_delicacies` but without `servus_foundation`
- **THEN** NeoForge refuses to start and reports the missing dependency `servus_foundation` for `servus_delicacies`

### Requirement: Delicacies creative tab
Servus: Delicacies SHALL add its own creative tab titled "Servus: Delicacies", placed after the shared Servus tab, that lists every Delicacies item. Its icon SHALL be the Pretzel.

#### Scenario: Tab in the creative inventory
- **WHEN** a player in creative mode opens the creative inventory
- **THEN** a tab titled "Servus: Delicacies" with a Pretzel icon is shown and lists the Pretzel

### Requirement: Pretzel
The Pretzel SHALL be a Delicacies item that stacks to 64 and is named "Pretzel". Until the bakery gives it a recipe and food values it SHALL be obtainable from the creative tab only and SHALL NOT be edible.

#### Scenario: Pretzel from the tab
- **WHEN** a player takes a Pretzel from the Delicacies creative tab
- **THEN** the item is named "Pretzel" and shows the pretzel sprite

### Requirement: English names
Every name Servus: Delicacies shows SHALL come from its `en_us` language file; it SHALL ship no other language. Names SHALL be English wherever English has one.

#### Scenario: No missing keys
- **WHEN** a player looks at the Delicacies creative tab and its items
- **THEN** every title and item name reads as English text, never as a raw translation key
