# Design

## Context

Three Gradle projects: `foundation`, `manufacturing`, `storage`. Shared build logic lives in the root `build.gradle` (`allServusMods`, combined `clientAll`, `serverAll`, `gameTestServerAll` runs, the Modonomicon dev dependency added when `:manufacturing` exists). `manufacturing/build.gradle` wires the data run that writes `docs/src/data/gamedata.json` (system properties `servus.gamedata.resources` and `servus.gamedata.generated` list project directories). GameTests per project run with `neoforge.enabledGameTestNamespaces = mod_id`.

The mod id appears as `ServusManufacturingIds.MOD_ID` and also as the literal `"servus_manufacturing"` in a few places: reload listener ids (`AthanorTierData`, `VeinData`, `JournalChapters`), the journal root (`Journal.ROOT`) and the command literal (`JournalCommand`). The package `servus.manufacturing.alchemy` holds the shared alchemy classes (`AlchemyMachines`, `Principle*`, recipes, Calciner, Coagulator) and the sub packages `athanor`, `calciner`, `coagulator`, `hollow`, `purifier`, `sight`, `structure`. Next to it are `client`, `datagen`, `gametest`, `integration`, `journal`, `machine`, `registry`.

`m8-foundation-merge` did the same kind of rename for Lib and Logistics, and this design follows it. The open changes `m10-tool-mounts` (Storage) and `m9-duct-resting-pose` (Foundation) do not mention Manufacturing.

## Goals / Non-Goals

**Goals:**
- One mod `servus_alchemy` with the same content and behavior, all tests green under the new ids.
- After the rename, nothing outside archived changes says Manufacturing.

**Non-Goals:**
- Changing or rewording stale Factorio era content beyond the requirements and lines this rename touches anyway.
- Registry aliases or other compatibility for old ids.

## Decisions

### Gradle
- `git mv manufacturing alchemy`. `settings.gradle` includes `foundation`, `alchemy`, `storage`. `alchemy/gradle.properties`: `mod_id=servus_alchemy`, `mod_name=Servus: Alchemy`, description unchanged apart from the name.
- Root `build.gradle`: `allServusMods` lists `alchemy`; the Modonomicon block checks `findProject(':alchemy')`; comments say Alchemy. `alchemy/build.gradle`: the data run properties list `alchemy` instead of `manufacturing`. The comments in `foundation/build.gradle` and `storage/build.gradle` about JEI coming from Manufacturing say Alchemy.
- The ignored `manufacturing/runs` directory (server `eula.txt`, RCON `server.properties`, dev worlds) is moved to `alchemy/runs` by hand after the `git mv`, so the dev server and GameTest setups keep working. Leftover `manufacturing/build` output is deleted.
- The untracked `lib/` and `logistics/` directories hold only build output (`build/`, old jars, about 200 MB) from before the foundation merge. Nothing tracked references them, so they are deleted with `rm -rf`.

### Packages and class names
- `io.github.dirnei.mc.servus.manufacturing.*` becomes `io.github.dirnei.mc.servus.alchemy.*`.
- `...manufacturing.alchemy.*` becomes `...alchemy.*`: the shared classes sit in the root package next to `ServusAlchemy`, and the sub packages move up one level (`alchemy.athanor`, `alchemy.calciner`, `alchemy.coagulator`, `alchemy.hollow`, `alchemy.purifier`, `alchemy.sight`, `alchemy.structure`). No name clashes: the sub package names differ from `client`, `datagen`, `gametest`, `integration`, `journal`, `machine`, `registry`, and `client.sight` stays under `client`. The tests in `src/test/.../manufacturing/alchemy` move the same way.
- Classes named `ServusManufacturing*` become `ServusAlchemy*` (`ServusAlchemy`, `ServusAlchemyIds`, `ServusAlchemyEvents`, `ServusAlchemyClient`, the datagen providers, the registries, `ServusAlchemyJeiPlugin`, `ServusAlchemyJadePlugin`, `ServusAlchemyIdsTest`).
- `machine/ManufacturingMachines` becomes `machine/AlchemyMachineDefaults` (test `AlchemyMachineDefaultsTest`), because `AlchemyMachines` already names the shared tank data helper, and this class registers the `MachineDefaults` entries.
- The id literals `"servus_manufacturing"` in `AthanorTierData`, `VeinData`, `JournalChapters`, `Journal` and `JournalCommand` become `ServusAlchemyIds.MOD_ID`, so the id has one source. This also renames the command to `/servus_alchemy`.
- `@Mod`, `@EventBusSubscriber(modid = ...)`, GameTest holders, payload registrars, `DeferredRegister` namespaces, the JEI plugin uid and the Jade uids all read `ServusAlchemyIds.MOD_ID` already or after the rename. Nothing new from NeoForge is used. The mod id comes from `alchemy/gradle.properties` through the `neoforge.mods.toml` template expansion in the root build.

