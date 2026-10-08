# Tasks

## 1. Shared wood material

- [x] 1.1 Add pure `foundation.wood.WoodPick` with JUnit `WoodPickTest` (three equal, two to one, three different picks middle, single, empty). Verify with `gradlew :foundation:test`.
- [x] 1.2 Add `servus_foundation:wood` component, `Wood` helpers, `WoodSlot`, `WoodRecipe` with serializer `servus_foundation:wood_shaped`, `WoodBlockItem` with tooltip, client `WoodModel` and `WoodModels` registry with the `ModifyBakingResult` wrapper, the board marker texture and the `LayeredModels` datagen helper. Verify with `gradlew build`.

## 2. Pure tile and light logic

- [x] 2.1 Add pure `storage.cpu.CpuTiles` (tile per part and face) with JUnit `CpuTilesTest` and pure `storage.pattern.PatternLights` with JUnit `PatternLightsTest`. Verify with `gradlew :storage:test`.

## 3. Textures

- [x] 3.1 Generate the overlay textures (stations, terminals, controller, garage, Pattern Storage with 10 light states, Crafting Station, CPU casing and 18 animated tiles with `.mcmeta`, Machine Adapter) and the board marker with scripts in the helpers dir. Verify the files exist and decode.

## 4. Blocks

- [x] 4.1 Wood on the ten Storage block entities and the Machine Adapter (`WoodSlot`, model data, update tag), items as `WoodBlockItem`, dismantle drops stamped, blocks registered with `WoodModels`. Verify with `gradlew build`.
- [x] 4.2 CPU `ACTIVE` state driven by order changes; Pattern Storage `LIGHTS` state driven by saves, deletes, load and placement. Verify with `gradlew build`.
- [x] 4.3 Datagen: layered models and blockstates for all eleven blocks (CPU per part and active), wood recipes, loot copying the wood, lang for the tooltip. Run `:foundation:runData`, `:storage:runData`, `:manufacturing:runData`; verify generated files and `gradlew build`.

## 5. Tests and docs

- [x] 5.1 GameTests: recipe records birch, birch item places a birch block and its drop keeps birch, dismantle keeps the wood, CPU parts and `ACTIVE` follow forming and orders, Pattern Storage lights follow saves and deletes. Verify with `:storage:runGameTestServer`, `:foundation:runGameTestServer`, `:manufacturing:runGameTestServer`.
- [x] 5.2 Docs: Storage and Foundation pages mention the wood from the recipe and the CPU and Pattern Storage looks. Verify with `npx vitest run` in `docs/`.

## 6. Integration

- [ ] 6.1 Full `gradlew build` and `gradlew test` green, all GameTest runs green, docs tests green. (The client is not launched in this run; the user checks the look in game.)
