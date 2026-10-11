# Tasks

## 1. Pure field logic

- [x] 1.1 `FieldPos`, `FieldGraph`, `FieldStrip`, `FieldRipeness` and `FieldWater` in `storage/src/main/java/.../storage/field/` (design D2), no Minecraft imports. JUnit in `storage/src/test/java/.../storage/field/`: `FieldGraphTest`, `FieldStripTest`, `FieldRipenessTest`, `FieldWaterTest`. Verify: `gradlew test`.

## 2. Farm Rail

- [x] 2.1 `FarmRailBlock` in `storage/.../field/`, registered in `ServusStorageBlocks` and `ServusStorageItems` (assisted item, creative tab), added to the `RAIL` block entity type in `ServusStorageBlockEntities` (design D1). `field.json` and the field getters in `StorageStatsManager` (design D7). Verify: `gradlew build`.

## 3. Field Head

- [x] 3.1 `FieldStatus`, `FieldHeadBlock`, `FieldHeadBlockEntity` (water tank, field refresh, hydration tickets, ripeness, NBT) and `FieldHeads` in `storage/.../field/`; `FIELD_HEAD` block, item and block entity type; fluid capability in `ServusStorageEvents`; farm rail change hooks (design D3, D4, D5). Verify: `gradlew build`.
- [x] 3.2 `FieldEvents` (crop growth, bone meal, break, place, trample, chunk load) in `storage/.../field/` (design D4). Verify: `gradlew build`.
- [x] 3.3 `FieldHeadMenu` in `storage/.../menu/`, `FIELD_HEAD` in `ServusStorageMenus`, `FieldHeadScreen` in `storage/.../client/` registered in `ServusStorageClient` (design D6). Verify: `gradlew build`.
- [x] 3.4 `/servus_warehouse field <pos>` and `field <pos> recheck` in `ServusStorageCommands`. Verify: `gradlew build`.

## 4. Data

- [x] 4.1 Datagen: Farm Rail multipart and item model through `RailModels`, Field Head blockstate and models, loot for both, pickaxe tag, recipes, lang (blocks, GUI lines, statuses, button) in `storage/.../datagen/`; placeholder textures `farm_rail.png`, `farm_rail_curve.png`, `farm_rail_cross.png`, `farm_rail_dock.png`, `field_head_top.png`, `field_head_side.png`, `field_head_bottom.png` via the png helper. Verify: `gradlew runData`, then `gradlew build`.

## 5. GameTests

- [x] 5.1 `FieldGameTests` in the storage gametest package: `farmRailIsARail` (joins the warehouse, links to a Warehouse Rail, not waterloggable), `shuttleDrivesOverFarmRails`, `fieldSplitsAndJoins`, `oneHeadPerField` (no rail, second head idle until the first is broken), `headTakesWaterOnly`, `headFillsThroughThePipe`, `hydrationReachesFourBlocks`, `hydrationEndsWhenDry`, `hydrationEndsWithRail`, `ripeAfterGrowth`, `ripeAfterBonemeal`, `breakForgetsRipe`, `harvestDueNeedsCropAndShare`, `menuSetsCropAndHarvestOnly`. Verify: `gradlew :storage:runGameTestServer` green.

## 6. Plan and docs

- [ ] 6.1 `PLAN.md` M10 item 2 notes what was built; `docs/src/content/docs/mods/storage.md` gets the Farm Rail and the Field Head. Verify: `openspec validate m10-farm-rail-field-head`.

## 7. Integration

- [ ] 7.1 Full gradle build and tests green: `gradlew build`, `gradlew test`, `gradlew :storage:runGameTestServer`. The game launch happens in the user's play check (7.2).
- [ ] 7.2 Play check in game by the user from main: craft farm rails and a Field Head, lay a field next to a warehouse, pipe water from a pump into the head, watch farmland stay wet 4 blocks out, set wheat in the GUI, bone meal crops and see the ripe count and harvest due. Verify: the user confirms in game.
