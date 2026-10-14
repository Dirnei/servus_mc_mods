# Spec Delta

## MODIFIED Requirements

### Requirement: Team provider can be replaced
A mod SHALL be able to install a team provider once at startup, and all Servus mods SHALL use it from then on.

#### Scenario: Custom provider
- **WHEN** a mod installs a provider that maps two players to the same id
- **THEN** both players share journal progress in Alchemy
