# Design

## Context

Three Gradle projects: `foundation`, `alchemy`, `storage`. The root `build.gradle` lists them in `allServusMods` for the combined `clientAll`, `serverAll` and `gameTestServerAll` runs. `alchemy/build.gradle` wires the data run that writes `docs/src/data/gamedata.json`; its system properties `servus.gamedata.resources` and `servus.gamedata.generated` name the project directories, and it runs after `:storage:runData`. The mod id comes from `storage/gradle.properties` through the `neoforge.mods.toml` template in the root build.

The mod id lives in `ServusStorageIds.MOD_ID`, and a few literals repeat it: the stats reload listener id in `StorageStatsManager`, the saved data names `servus_storage_production` (`ProductionLedger`) and `servus_storage_sites` (`SiteRegistry`), the client settings file `servus_storage-terminal.properties` (`ClientTerminalSettings`), model ids in `HeavyRackModels`, `ShuttleModels` and `ShuttleItemModel`, and two GameTests (`FieldGameTests`, `SpreaderGameTests`). The stats folder is `StorageStatsManager.DIRECTORY = "storage"`.

`m0-rename-alchemy` did the same rename for Manufacturing, and this design follows it.

## Goals / Non-Goals

**Goals:**
- One mod `servus_shuttles` with the same content and behavior, all tests green under the new ids.
- After the rename, nothing outside archived changes names the mod Storage or uses `servus_storage`.

**Non-Goals:**
- Renaming blocks, items or texts that use "storage" as a word: Storage Terminal (`storage_terminal`), Pattern Storage (`pattern_storage`), the storage network classes (`StorageNode*`), and phrases like "goes back to storage".
- Registry aliases or other compatibility for old ids.

## Decisions

### Name
- "Servus: Shuttles", plural, mod id `servus_shuttles`. The shuttles run the warehouse, the fields and the outposts, so the name covers the whole mod. "Servus: Warehouse" was the alternative: clear at once, but field farming does not fit under it. "Logistics" is out, it was the old name of Foundation.

### Gradle
- `git mv storage shuttles`. `settings.gradle` includes `foundation`, `alchemy`, `shuttles`. `shuttles/gradle.properties`: `mod_id=servus_shuttles`, `mod_name=Servus: Shuttles`, `mod_description=An automated warehouse run by shuttles on rails: storage, autocrafting, field farming and outposts.`
- Root `build.gradle`: `allServusMods` lists `shuttles`. `alchemy/build.gradle`: the data run lists and `mustRunAfter ':shuttles:runData'`.
- The ignored `storage/runs` directory (eula, RCON settings, dev worlds) is moved to `shuttles/runs` by hand after the `git mv`. Leftover `storage/build` output is deleted.

### Packages and class names
- `io.github.dirnei.mc.servus.storage.*` becomes `io.github.dirnei.mc.servus.shuttles.*`, main and test. The package `shuttles.shuttle` (the shuttle entity and modules) keeps its name; `shuttles.shuttle` reads fine and avoids touching a second level.
- Classes named after the mod: `ServusStorage*` becomes `ServusShuttles*` (entry point, ids, events, client, commands, payloads, shipping, registries, datagen providers, JEI plugin, `ServusStorageIdsTest` if present). `StorageStatsManager` becomes `ShuttlesStatsManager`, `StorageGameTests` becomes `ShuttlesGameTests`.
- Classes named after a block or the storage network keep their names: `StorageTerminal*`, `StorageTerminalRowsGameTests`, `StorageNode*`, `PatternStorage*`.
- The literals listed in Context use `ServusShuttlesIds.MOD_ID` where they are ids (listener id, saved data names, model ids, GameTest ids), so the id has one source. The stats reload listener becomes `servus_shuttles:shuttles_stats_manager`. Saved data becomes `servus_shuttles_production` and `servus_shuttles_sites`, the client file `servus_shuttles-terminal.properties`, its header comment "Servus: Shuttles Storage Terminal view settings".
- Nothing new from NeoForge is used. `@Mod`, `@EventBusSubscriber(modid = ...)`, GameTest holders, payload registrars, `DeferredRegister` namespaces, the JEI plugin uid and the Jade uids read `ServusShuttlesIds.MOD_ID`.

### Stats folder
- `ShuttlesStatsManager.DIRECTORY` becomes `"shuttles"`: `data/servus_shuttles/shuttles/{cpu,crafting_station,field,packing_station,pattern_storage,power,rack,shuttle,warehouse}.json`. The folder holds the stats of the whole mod, including fields and power, so the old name was wrong for the same reason the mod name was. Keeping `storage/` was the alternative: fewer spec deltas, but it would leave the one place modpack authors read most with the old name, and after the release it cannot change without breaking their datapacks.

