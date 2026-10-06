# Tasks

Groups 1 to 6 were built against the first design (rules on the controller, edited at the station). Group 8 reworks them to the stamped label design decided on 2026-10-06; the earlier checkmarks stay as history.

## 1. Pure zone and routing logic

- [x] 1.1 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/zone/ZoneNames.java` (`key(String)`: trim, lower case with `Locale.ROOT`, empty for blank) and `storage/src/test/java/io/github/dirnei/mc/servus/storage/zone/ZoneNamesTest.java` with tests: trims spaces, folds case, blank gives empty. Verify `gradlew :storage:test` is green.
- [x] 1.2 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/zone/PutAwayPlan.java` with the first design's rules and `PutAwayPlanTest.java`. Verify `gradlew :storage:test` is green.
- [x] 1.3 Add `STATION` to `WarehouseConnectionRule.NodeKind` in `storage/src/main/java/io/github/dirnei/mc/servus/storage/network/WarehouseConnectionRule.java` per design.md and extend `storage/src/test/java/io/github/dirnei/mc/servus/storage/network/WarehouseConnectionRuleTest.java`: station links to stock, station and rail on any side; station never links to an elevator in either direction; rail links to a station sideways and vertically like to stock. Verify `gradlew :storage:test` is green.

## 2. Zones on blocks and the Zone Label

- [x] 2.1 Add the `zone` field with `zone()`, `zoneKey()`, `setZone(String)`, `zonable()` and `docksRail()` to `storage/src/main/java/io/github/dirnei/mc/servus/storage/network/StorageNodeBlockEntity.java`, persist `"Zone"` in `saveAdditional`/`loadAdditional`, include it in the update tag of `CrateBlockEntity` and `HeavyRackBlockEntity`, add `WarehouseEvent.ZoneChanged(pos)` to `WarehouseEvent.java`, and switch `RailBlock.docksTo` and `WarehouseNetwork.railSideAccepted` to `docksRail()`. Verify `gradlew :storage:build` is green.
- [x] 2.2 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/zone/ZoneLabelItem.java` (`useOn`: named label stamps a zonable node, sneak clears, unnamed label shows the anvil message, never consumed), register `zone_label` in `ServusStorageItems.java` and the creative tab order, and return `SKIP_DEFAULT_BLOCK_INTERACTION` for it in `useItemOn` of `CrateBlock.java` and `HeavyRackBlock.java`. Add the shapeless recipe (paper and a dye), item model and lang in the datagen providers and run `gradlew :storage:runData`. Verify `gradlew :storage:build` is green.
- [x] 2.3 Draw the zone name on the front in `storage/src/main/java/io/github/dirnei/mc/servus/storage/client/CrateRenderer.java` above the item display and in `HeavyRackRenderer.java` above the shelf, and add the zone in brackets to the title in `HeavyRackBlockEntity.getDisplayName`. Verify with the GameTest `zoneLabelStampsAndClears` and a client launch.

## 3. Zone rules and zone aware insert (first design)

- [x] 3.1 Add `ZoneRules.java` on the controller and expose it through the network. Verify `gradlew :storage:build` is green.
- [x] 3.2 Track `zoneOf` per source in `WarehouseNetwork.java`, route `insert` through `PutAwayPlan`, add `zoneBlockCounts()` and the `zones <pos>` command. Add the GameTest `insertRespectsZones`. Verify `gradlew :storage:runGameTestServer` passes all storage tests.

## 4. Dispatch Station block and instant put-away

- [x] 4.1 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/station/DispatchStationBlock.java` and `DispatchStationBlockEntity.java` (kind `STATION`, buffer, claims, blocked reason, insert only handler view, posts `WarehouseEvent.Intake`), register block, block entity, item and capability, read `dispatch_buffer_slots` and `put_away_per_tick` in `StorageStatsManager.java` and add them to `warehouse.json`. Verify `gradlew :storage:build` is green.
- [x] 4.2 Add `stations`, `pendingStations` and `blockedStations` to `WarehouseNetwork.java`, handle `Intake`, drain pending stations in `onTick`, set and clear `BACKED_UP`, retry blocked stations, and pass stations with connectors into `rebuildCandidates`. Add GameTests `stationPutsAwayInstantly`, `stationBacksUpWhenZoneFull` and `chestOnStationIsNotStock`. Verify `gradlew :storage:runGameTestServer` passes all storage tests.
- [x] 4.3 Datagen for the station (dark, online and backed up fronts, loot, recipe, tags, lang) and placeholder textures. Add the GameTest `stationDocksAndDismantles`. Run `gradlew :storage:runData` and verify `gradlew :storage:build` is green and the client launches.

## 5. Put-away by shuttle

- [x] 5.1 Add `ShuttleJob` (sealed) and `PutAwayJob`, give `ShuttleFleet.java` the `Leg` states, `railsBeside(BlockPos)`, `canCarry(station, target)`, the shared queue, the `takeFromStation`, `insertAt`, `returnToStation` and `releaseClaim` host calls and the put-away count in `summary()`, implement the host calls in `WarehouseNetwork.java`, and let the station drain queue jobs when `canCarry` holds. Verify `gradlew :storage:test` stays green and `gradlew :storage:build` is green.
- [x] 5.2 Add the GameTest `shuttleCarriesPutAway` and keep `shuttlePicksFromRail` green. Verify `gradlew :storage:runGameTestServer` passes all storage tests.

