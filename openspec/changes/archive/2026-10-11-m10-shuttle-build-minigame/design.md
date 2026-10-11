# Design

## Context

After `m10-shuttle-tiers` and `m10-shuttle-looks`:
- `ShuttleWorkshopBlockEntity` holds a one slot shuttle handler, a six slot supply handler and `int buildStep` (saved, always 0). Its shuttle handler resets `buildStep` in `onContentsChanged`, so every install, which rewrites the shuttle stack, resets it.
- `ShuttleWorkshopMenu` routes buttons 0 to 3 through `clickMenuButton` (remove Speed, Handling, tool, Build now) and gets `ShuttleTiers` from the opening buffer through `TIERS_CODEC`. `ShuttleWorkshopScreen` is 176 by 204 with the info, the sheet and Build now.
- `ShuttleTiers.Tier(stacks, maxSpeed, maxHandling, build)`; `ShuttleStatsReader` reads `storage/shuttle.json` with defaults tier by tier.
- `buildNow()` takes the sheet's supplies and puts `held.transmuteCopy(next, 1)` into the slot (all components carried).
- The shuttle item model is composed from parts (`ShuttleItemModel`); the left side of the hold in model space (x = 2, y 5 to 8, z 5 to 11) is free. `ShuttleGarageRenderer` draws the synced shuttle stack with `renderStatic(NONE)` around the model centre and the cargo after it.
- `FieldHeadMenu` syncs numbers through `DataSlot`s; `FieldHeadBlockEntity.work()` asks `WarehouseNetwork.hasFieldJob(pos)`, which asks `ShuttleFleet.hasFieldJob(head)`. A `Shuttle` in the fleet keeps a count one copy of its slot stack (`item`).
- Payloads live in `ServusStoragePayloads` (`playToServer` handlers check `player.containerMenu instanceof ...`).

## Goals / Non-Goals

**Goals:**
- Step logic that is pure and tested; the client draws, the server counts.
- One more state on the workshop (a build by hand and its step), saved, with the slots locked while it runs.
- A name plate component that every tier build carries, drawn in the world and named in the item name.

**Non-Goals:**
- Scoring how well a step was played, animations of the shuttle parts, a plate on the inventory icon (item models are baked quads), new items or blocks.

## Decisions

### D1. Steps in the stats file
`ShuttleTiers.Tier` gains `List<BuildStep> steps` with `record BuildStep(StepKind kind, int count)` and `enum StepKind { BOLTS, WIRES, WELD }` (pure, in `shuttle/module/build/`). A four argument `Tier` constructor stays for the code and tests that have no steps; a tier with a build and no steps gets `BuildStep.DEFAULTS` (bolts 4, wires 4, weld 1, bolts 3); `ShuttleTiers.nextSteps(tier)` gives the steps of the build into the tier above. JSON, beside `build`:
```
"steps": [ { "kind": "bolts", "count": 4 }, { "kind": "wires", "count": 4 }, { "kind": "weld", "count": 1 }, { "kind": "bolts", "count": 3 } ]
```
Counts clamp: bolts 1 to 8, wires 1 to 8 (one per duct colour), weld 1 to 3 seams. An unknown kind drops that step; an empty list falls back to the defaults. `TIERS_CODEC` writes the steps so the client knows them.

### D2. Pure step logic: `HandBuild` and three boards
In `storage/shuttle/module/build/`, no Minecraft imports:
- `HandBuild`: `next(int current, int reported, int total)` gives the step after an accepted report or `current` when the report is not the current step; `done(step, total)`.
- `BoltBoard(long seed, int count, int width, int height)`: spot positions spread over the area (min distance so spots do not overlap), `current()`, `click(x, y)` accepts only within `RADIUS` of the current spot, `done()`.
- `WireBoard(long seed, int count)`: left colours in order, the right side a seeded permutation of the same colours, `select(left)`, `connect(right)` succeeds only when colours match, `done()`.
- `WeldBoard(long seed, int seams, int width, int height)`: each seam is a polyline of points (a seeded gentle curve across the area); `cover(x, y)` welds points within `RADIUS`; a seam is done at 90 percent, the board when every seam is; seams are played one after another.
Seeds come from the workshop position and the step index, so a reopened GUI shows the same layout. The boards live only on the client; the server never needs them, which is fine because the build cannot fail.

### D3. Workshop state and locks
`ShuttleWorkshopBlockEntity`: `boolean handBuild` and `int buildStep` (saved as `HandBuild` and `BuildStep`). `startHandBuild()` needs `canBuild()`; `reportStep(int)` advances through `HandBuild.next`; `cancelHandBuild()` clears both; `engrave(String)` needs every step done, then runs `buildNow()` and sets the plate. The shuttle handler no longer resets anything in `onContentsChanged` (fixing the reset on install); `buildNow()` and `cancel` reset. `buildStep` clamps to the step count on load and after a datapack reload changed the steps.
Locks: `handBuild` makes `isItemValid` false and `extractItem` return empty on both handlers (so hoppers and shift clicks cannot move them either), the install slot refuses, buttons 0 to 3 return false. `onRemove` still drops both handlers; the progress goes with the block.

