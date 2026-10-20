## MODIFIED Requirements

### Requirement: All three mods load together
`servus_foundation`, `servus_alchemy`, `servus_shuttles`, `servus_delicacies` and `servus_brewery` SHALL load together in one game instance without conflicts.

#### Scenario: Full family installed
- **WHEN** a game starts with all five Servus mods
- **THEN** all five appear in the mod list and the game reaches the title screen or the server reaches the "Done" state

### Requirement: Alchemy and Shuttles are independent
Alchemy, Shuttles and Delicacies SHALL NOT declare a dependency, required or optional, on each other or on Brewery and SHALL NOT reference each other's or Brewery's classes. Brewery SHALL NOT declare a dependency on Alchemy or Shuttles and SHALL NOT reference their classes. All four MAY reference any Foundation class, including the Utility Duct, and Brewery MAY reference any Delicacies class.

#### Scenario: Shuttles without Alchemy
- **WHEN** a game starts with `servus_foundation` and `servus_shuttles` only
- **THEN** the game loads without any dependency error

#### Scenario: Alchemy without Shuttles
- **WHEN** a game starts with `servus_foundation` and `servus_alchemy` only
- **THEN** the game loads without any dependency error

#### Scenario: Delicacies without Alchemy and Shuttles
- **WHEN** a game starts with `servus_foundation` and `servus_delicacies` only
- **THEN** the game loads without any dependency error

#### Scenario: Delicacies without Brewery
- **WHEN** a game starts with `servus_foundation` and `servus_delicacies` but without `servus_brewery`
- **THEN** the game loads without any dependency error

#### Scenario: Brewery without Alchemy and Shuttles
- **WHEN** a game starts with `servus_foundation`, `servus_delicacies` and `servus_brewery` only
- **THEN** the game loads without any dependency error

## ADDED Requirements

### Requirement: Brewery loads with Foundation and Delicacies
`servus_brewery` SHALL declare `servus_delicacies` as a required dependency with a version range that includes the Delicacies version it was built against, and SHALL load when only it, `servus_foundation` and `servus_delicacies` are installed.

#### Scenario: Brewery with Foundation and Delicacies
- **WHEN** a game starts with `servus_foundation`, `servus_delicacies` and `servus_brewery` and no other Servus mod
- **THEN** all three mods load and the game reaches the title screen or the server reaches the "Done" state

#### Scenario: Brewery without Delicacies
- **WHEN** a game starts with `servus_foundation` and `servus_brewery` but without `servus_delicacies`
- **THEN** NeoForge refuses to start and reports the missing dependency `servus_delicacies` for `servus_brewery`
