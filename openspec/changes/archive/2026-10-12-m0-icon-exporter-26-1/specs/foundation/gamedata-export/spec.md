## MODIFIED Requirements

### Requirement: Item icons are rendered by a dev run
A dev client run with all Servus mods SHALL render every item listed in `gamedata.json` as a 64x64 PNG with a transparent background, the way the inventory draws it, and quit when done. Block items and items with composed models SHALL be drawn as their 3D GUI model with the GUI lighting; parts of a model that reach past the slot square MAY be cut. Icons of `servus_*` items SHALL be written to `docs/src/icons/<namespace>/<path>.png` and committed. Icons of other namespaces SHALL be written to the same tree but ignored by git. Without the run's system property the game SHALL NOT export icons.

#### Scenario: Export run
- **WHEN** `gradlew iconExport` finishes
- **THEN** `docs/src/icons/servus_storage/shuttle.png` exists, `docs/src/icons/minecraft/redstone.png` exists, and `git status` lists no vanilla icon

#### Scenario: Block items and composed models
- **WHEN** `gradlew iconExport` finishes
- **THEN** `docs/src/icons/servus_storage/crate.png` shows the crate block in the inventory's three quarter view and `docs/src/icons/servus_storage/shuttle.png` shows the composed shuttle model, each 64x64 with transparent corners and visible pixels

#### Scenario: Normal client
- **WHEN** the client is started with the normal run config
- **THEN** no icons are written and the title screen stays open
