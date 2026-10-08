# Tasks

## 1. Projects and code

- [x] 1.1 `git mv lib foundation`; `settings.gradle`, `foundation/gradle.properties` (`servus_foundation`, "Servus: Foundation"), root `build.gradle` (`allServusMods`, `foundation_version_range`, Manufacturing data run paths), `manufacturing/build.gradle` and `storage/build.gradle` point at `:foundation`. Rename packages `servus.lib` to `servus.foundation` and classes `ServusLib*` to `ServusFoundation*` (entry `ServusFoundation`, ids `ServusFoundationIds`) in `foundation/src`, and rewrite imports in `manufacturing/src` and `storage/src`. Logistics still builds against Foundation at this point. Verify with `gradlew build`.
- [x] 1.2 Move `logistics/src/main/java/.../logistics` to `foundation/src/main/java/.../foundation/logistics` and its tests likewise; rename packages to `servus.foundation.logistics`; replace `ServusLogistics` with `logistics/ServusLogisticsContent.register(IEventBus)` called from `ServusFoundation`; replace `ServusLogisticsIds` uses with `ServusFoundationIds`; rewrite Manufacturing imports; delete the `logistics` project and its mod metadata template; remove `logistics` from `settings.gradle`, root and Manufacturing build files; the metadata templates of Manufacturing and Storage require only `servus_foundation`. Verify with `gradlew build` and `gradlew test`.

## 2. Resources and datagen

- [x] 2.1 Move `assets/servus_lib`, `assets/servus_logistics` and `data/servus_logistics` into `foundation/src/main/resources/{assets,data}/servus_foundation`; switch every `servus_lib` and `servus_logistics` string in code (ids, lang keys such as `gui.servus_lib.side.*`, `message.servus_lib.*`, texture and model paths, datapack loader namespaces) in all three projects to `servus_foundation`. Verify with `gradlew build` and a grep that finds no `servus_lib` or `servus_logistics` under `foundation/src/main`, `manufacturing/src/main` and `storage/src/main` (generated resources excepted).
- [x] 2.2 Replace `ServusFoundationDataGen` and the Logistics datagen subscriber with one subscriber registering one provider per kind (language, block states, item models, block tags, recipes, loot tables), each provider calling the API part and the Logistics part. Delete both old `src/generated/resources` trees, run `:foundation:runData`, `:storage:runData` and `:manufacturing:runData`, verify the duct, tank, carrot, goggles and Data Cable assets, recipes, loot tables and tags exist under `servus_foundation`, then `gradlew build`.

## 3. Tests

- [x] 3.1 Copy `eula.txt` and the RCON `server.properties` from `logistics/runs/server` to `foundation/runs/server` if missing. Run `:foundation:runGameTestServer` (former Logistics tests, 24), `:manufacturing:runGameTestServer` (39) and `:storage:runGameTestServer` (105); all green. Fix test helpers that referenced old ids (`CraftingRecipeReaderTest`, `GameDataJsonTest`, `DataCableGameTests`). Verify with `gradlew test` and the three GameTest runs.

## 4. Specs and planning

- [x] 4.1 `git mv` every capability under `openspec/specs/lib/` and `openspec/specs/logistics/` to `openspec/specs/foundation/`; edit each moved spec's title line and the Purpose texts that name Lib or Logistics as owner; move `openspec/changes/m8-data-cable/specs/lib/data-cable` to `.../specs/foundation/data-cable` and update its proposal and design paths. Verify with `openspec validate m8-foundation-merge --strict` (no "target spec does not exist" notices) and `openspec validate m8-data-cable --strict`.
- [x] 4.2 Update PLAN.md (section 1 table and dependency text, repository layout, 2.2 Servus: Foundation, 2.3 and 2.4 dependency remarks, section 3 API heading, M8 list with `m8-foundation-merge` first and `m8-data-cable` as the duct Data line), CLAUDE.md (stack, projects, mod ids, rules), `openspec/config.yaml` context and README. Verify with a grep for "Servus Lib", "Logistics depend" and "four mods" in these files, and `openspec validate --all`.

## 5. Docs and CI

- [ ] 5.1 Merge `docs/src/content/docs/mods/lib.md` and `logistics.md` into `foundation.md`, update the sidebar in `docs/astro.config.mjs`, regenerate `docs/src/data/gamedata.json` with `:manufacturing:runData`, update `docs/src/lib/gamedata.test.ts` and `recipegraph.test.ts` for the new ids. Verify with `npm test` and `npm run build` in `docs/`.
- [x] 5.2 Point `.github/workflows/build.yml` at `foundation/build/libs` instead of `lib` and `logistics`. Verify by reading the workflow and checking the paths exist after `gradlew build`.

## 6. Integration

- [ ] 6.1 Grep the repo (excluding `openspec/changes/archive`, `.git`, build outputs and `docs/dist`) for `servus_lib`, `servus_logistics`, `servus.lib.`, `servus.logistics.`, `':lib'`, `':logistics'`: no hits except intended mentions of the old ids in specs. Full `gradlew build` and `gradlew test` green, all GameTest runs green, `runClientAll` and `:foundation:runClient` reach the title screen showing "Servus: Foundation", "Servus: Manufacturing" and "Servus: Storage". Tell the user that dev worlds lost the old namespace blocks and items.
