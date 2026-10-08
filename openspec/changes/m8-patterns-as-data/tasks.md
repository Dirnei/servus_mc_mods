# Tasks

## 1. Patterns as data

- [ ] 1.1 Rewrite `storage/.../pattern/PatternStorageBlockEntity.java` to keep `EncodedPattern` places as data (save, load, `size()`), add the `patterns` data component in `registry/ServusStorageComponents.java` with `applyImplicitComponents`/`collectImplicitComponents`/`removeComponentsFromTag`; `PatternStorageBlock` shows the action bar count instead of a menu and its carrot dismantle drops the item with components; `PatternEditing` saves without Blank Patterns (`no_storage`, `full`) and deletes without returning anything; move the stack helper from `PatternItem` to `CraftingGrid`. Delete `PatternItem`, `PatternStorageMenu`, `PatternStorageScreen`, the items, the old `pattern` component, the menu type and screen registration. Verify with `gradlew build`.
- [ ] 1.2 Datagen: loot table copies the `patterns` component, Pattern Storage recipe without blanks, remove blank and wipe recipes, item models and lang for the items, add lang `message.servus_storage.pattern_storage.count` and `gui.servus_storage.production.pattern_message.no_storage`, drop `no_blank`; delete the two item textures; run `:storage:runData`. Verify the generated files and `gradlew build`.

## 2. Tests

- [ ] 2.1 Rewrite `storage/.../gametest/PatternGameTests.java` and adapt `StationJobs` and any other test that used pattern items: data storage and capacity, save without blanks, refusals `no_storage`, `full`, `duplicate`, delete, break and place keeps patterns, terminal snapshot. Verify with `:storage:runGameTestServer` all green and `gradlew test`.

## 3. Docs

- [ ] 3.1 Update the Storage docs page and PLAN.md 2.4 wording about pattern items, delete the docs icons of the two items, run `:manufacturing:runData` for `gamedata.json` and `npx vitest run` in `docs/`. Verify the tests pass and no `blank_pattern` or `servus_storage:pattern"` remains in docs.
