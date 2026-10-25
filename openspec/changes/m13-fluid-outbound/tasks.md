# Tasks: m13-fluid-outbound

## 1. Fluid Outbound

- [ ] 1.1 Add `FluidKeep` (pure: fluid key, amount, `clamp(capacity)`, `accepts(key)`) in `shuttles/.../fluidstation/`. Add `FluidKeepTest` with these cases:
  - an amount is clamped to 0 and to the capacity;
  - a tank with no keep accepts nothing;
  - a tank with a keep accepts only its fluid.

  Verify with `gradlew :shuttles:test`.
- [ ] 1.2 Add `FluidOutboundBlock` and `FluidOutboundBlockEntity`:
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
- [ ] 1.3 Add `FluidOutboundMenu`, `FluidOutboundScreen` and `SetFluidKeepPayload` (registered in `ServusShuttlesPayloads`): keep cells, steppers, gauges with keep marks, the fluids-only side panel and the state line. Add buckets by hand and the potato report. Add GameTests for these cases:
  - setting a keep by payload with a water bucket;
  - stepping is clamped;
  - an empty bucket takes water;
  - the potato report lists keeps.

  Verify with `gradlew :shuttles:runGameTestServer`.

## 2. Restock

- [ ] 2.1 Switch `restockFluids` to Outbound tanks with a keep, and filter `restockSources` to rack sections. Set the Outbound lamp through `FluidActivity` (missing, no tanker, working, idle). Bring cargo back when the Outbound is removed mid run. Add GameTests for these cases:
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

- [ ] 3.1 Rebuild `TankerFixtures` on Fluid Inbound, Fluid Rack and Fluid Outbound, and port every case of `TankerRunGameTests`, `TankerLampGameTests` and `TankerRestockGameTests` to it. Move `GantryFixtures.shape` to `RailFixtures.shape` for `ElevatorGameTests`. Verify with `gradlew :shuttles:runGameTestServer`; every ported test passes while the gantry still exists.
- [ ] 3.2 Move `FluidRouting`, `FluidPutAwayPlan`, `FluidRestockPlan` and `FluidStoreRef`, with their tests, to `shuttles/.../fluid/`. Verify with `gradlew :shuttles:test`.
- [ ] 3.3 Delete the gantry:
  - the package rest, menu, screen, renderer, `GantryModels`, `SetGantryLockPayload`, `GANTRY_CONTENTS`, `FluidLockEntries`;
  - the registry entries and the creative tab entry;
  - `NodeKind.GANTRY` and its rule branches, updating `WarehouseConnectionRuleTest`;
  - the network gantry sets, views and host methods;
  - the fleet spout state and save fields;
  - `gantry.json` and its stats fields, the textures, `tools/gantry-textures/`, and the gantry lang keys;
  - the four gantry GameTest classes and `GantryFixtures`, with their `ShuttlesGameTests` entries.

  Update the tanker tooltip to name the new blocks. Verify with `gradlew :shuttles:runData`, `gradlew build` and `gradlew :shuttles:runGameTestServer`, and confirm that `grep -ri gantry shuttles/src` finds nothing.

## 4. Look, controller, docs, plan

- [ ] 4.1 Add the Outbound models (amber band variant of `FluidStationModels`), blockstates with lamp variants, the recipe, loot, lang and the pickaxe tag. List the overlapping box pairs and their plane checks in the task notes. Verify with `gradlew :shuttles:runData` and `gradlew :shuttles:build`.
- [ ] 4.2 Remove the gantry count from `StorageRow`, the controller and the Production Terminal tooltips, and from `ServusShuttlesCommands.fluids`. Update `StorageFillGameTests` (stations left out). Verify with `gradlew :shuttles:runGameTestServer`.
- [ ] 4.3 Docs:
  - in `docs/src/content/docs/mods/shuttles.md`, replace the Fluid Gantry with the Fluid Outbound and describe the full chain;
  - run `gradlew :alchemy:runData` to refresh `gamedata.json`;
  - verify that the docs site builds.
- [ ] 4.4 Groom PLAN.md M13: remove the gantry sentence and the Export red lamp point, and keep the other open points. Verify by reading the section.

## 5. Integration

- [ ] 5.1 Full gradle build, tests green, and the game launches with all mods. In game, run the whole chain: pump, Fluid Inbound, Tanker, Fluid Rack, Tanker, Fluid Outbound, machine. Check:
  - the keep row and lamps;
  - the coupling at all three port kinds, from several angles, for flicker;
  - that an old world with a gantry loads (the gantry is gone, and nothing crashes).

## Workflow follow-up

- Stacked last, after `m13-fluid-side-modes`, `m13-fluid-rack` and `m13-fluid-inbound`.
- On archive:
  - edit the Purpose of `openspec/specs/shuttles/tanker/spec.md` by hand (text in design.md);
  - delete the emptied `openspec/specs/shuttles/fluid-gantry/` folder;
  - add Added (Fluid Outbound) and Changed lines under `## Unreleased` in `shuttles/CHANGELOG.md`: "Removed the Fluid Gantry; empty your gantries before updating, placed gantries and their fluid are lost."
