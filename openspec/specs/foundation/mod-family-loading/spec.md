# foundation/mod-family-loading Specification

## Purpose

Defines how the Servus mods load in NeoForge 26.1.2 and which dependency relationships between them are allowed, so that any combination of Foundation plus content mods works.

## Requirements

### Requirement: Mod identity
Each mod SHALL carry its mod id, display name, version, MIT license and the NeoForge and Minecraft version ranges it supports in its mod metadata.

#### Scenario: Mod list entry
- **WHEN** the player opens the Mods screen on the client
- **THEN** each installed Servus mod shows its display name, version and license "MIT"

### Requirement: Foundation loads standalone
`servus_foundation` SHALL load in a Minecraft 26.1.2 game on NeoForge 26.1.2.x with no other Servus mod present and without errors or warnings about missing dependencies.

#### Scenario: Only Foundation installed
- **WHEN** a client or dedicated server on Minecraft 26.1.2 with NeoForge 26.1.2.x starts with only `servus_foundation` among the Servus mods
- **THEN** the game reaches the title screen or the server reaches the "Done" state
- **AND** the mod list contains `servus_foundation` with the display name "Servus: Foundation"

### Requirement: All three mods load together
`servus_foundation`, `servus_alchemy` and `servus_shuttles` SHALL load together in one game instance without conflicts.

#### Scenario: Full family installed
- **WHEN** a game starts with all three Servus mods
- **THEN** all three appear in the mod list and the game reaches the title screen or the server reaches the "Done" state

### Requirement: Content mods require Foundation
Each content mod SHALL declare `servus_foundation` as a required dependency with a version range that includes the Foundation version it was built against.

#### Scenario: Content mod without Foundation
- **WHEN** a game starts with a content mod but without `servus_foundation`
- **THEN** NeoForge refuses to start and reports the missing dependency `servus_foundation` for that mod

### Requirement: Standard transfer capabilities
Every Foundation block that holds items, fluids or energy for other blocks (duct endpoints, tanks, pumps, machines, the Sink, the Machine Adapter) SHALL expose them through the NeoForge transfer capabilities of NeoForge 26.1 (item, fluid and energy block capabilities), so pipes and machines of other mods move items, fluids and energy in and out exactly as Servus blocks do. A side that is set to Off for a resource SHALL expose no handler for it.

#### Scenario: Foreign pipe pulls from a Servus tank
- **WHEN** another mod queries the fluid block capability of a Fluid Tank that holds water
- **THEN** it receives a handler that reports the water and lets it extract water up to the stored amount

#### Scenario: Foreign cable charges a Servus machine
- **WHEN** another mod queries the energy block capability of a Foundation machine on a side that accepts power
- **THEN** it receives a handler that accepts energy up to the machine's free capacity

#### Scenario: Closed side
- **WHEN** another mod queries the fluid block capability of a machine side whose Fluids layer is set to Off
- **THEN** it receives no handler

### Requirement: Alchemy and Shuttles each load with Foundation
Each content mod (`servus_alchemy`, `servus_shuttles`) SHALL load when only it and `servus_foundation` are installed.

#### Scenario: Alchemy with Foundation
- **WHEN** a game starts with `servus_foundation` and `servus_alchemy` and no other Servus mod
- **THEN** both mods load and the game reaches the title screen or the server reaches the "Done" state

#### Scenario: Shuttles with Foundation
- **WHEN** a game starts with `servus_foundation` and `servus_shuttles` and no other Servus mod
- **THEN** both mods load and the game reaches the title screen or the server reaches the "Done" state

### Requirement: Alchemy and Shuttles are independent
Alchemy and Shuttles SHALL NOT declare a dependency, required or optional, on each other and SHALL NOT reference each other's classes. Both MAY reference any Foundation class, including the Utility Duct.

#### Scenario: Shuttles without Alchemy
- **WHEN** a game starts with `servus_foundation` and `servus_shuttles` only
- **THEN** the game loads without any dependency error

#### Scenario: Alchemy without Shuttles
- **WHEN** a game starts with `servus_foundation` and `servus_alchemy` only
- **THEN** the game loads without any dependency error