## 6. Station GUI (first design)

- [x] 6.1 Add `GhostSlot.java`, `DispatchStationMenu.java` with a rule editor and the payloads. Verify `gradlew :storage:build` is green.
- [x] 6.2 Add `DispatchStationScreen.java` and `ClientZoneRules.java`, register the screen, add the GUI texture and lang keys. Verify with the GameTest `stationMenuEditsRules` and a client launch.

## 7. Specs and plan

- [x] 7.1 Run `openspec validate m6-dispatch-station --strict` and fix any finding. Verify validate passes.

## 8. Rework: the label defines the zone, blocks carry a copy

- [x] 8.1 Rework `storage/src/main/java/io/github/dirnei/mc/servus/storage/zone/PutAwayPlan.java` to `Filter<K>` with `match`, `tiers(key, filtered, tagsOf)` returning exact and tag sets, and `Reason` `NO_ROOM` or `NO_STORAGE`; rewrite `PutAwayPlanTest.java`: exact match, tag match, both kinds split into tiers, no match gives empty tiers, an empty filter never matches. Delete `ZoneRules.java`. Verify `gradlew :storage:test` is green.
- [x] 8.2 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/zone/ZoneFilter.java` (record with `CODEC` and `STREAM_CODEC`, `toPure()`), `ZoneDefinition.java` (name plus filter, NBT save and load), and `storage/src/main/java/io/github/dirnei/mc/servus/storage/registry/ServusStorageComponents.java` registering `zone_filter`; register it in `ServusStorage.java`. Change `StorageNodeBlockEntity` to hold a `ZoneDefinition` (`zone()` returns the name, `zoneFilter()`, `setZone(ZoneDefinition)`, `clearZone()`), keep the update tags of crate and rack in step, and make `ZoneLabelItem.useOn` copy name and filter from the stack (unnamed label: message "needs a name"). Remove the rules field and persistence from `WarehouseControllerBlockEntity.java` and restore the plain dismantle in `WarehouseControllerBlock.onWrench`. Verify `gradlew :storage:build` is green.
- [x] 8.3 Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/menu/ZoneLabelMenu.java` (hand, nine ghost slots over an `ItemStackHandler` written back to the stack's `zone_filter`, `setName`, `setTags`, `stillValid`), payloads `SetLabelNamePayload` and `SetLabelTagsPayload` in `storage/src/main/java/io/github/dirnei/mc/servus/storage/zone/`, register menu and payloads, and open the menu from `ZoneLabelItem.use`. Add `storage/src/main/java/io/github/dirnei/mc/servus/storage/client/ZoneLabelScreen.java` (name box, ghost row, tag box) with its texture and lang keys and register it in `ServusStorageClient.java`. Add the GameTest `zoneLabelMenuEditsDefinition` and update `zoneLabelStampsAndClears` to stamp a filter. Run `gradlew :storage:runData` and verify `gradlew :storage:runGameTestServer` passes all storage tests.
- [x] 8.4 Rework `WarehouseNetwork.java`: `zoneOf` holds `ZoneDefinition`s, `insert`, `chooseTarget` and `drain` walk exact, tag and general tiers from `PutAwayPlan.tiers`, `zoneBlockCounts()` and a new `zoneSummary(key)` (block count, item names, tags) replace the rules access, the blocked reason uses `NO_ROOM` and `NO_STORAGE`. Update `ServusStorageCommands.zones` and the station's blocked texts. Rewrite the GameTests `insertRespectsZones` and `stationBacksUpWhenZoneFull` for the new tiers. Verify `gradlew :storage:runGameTestServer` passes all storage tests.
- [x] 8.5 Rework `DispatchStationMenu.java` (buffer and inventory only, `ZoneRulesSyncPayload` carries zone rows with item names and tags plus the station state), delete the select, tags and overflow payloads, and rework `DispatchStationScreen.java` to the read only zone list with hover tooltips and the status line; redraw the GUI texture and update lang keys. Replace the GameTest `stationMenuEditsRules` with `stationMenuShowsZones`. Run `gradlew :storage:runData` and verify `gradlew :storage:runGameTestServer` passes all storage tests and the client launches.
- [x] 8.6 Drop the zone list from the station GUI (user request on 2026-10-07): `DispatchStationScreen.java` shows buffer and status only, `StationStatePayload` replaces `ZoneRulesSyncPayload`, `ClientStationState` replaces `ClientZoneRules`, the GameTest `stationMenuShowsZones` becomes `stationMenuFeedsBuffer`, texture and lang updated. Verify `gradlew :storage:runGameTestServer` passes all storage tests.
- [x] 8.7 Run `openspec validate m6-dispatch-station --strict`, then full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. In game: define two labels in the label GUI (Ores with `c:ores`, Food with bread), stamp two aisles, add a station fed by a Utility Duct from a chest and one shuttle; watch ores and food go to their aisles, other items to unzoned racks, a full aisle spill into unzoned storage, and the station back up with the red lamp when everything is full. Commit.
