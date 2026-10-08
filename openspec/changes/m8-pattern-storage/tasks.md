# Tasks

## 1. Pure logic

- [x] 1.1 Add `storage/.../pattern/TemplateCounts` (no Minecraft imports): counts per key of a 9 cell template, empty cells skipped, equality of two templates' counts. Add `storage/src/test/.../pattern/TemplateCountsTest` with `countsPerKey`, `emptyCellsIgnored`, `sameCountsInAnyLayout`, `differentItemsDiffer`. Verify with `gradlew :storage:test`.

## 2. Pattern items

- [ ] 2.1 Add `EncodedPattern` with codec and stream codec and the `pattern` data component in `ServusStorageComponents`; items `blank_pattern` and `pattern` (`PatternItem` with tooltip) in `ServusStorageItems` and the creative tab; recipes (blank from paper and redstone, pattern alone back to blank) in `ServusStorageRecipeProvider`; item models and textures (PNG helper); lang. Run `:storage:runData`, verify with `gradlew build`.

## 3. Pattern Storage

- [ ] 3.1 Add `patterns_per_block` to `StorageStatsManager` with `data/servus_storage/storage/pattern_storage.json`; `PatternStorageBlock`, `PatternStorageBlockEntity` (encoded patterns only, drops contents), `PatternStorageMenu`, `PatternStorageScreen`, registries, `DataLink` capability in `ServusStorageEvents`, blockstate, model, loot, pickaxe tag, recipe, lang, textures. Run `:storage:runData`, verify with `gradlew build`.

## 4. Patterns on the line and the CPU

- [ ] 4.1 Add `PatternLibrary` (entries, capacity, first free slot, find, duplicate check) and `PatternRunners.canRun` (Crafting Station with the same recipe id and counts); `CpuBlockEntity.Host.producers()` appends runnable pattern recipes not already reported. Verify with `gradlew build` and `:storage:runGameTestServer`.

## 5. Production Terminal

- [ ] 5.1 Extend `ProductionPayload`, `ProductionRows` (`PatternRow`), `ProductionView` and `ClientProduction` with patterns and capacity, and the menu signature. Add the editor to `ProductionTerminalMenu` (ghost slots, inventory slots, editing state, save, delete, cancel, open new) and `EditPatternPayload`; register it in `ServusStoragePayloads`; extend the `OpenStationEditorPayload` handler and add a JEI transfer handler for the terminal. Grep that every payload TYPE is registered. Verify with `gradlew build`.
- [ ] 5.2 `ProductionTerminalScreen`: Patterns tab (capacity bar, plus line, pattern lines with tooltips and clicks), the editor popup with dimmed list and the inventory frames, preview from the client recipe manager; lang. Verify with `gradlew build`.

## 6. Tests

- [ ] 6.1 Add `storage/.../gametest/PatternGameTests` covering membership, encoded only slots, capacity from JSON over two blocks, save new (blank used, stored first free), refusals `no_blank`, `full`, `duplicate`, edit in place, delete returns a blank, CPU order on a line with a sticks pattern and a matching station, and the view listing patterns and capacity. Verify with `:storage:runGameTestServer`, `:foundation:runGameTestServer`, `:manufacturing:runGameTestServer` all green.

## 7. Docs and plan

- [ ] 7.1 Describe patterns, Pattern Storage and the Patterns tab on `docs/src/content/docs/mods/storage.md`; remove the tier question from PLAN.md section 5; regenerate `docs/src/data/gamedata.json` with `:manufacturing:runData`. Verify with `npx vitest run` and `npm run build` in `docs/`.

## 8. Integration

- [ ] 8.1 Full `gradlew build` and `gradlew test` green, all three GameTest runs green, `runClientAll` reaches the title screen without asset warnings for the new blocks and items.
