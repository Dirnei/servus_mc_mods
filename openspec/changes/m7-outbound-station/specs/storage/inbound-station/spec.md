# Spec Delta

## MODIFIED Requirements

### Requirement: Station block and buffer
The Inbound Station SHALL be a block with a horizontal facing and an intake buffer of `inbound_buffer_slots` slots (example 9, from `data/<ns>/storage/warehouse.json`). Every side SHALL be in one of two modes, accept or off, all sides accepting by default. The buffer SHALL be exposed as an item handler on accept sides that accepts inserts and refuses extraction, so hoppers, ducts and machines feed it but never take from it; off sides SHALL expose nothing. The Configuration Carrot used on a face SHALL toggle that face's mode and the GUI SHALL show six side buttons that toggle the same way. Breaking the station SHALL drop the buffer contents.

#### Scenario: Hopper feeds the station
- **WHEN** a hopper above the station holds 20 cobblestone
- **THEN** the cobblestone moves into the station's buffer

#### Scenario: Hopper cannot drain it
- **WHEN** a hopper under the station points into a chest and the buffer holds bread
- **THEN** the bread stays in the buffer

#### Scenario: Side switched off
- **WHEN** the player uses the carrot on the top face and a hopper above offers cobblestone
- **THEN** the cobblestone stays in the hopper until the top is switched on again

#### Scenario: Broken with contents
- **WHEN** the station is broken while the buffer holds 10 bread
- **THEN** 10 bread drop at its position

### Requirement: Station is obtainable and dismantled
The station SHALL be craftable from vanilla materials, SHALL drop itself when broken, and SHALL be dismantled by sneaking with the Configuration Carrot, dropping its buffer contents and keeping its side modes in the dropped item. The facing is set at placement; the carrot without sneaking configures sides and does not turn the station.

#### Scenario: Crafting
- **WHEN** the player places iron ingots, a chest and a hopper in the station pattern (for example a chest over a hopper framed by ingots)
- **THEN** a Inbound Station is produced

#### Scenario: Dismantle
- **WHEN** the player sneaks and uses the Configuration Carrot on a station holding bread
- **THEN** the station and the bread drop as items
