## REMOVED Requirements

### Requirement: Data run writes the game data file
**Reason**: The docs site no longer reads `docs/src/data/gamedata.json`; nothing uses the export.
**Migration**: None. Datapack values are documented on the docs site's reference pages.

### Requirement: Machine stats are effective values
**Reason**: Part of the removed game data export.
**Migration**: None.

### Requirement: Recipes are exported with effective times
**Reason**: Part of the removed game data export.
**Migration**: None.

### Requirement: Output is stable
**Reason**: Part of the removed game data export.
**Migration**: None.

### Requirement: CI rejects a stale export
**Reason**: The docs workflow no longer runs the export, so there is nothing to keep fresh.
**Migration**: None.

### Requirement: Crafting recipes of all Servus mods are exported
**Reason**: Part of the removed game data export.
**Migration**: None.

### Requirement: Shaped recipes keep their grid
**Reason**: Part of the removed game data export.
**Migration**: None.

### Requirement: Special recipes are marked
**Reason**: Part of the removed game data export.
**Migration**: None.

### Requirement: Item icons are rendered by a dev run
**Reason**: The docs site no longer shows rendered item icons; the `iconExport` run goes with the export.
**Migration**: None.

### Requirement: Items, deposits and techs are exported
**Reason**: Part of the removed game data export.
**Migration**: None.
