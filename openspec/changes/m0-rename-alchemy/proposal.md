# Proposal

## Why

Servus: Manufacturing started as a Factorio style factory mod, but since M9 it is Paracelsus alchemy: principles, Calciner, Coagulator, Athanor, gnome veins and the journal. The name promises a factory the mod no longer is and hides what makes it stand out. Nothing is released, so the ids can change for free now.

This is M0 housekeeping like `m0-code-cleanup`, not an open point of PLAN.md. PLAN.md gets the new name everywhere and no new open point.

## What Changes

- `servus_manufacturing` becomes `servus_alchemy`: display name "Servus: Alchemy", Gradle project `alchemy`, Java packages under `io.github.dirnei.mc.servus.alchemy`. The `manufacturing` Gradle project is removed.
- The former `manufacturing.alchemy.*` packages move up one level (`alchemy.athanor`, `alchemy.calciner`, ...), so there is no `alchemy.alchemy`.
- Classes named `ServusManufacturing*` become `ServusAlchemy*`; `ManufacturingMachines` (the machine stats defaults) becomes `AlchemyMachineDefaults`, because `AlchemyMachines` already exists.
- Every registry id, recipe type, fluid, effect, lang key, asset and data path, the Modonomicon book and research ids, the JEI and Jade plugin ids, the datapack folders (`data/<namespace>/servus_alchemy/journal`, `vein`, `athanor_tier`) and the command (`/servus_alchemy journal`) move to the `servus_alchemy` namespace. No aliases or data fixers (unreleased).
- Main specs under `openspec/specs/manufacturing/` move to `openspec/specs/alchemy/`, keeping their capability names.
- PLAN.md, CLAUDE.md, `openspec/config.yaml`, README, PROGRESSION, the CI workflow, the root and project build files, the docs site (the mod page becomes `mods/alchemy`), its tests and `gamedata.json` use the new name.
- The untracked leftover folders `lib/` and `logistics/` in the repository root (build output from before the foundation merge) are deleted.
- Behavior of every block, item, GUI, recipe and network stays the same.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `foundation/mod-family-loading`: the mod id `servus_alchemy`; the two requirements whose scenarios name Manufacturing are replaced by "Alchemy and Storage each load with Foundation" and "Alchemy and Storage are independent".
- `foundation/gamedata-export`: the export is written by the Alchemy data run (`:alchemy:runData`), names the three current mods and uses an Alchemy item in the display name example.
- `foundation/teams`: the provider scenario names journal progress in Alchemy.
- `foundation/data-packets`: deliveries name Alchemy machines.
- `storage/crafting-cpu`: power wait names Alchemy machines.
- `docs/balance-lab`: the mod pages are Foundation, Alchemy and Storage.
- `docs/recipe-graph`: colours and filter for Foundation, Alchemy and Storage; "Graph shows only Servus recipes" is replaced by "Graph draws Servus recipes only" with a current recipe as its example.
- `alchemy/athanor` (moved from `manufacturing/athanor`): recipe type `servus_alchemy:athanor`, tier folder, Salt Crystal Block owner.
- `alchemy/calciner` (moved): recipe type `servus_alchemy:calcination`.
- `alchemy/calciner-structure` (moved): Alchemy adds the parts.
- `alchemy/coagulator` (moved): recipe type `servus_alchemy:coagulation`, first materials owner.
- `alchemy/coagulator-structure` (moved): Alchemy adds the Principle Vat.
- `alchemy/elemental-sight` (moved): effect id `servus_alchemy:elemental_sight`.
- `alchemy/gnome-hollow` (moved): vein folder `servus_alchemy/vein`.
- `alchemy/journal` (moved): chapter folder, book id, item id and creative tab, command `/servus_alchemy journal`, recipe lists.
- `alchemy/principles` (moved): fluid ids `servus_alchemy:salt`, `sulfur`, `mercury`.
- `alchemy/purifier` (moved): Ash and Caput Mortuum owner, recipe type `servus_alchemy:purification`.
- `alchemy/recipe-viewer` (moved): Servus Alchemy works without JEI.

`alchemy/data-line` moves too, but only its title and Purpose name the mod, so it has no delta.

## Non-goals

- Any behavior, balance, texture or model change.
- Rewording the stale Factorio era content left in specs and docs (deposits, techs, assembler examples) or the old "Lib" and "Logistics" mentions in requirements this change does not touch otherwise.
- Archived changes keep their wording as history.
- Migrating dev worlds: blocks and items of `servus_manufacturing` disappear from saved worlds.

## Impact

- All Java sources and tests of `manufacturing` (moved to `alchemy`), one test fixture in Storage (`TerminalSortTest`), comments in Foundation and Storage build files.
- Gradle: `settings.gradle`, root `build.gradle` (combined runs, Modonomicon dev dependency), `alchemy/build.gradle` (data run system properties), `alchemy/gradle.properties`, the mod metadata template.
- Resources: `assets/servus_manufacturing` and `data/servus_manufacturing` move to `servus_alchemy`; `src/generated/resources` is regenerated.
- Dev run directory: `manufacturing/runs` (eula, RCON settings, worlds) moves to `alchemy/runs`.
- Docs site: mod page, sidebar, lab components and tests, datapack reference pages, `gamedata.json`.
- CI: `.github/workflows/build.yml` runs `:alchemy:runData` and collects `alchemy/build/libs`.
- Planning: PLAN.md, CLAUDE.md, `openspec/config.yaml`, README, PROGRESSION; main specs move to `openspec/specs/alchemy/`.
