# Tasks

## 1. Cleanup

- [ ] 1.1 Delete the untracked leftover directories `lib/` and `logistics/` in the repository root after checking `git ls-files lib logistics` lists nothing and no tracked file references them. Verify: both directories are gone and `git status` is unchanged.

## 2. Project and code

- [x] 2.1 `git mv manufacturing alchemy`; move the ignored `manufacturing/runs` to `alchemy/runs` and delete what is left of `manufacturing/`. Update `settings.gradle`, `alchemy/gradle.properties` (`servus_alchemy`, "Servus: Alchemy"), root `build.gradle` (`allServusMods`, Modonomicon block, comments), `alchemy/build.gradle` (data run project lists, comments) and the JEI comments in `foundation/build.gradle` and `storage/build.gradle`. Verify: `gradlew projects` lists `:alchemy` and no `:manufacturing`.
- [x] 2.2 Rename the packages in `alchemy/src/main/java` and `alchemy/src/test/java` from `servus.manufacturing` to `servus.alchemy`, moving `manufacturing.alchemy.*` up one level (shared classes into the root package, `athanor`, `calciner`, `coagulator`, `hollow`, `purifier`, `sight`, `structure` directly under `alchemy`). Rename `ServusManufacturing*` classes to `ServusAlchemy*` and `machine/ManufacturingMachines` to `machine/AlchemyMachineDefaults` (test `AlchemyMachineDefaultsTest`). Set `ServusAlchemyIds.MOD_ID = "servus_alchemy"` and replace the `"servus_manufacturing"` literals in `AthanorTierData`, `VeinData`, `JournalChapters`, `Journal` and `JournalCommand` with `ServusAlchemyIds.MOD_ID`. Change the namespace in `storage/.../terminal/TerminalSortTest.java`. Verify: `gradlew build` and `gradlew test` green.

## 3. Resources and datagen

- [ ] 3.1 Move `alchemy/src/main/resources/assets/servus_manufacturing` and `data/servus_manufacturing` (including the nested `servus_manufacturing/{journal,vein,athanor_tier}` folder) to `servus_alchemy`. Replace every `servus_manufacturing` string in hand written JSON, Java (lang keys, texture and model paths, book and research ids) and the mod metadata template, and "Manufacturing" in English lang text and book content. Verify: `gradlew build`, and a grep for `manufactur` (any case) under `alchemy/src/main` and `alchemy/src/test` finds nothing.
- [ ] 3.2 Delete `alchemy/src/generated/resources`, run `:foundation:runData`, `:storage:runData` and `:alchemy:runData`. Verify: the generated assets, recipes, loot tables, tags, Modonomicon book and multiblocks exist under `servus_alchemy` only, `docs/src/data/gamedata.json` has no `servus_manufacturing`, and `gradlew build` is green.
- [ ] 3.3 Run `:alchemy:runGameTestServer`, `:foundation:runGameTestServer` and `:storage:runGameTestServer`. Verify: all three green (the Athanor tier, vein and journal tests load the shipped data from the new folders).

## 4. Specs and planning

- [ ] 4.1 `git mv openspec/specs/manufacturing openspec/specs/alchemy`. Edit each moved spec's title line to `# alchemy/<cap> Specification` and the Purpose texts naming Manufacturing (`athanor`, `data-line`, `journal`, `principles`, `recipe-viewer`), and the Purpose of `storage/crafting-station`. Verify: `openspec validate m0-rename-alchemy --strict` is valid with no "target spec does not exist" notices, and `openspec validate --specs` is green.
- [ ] 4.2 Update PLAN.md (mod table, dependency text, repository layout and package list, 2.1 "Servus: Alchemy", 2.2, 2.4 "Integration: Storage + Alchemy", "Open: Alchemy", upgrade items line, the Done row reworded to "Factorio style factory"), CLAUDE.md (stack, projects, mod ids, dependency rule), `openspec/config.yaml` context, README (table, run commands, data run, data paths) and PROGRESSION.md. No line says the old name is kept for later. No em dashes. Verify: a grep for `manufactur` (any case) in these files finds nothing and `openspec validate --all` is green.

## 5. Docs and CI

- [ ] 5.1 Rename `docs/src/content/docs/mods/manufacturing.md` to `mods/alchemy.md` (title "Servus: Alchemy"), update the sidebar slug in `docs/astro.config.mjs`, the links in `index.mdx`, `foundation.md` and `storage.md`, `getting-started.md`, the lab pages and the datapack reference pages (`athanor`, `journal`, `machines`, `recipes`, `veins`). In code, `MODS` in `docs/src/lib/recipegraph.ts` and the recipe type labels, `MOD_NAMES` in `RecipeGraph.tsx`, the default target in `ChainView.tsx`, and the ids in the docs tests (`draft`, `gamedata`, `proposal`, `ratios`, `recipegraph`, `research`). Verify: `npm test` and `npm run build` in `docs/` green.
- [ ] 5.2 Point `.github/workflows/build.yml` at `:alchemy:runData` (step and error message) and `alchemy/build/libs/*.jar`. Verify: read the workflow and check `alchemy/build/libs` holds the jar after `gradlew build`.

## 6. Integration

- [ ] 6.1 Grep the repo (excluding `openspec/changes/archive`, `.git`, build output, `runs`, `node_modules` and `docs/dist`) for `manufactur` in any case: no hits. Full `gradlew build` and `gradlew test` green, the three GameTest runs green, and `runClientAll` reaches the title screen with "Servus: Foundation", "Servus: Alchemy" and "Servus: Storage" in the mod list. Tell the user that dev worlds lost the old namespace blocks and items. Then the user play checks from main: the journal opens with its chapters, JEI shows the alchemy categories, Jade shows a Calciner's tanks, `/servus_alchemy journal` answers.
