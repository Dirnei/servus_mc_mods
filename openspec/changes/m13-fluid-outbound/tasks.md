# Tasks: m13-fluid-outbound

## 1. Fluid Outbound

- [x] 1.1 Add `FluidKeep` (pure: fluid key, amount, `clamp(capacity)`, `accepts(key)`) in `shuttles/.../fluidstation/`. Add `FluidKeepTest` with these cases:
  - an amount is clamped to 0 and to the capacity;
  - a tank with no keep accepts nothing;
  - a tank with a keep accepts only its fluid.

  Verify with `gradlew :shuttles:test`.
- [x] 1.2 Add `FluidOutboundBlock` and `FluidOutboundBlockEntity`:
  - `OUTBOUND_MODES`, BACK on Push, FRONT locked;
  - keeps saved, dropped and synced;
  - the tank validator from the keep;
  - push through `FluidSideMover`.

  Register block, block entity, item, creative tab and the per-block fluid capability. Add `FluidOutboundGameTests` for these cases:
  - placement;
  - break keeps the keep row;
  - the keep fluid guards the tank;
  - a cleared keep keeps its content;
  - Push into a Fluid Tank;
  - Output to a duct on pull;
  - no fill through any side;
  - not stock.

  Verify with `gradlew :shuttles:runGameTestServer`.
- [x] 1.3 Add `FluidOutboundMenu`, `FluidOutboundScreen` and `SetFluidKeepPayload` (registered in `ServusShuttlesPayloads`): keep cells, steppers, gauges with keep marks, the fluids-only side panel and the state line. Add buckets by hand and the potato report. Add GameTests for these cases:
  - setting a keep by payload with a water bucket;
  - stepping is clamped;
  - an empty bucket takes water;
  - the potato report lists keeps.

  Verify with `gradlew :shuttles:runGameTestServer`.

## 2. Restock

- [x] 2.1 Switch `restockFluids` to Outbound tanks with a keep, and filter `restockSources` to rack sections. Set the Outbound lamp through `FluidActivity` (missing, no tanker, working, idle). Bring cargo back when the Outbound is removed mid run. Add GameTests for these cases:
  - a rack restocks the Outbound;
  - runs on the way count;
  - below min load waits;
  - Mercury missing gives a red lamp;
  - no tanker gives a red lamp;
  - stock appearing restarts runs;
  - the Outbound broken mid run returns the cargo to a rack;
  - an Inbound is no restock source.

  Verify with `gradlew :shuttles:runGameTestServer`.

## 3. Remove the gantry

- [x] 3.1 Rebuild `TankerFixtures` on Fluid Inbound, Fluid Rack and Fluid Outbound, and port every case of `TankerRunGameTests`, `TankerLampGameTests` and `TankerRestockGameTests` to it. Move `GantryFixtures.shape` to `RailFixtures.shape` for `ElevatorGameTests`. Verify with `gradlew :shuttles:runGameTestServer`; every ported test passes while the gantry still exists.
- [x] 3.2 Move `FluidRouting`, `FluidPutAwayPlan`, `FluidRestockPlan` and `FluidStoreRef`, with their tests, to `shuttles/.../fluid/`. Verify with `gradlew :shuttles:test`.
- [x] 3.3 Delete the gantry:
  - the package rest, menu, screen, renderer, `GantryModels`, `SetGantryLockPayload`, `GANTRY_CONTENTS`, `FluidLockEntries`;
  - the registry entries and the creative tab entry;
  - `NodeKind.GANTRY` and its rule branches, updating `WarehouseConnectionRuleTest`;
  - the network gantry sets, views and host methods;
  - the fleet spout state and save fields;
  - `gantry.json` and its stats fields, the textures, `tools/gantry-textures/`, and the gantry lang keys;
  - the four gantry GameTest classes and `GantryFixtures`, with their `ShuttlesGameTests` entries.

  Update the tanker tooltip to name the new blocks. Verify with `gradlew :shuttles:runData`, `gradlew build` and `gradlew :shuttles:runGameTestServer`, and confirm that `grep -ri gantry shuttles/src` finds nothing.

## 4. Look, controller, docs, plan

- [x] 4.1 Add the Outbound models (amber band variant of `FluidStationModels`), blockstates with lamp variants, the recipe, loot, lang and the pickaxe tag. List the overlapping box pairs and their plane checks in the task notes. Verify with `gradlew :shuttles:runData` and `gradlew :shuttles:build`.
- [x] 4.2 Remove the gantry count from `StorageRow`, the controller and the Production Terminal tooltips, and from `ServusShuttlesCommands.fluids`. Update `StorageFillGameTests` (stations left out). Verify with `gradlew :shuttles:runGameTestServer`.
- [ ] 4.3 Docs:
  - in `docs/src/content/docs/mods/shuttles.md`, replace the Fluid Gantry with the Fluid Outbound and describe the full chain;
  - verify that the docs site builds.
  - Deferred to release: `docs/` describes only released mod versions.
