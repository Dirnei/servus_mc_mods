# Spec Delta

## ADDED Requirements

### Requirement: Station online lamp
The Crafting Station SHALL show an online lamp on its front, lit while a formed Crafting CPU or an online warehouse is on its data line and dark otherwise. The lamp SHALL be refreshed about once a second, not by scanning blocks every tick.

#### Scenario: On a line with a CPU
- **WHEN** a Crafting Station touches a data duct whose line has a formed CPU
- **THEN** its lamp is lit within about a second

#### Scenario: Alone
- **WHEN** a Crafting Station touches no data duct
- **THEN** its lamp is dark

### Requirement: Station shows crafting
While the Crafting Station holds a job and crafts, its front SHALL show an animated working face; otherwise the front is still.

#### Scenario: Crafting
- **WHEN** the station crafts a pattern job
- **THEN** its front animates until the job ends
