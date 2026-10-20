## Purpose

Defines how Servus: Brewery, the beer mod of the family, presents itself in the game: its identity, its dependencies, its creative tab and its language.

## ADDED Requirements

### Requirement: Brewery identity
Servus: Brewery SHALL load with the mod id `servus_brewery` and the display name "Servus: Brewery", and SHALL declare `servus_foundation` and `servus_delicacies` as its only required Servus dependencies.

#### Scenario: Mod list
- **WHEN** a game starts with `servus_foundation`, `servus_delicacies` and `servus_brewery`
- **THEN** the Mods screen lists "Servus: Brewery" with its version and the license "MIT"

#### Scenario: Without Delicacies
- **WHEN** a game starts with `servus_foundation` and `servus_brewery` but without `servus_delicacies`
- **THEN** NeoForge refuses to start and reports the missing dependency `servus_delicacies` for `servus_brewery`

#### Scenario: Without Foundation
- **WHEN** a game starts with `servus_brewery` but without `servus_foundation`
- **THEN** NeoForge refuses to start and reports the missing dependency `servus_foundation` for `servus_brewery`

### Requirement: Brewery creative tab
Servus: Brewery SHALL add its own creative tab titled "Servus: Brewery", placed after the Delicacies tab, that lists every Brewery item. Its icon SHALL be the Beer Glass.

#### Scenario: Tab in the creative inventory
- **WHEN** a player in creative mode opens the creative inventory
- **THEN** a tab titled "Servus: Brewery" with a Beer Glass icon is shown after the "Servus: Delicacies" tab and lists the Beer Glass

### Requirement: Beer Glass
The Beer Glass SHALL be a Brewery item, the empty half litre glass, that stacks to 64 and is named "Beer Glass". Until the beer change lets it be made and filled it SHALL be obtainable from the creative tab only and SHALL do nothing when used.

#### Scenario: Glass from the tab
- **WHEN** a player takes a Beer Glass from the Brewery creative tab
- **THEN** the item is named "Beer Glass" and shows an empty glass with a handle

### Requirement: English names
Every name Servus: Brewery shows SHALL come from its `en_us` language file; it SHALL ship no other language. Names SHALL be English wherever English has one.

#### Scenario: No missing keys
- **WHEN** a player looks at the Brewery creative tab and its items
- **THEN** every title and item name reads as English text, never as a raw translation key
