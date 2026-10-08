# Spec Delta

## ADDED Requirements

### Requirement: Opening the adapter GUI
Right clicking a Machine Adapter SHALL open its GUI whatever the player holds, the Configuration Carrot included. A sneaking player holding an item SHALL use that item on the adapter instead, so blocks can be placed against it and the carrot dismantles it.

#### Scenario: Holding a block
- **WHEN** a player holding cobblestone right clicks a Machine Adapter without sneaking
- **THEN** the adapter GUI opens and no cobblestone is placed

#### Scenario: Empty hand
- **WHEN** a player with an empty hand right clicks a Machine Adapter
- **THEN** the adapter GUI opens

#### Scenario: Sneaking with a block
- **WHEN** a sneaking player holding cobblestone right clicks the top of a Machine Adapter
- **THEN** the cobblestone is placed on top and no GUI opens

#### Scenario: Sneaking with the carrot
- **WHEN** a sneaking player right clicks a Machine Adapter with the Configuration Carrot
- **THEN** the adapter is dismantled and drops as an item
