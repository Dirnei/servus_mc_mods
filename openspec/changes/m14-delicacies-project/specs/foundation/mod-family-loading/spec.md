## MODIFIED Requirements

### Requirement: All three mods load together
`servus_foundation`, `servus_alchemy`, `servus_shuttles` and `servus_delicacies` SHALL load together in one game instance without conflicts.

#### Scenario: Full family installed
- **WHEN** a game starts with all four Servus mods
- **THEN** all four appear in the mod list and the game reaches the title screen or the server reaches the "Done" state

### Requirement: Alchemy and Shuttles each load with Foundation
Each content mod (`servus_alchemy`, `servus_shuttles`, `servus_delicacies`) SHALL load when only it and `servus_foundation` are installed.

#### Scenario: Alchemy with Foundation
- **WHEN** a game starts with `servus_foundation` and `servus_alchemy` and no other Servus mod
- **THEN** both mods load and the game reaches the title screen or the server reaches the "Done" state

#### Scenario: Shuttles with Foundation
- **WHEN** a game starts with `servus_foundation` and `servus_shuttles` and no other Servus mod
- **THEN** both mods load and the game reaches the title screen or the server reaches the "Done" state

#### Scenario: Delicacies with Foundation
- **WHEN** a game starts with `servus_foundation` and `servus_delicacies` and no other Servus mod
- **THEN** both mods load and the game reaches the title screen or the server reaches the "Done" state

### Requirement: Alchemy and Shuttles are independent
Alchemy, Shuttles and Delicacies SHALL NOT declare a dependency, required or optional, on each other and SHALL NOT reference each other's classes. All three MAY reference any Foundation class, including the Utility Duct.

#### Scenario: Shuttles without Alchemy
- **WHEN** a game starts with `servus_foundation` and `servus_shuttles` only
- **THEN** the game loads without any dependency error

#### Scenario: Alchemy without Shuttles
- **WHEN** a game starts with `servus_foundation` and `servus_alchemy` only
- **THEN** the game loads without any dependency error

#### Scenario: Delicacies without Alchemy and Shuttles
- **WHEN** a game starts with `servus_foundation` and `servus_delicacies` only
- **THEN** the game loads without any dependency error
