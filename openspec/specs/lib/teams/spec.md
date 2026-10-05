# lib/teams Specification

## Purpose

Progress and shared systems in the Servus family belong to a team. Lib decides which team a player is on so every mod agrees, and team mods can replace the rule.

## Requirements

### Requirement: Every player has a team id
Lib SHALL answer a team id for every server player. Without a team mod the team id SHALL be the player's own UUID.

#### Scenario: Default
- **WHEN** no team provider was installed and a player asks for their team
- **THEN** the team id equals the player's UUID

### Requirement: Team provider can be replaced
A mod SHALL be able to install a team provider once at startup, and all Servus mods SHALL use it from then on.

#### Scenario: Custom provider
- **WHEN** a mod installs a provider that maps two players to the same id
- **THEN** both players share research progress in Manufacturing
