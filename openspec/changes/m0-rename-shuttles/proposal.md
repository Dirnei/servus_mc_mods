# Proposal

## Why

The first public release goes to CurseForge, and there "Servus: Storage" sits next to AE2, Refined Storage, Sophisticated Storage and Functional Storage without saying what makes it different. The mod is more than storage: shuttles on rails run the warehouse, the packing and crafting stations, outposts and field farming with harvesters. The shuttles are what players remember, so the mod becomes "Servus: Shuttles". The mod id is effectively locked once the first file is uploaded, and nothing is released yet, so the ids change for free now.

This is M0 housekeeping like `m0-rename-alchemy`, not an open point of PLAN.md. PLAN.md gets the new name everywhere and no new open point.

## What Changes

- `servus_storage` becomes `servus_shuttles`: display name "Servus: Shuttles", description "An automated warehouse run by shuttles on rails: storage, autocrafting, field farming and outposts.", Gradle project `shuttles`, Java packages under `io.github.dirnei.mc.servus.shuttles`. The `storage` Gradle project is removed.
- Classes named after the mod (`ServusStorage*`, `StorageStatsManager`, `StorageGameTests`) become `ServusShuttles*`, `ShuttlesStatsManager`, `ShuttlesGameTests`. Classes named after the Storage Terminal or the storage network (`StorageTerminal*`, `StorageNode*`) keep their names.
- Every registry id, tag, lang key, asset and data path, saved data name, client settings file, JEI and Jade plugin id and command moves to the `servus_shuttles` namespace. No aliases or data fixers (unreleased).
- The stats folder `data/<namespace>/storage/` (`shuttle.json`, `field.json`, `power.json`, `warehouse.json`, ...) becomes `data/<namespace>/shuttles/`, because it holds the stats of the whole mod, not only storage.
- Main specs under `openspec/specs/storage/` move to `openspec/specs/shuttles/`, keeping their capability names.
- PLAN.md, CLAUDE.md, `openspec/config.yaml`, README, PROGRESSION, the CI workflow, `server/pack.ps1`, the root and project build files, the docs site (the mod page becomes `mods/shuttles`), its tests, icons and `gamedata.json` use the new name.
- Blocks keep their names: the Storage Terminal, Pattern Storage and every other block, item and GUI text that uses "storage" as a word stay as they are.
- Behavior of every block, item, GUI, recipe and network stays the same.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `foundation/mod-family-loading`: the mod id `servus_shuttles`; the two requirements named "Alchemy and Storage ..." are replaced by "Alchemy and Shuttles each load with Foundation" and "Alchemy and Shuttles are independent".
- `foundation/gamedata-export`: crafting recipes, the heavy rack example and the icon folder use Shuttles and `servus_shuttles`; "Deposits, techs and items are exported" is replaced by "Items, deposits and techs are exported", because its scenario names the old mod.
- `docs/balance-lab`: the mod pages are Foundation, Alchemy and Shuttles.
- `docs/recipe-graph`: "Mod colours and filter" is replaced by "Colours and filter per mod" for Foundation, Alchemy and Shuttles, because its scenario names the old mod.
- `shuttles/crafting-cpu` (moved from `storage/crafting-cpu`): stats folder `data/<ns>/shuttles/`, Shuttles ships the later step.
- `shuttles/crafting-station` (moved): power stats in `shuttles/power.json`.
- `shuttles/field-head` (moved): stats folder, tags `servus_shuttles:fertilizer` and `servus_shuttles:anti_corrosion`.
- `shuttles/harvester`, `shuttles/heavy-duty-rack`, `shuttles/inbound-station`, `shuttles/rail-elevator`, `shuttles/shuttle-garage`, `shuttles/shuttle-tiers`, `shuttles/shuttles`, `shuttles/site-wake`, `shuttles/warehouse-controller` (moved): stats folder `data/<ns>/shuttles/`.
- `shuttles/rail-corrosion` (moved): stats folder and tag `servus_shuttles:anti_corrosion`.

The other specs under `storage/` move too, but only their title and Purpose name the mod, so they have no delta.

## Non-goals

- Any behavior, balance, texture or model change.
- Renaming blocks or GUI texts that use "storage" as a word (Storage Terminal, Pattern Storage, "goes back to storage").
- Archived changes keep their wording as history.
- Migrating dev worlds: blocks and items of `servus_storage` disappear from saved worlds.
- The CurseForge project page, logo and the modpack. Those come after the rename.

## Impact

- All Java sources and tests of `storage` (moved to `shuttles`), the gamedata tests in Alchemy (`CraftingRecipeReaderTest`, `GameDataJsonTest`).
- Gradle: `settings.gradle`, root `build.gradle` (`allServusMods`), `alchemy/build.gradle` (data run project lists, `mustRunAfter`), `shuttles/gradle.properties`.
- Resources: `assets/servus_storage` and `data/servus_storage` move to `servus_shuttles`, the stats folder to `shuttles/`; `src/generated/resources` is regenerated.
- Dev run directory: `storage/runs` moves to `shuttles/runs`.
- Docs site: mod page, sidebar, links, lab components and tests, icons folder, `gamedata.json`.
- CI and tools: `.github/workflows/build.yml`, `server/pack.ps1`.
- Planning: PLAN.md, CLAUDE.md, `openspec/config.yaml`, README, PROGRESSION; main specs move to `openspec/specs/shuttles/`.
- Ordering: the open change `m5-duct-filter-picker` moves classes out of `storage/`, and `m5-named-channels`, `m5-data-potato` and `m12-receptaculum-pipes` touch the repo widely. This change is applied when no other change is open, so none of them has to be rewritten.
