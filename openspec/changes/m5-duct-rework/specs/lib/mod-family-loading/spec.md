# Spec Delta

## MODIFIED Requirements

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
