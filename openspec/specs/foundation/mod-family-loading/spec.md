# foundation/mod-family-loading Specification

## Purpose

Defines how the three Servus mods load in NeoForge 1.21.1 and which dependency relationships between them are allowed, so that any combination of Foundation plus content mods works.

## Requirements

### Requirement: Lib loads standalone
`servus_lib` SHALL load in a NeoForge 21.1.x game with no other Servus mod present and without errors or warnings about missing dependencies.

#### Scenario: Only Lib installed
- **WHEN** a client or dedicated server starts with only `servus_lib` among the Servus mods
- **THEN** the game reaches the title screen or the server reaches the "Done" state
- **AND** the mod list contains `servus_lib` with the display name "Servus Lib"

### Requirement: Each content mod loads with Lib alone
Each content mod (`servus_manufacturing`, `servus_logistics`, `servus_storage`) SHALL load when only it and `servus_lib` are installed.

#### Scenario: Manufacturing with Lib
- **WHEN** a game starts with `servus_lib` and `servus_manufacturing` and no other Servus mod
- **THEN** both mods load and the game reaches the title screen or the server reaches the "Done" state

#### Scenario: Logistics with Lib
- **WHEN** a game starts with `servus_lib` and `servus_logistics` and no other Servus mod
- **THEN** both mods load and the game reaches the title screen or the server reaches the "Done" state

#### Scenario: Storage with Lib
- **WHEN** a game starts with `servus_lib` and `servus_storage` and no other Servus mod
- **THEN** both mods load and the game reaches the title screen or the server reaches the "Done" state

### Requirement: All four mods load together
`servus_lib`, `servus_manufacturing`, `servus_logistics` and `servus_storage` SHALL load together in one game instance without conflicts.

#### Scenario: Full family installed
- **WHEN** a game starts with all four Servus mods
- **THEN** all four appear in the mod list and the game reaches the title screen or the server reaches the "Done" state

### Requirement: Content mods require Lib
Each content mod SHALL declare `servus_lib` as a required dependency with a version range that includes the Lib version it was built against.

#### Scenario: Content mod without Lib
- **WHEN** a game starts with a content mod but without `servus_lib`
- **THEN** NeoForge refuses to start and reports the missing dependency `servus_lib` for that mod

### Requirement: Content mods are independent of each other
Storage SHALL NOT declare a dependency, required or optional, on another content mod, and SHALL NOT reference another content mod's classes. Manufacturing SHALL declare `servus_logistics` as a required dependency, because its ducts are the Utility Duct, and MAY reference Logistics classes. Logistics SHALL depend on Lib only.

#### Scenario: Two content mods without the third
- **WHEN** a game starts with `servus_lib`, `servus_logistics` and `servus_storage` but not `servus_manufacturing`
- **THEN** the game loads without any dependency error

#### Scenario: Storage without the others
- **WHEN** a game starts with `servus_lib` and `servus_storage` only
- **THEN** the game loads without any dependency error

#### Scenario: Logistics without Manufacturing
- **WHEN** a game starts with `servus_lib` and `servus_logistics` only
- **THEN** the game loads without any dependency error

#### Scenario: Manufacturing without Logistics
- **WHEN** a game starts with `servus_lib` and `servus_manufacturing` but not `servus_logistics`
- **THEN** NeoForge refuses to start and reports the missing dependency `servus_logistics` for Manufacturing

### Requirement: Mod identity
Each mod SHALL carry its mod id, display name, version, MIT license and the NeoForge and Minecraft version ranges it supports in its mod metadata.

#### Scenario: Mod list entry
- **WHEN** the player opens the Mods screen on the client
- **THEN** each installed Servus mod shows its display name, version and license "MIT"