### Resources and datagen
- `shuttles/src/main/resources/{assets,data}/servus_storage` move to `servus_shuttles`, the stats folder inside `data` to `shuttles/`, the GameTest structures with it.
- Every `servus_storage` string in JSON, lang keys, model and texture references and the Java datagen becomes `servus_shuttles`. English lang text naming the mod says "Servus: Shuttles". Item and block names do not change.
- `shuttles/src/generated/resources` is deleted and regenerated by `:shuttles:runData`; then `:alchemy:runData` rewrites `gamedata.json`.

### Specs and planning files
- `git mv openspec/specs/storage openspec/specs/shuttles`. Each moved spec's title line becomes `# shuttles/<cap> Specification`; Purpose texts naming the mod (`crafting-station`: "Storage's own producer", `data-carriers`: "Lets Storage devices") are edited directly. Requirement text changes only through this change's deltas, which target `shuttles/*`, so the move happens before archive.
- Requirements whose name or scenario names say Storage are REMOVED and ADDED under new names, because a MODIFIED delta keeps the requirement and scenario names: two in `mod-family-loading`, one each in `recipe-graph` and `gamedata-export`. Every other delta is MODIFIED.
- PLAN.md, CLAUDE.md (stack, projects, mod ids, dependency rule, capability path example `storage/crates`), `openspec/config.yaml` context, README and PROGRESSION.md say Shuttles. Where "Storage" names the mod it becomes "Shuttles"; where it names storing items it stays.

### Docs, CI and tools
- `docs/src/content/docs/mods/storage.md` becomes `mods/shuttles.md` (title "Servus: Shuttles"), with the sidebar slug in `docs/astro.config.mjs` and the links in `index.mdx`, `getting-started.md`, `foundation.md` and `alchemy.md`. Datapack paths on the page say `data/<namespace>/shuttles/`.
- Lab code: `MODS` and the `ServusMod` type in `docs/src/lib/recipegraph.ts`, `MOD_NAMES` in `RecipeGraph.tsx`, the store in `store.ts`, `recipegraph.test.ts` and the fixture `tier1-gamedata.json`.
- `git mv docs/src/icons/servus_storage docs/src/icons/servus_shuttles`; `gradlew iconExport` afterwards must leave `git status` clean in that folder.
- `.github/workflows/build.yml` collects `shuttles/build/libs/*.jar`; `server/pack.ps1` builds `shuttles`.

### Verification
- No new logic, so no new JUnit tests. The existing JUnit tests (all projects, including the Alchemy gamedata tests with their fixtures changed) and the docs tests cover the pure parts.
- GameTests: `:shuttles:runGameTestServer`, `:foundation:runGameTestServer`, `:alchemy:runGameTestServer` green. The field, spreader, CPU and shuttle tests load the stats from the new folder.
- Manual: `runClientAll` reaches the title screen with "Servus: Shuttles" in the mod list; in a new world a warehouse with rack, rail, garage and shuttle fetches an item into the Storage Terminal, JEI shows the Shuttles items, and the Field Head's fertilizer picker lists bone meal (tag `servus_shuttles:fertilizer`).
- A grep over the repo (excluding `openspec/changes/archive`, this change's own directory, `.git`, `.claude`, build output, `runs`, `node_modules`, `docs/dist`, `server/data`, `server/dist`, `server/mods`) for `servus_storage`, `servus.storage`, `ServusStorage`, `:storage` and `Servus: Storage` finds nothing apart from the requirements in `openspec/specs/` that this change's deltas replace.

### PLAN.md open decisions
None settled. This is a rename only.

## Risks / Trade-offs

- [A missed id string only fails at runtime (lang key, tag, model id, stats folder)] -> the closing grep must come back empty, and the manual check runs a shuttle, opens the terminal and the fertilizer picker.
- [Generic "storage" is everywhere, so a blind replace would rename the Storage Terminal or Pattern Storage] -> replace only the patterns `servus_storage`, `servus.storage`, `ServusStorage`, `StorageStatsManager`, `StorageGameTests`, `:storage`, `Servus: Storage` and the stats folder; read every remaining "Storage" that names the mod by hand.
- [Dev worlds lose every Shuttles block and item] -> accepted, nothing is released; the user is told before the play check.
- [`git mv` does not move ignored `runs/`] -> moved by hand as described above.
- [Open changes touch `storage/`] -> applied only when no other change is open (proposal, Impact).
- [Helper scripts and the user's server setup name `servus_storage` jars] -> `server/pack.ps1` is updated here; `server/mods` is refilled by the next `pack.ps1` run.
