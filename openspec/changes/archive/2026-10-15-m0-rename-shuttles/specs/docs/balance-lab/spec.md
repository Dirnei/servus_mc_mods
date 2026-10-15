# Spec Delta

## MODIFIED Requirements

### Requirement: Public documentation skeleton
The site SHALL have a landing page, a getting started page, an overview page per mod (Foundation, Alchemy, Shuttles) and a datapack reference describing the JSON formats of machine stats, deposits, techs and Servus recipes. Public pages SHALL work without JavaScript.

#### Scenario: Datapack reference
- **WHEN** a modpack author opens the machine stats reference
- **THEN** it lists every machine stats field with its meaning and an example file

#### Scenario: Mod pages
- **WHEN** a reader opens the mods section
- **THEN** it lists Servus: Foundation, Servus: Alchemy and Servus: Shuttles, and the Foundation page covers the Utility Duct, the Fluid Tank, facades, goggles and the Configuration Carrot