### D4. Menu, buttons, payloads
Buttons through `clickMenuButton`: 4 starts a build by hand, 5 cancels. Two client to server payloads, registered in `ServusStoragePayloads` and handled only when `player.containerMenu instanceof ShuttleWorkshopMenu`: `ShuttleBuildStepPayload(int step)` and `EngraveShuttlePayload(String name)`. Two `DataSlot`s carry `handBuild` and `buildStep` to the client. The name rule is pure: `NamePlate.clean(String)` strips formatting codes (the section sign and the character after it) and control characters, trims, and cuts to 20 characters.

### D5. The work panel
`ShuttleWorkshopScreen` grows to 320 wide: the old 176 wide layout stays on the left, a 136 by 196 panel on the right. The panel always draws the shuttle large (`GuiGraphics.renderItem` under a pose scaled by 5, so the composed look shows), the plate name under it, and at the bottom Build by hand, or Cancel while a build runs. During a build the panel shows "Step n of m" and the step's hint and draws the board over the shuttle: bolts as dots (done grey, current pulsing yellow, waiting dim), wires as eight coloured contacts on two sides with drawn connections, weld as a dim seam that turns bright where welded. Clicks and drags in the panel go to the board; when it is done the screen sends the step payload once. After the last step an `EditBox` (max 20) and Engrave replace the board; Enter engraves, and `keyPressed` keeps the inventory key from closing the screen while the box has focus, as `ZoneLabelScreen` does. Colours of the wires come from Foundation's `DuctChannels.color(index)` (white, orange, magenta, light blue, yellow, lime, pink, cyan), drawn as fills.

### D6. Name plate component
`ServusStorageComponents.SHUTTLE_NAME`: `DataComponentType<String>` with `Codec.STRING` and `ByteBufCodecs.STRING_UTF8`, id `shuttle_name`. `transmuteCopy` carries it through Build now. `ShuttleItem.getName(stack)` returns `item.servus_storage.shuttle.named` ("%s (%s)", plate and tier name) when a plate is present, so the hover name, the garage slots and every inventory show it; an anvil `CUSTOM_NAME` still wins in `getHoverName`, as vanilla does. A plate on a Mk1 never happens because only builds write it.

### D7. No plate text in the world
Decided in the play check: the plate is not drawn on the shuttle model; it shows only in the item name, the garage GUI and the Field Head. `ShuttleGarageRenderer` draws no text.

### D8. Field Head names the worker
`ShuttleFleet.fieldWorker(BlockPos head)` gives the item of the shuttle whose run serves that head; `WarehouseNetwork.fieldWorker` and `FieldHeadBlockEntity.workerName()` pass the plate on (empty without one). `FieldHeadMenu` keeps the last name it sent and, on the server in `broadcastChanges`, sends `FieldWorkerPayload(String name)` to its player when it changes (once on open). The client keeps it in `ClientFieldWorker` and `FieldHeadScreen` shows "Berta is working the field" instead of the plain working line when the reason is WORKING and a name is known. There is no goggles readout for shuttles, so nothing to add there.

### PLAN.md open decisions
None of section 5 is settled here. This change settles the step kinds, that the boards are client only and that a plate has at most 20 characters.

## Pure logic and tests

- JUnit in `storage/src/test/.../shuttle/module/build/`: `HandBuildTest` (accepts only the current step, done at the end, out of range ignored), `BoltBoardTest` (count spots, far click ignored, order enforced, done, same seed same layout, spots apart), `WireBoardTest` (permutation of the same colours, wrong pair refused, done after all, seed stable), `WeldBoardTest` (cover far away does nothing, a sweep along the seam finishes it, seams one after another). `NamePlateTest` (formatting codes stripped, trimmed, cut to 20, empty stays empty). `ShuttleTiersTest` and a reader test for steps (defaults, clamps, unknown kind dropped).
- GameTests in `ShuttleModuleGameTests`: `handBuildLocksAndCancels`, `handBuildStepsInOrder` (out of order report ignored), `engraveFinishesWithPlate`, `buildNowKeepsPlate`, `handBuildSurvivesSaveAndLoad` (save and load the block entity tag), `installKeepsBuildStep` (the reset fix, outside a build), `fieldHeadNamesWorker` if the harvester GameTest fixture makes it cheap, else covered by the play check.
- Manual: the play check at the end of M10.

## Risks / Trade-offs

- The client could report every step at once; that only skips a minigame that has no reward but a name, so the server checks order, not timing.
- A 320 wide GUI is wide on GUI scale 4 at 1280 pixels; it still fits at 1920 by 1080 with scale 4 (480 scaled pixels).
- Plate text on a moving shuttle can flicker at a distance; `POLYGON_OFFSET` and the small outward offset keep it off the hold face.

## Migration Plan

Unreleased: none.
