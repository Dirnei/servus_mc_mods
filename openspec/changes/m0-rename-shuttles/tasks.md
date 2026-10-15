# Tasks

## 1. Project and code

- [x] 1.1 Check `openspec list` shows no other open change. `git mv storage shuttles`; move the ignored `storage/runs` to `shuttles/runs` and delete what is left of `storage/`. Update `settings.gradle`, `shuttles/gradle.properties` (`servus_shuttles`, "Servus: Shuttles", new description), root `build.gradle` (`allServusMods`, comments) and `alchemy/build.gradle` (data run project lists, `mustRunAfter`). Verify: `gradlew projects` lists `:shuttles` and no `:storage`.
- [x] 1.2 Rename the packages in `shuttles/src/main/java` and `shuttles/src/test/java` from `servus.storage` to `servus.shuttles`. Rename `ServusStorage*` classes to `ServusShuttles*`, `stats/StorageStatsManager` to `ShuttlesStatsManager` (with `DIRECTORY = "shuttles"`), `gametest/StorageGameTests` to `ShuttlesGameTests`; leave `StorageTerminal*`, `StorageNode*` and `PatternStorage*` as they are. Set `ServusShuttlesIds.MOD_ID = "servus_shuttles"` and replace the `servus_storage` literals in `ShuttlesStatsManager`, `ProductionLedger`, `SiteRegistry`, `ClientTerminalSettings`, `HeavyRackModels`, `ShuttleModels`, `ShuttleItemModel`, `FieldGameTests` and `SpreaderGameTests` with `ServusShuttlesIds.MOD_ID` or the new name. Update the namespaces in Alchemy's `CraftingRecipeReaderTest` and `GameDataJsonTest`. Verify: `gradlew build` and `gradlew test` green.

## 2. Resources and datagen

- [ ] 2.1 Move `shuttles/src/main/resources/assets/servus_storage` and `data/servus_storage` to `servus_shuttles`, and the stats folder `data/servus_shuttles/storage` to `data/servus_shuttles/shuttles`. Replace every `servus_storage` string in hand written JSON and Java (lang keys, tags, texture and model paths), and "Servus: Storage" in English lang text. Verify: `gradlew build`, and a grep for `servus_storage`, `servus.storage`, `ServusStorage` and `Servus: Storage` under `shuttles/src/main` and `shuttles/src/test` finds nothing.
- [ ] 2.2 Delete `shuttles/src/generated/resources`, run `:foundation:runData`, `:shuttles:runData` and `:alchemy:runData`. Verify: the generated assets, recipes, loot tables and tags exist under `servus_shuttles` only, `docs/src/data/gamedata.json` has no `servus_storage`, and `gradlew build` is green.
- [ ] 2.3 Run `:shuttles:runGameTestServer`, `:foundation:runGameTestServer` and `:alchemy:runGameTestServer`. Verify: all three green (the field, spreader, CPU and shuttle tests load the stats from `shuttles/`).

## 3. Specs and planning

- [ ] 3.1 `git mv openspec/specs/storage openspec/specs/shuttles`. Edit each moved spec's title line to `# shuttles/<cap> Specification` and the Purpose texts naming the mod (`crafting-station`, `data-carriers`). Verify: `openspec validate m0-rename-shuttles --strict` is valid with no "target spec does not exist" notices, and `openspec validate --specs` is green.
- [ ] 3.2 Update PLAN.md (mod table, dependency text, repository layout and package list, the Storage sections and open points where they name the mod), CLAUDE.md (stack, projects, mod ids, dependency rule, capability path example), `openspec/config.yaml` context, README (table, run commands, data paths) and PROGRESSION.md. "Storage" stays where it means storing items. No line says the old name is kept for later. No em dashes. Verify: a grep for `servus_storage`, `:storage` and `Servus: Storage` in these files finds nothing, and `openspec validate --all` is green.

## 4. Docs, CI and tools

- [ ] 4.1 Rename `docs/src/content/docs/mods/storage.md` to `mods/shuttles.md` (title "Servus: Shuttles", datapack paths `data/<namespace>/shuttles/`), update the sidebar slug in `docs/astro.config.mjs` and the links and mentions in `index.mdx`, `getting-started.md`, `foundation.md` and `alchemy.md`. In code, `MODS` and `ServusMod` in `docs/src/lib/recipegraph.ts`, `MOD_NAMES` in `RecipeGraph.tsx`, `store.ts`, `recipegraph.test.ts` and `fixtures/tier1-gamedata.json`. `git mv docs/src/icons/servus_storage docs/src/icons/servus_shuttles`. Verify: `npm test` and `npm run build` in `docs/` green.
- [ ] 4.2 Point `.github/workflows/build.yml` at `shuttles/build/libs/*.jar` and `server/pack.ps1` at the `shuttles` project. Verify: read both files, run `gradlew build` and check `shuttles/build/libs` holds `servus_shuttles-*.jar`.

## 5. Integration

- [ ] 5.1 Grep the repo (excluding `openspec/changes/archive`, this change's directory, `.git`, `.claude`, build output, `runs`, `node_modules`, `docs/dist`, `server/data`, `server/dist`, `server/mods`) for `servus_storage`, `servus.storage`, `ServusStorage`, `:storage` and `Servus: Storage`: no hits apart from the requirements in `openspec/specs/` that this change's deltas replace at archive. Full `gradlew build` and `gradlew test` green, the three GameTest runs green, and `runClientAll` reaches the title screen with "Servus: Foundation", "Servus: Alchemy" and "Servus: Shuttles" in the mod list. Tell the user that dev worlds lost the old namespace blocks and items. Then the user play checks from main: a shuttle fetches an item into the Storage Terminal, JEI shows the Shuttles items, the Field Head's fertilizer picker lists bone meal.
