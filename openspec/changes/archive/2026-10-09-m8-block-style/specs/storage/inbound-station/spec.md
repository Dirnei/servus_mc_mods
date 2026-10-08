# Spec Delta

## MODIFIED Requirements

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from vanilla materials including planks, recording the wood of the planks, SHALL drop itself with its wood when broken, and SHALL be dismantled by sneaking with the Configuration Carrot, dropping its buffer contents and keeping its side modes and wood in the dropped item. The facing is set at placement; the carrot without sneaking configures sides and does not turn the station.

#### Scenario: Crafting
- **WHEN** the player places iron ingots, a chest, a hopper and planks in the station pattern (for example a chest framed by ingots over a hopper between planks)
- **THEN** a Inbound Station carrying the wood of the planks is produced

#### Scenario: Dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a station holding bread
- **THEN** the station and the bread drop as items