### Resources and datagen
- `alchemy/src/main/resources/{assets,data}/servus_manufacturing` move to `servus_alchemy`. That includes the machine stats (`machine/`), the GameTest structures (`structure/`) and the nested datapack folders `servus_manufacturing/{journal,vein,athanor_tier}`, which become `data/servus_alchemy/servus_alchemy/...`.
- Every `servus_manufacturing` string in JSON, lang keys (`block.servus_manufacturing.*`, `item.*`, `fluid_type.*`, `effect.*`, `gui.*`, `book.*`), model and texture references and Modonomicon book and research data becomes `servus_alchemy`. English lang text that names the mod ("Servus: Manufacturing", creative tab) says Alchemy.
- `alchemy/src/generated/resources` is deleted and regenerated by `:alchemy:runData`. The data run also rewrites `docs/src/data/gamedata.json`. `:foundation:runData` and `:storage:runData` run first, as today.

### Specs and planning files
- `git mv openspec/specs/manufacturing openspec/specs/alchemy`. In each moved spec, the title line (`# manufacturing/<cap> Specification`) and Purpose texts that name Manufacturing are edited directly. Requirement text changes only through this change's deltas. The deltas target `alchemy/*`, so the move has to happen before archive. Until then, `openspec validate` prints "target spec does not exist" notices for the alchemy deltas. The `storage/crafting-station` Purpose ("a warehouse without Manufacturing") is edited directly too.
- Where a requirement's scenario names the mod, a MODIFIED delta would have to rename the scenario, which OpenSpec refuses. Those requirements are REMOVED and ADDED under a new name (`mod-family-loading` twice, `recipe-graph` once).
- PLAN.md: the mod table, the dependency text, the repository layout and package list, section 2.1 "Servus: Alchemy", 2.2 and 2.4 ("Integration: Storage + Alchemy"), "Open: Alchemy", the upgrade items line. The Done table row "M2, M3 Factorio style Manufacturing" is reworded to "Factorio style factory", which keeps the history without naming the mod. Nothing says the old name is kept for later.
- CLAUDE.md (stack, projects, mod ids, dependency rule), `openspec/config.yaml` context, README (table, run commands, data run, data paths) and PROGRESSION.md say Alchemy.

### Docs and CI
- `docs/src/content/docs/mods/manufacturing.md` becomes `mods/alchemy.md` (title "Servus: Alchemy"), and the sidebar slug in `docs/astro.config.mjs` follows. Links `/servus_mc_mods/mods/manufacturing/` in `index.mdx`, `foundation.md` and `storage.md` point to `/mods/alchemy/`. The getting started table, the lab pages and the datapack reference pages use the new name, ids and folders.
- `RecipeGraph.tsx` `MOD_NAMES` key `alchemy: 'Alchemy'`; `docs/src/lib/recipegraph.ts` derives the mod from the id namespace, so its `MODS` list and `ServusMod` type use `alchemy`. `ChainView.tsx` default target id gets the new namespace. The docs tests (`draft`, `gamedata`, `proposal`, `ratios`, `recipegraph`, `research`) use `servus_alchemy` ids.
- `.github/workflows/build.yml` runs `./gradlew :alchemy:runData`, names it in the stale export error and collects `alchemy/build/libs/*.jar`.

### Verification
- No new logic, so no new JUnit tests. The existing JUnit tests (all projects, including `ServusAlchemyIdsTest`, `AlchemyMachineDefaultsTest`, `TerminalSortTest` with its fixture namespace changed) and the docs tests cover the pure parts.
- GameTests: `:alchemy:runGameTestServer`, `:foundation:runGameTestServer`, `:storage:runGameTestServer` green.
- Manual: `runClientAll` reaches the title screen with "Servus: Alchemy" in the mod list; in a new world the journal opens with its chapters, JEI shows the calcination and coagulation categories, Jade shows a Calciner's tanks, `/servus_alchemy journal` answers. These cover the ids that only fail at runtime.
- A grep over the repo (excluding `openspec/changes/archive`, this change's own directory, `.git`, build output, `node_modules`, `docs/dist`) for `manufactur` (any case) finds nothing apart from the requirements in `openspec/specs/` that this change's deltas replace; those go at archive.

### PLAN.md open decisions
None settled. This is a rename only.

## Risks / Trade-offs

- [A missed id string only fails at runtime (lang key, book id, datapack folder)] -> the closing grep must come back empty and the manual check opens the journal, JEI and Jade.
- [Datapack loaders look for `servus_manufacturing` subfolders by name] -> they take the folder from `ServusAlchemyIds.MOD_ID` after the rename; the Athanor, vein and journal GameTests load shipped data from the new folders.
- [Dev worlds lose every alchemy block and item] -> accepted, nothing is released; the user is told before the play check.
- [`git mv` does not move ignored `runs/`] -> moved by hand as described above.
- [Helper scripts outside the repo name `:manufacturing:` tasks] -> updated in the session helpers; not part of the repo.
- [Open worktree branches touching `manufacturing/` will conflict] -> none of the open changes touch it; apply this change when no other change is being applied in a worktree.
