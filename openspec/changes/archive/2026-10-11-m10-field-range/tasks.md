# Tasks

## 1. Pure logic

- [x] 1.1 Radius as an argument: `FieldStrip.plots/boxes`, `FieldReach.connected`, `PlotReach.assign` take the radius, `RADIUS` removed, `PlotReach` caches offsets per radius; new `FieldRadius` (`of`, `maxUpgrades`) in `storage/.../field/` (design D1). JUnit: update `FieldStripTest`, `FieldReachTest`, `PlotReachTest` for radius 2 and 4; new `FieldRadiusTest` (base, upgrades, clamp at max, max below base). Verify: `gradlew :storage:test`.
- [x] 1.2 `ModuleSpec.wide`, `ShuttleProfile.liftless`, new pure `LiftlessSteps.filter` in `storage/.../shuttle/`; `FieldWork` gets `NO_WAY` and a `reachable` argument after `harvester` (design D4, D6). JUnit: `ShuttleProfileTest` (Harvester liftless, plain not), new `LiftlessStepsTest` (vertical dropped, horizontal through a shaft node kept), `FieldWorkTest` (`NO_WAY` order). Verify: `gradlew :storage:test`.

## 2. Field Head radius

- [x] 2.1 `field.json` `radius` and `max_radius`, `StorageStatsManager.fieldRadius/fieldMaxRadius`; `range_upgrade` item in `ServusStorageItems`, recipe, model, texture and lang through datagen; the head's `RangeUpgrades` handler with limit from `FieldRadius.maxUpgrades`, saved, `requestRefresh` on change, `radius()` used in `serve()` and `refreshReach()`; drop in `FieldHeadBlock.onRemove` (design D2, D3). `HarvestRun.reach` passes the head's radius. Verify: `gradlew runData`, `gradlew build`.
- [x] 2.2 `FieldHeadMenu` upgrade slot (index 1, shift click into it) and a radius data slot; `FieldHeadScreen` draws the slot beside the shorter harvest only toggle and shows the radius in the size line; lang through datagen. Verify: `gradlew runData`, `gradlew build`.
- [x] 2.3 GameTests in the field GameTests: `radiusTwoByDefault` (dirt 2 out is a plot, 3 out is not), `rangeUpgradesWiden` (two upgrades reach 4, hydration ticket covers 4 out), `rangeUpgradeLimit` (third upgrade refused), `rangeUpgradesDropWithHead`. Fix existing field GameTests that assumed 4 blocks (give them two upgrades or move their soil in). Verify: `gradlew :storage:runGameTestServer` green.

## 3. Harvesters and lifts

- [x] 3.1 `ShuttleFleet.ways(shuttle)` used by every per shuttle way; `storable(shuttle, from)` for harvest put-away with `Host.putAwayTarget` applying its predicate to every target; `fieldReachable(head)`; `WarehouseNetwork.tickFields` skips unreachable fields and `FieldHeadBlockEntity.work()` passes the flag; `NO_WAY` lang; Harvester tooltip line about lifts (design D4, D5, D6). Verify: `gradlew runData`, `gradlew build`.
- [x] 3.2 GameTests: `harvesterCrossesShaft` (garage and field on one floor with a shaft between, the field gets worked and no car is called), `harvesterRefusesLift` (field one floor up, the head says `NO_WAY` and the harvester stays home), `harvestSkipsStorageUpstairs` (zoned crate upstairs, unzoned crate on the floor gets the wheat), `plainShuttleStillRides` covered by the existing lift GameTests staying green. Verify: `gradlew :storage:runGameTestServer` green.

## 4. Docs and plan

- [x] 4.1 `docs/src/content/docs/mods/storage.md`: field radius, Range Upgrade, harvesters and lifts; `PLAN.md` M10 item 4 marked built with the decisions. Verify: `openspec validate m10-field-range`, docs tests green.

## 5. Integration

- [x] 5.1 Full gradle build and tests green: `gradlew build`, `gradlew test`, `gradlew :storage:runGameTestServer`. The game launch with all three mods happens in the user's play check (5.2).
- [x] 5.2 Play check in game by the user from main: a new Field Head reaches 2 blocks, two Range Upgrades reach 4 and the farmland stays wet that far, a third upgrade is refused, the radius shows in the GUI; a harvester drives through a shaft on its floor, refuses a field one floor up with the new reason, and puts its harvest only into storage on its floor; the Harvester tooltip warns about lifts. Verify: the user confirms in game.
