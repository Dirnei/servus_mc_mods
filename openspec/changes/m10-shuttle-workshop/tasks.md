# Tasks

## 1. Pure module logic

- [x] 1.1 `ModuleSlot`, `ModuleSpec`, `ShuttleProfile`, `JobNeeds` and `CargoBatch` in `storage/src/main/java/.../storage/shuttle/module/` (design D1), no Minecraft imports. JUnit in `storage/src/test/java/.../storage/shuttle/module/`: `ShuttleProfileTest` (plain is one stack, a Cargo Hold takes the stack count given, field only when any module is, module ids collected), `JobNeedsTest` (warehouse needs fit plain and Cargo Hold profiles and refuse field only ones, a needed module must be fitted, a field job takes a field only profile), `CargoBatchTest` (queue order kept, stops at room, skips other ways, zero room takes nothing). Verify: `gradlew test`.

## 2. Modules on the shuttle item

- [x] 2.1 `ShuttleModules` record with codecs and `profile(ItemStack, int)`, `ShuttleModuleItem`, `ShuttleItem` with the module tooltip; register `SHUTTLE_MODULES` in `ServusStorageComponents`, `CARGO_HOLD` and the `ShuttleItem` in `ServusStorageItems` (creative tab), `cargo_hold_stacks` in `warehouse.json` and `StorageStatsManager` (design D2, D7). Verify: `gradlew build`.

## 3. Shuttle Workshop

- [x] 3.1 `ShuttleWorkshopBlock` in `storage/.../shuttle/module/`, registered in `ServusStorageBlocks` and `ServusStorageItems`; `ShuttleWorkshopMenu` with `ModuleContainer` and the module slot class in `storage/.../menu/`, `SHUTTLE_WORKSHOP` in `ServusStorageMenus`; `ShuttleWorkshopScreen` in `storage/.../client/` registered in `ServusStorageClient` (design D3). Verify: `gradlew build`.
- [ ] 3.2 Datagen: blockstate and block model, item models for workshop and Cargo Hold, loot, pickaxe tag, recipes (workshop, Cargo Hold), lang (block, item, tooltip lines, GUI labels) in `storage/.../datagen/`; placeholder textures `shuttle_workshop_top.png`, `shuttle_workshop_side.png`, `shuttle_workshop_bottom.png` and `item/cargo_hold.png` via the png helper. Verify: `gradlew runData`, then `gradlew build`.
- [ ] 3.3 GameTests in `ServusStorageGameTests`: `workshopFitsCargoHold` (fit, take the shuttle out with the module, put it back and see the module, remove the module, plain shuttle stacks with a new one), `workshopRefusesWrongModules` (no module without a shuttle, Cargo Hold not in the tool slot, a stack of shuttles moves one), `workshopReturnsShuttleOnClose`. Verify: `gradlew :storage:runGameTestServer` green.

## 4. Fleet with identity and modules

- [ ] 4.1 `ShuttleJob.needs()` and `sameWay`, `ShuttleFleet` with per slot shuttles and profiles, `adoptGarage(pos, profiles, active)`, `outSlots`, matching in `assignJobs`, `hasShuttles` and `canReach` for warehouse capable shuttles (design D4, D5); `ShuttleGarageBlockEntity.profiles()` and `addShuttle(ItemStack)`; `WarehouseNetwork` passes profiles. Verify: `gradlew build` and the existing shuttle GameTests green with `gradlew :storage:runGameTestServer`.
- [ ] 4.2 Several loads per shuttle with batching at assignment and at the source (design D6) in `ShuttleFleet`. GameTests in `ServusStorageGameTests`: `cargoHoldPicksTwoStacks` (100 cobblestone from a rack in one trip, nothing left queued while the shuttle is out, all 100 delivered), `plainShuttleTakesOneJob` (the 36 job stays queued), `cargoHoldPutAwayLoadsAll` (three stacks leave the station at once). Verify: `gradlew :storage:runGameTestServer` green.
- [ ] 4.3 `ShuttleGarageMenu` locks by the out mask, `ShuttleGarageScreen` unchanged in look (design D4). GameTest `garageLocksSlotOfShuttleOut`. Verify: `gradlew :storage:runGameTestServer` green.

## 5. Plan and docs

- [ ] 5.1 `PLAN.md` M10 item 1 notes what was built; `docs/src/content/docs/mods/storage.md` gets the Shuttle Workshop and the Cargo Hold. Verify: `openspec validate m10-shuttle-workshop`.

## 6. Integration

- [ ] 6.1 Full gradle build and tests green: `gradlew build`, `gradlew test`, `gradlew :storage:runGameTestServer`. The game launch with all three mods happens in the user's play check (6.2).
- [ ] 6.2 Play check in game by the user from main: craft a workshop and a Cargo Hold, fit it, tooltip, put the shuttle in a garage, request more than a stack from a rack and watch one trip, garage slot lock. Verify: the user confirms in game.