- [x] 4.4 Groom PLAN.md M13: remove the gantry sentence and the Export red lamp point, and keep the other open points. Verify by reading the section.

## 5. Integration

- [ ] 5.1 Full gradle build, tests green, and the game launches with all mods. In game, run the whole chain: pump, Fluid Inbound, Tanker, Fluid Rack, Tanker, Fluid Outbound, machine. Check:
  - the keep row and lamps;
  - the coupling at all three port kinds, from several angles, for flicker;
  - that an old world with a gantry loads (the gantry is gone, and nothing crashes).

## 6. Play check decisions: one tank, the item station on a tank, wood

- [x] 6.1 One keep. `FluidOutboundBlockEntity` keeps one `FluidKeep` (`keep()`, `setKeepFluid`, `stepKeep`), saved as `KeepFluid` and `KeepAmount`; `FluidOutboundMenu` and `FluidOutboundScreen` show one keep cell, one gauge with its mark and one set of step buttons; `SetFluidKeepPayload` carries no tank; the potato line adds the keep. Update `FluidOutboundGameTests` (keep, cleared keep, push, output, keep payload, clamp at 16,000 mB, bucket, potato) and `TankerRestockGameTests`. Verify with `gradlew :shuttles:build` and `gradlew :shuttles:runGameTestServer`.
- [x] 6.2 Recipe and look. The recipe is an Outbound Station on top of a Fluid Tank as a wood material recipe; the upper half uses the Outbound Station models by lamp state on the shared tank (`FluidStationModels`). Add to the recipe GameTest that a spruce Outbound Station over a Fluid Tank gives a spruce Fluid Outbound. Verify with `gradlew :shuttles:runData`, `gradlew :shuttles:build` and `gradlew :shuttles:runGameTestServer`.

## 7. Keep picker

- [x] 7.1 Add `foundation/.../client/PickerEntries` (`sourceFluids()`, `nameOr`) and build `FilterPickerEntries` on it. Verify with `gradlew :foundation:build`.
- [x] 7.2 `SetFluidKeepPayload` carries `Optional<Identifier>`; `FluidOutboundMenu.pickKeep` clears on empty and sets only a registered source fluid (`keepableFluid`); `ServusShuttlesPayloads` calls it. Remove `setKeepFromCarried`. Verify with `gradlew :shuttles:compileJava`.
- [x] 7.3 `FluidOutboundScreen`: replace the keep cell with an `ItemPicker` ("Keep nothing" plus `PickerEntries.sourceFluids()`), the filled container shortcut on the picker button, every input to the open popup only. Lang keys `keep_picker` and `keep_none` replace `keep_cell` and `keep_cell.empty` in `ServusShuttlesLanguageProvider`. Verify with `gradlew :shuttles:runData` and `gradlew :shuttles:build`.
- [x] 7.4 `FluidOutboundGameTests`: `keepSetByPicker` (water, then nothing clears), `unknownKeepFluidRefused` (unknown id, `flowing_lava`, `empty`), `keepPayloadRoundTrip` (codec with and without an id), replacing `keepSetByCellWithBucket`. Verify with `gradlew :shuttles:runGameTestServer`.

## Workflow follow-up

- Stacked last, after `m13-fluid-side-modes`, `m13-fluid-rack` and `m13-fluid-inbound`.
- On archive:
  - edit the Purpose of `openspec/specs/shuttles/tanker/spec.md` by hand (text in design.md);
  - delete the emptied `openspec/specs/shuttles/fluid-gantry/` folder;
  - add Added (Fluid Outbound) and Changed lines under `## Unreleased` in `shuttles/CHANGELOG.md`: "Removed the Fluid Gantry; empty your gantries before updating, placed gantries and their fluid are lost."

## Implementation notes

