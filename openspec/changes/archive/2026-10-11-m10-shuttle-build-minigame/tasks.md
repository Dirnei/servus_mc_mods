# Tasks

## 1. Pure build logic

- [x] 1.1 `HandBuild`, `BoltBoard`, `WireBoard`, `WeldBoard` and `NamePlate` in `storage/src/main/java/.../storage/shuttle/module/build/`, no Minecraft imports (design D2, D4). `ShuttleTiers.Tier` gains `steps` with `BuildStep`, `StepKind`, `BuildStep.DEFAULTS`, the four argument constructor kept (design D1). JUnit in `storage/src/test/java/.../storage/shuttle/module/build/`: `HandBuildTest`, `BoltBoardTest`, `WireBoardTest`, `WeldBoardTest`, `NamePlateTest`; `ShuttleTiersTest` gets the default steps and clamps. Verify: `gradlew test`.

## 2. Data and component

- [x] 2.1 `ShuttleStatsReader` reads `steps` per tier (defaults, clamps, unknown kind dropped), `shuttle.json` lists the shipped steps for Mk2 and Mk3, `TIERS_CODEC` carries the steps (design D1). `SHUTTLE_NAME` component in `ServusStorageComponents`, `ShuttleItem.getName` with the plate (design D6). Verify: `gradlew build`.

## 3. Workshop server side

- [x] 3.1 `ShuttleWorkshopBlockEntity`: `handBuild` and `buildStep` saved and clamped, `startHandBuild`, `reportStep`, `cancelHandBuild`, `engrave`, locks on both handlers, no reset in `onContentsChanged` (design D3). `ShuttleWorkshopMenu`: buttons 4 and 5, locked slots and buttons while a build runs, two data slots, `reportStep` and `engrave` entry points; `ShuttleBuildStepPayload` and `EngraveShuttlePayload` registered in `ServusStoragePayloads` (design D4). Verify: `gradlew build`, and grep that every new payload `TYPE` is registered.
- [x] 3.2 GameTests in `ShuttleModuleGameTests`: `handBuildLocksAndCancels`, `handBuildStepsInOrder`, `engraveFinishesWithPlate`, `buildNowKeepsPlate`, `handBuildSurvivesSaveAndLoad`, `installKeepsBuildStep`. Verify: `gradlew :storage:runGameTestServer` green.

## 4. Client

- [x] 4.1 `ShuttleWorkshopScreen` at 320 wide with the work panel: large shuttle, plate name, Build by hand and Cancel, step header and hint, the three boards drawn and played, step payload once per finished step, name box and Engrave with key handling (design D5). Lang for every new string through datagen. Verify: `gradlew runData`, then `gradlew build`.
- [x] 4.2 Plate text in `ShuttleGarageRenderer` on the side spot of the hold (design D7). Removed again in the play check: the plate shows only in the item name. Verify: `gradlew build`.

## 5. Field Head

- [x] 5.1 `ShuttleFleet.fieldWorker`, `WarehouseNetwork.fieldWorker`, `FieldHeadBlockEntity.workerName`, `FieldWorkerPayload` sent by `FieldHeadMenu` on change and registered in `ServusStoragePayloads`, `ClientFieldWorker`, the named working line in `FieldHeadScreen` with its lang (design D8). Verify: `gradlew build` and `gradlew :storage:runGameTestServer` green.

## 6. Docs and plan

- [x] 6.1 `docs/src/content/docs/mods/storage.md`: the build by hand, its steps and the name plate. `PLAN.md` M10 item 3 marked built with what was decided. Verify: `openspec validate m10-shuttle-build-minigame`, docs tests green.

## 7. Integration

- [x] 7.1 Full gradle build and tests green: `gradlew build`, `gradlew test`, `gradlew :storage:runGameTestServer`. The game launch happens in the user's play check (7.2).
- [x] 7.2 Play check in game by the user from main: build a Mk2 by hand (bolts, wires, weld), close the GUI mid build and come back, cancel one build, engrave a name, see it on the shuttle on the rails, in the garage tooltip and in the Field Head while it harvests, build a Mk3 with Build now and keep the plate. Verify: the user confirms in game.
