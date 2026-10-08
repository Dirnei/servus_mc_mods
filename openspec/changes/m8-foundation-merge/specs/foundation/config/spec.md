# Spec Delta

## MODIFIED Requirements

### Requirement: Common config file exists
Servus: Foundation SHALL register a common config so that `config/servus_foundation-common.toml` is created with documented defaults the first time the game runs with the mod.

#### Scenario: First launch
- **WHEN** the game starts with Servus: Foundation and no config file exists
- **THEN** `servus_foundation-common.toml` is written with every option at its default and a comment describing it