- Build and unit tests are green (foundation 549, shuttles 955, alchemy 267, delicacies 87, brewery 72), and all 1078 GameTests pass, 12 of them in the new `FluidOutboundGameTests`. 5.1 waits for the in game play check.
- Names and decisions that differ from the design:
  - (Replaced by the keep picker in 7.) The keep cell click sends `SetFluidKeepPayload(tank)` only; the server reads the fluid from the cursor stack itself (`Transfer.fluidOf`, which uses `Capabilities.Fluid.ITEM`), so the client never names the fluid. The amount steps are menu buttons (`FluidOutboundMenu.keepButton`), not part of the payload.
  - Foundation has no number stepper widget (the gantry drew its own buttons), so the -1 B, +1 B, -10 B and +10 B buttons are plain buttons in a 2 by 2 grid under each gauge. The PLAN.md point on Foundation widgets for keep steps now names the Fluid Outbound.
  - `FluidKeep<K>` is a generic record (fluid or null, amount) with `clamp`, `step`, `accepts`, `withFluid` and `target`.
  - The station base gained `tankTakes(tank, stack)`, used as each `FluidSlot`'s validator and by `sideTankAccepts`, so buckets, sides and tankers all respect the keep fluid. The Outbound's tanks are `TankRole.ANY`: its modes (Push, Output, Off) already allow no fill from a side, and buckets by hand must fill.
  - The shared GUI moved into `FluidStationMenu` and `FluidStationScreen`; `FluidInboundMenu`/`FluidInboundScreen` and the new `FluidOutboundMenu`/`FluidOutboundScreen` extend them. The waiting fluid data slot carries the no room fluid on the Inbound and the missing fluid on the Outbound; the Outbound's state key is `gui.servus_shuttles.fluid_station.state.missing`.
  - `restockFluids` now also adds an Outbound that lacks fluid and that no Tanker reaches to the no tanker set, which closes the PLAN.md open point. `restockSources` needs no rack filter of its own: with the gantry gone `fluidStores()` holds only Fluid Rack sections.
  - With every fluid store a port, `fluidStoreDocks`, `gantryPumping`, `Shuttle.pumpingIntoGantry`, `StorageNodeBlockEntity.bendsRail`, `WarehouseNetwork.StoreKind` and the lock and mode of `TankView` went with the gantry, and `FluidActivityHolder` was folded into `FluidStationBlockEntity`, its only implementer. `FleetSaves` had no gantry fields.
  - `FluidRole` moved to `shuttles/.../fluid/` with the planners (without `of(GantryMode)`), and `GantryLook.level` became `fluid/FluidLevel.step` with `FluidLevelTest`; `TankerLook.capTopInBlock` (gantry spout only) was removed.
  - `RailFixtures` holds `railAlongZ` and `shape` from `GantryFixtures`.
  - Loot: like the Fluid Inbound the block has `noLootTable()`; the block entity drops the item with tanks, keeps and sides.
- 3.1 and 3.3 were done in one pass, so the ported suites ran once the gantry was already gone rather than beside it; coverage is checked case by case below.
- Tanker tests: `TankerFixtures` puts the Fluid Inbound and the Fluid Outbound east of the rail facing it and one bay Fluid Rack sections west of it; locks became water zones. Not ported, because a test elsewhere covers the same case: `smallAmountWaits` (`FluidInboundTankerGameTests.smallAmountWaitsOrange`), `noRoomNoRunRedLamp` (`FluidInboundTankerGameTests.redLampNoRoom`) and `targetModeChangedReroutes` (gantry mode only; `FluidRackTankerGameTests.brokenTargetRackReroutes` covers the reroute). `importIsNoRestockSource` became `inboundIsNoRestockSource`; new are `noTankerRedLamp` and `outboundBrokenMidRunReturnsCargo`. `restockLeftoverBackToRack` keeps the same amounts within the station's 8000 mB tanks.
- `openspec validate` notes that the `Coupling at a port` delta of `shuttles/tanker` modifies a requirement the main spec gets only when `m13-fluid-rack` and `m13-fluid-inbound` are archived; archive those first.
- 4.1 overlapping box pairs: the Fluid Outbound uses the Fluid Inbound's boxes unchanged (`FluidStationModels`), only the `#band` texture differs (orange concrete instead of cyan), so every pair and plane check listed in `m13-fluid-inbound` tasks.md 4.1 holds as is. The window row models are now generated once for both stations (`FluidStationModels.windowRows`) and are byte for byte the same as before.
- In game play check for the user:
  - craft a Fluid Outbound (Fluid Inbound pattern with orange dye), place it next to a Fluid Inbound and check the amber against the teal band, the item in hand and in the GUI;
  - set keeps in the GUI: a water bucket on the cursor on the first cell (the bucket stays), the step buttons, the gauge mark, the amount label, an empty hand clearing a cell, the side panel labels showing the kept fluid of an empty tank;
  - run the whole chain: Electric Pump into a Fluid Inbound, a Tanker to a Fluid Rack, a Tanker restocking the Outbound, the Outbound's back pushing into a machine or Fluid Tank;
  - lamps: green when every keep is met, orange while runs go, red with the GUI line for a fluid missing from the racks and for no Tanker;
  - the tanker's coupling at all three port kinds, from several angles and while it slides, for flicker;
  - an old world with a Fluid Gantry loads without a crash and the gantry is gone.

## Implementation notes (play check revision)

- 6.1 and 6.2 are built together with `m13-fluid-inbound` 6.1 to 6.4 (shared base, models and tests); build, unit tests and all GameTests are green, see the notes there.
- The upper half is the Outbound Station's own models by lamp state on the shared `fluid_station_tank`; its plane check is the one listed in `m13-fluid-inbound` tasks.md.
- The play check notes above (orange dye, amber band) are outdated; the play check list of `m13-fluid-inbound` covers both stations now.

## Implementation notes (keep picker)

- The keep cell went back to the gantry's searchable dropdown at the user's request. The removed `FluidLockEntries` is not restored: the picker lists source fluids from the new Foundation `PickerEntries`, which the duct fluid filter picker now shares, so fluids show their still texture rather than a bucket and fluids without a bucket are listed too.
- Play check: open the picker, search, pick Water and "Keep nothing"; click the picker button with a lava bucket on the cursor (keeps lava, no popup, bucket stays); with the popup open the step buttons, side panel and slots do not react.
