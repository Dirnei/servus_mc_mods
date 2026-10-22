# Tasks: m0-material-kinds

## 1. Pure logic

- [x] 1.1 Move `foundation/.../wood/WoodPick.java` to `foundation/.../material/MaterialPick.java` and `WoodPickTest` to `foundation/src/test/.../material/MaterialPickTest.java`. Keep the five cases and add `stoneIdsFollowTheSameRule` (two `minecraft:stone_bricks`, one `minecraft:bricks` picks stone bricks). Verify with `gradlew :foundation:test`.

## 2. Foundation core

- [x] 2.1 Add `MaterialKind` (enum `WOOD`, `STONE` with serialized name, ingredient tag, default block, placeholder texture and tooltip key, `CODEC` via `StringRepresentable.fromEnum`) in `foundation/.../material/`. Add the tag key `servus_foundation:stones`. Verify with `gradlew :foundation:compileJava`.
- [x] 2.2 Move and rename the rest of `foundation/.../wood/` to `foundation/.../material/`: `Material` (with `register(blockId, kind)`, `kindOf`, `or(id, kind)`, `of(stack)`, `block(id, kind)`, `stamp`, `PROPERTY`, `SWAPS`), `MaterialSlot` (save key `Material`, default from the owner block's kind), `MaterialHolder`, `MaterialBlockItem` (tooltip key from the kind), `MaterialContent` (component `material`, serializer `material_shaped`), `MaterialRecipe` (`kind` field in both codecs, kind tag in `assemble`). Delete the old package. Verify with `gradlew :foundation:compileJava` (users fail until 2.4).
- [x] 2.3 Rename the client classes in `foundation/.../material/client/`: `MaterialModel` (full name for the vanilla `Material` class), `MaterialItemModel`, `MaterialModels` (skin per block kind, one item model type `servus_foundation:material` with a `kind` field), `MaterialSkin` (`of(kind)`), `MaterialTextures`. Verify with `gradlew :foundation:compileJava`.
- [x] 2.4 Update Foundation users: `ServusFoundation`, `adapter/AdapterContent`, `adapter/MachineAdapterBlock`, `adapter/MachineAdapterBlockEntity`, `block/ConnectableBlock`; datagen `WoodRecipes` renamed to `MaterialRecipes` with a kind parameter, `LayeredModels`, `AdapterBlockLoot`, `model/ModelProvider` (type and kind), `ServusFoundationRecipeProvider` (Machine Adapter passes `MaterialKind.WOOD`), `ServusFoundationLanguageProvider` (add `tooltip.servus_foundation.stone` "Stone: %s"). Verify with `gradlew :foundation:build`.
- [x] 2.5 Add the `servus_foundation:stones` item tag to `datagen/ServusFoundationItemTagsProvider` with the list in design.md D7. Run `gradlew :foundation:runData` and check the tag file, the Machine Adapter recipe (`material_shaped`, `"kind": "wood"`), its item model definition and its loot table. Verify with `gradlew :foundation:build`.

## 3. Shuttles

- [x] 3.1 Update every Shuttles user of the old package: `ServusShuttles`, `client/ElevatorRenderer`, `client/ServusShuttlesClient` (rack skin), `controller/WarehouseControllerBlockEntity`, `controller/WarehouseControllerItem`, `cpu/CpuBlock`, `datagen/ElevatorModels`, `datagen/RailModels`, `datagen/ServusShuttlesBlockLoot`, `datagen/ServusShuttlesRecipeProvider` (all calls pass `MaterialKind.WOOD`), `field/FieldHeadBlockEntity`, `garage/ShuttleGarageBlock`, `garage/ShuttleGarageBlockEntity`, `packing/PackingStationBlock`, `production/DataLineBlockEntity`, `production/ProductionTerminalBlock`, `rail/ElevatorBlockEntity`, `rail/ElevatorItem`, `rail/RailBlockEntity`, `rail/RailItem`, `registry/ServusShuttlesItems`, `shuttle/module/ShuttleWorkshopBlockEntity`, `station/StationBlockEntity`, `terminal/StorageTerminalBlock`, `terminal/StorageTerminalBlockEntity`, and the GameTests `BlockStyleGameTests`, `ElevatorGameTests`, `FieldLookGameTests`, `PatternResultGameTests`, `RailLookGameTests`, `ShuttleModuleGameTests`. Rename the hand written `Wood` key in `rack/HeavyRackBlockEntity` to `Material`. Verify with `gradlew :shuttles:build`.
- [x] 3.2 Run `gradlew :shuttles:runData` and check that the wood block recipes, item model definitions and loot tables changed only in type, kind and component id. Verify with `gradlew build`.

## 4. Stone kind GameTest

- [x] 4.1 Add `foundation/.../material/MaterialGameTests.java` (registered like the other Foundation GameTests): `stoneKindDefaultsToBricks`, `stoneTagHoldsFullCubes` (Stone Bricks and Polished Deepslate in, Stone Brick Slab out), `stoneRecipePicksMajority` (a stone kind `MaterialRecipe` built in code, grid of two Stone Bricks and one Bricks, result carries Stone Bricks). Verify with `gradlew :foundation:runGameTestServer`.
- [x] 4.2 Run all GameTests: `gradlew :foundation:runGameTestServer` and `gradlew :shuttles:runGameTestServer` green (or failures shown to be pre-existing on main).

## 5. Data and docs

- [x] 5.1 Run `gradlew :alchemy:runData` and keep the regenerated `docs/src/data/gamedata.json` (recipe types `servus_foundation:material_shaped`). Run the docs tests (`npx vitest run` in `docs/`).
- [x] 5.2 Check `docs/src/content/docs/mods/foundation.md` and `shuttles.md` for wording about the wood mechanism; they describe player behavior only, so no change is expected. Verify with the docs build (`npm run build` in `docs/`).

## 6. Integration

- [x] 6.1 Full `gradlew build` and `gradlew test` green, Foundation and Shuttles GameTests green, `openspec validate m0-material-kinds --strict` passes, and the game launches with all mods (the GameTest servers loaded every mod; the client launch is part of the play check 6.2, from the main checkout).
- [x] 6.2 Play check by the user from the main checkout: craft a few wood blocks (Storage Terminal, Shuttle Garage, Warehouse Rail, Machine Adapter, Heavy Duty Rack) from different planks; they look, drop (mined and dismantled) and name their wood in the tooltip as before; JEI shows the recipes cycling any planks.
