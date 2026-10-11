# Tasks

## 1. Pure logic

- [x] 1.1 Pure `LiftUpgrades` in `storage/.../shuttle/` (`speed`, `accel`, `bottom`, `heirOnRemove`, design D4, D5). JUnit `LiftUpgradesTest`: speed and accel per count, bottom of a column, heir is the segment above or none. Verify: `gradlew :storage:test`.

## 2. Slots, GUI and moves

- [x] 2.1 `warehouse.json` and `StorageStatsManager`: `lift_upgrade_slots`, `lift_speed_per_upgrade`, `lift_accel_per_upgrade` (design D6). `LiftUpgradeSlots` (Speed Upgrade only) in `ElevatorBlockEntity`, saved, change callback posting the new `WarehouseEvent.LiftUpgradesChanged` (design D1). Verify: `gradlew build`.
- [x] 2.2 `ElevatorMenu`, registration in `ServusStorageMenus`, `ElevatorScreen` in `storage/.../client/` registered in `ServusStorageClient`, `ElevatorBlock.useItemOn` and `useWithoutItem` opening the bottom's menu, lang through datagen (design D2, D3). Verify: `gradlew runData`, `gradlew build`.
- [x] 2.3 `ElevatorBlock.onPlace` and `onRemove` move the upgrades to the new lowest segment or drop them (design D4). Verify: `gradlew build`.

## 3. Faster cars

- [x] 3.1 `LiftCar.upgrades`, `ShuttleFleet.refreshShafts` reads the count from the bottom block entity, per car speed and accel in `tick` and `sendLift`, `WarehouseNetwork` handles `LiftUpgradesChanged` (design D5). Verify: `gradlew build`.

## 4. GameTests

- [ ] 4.1 Storage GameTests in `ServusStorageGameTests`: `liftUpgradesLiveInTheBottom` (menu opened from the top segment, upgrades land in the bottom, a Handling Upgrade is refused), `liftUpgradesMoveDown` (segment placed below takes them), `liftUpgradesClimbWhenBottomBreaks` and `liftUpgradesDropWithLastSegment`, `upgradedCarIsFaster` (the car's count and its speed after the same number of ticks). Verify: `gradlew :storage:runGameTestServer` green.

## 5. Docs

- [ ] 5.1 `docs/src/content/docs/mods/storage.md`: elevator upgrades. `PLAN.md` M10: the change as a play check follow up with what was built. Verify: `openspec validate m10-lift-upgrades`, docs tests green.

## 6. Integration

- [ ] 6.1 Full gradle build and tests green: `gradlew build`, `gradlew test`, `gradlew :storage:runGameTestServer`.
- [ ] 6.2 Play check in game by the user from main: open the GUI from an upper segment, fit Speed upgrades, see the car run faster, place a segment under the shaft and break the bottom and find the upgrades still in the shaft, break the whole shaft and pick them up. Verify: the user confirms in game.
