# Tasks

Paths below are relative to `alchemy/src/main/java/io/github/dirnei/mc/servus/alchemy/` unless they start at the repo root.

## 1. Keep the title case helper

- [x] 1.1 Add `IdText.titleCase` (moved unchanged from `datagen/gamedata/GameDataExportProvider`) and point `datagen/AlchemyRecipeTable` and `hollow/VeinSeedItem` at it. JUnit `alchemy/src/test/.../alchemy/IdTextTest`: one word, several words, empty parts from double underscores, empty path. Run `gradlew :alchemy:test`.

## 2. Remove the game data export

- [x] 2.1 Delete `datagen/gamedata/` (`GameDataExportProvider`, `GameDataJson`, `CraftingRecipeReader`) and `alchemy/src/test/.../alchemy/datagen/gamedata/` (`GameDataJsonTest`, `CraftingRecipeReaderTest`); drop the provider from `datagen/ServusAlchemyDataGen`.
- [x] 2.2 In `alchemy/build.gradle` remove the `servus.gamedata.*` system properties and the `runData` ordering after Foundation and Shuttles; keep `--existing` for Foundation's textures and the Gson test dependency (used by `VeinFilesTest`).
- [x] 2.3 Rerun `gradlew :alchemy:runData` and check that `git status` shows no generated diff. Run `gradlew build`.

## 3. Remove the icon renderer

- [x] 3.1 Delete `client/IconExporter`, `client/IconExportRenderer`, `client/IconExportRenderState` and their two hooks in `client/ServusAlchemyClient`; remove the `iconExport` run and task from the root `build.gradle`.
- [x] 3.2 Remove the `src/data/` and `src/icons/` entries from `docs/.gitignore`. Run `gradlew build`.

## 4. Docs and verification

- [x] 4.1 Replace the CLAUDE.md verification bullet about `docs/src/data/gamedata.json` with the docs site bullet; check PLAN.md for mentions of the export or the Balance lab.
- [x] 4.2 Grep the repo (outside archived changes) for `gamedata`, `GameData`, `iconExport`, `servus.icons` and `docs/src/icons`; nothing may remain except this change.
- [x] 4.3 Delta specs: remove every requirement of `docs/balance-lab` and `docs/recipe-graph`, add `docs/site` for the rewritten site. Run `openspec validate m0-remove-gamedata-export --strict`.
- [x] 4.4 Run `gradlew build :foundation:runGameTestServer :shuttles:runGameTestServer :alchemy:runGameTestServer`.
