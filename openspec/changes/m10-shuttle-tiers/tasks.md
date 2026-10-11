# Tasks

## 1. Pure tier logic

- [x] 1.1 `ShuttleTiers` (with `Tier`, `Supply`, `DEFAULT`), `ShuttleBuild`, new `ModuleSpec(id, fieldOnly)` and `ShuttleProfile(modules, fieldOnly, stacks, speed, handling)` in `storage/src/main/java/.../storage/shuttle/module/`, no Minecraft imports; `ModuleSlot` removed (design D2, D3, D8). JUnit in `storage/src/test/java/.../storage/shuttle/module/`: `ShuttleTiersTest` (speed per count, counts clamped to the tier max, Handling factor, stop ticks 18 and 12 with the example numbers, work ticks 6, never below one tick, room per kind, tier clamp), `ShuttleProfileTest` rewritten (stacks from the tier, field only from the tool, speed and handling from counts), `ShuttleBuildTest` (complete sheet, missing counts, extras untouched, top tier has no sheet), `JobNeedsTest` updated. Verify: `gradlew test`.

## 2. Items, component and stats

- [x] 2.1 `ShuttleItem(tier)` with `isShuttle`, `tier`, `ofTier` and the tooltip of tool and counts; `SHUTTLE_MK1` (`shuttle`), `SHUTTLE_MK2`, `SHUTTLE_MK3`, `HANDLING_UPGRADE` in `ServusStorageItems`, `CARGO_HOLD` removed; `ShuttleModules(tool, speed, handling)` with codecs and `profile(ItemStack, ShuttleTiers)`; `ShuttleUpgrades.kind(stack)` (design D1, D2, D7). `StorageStatsManager` reads `storage/shuttle.json` into `shuttleTiers()` with a test setter and drops `cargo_hold_stacks`, `shuttle_speed`, `harvester_stacks`; new `shuttle.json`, cleaned `warehouse.json` and `field.json` in `storage/src/main/resources/data/servus_storage/storage/` (design D4). Every `SHUTTLE` use in main and GameTest code moves to `SHUTTLE_MK1` or `ShuttleItem.isShuttle`. Verify: `gradlew build`.

## 3. Fleet

- [x] 3.1 `ShuttleGarageBlockEntity.slots()`, `ShuttleFleet.adoptGarage(pos, stacks, active)` keeping the stack per shuttle, per shuttle speed in `tick` and `views`, `Host.shuttleSpeed()` removed from `ShuttleFleet.Host` and `WarehouseNetwork`, field cargo from `profile.stacks()`, work ticks through `ShuttleTiers.workTicks` (design D5, D6). `ShuttleView` gains the shuttle stack and `ShuttleGarageRenderer` draws it (design D9). Verify: `gradlew build` and `gradlew :storage:runGameTestServer` green, timeouts raised where the slower base speed needs it.
- [x] 3.2 Stop times: `stop` and `stopAt` on `Shuttle`, countdown in `tick`, the stop in `arrive` for pick and put-away stops, ended by any new leg (design D6). GameTests in `ShuttleModuleGameTests`: `stopsDelayTheDelivery` (a plain shuttle still carries the pick when travel alone would be done), `speedShortensTheTrip` (two Speed upgrades deliver by then), `mk1PicksTwoStacks` (100 cobblestone in one trip), `mk1LeavesTheRestQueued` (200 cobblestone: two jobs ride, two wait), `mk2PutAwayLoadsThree`. Verify: `gradlew :storage:runGameTestServer` green.

## 4. Shuttle Workshop

- [x] 4.1 `ShuttleWorkshopBlockEntity` (shuttle and supply handlers, `buildStep`, save and load, drops on removal, `buildNow()`), `ShuttleWorkshopBlock` as a `BaseEntityBlock` opening the menu with the tiers in the buffer, block entity type in `ServusStorageBlockEntities`, menu type with `IMenuTypeExtension.create` in `ServusStorageMenus` (design D8). Verify: `gradlew build`.
- [x] 4.2 `ShuttleWorkshopMenu` rewritten: shuttle slot, install slot, six supply slots, inventory, shift click routing, `clickMenuButton` 0 to 3; `ShuttleWorkshopScreen` rewritten: tier, Speed and Handling with max, tool, remove buttons, the next tier's sheet with have and need, Build now (design D8). Verify: `gradlew build`.
- [x] 4.3 GameTests in `ShuttleModuleGameTests` replacing the Cargo Hold ones: `installStackUpToMax`, `installRefusesWithoutShuttleOrUpgrade`, `toolInstallsOnce` (the only tool there is cannot swap with another yet), `removeButtonsGiveItemsBack` (also a bare shuttle stacks with a new one), `buildNowNeedsSuppliesAndKeepsUpgrades`, `workshopKeepsAndDropsContents`. Verify: `gradlew :storage:runGameTestServer` green.

## 5. Data, docs and plan

- [x] 5.1 Datagen in `storage/.../datagen/`: item models for Mk2, Mk3 and the Handling Upgrade, lang (items, tooltips, GUI strings), the Handling Upgrade recipe, Cargo Hold entries removed, creative tab order; placeholder textures `item/shuttle_mk2.png`, `item/shuttle_mk3.png`, `item/handling_upgrade.png` with the png helper, `item/cargo_hold.png` removed (design D10). Verify: `gradlew runData`, then `gradlew build`.
- [x] 5.2 `docs/src/content/docs/mods/storage.md`: shuttle tiers, upgrades, stops and the workshop instead of the Cargo Hold. `PLAN.md` M10 item 1 marked built with what was decided. Verify: `openspec validate m10-shuttle-tiers`.

## 6. Integration

- [x] 6.1 Full gradle build and tests green: `gradlew build`, `gradlew test`, `gradlew :storage:runGameTestServer`. The game launch with all three mods happens in the user's play check (6.2).
- [ ] 6.2 Play check in game by the user from main: craft a Mk1 shuttle and a Handling Upgrade, install Speed and Handling in the workshop up to the max, swap the Harvester in and out, build a Mk2 with Build now, watch shuttles stop at racks and stations, compare a fast and a slow shuttle. Verify: the user confirms in game.
