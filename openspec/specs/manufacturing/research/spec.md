# manufacturing/research Specification

## Purpose

Research gates progression: a team picks a tech, feeds packs into Research Labs, and the finished tech unlocks recipes. Techs are data, progress is per team and survives restarts.

## Requirements

### Requirement: Techs are data
A tech SHALL be a JSON file under `data/<namespace>/servus_manufacturing/tech/` with an icon, parent techs, a pack cost, ticks per pack and a list of unlocked recipe ids. Techs SHALL reach the client without a custom sync.

#### Scenario: Built in techs
- **WHEN** the server starts
- **THEN** the techs electric_extractor, crusher, stone_processing and logistics exist with their costs in red packs (example values, retunable in data), and logistics has crusher as its only parent

#### Scenario: Datapack tech
- **WHEN** a datapack adds a tech file
- **THEN** after a world reload it appears in the tech tree screen

### Requirement: Availability through parents
A tech SHALL be available only when every parent is completed and it is not completed itself. Starting an unavailable tech SHALL be refused.

#### Scenario: Root tech
- **WHEN** a tech has no parents and is not completed
- **THEN** it is available

#### Scenario: Locked child
- **WHEN** a tech's parent is not completed
- **THEN** the tech is locked and a request to start it is ignored

### Requirement: Team progress persists and syncs
Completed techs, the current research and consumed packs SHALL be stored per team in world data, survive a restart, and be sent to every online player of the team whenever they change and on login.

#### Scenario: Restart
- **WHEN** a team completed a tech and the server restarts
- **THEN** the tech is still completed for that team

#### Scenario: Two players, one team
- **WHEN** one player of a team starts a research
- **THEN** the other online player of the team sees it as current without reopening anything

### Requirement: Research Lab consumes packs for the current research
A Research Lab SHALL belong to the team of the player who placed it. While that team has a current research, the lab has energy, and a lab slot holds a pack the research still needs, the lab SHALL consume one such pack per `ticks_per_pack` of the tech, spending its energy per tick meanwhile. When the last required pack is consumed the tech SHALL be completed.

#### Scenario: Ten packs
- **WHEN** a team researches a tech costing 10 red packs at 100 ticks per pack, and a powered lab holds 10 red packs
- **THEN** after 1000 ticks the tech is completed and the packs are gone

#### Scenario: Two labs
- **WHEN** two powered labs of the same team each hold packs
- **THEN** the research completes in about half the time

#### Scenario: Nothing to research
- **WHEN** the team has no current research
- **THEN** the lab idles, keeps its packs and spends no energy

#### Scenario: Switching research
- **WHEN** a team switches to another tech and later back
- **THEN** the packs consumed for the first tech still count

### Requirement: Tech tree screen
The lab GUI SHALL open a tech tree screen listing every tech with name, cost, and status (locked, available, researching with progress, completed). Clicking an available tech SHALL make it the team's current research.

#### Scenario: Start research
- **WHEN** the player clicks an available tech in the screen
- **THEN** the tech becomes current and the screen shows it as researching

### Requirement: Lab accepts only pack items
The lab SHALL accept into its slots only items that appear in the cost of some tech.

#### Scenario: Cobblestone
- **WHEN** a hopper offers cobblestone to a lab
- **THEN** it is rejected
