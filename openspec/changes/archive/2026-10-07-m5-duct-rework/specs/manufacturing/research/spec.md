# Spec Delta

## MODIFIED Requirements

### Requirement: Techs are data
A tech SHALL be a JSON file under `data/<namespace>/servus_manufacturing/tech/` with an icon, parent techs, a pack cost, ticks per pack and a list of unlocked recipe ids. Techs SHALL reach the client without a custom sync.

#### Scenario: Built in techs
- **WHEN** the server starts
- **THEN** the techs electric_extractor, crusher, stone_processing and logistics exist with their costs in red packs (example values, retunable in data), and logistics has crusher as its only parent

#### Scenario: Datapack tech
- **WHEN** a datapack adds a tech file
- **THEN** after a world reload it appears in the tech tree screen
