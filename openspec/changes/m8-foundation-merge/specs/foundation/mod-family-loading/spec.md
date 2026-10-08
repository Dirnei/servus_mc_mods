# Spec Delta

## ADDED Requirements

### Requirement: Foundation loads standalone
`servus_foundation` SHALL load in a NeoForge 21.1.x game with no other Servus mod present and without errors or warnings about missing dependencies.

#### Scenario: Only Foundation installed
- **WHEN** a client or dedicated server starts with only `servus_foundation` among the Servus mods
- **THEN** the game reaches the title screen or the server reaches the "Done" state
- **AND** the mod list contains `servus_foundation` with the display name "Servus: Foundation"

### Requirement: Each content mod loads with Foundation alone
Each content mod (`servus_manufacturing`, `servus_storage`) SHALL load when only it and `servus_foundation` are installed.

#### Scenario: Manufacturing with Foundation
- **WHEN** a game starts with `servus_foundation` and `servus_manufacturing` and no other Servus mod
- **THEN** both mods load and the game reaches the title screen or the server reaches the "Done" state

#### Scenario: Storage with Foundation
- **WHEN** a game starts with `servus_foundation` and `servus_storage` and no other Servus mod
- **THEN** both mods load and the game reaches the title screen or the server reaches the "Done" state

### Requirement: All three mods load together
`servus_foundation`, `servus_manufacturing` and `servus_storage` SHALL load together in one game instance without conflicts.

#### Scenario: Full family installed
- **WHEN** a game starts with all three Servus mods
- **THEN** all three appear in the mod list and the game reaches the title screen or the server reaches the "Done" state

### Requirement: Content mods require Foundation
Each content mod SHALL declare `servus_foundation` as a required dependency with a version range that includes the Foundation version it was built against.

#### Scenario: Content mod without Foundation
- **WHEN** a game starts with a content mod but without `servus_foundation`
- **THEN** NeoForge refuses to start and reports the missing dependency `servus_foundation` for that mod

### Requirement: Manufacturing and Storage are independent
Manufacturing and Storage SHALL NOT declare a dependency, required or optional, on each other and SHALL NOT reference each other's classes. Both MAY reference any Foundation class, including the Utility Duct.

#### Scenario: Storage without Manufacturing
- **WHEN** a game starts with `servus_foundation` and `servus_storage` only
- **THEN** the game loads without any dependency error

#### Scenario: Manufacturing without Storage
- **WHEN** a game starts with `servus_foundation` and `servus_manufacturing` only
- **THEN** the game loads without any dependency error

## REMOVED Requirements

### Requirement: Lib loads standalone
**Reason**: Servus Lib was merged with Servus: Logistics into Servus: Foundation.
**Migration**: See "Foundation loads standalone".

### Requirement: Each content mod loads with Lib alone
**Reason**: Lib is now Foundation and Logistics is no longer a content mod.
**Migration**: See "Each content mod loads with Foundation alone".

### Requirement: All four mods load together
**Reason**: The family has three mods.
**Migration**: See "All three mods load together".

### Requirement: Content mods require Lib
**Reason**: Lib is now Foundation.
**Migration**: See "Content mods require Foundation".

### Requirement: Content mods are independent of each other
**Reason**: Logistics is no longer a content mod, so only Manufacturing and Storage remain to keep apart.
**Migration**: See "Manufacturing and Storage are independent".
