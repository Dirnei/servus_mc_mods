## MODIFIED Requirements

### Requirement: Foundation loads standalone
`servus_foundation` SHALL load in a Minecraft 26.1.2 game on NeoForge 26.1.2.x with no other Servus mod present and without errors or warnings about missing dependencies.

#### Scenario: Only Foundation installed
- **WHEN** a client or dedicated server on Minecraft 26.1.2 with NeoForge 26.1.2.x starts with only `servus_foundation` among the Servus mods
- **THEN** the game reaches the title screen or the server reaches the "Done" state
- **AND** the mod list contains `servus_foundation` with the display name "Servus: Foundation"

## ADDED Requirements

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
