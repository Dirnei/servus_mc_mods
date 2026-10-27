# Proposal: m0-remove-gamedata-export

## Why

The docs site was rewritten to describe released mod versions only. The Balance lab is gone, and the docs workflow no longer runs `:alchemy:runData` or reads `docs/src/data/gamedata.json`. The game data export and the icon renderer that fed the lab now have no users, but they still run on every Alchemy data run, keep test fixtures alive and ship a dev only client hook in the Alchemy jar.

Milestone M0 (housekeeping). PLAN.md has no open point for it; the docs rewrite made it dead code.

## What Changes

- Remove the game data export from Alchemy datagen: `GameDataExportProvider`, `GameDataJson`, `CraftingRecipeReader` and their JUnit tests.
- Move the one helper other code still uses, the id path to title case conversion, to a small pure Alchemy class with its own JUnit test. Lang, models, recipes and every other datagen output stay byte for byte the same.
- Remove the `servus.gamedata.*` properties and the run order constraint that only the export needed from `alchemy/build.gradle`.
- Remove the icon renderer: the `iconExport` run and task in the root `build.gradle`, and `IconExporter`, `IconExportRenderer`, `IconExportRenderState` with their hooks in `ServusAlchemyClient`.
- Remove the `src/data/` and `src/icons/` entries from `docs/.gitignore`.
- Retire the `docs/balance-lab` and `docs/recipe-graph` specs and describe the rewritten site in a new `docs/site` spec: published to GitHub Pages, released mods only, mod info box, developer guide.
- Replace the CLAUDE.md verification bullet about the export with one about checking the docs site against release tags.
- **BREAKING (dev tooling only):** `gradlew iconExport` and the `docs/src/data/gamedata.json` output of `runData` no longer exist. No player facing change.

## Capabilities

### New Capabilities

- `docs/site`: the docs site as rewritten, published to GitHub Pages, released mods only, with a mod info box and a developer guide.

### Modified Capabilities

- `foundation/gamedata-export`: every requirement is removed, so the capability goes away on archive.
- `docs/balance-lab`, `docs/recipe-graph`: every requirement is removed; the two still true site requirements move to `docs/site`.

## Impact

- Code: `alchemy/src/main/java/.../alchemy/datagen/gamedata/`, `alchemy/src/test/java/.../alchemy/datagen/gamedata/`, `alchemy/.../client/IconExport*.java`, `ServusAlchemyClient`, `ServusAlchemyDataGen`, `AlchemyRecipeTable`, `hollow/VeinSeedItem`.
- Build: `build.gradle` (root), `alchemy/build.gradle`.
- Docs and workflow: `docs/.gitignore`, `CLAUDE.md`. The docs workflow already stopped using the export.
- No datapack, save or network format change.
