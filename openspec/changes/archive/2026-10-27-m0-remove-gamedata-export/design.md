# Design: m0-remove-gamedata-export

## Context

The export ran as the last data provider of `:alchemy:runData` and wrote `docs/src/data/gamedata.json`; the `iconExport` client run read that file and rendered PNGs to `docs/src/icons`. The rewritten docs site uses neither.

## Decisions

- **Title case helper moves, the rest goes.** `GameDataExportProvider.titleCase` is used outside the export by `AlchemyRecipeTable` (journal labels and recipe names) and by `VeinSeedItem.veinName` (the fallback vein name in game). It moves unchanged to `io.github.dirnei.mc.servus.alchemy.IdText`, a pure Java class with a JUnit test, so generated lang and book files do not change.
- **Gson stays a test dependency of Alchemy.** `VeinFilesTest` still parses JSON with it; only the comment that tied it to the game data reader changes.
- **`--existing foundation/src/main/resources` stays on the data run.** Model generation needs Foundation's shared machine textures, independent of the export.
- **The `runData` ordering after Foundation and Shuttles goes.** Only the export read the other mods' generated files.
- **Archived changes keep their history.** Earlier archived changes that mention the export are not edited.

## Risks

- A forgotten caller of a removed class fails the build, so `gradlew build` is the check. Datagen output is checked by rerunning `runData` for Alchemy and confirming `git status` shows no generated diff.
