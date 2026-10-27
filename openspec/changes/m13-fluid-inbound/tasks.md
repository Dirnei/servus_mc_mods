# Tasks: m13-fluid-inbound

## 1. Station base

- [ ] 1.1 Add `fluid_station_tanks` and `fluid_station_tank_capacity` to `warehouse.json` and `ShuttlesStatsManager`, clamped to 1 to 6. Verify with `gradlew :shuttles:build`.
- [ ] 1.2 Add `FluidStationBlock` (abstract, `HALF`, forwarding, two-half removal, `PushReaction.BLOCK`) and `FluidStationItem` (placement check with messages, saved data) in `shuttles/.../fluidstation/`. Add `FluidStationBlockEntity` (abstract): tanks as `FluidSlot`s, a Fluids-only `SidedBlockEntity`, `"TankLabels"` in the update tag, `NodeKind.FLUID_STATION`, and front rail only. Update `WarehouseConnectionRule`, `docksRail` and `WarehouseConnectionRuleTest` (a fluid station links only to rails). Verify with `gradlew :shuttles:test` and `gradlew :shuttles:build`.

## 2. Fluid Inbound

- [ ] 2.1 Add `FluidInboundBlock` and `FluidInboundBlockEntity` (`INBOUND_MODES`, Input default, FRONT locked) and register block, block entity, item and creative tab entry. Register `Capabilities.Fluid.BLOCK` per block with the half-and-face to relative-side mapping and `FluidSideHandler`. Call `FluidSideMover.tick` at `push_interval` with both halves' neighbours. Add `FluidInboundGameTests` for these cases:
  - placement;
  - refused without room;
  - breaking either half removes both;
  - the drop keeps tanks and sides;
  - a pump fills two fluids into two tanks;
  - Pull fills only the chosen tank;
  - an Off side stays connected and moves nothing;
  - the front has no endpoint;
  - a duct on pull drains nothing.

  Register them in `ShuttlesGameTests`. Verify with `gradlew :shuttles:runGameTestServer`.
- [ ] 2.2 Add `FluidInboundMenu` and `FluidInboundScreen`: gauges, a fluids-only side panel with mode and tank buttons, and the state line through `StationStatePayload`. Register the menu and screen. Add buckets by hand (fill by All order, empty from the first tank with a container's worth) and the Data Potato report `FluidStationReport`. Add GameTests for the bucket into an empty station and the potato report. Verify with `gradlew :shuttles:runGameTestServer`.

## 3. Network and tanker

- [ ] 3.1 Add `fluidStations` to `WarehouseNetwork`, and `dockRails` for the front rail.
  - Generalise the put-away source loop in `tickFluids()` over gantry Import tanks and Inbound tanks with a `FluidSource` view.
  - Replace `FluidJob.sourceMode`/`targetMode` with the ref kind.
  - Move the lamp reasons to `FluidActivity` and `FluidActivityHolder`, used by the gantry and the station.

  Add GameTests for these cases:
  - the station joins only by its front rail;
  - it is not stock;
  - a pump-to-rack run;
  - a red lamp for no room;
  - a red lamp for no tanker;
  - the station broken mid run sends the tanker home empty.

  Keep `TankerRunGameTests` and `TankerLampGameTests` green. Verify with `gradlew :shuttles:runGameTestServer`.
- [ ] 3.2 Set the tanker coupling at station stops in `holdForPump` and `endPumping`. Add a GameTest: while pumping at the station, the shuttle view carries the coupling side toward it. Verify with `gradlew :shuttles:runGameTestServer`.

## 4. Look and docs

- [ ] 4.1 Add `FluidStationModels` datagen (both halves, teal band, sight glass recesses, lamp sockets, port flange) and blockstates with lamp variants. Add `FluidStationRenderer` for the sight glasses with `FluidColumn`, plus the recipe, loot, lang and pickaxe tag. List every overlapping box pair and its plane check in the task notes. Verify with `gradlew :shuttles:runData` and `gradlew :shuttles:build`.
- [ ] 4.2 Document the Fluid Inbound in `docs/src/content/docs/mods/shuttles.md`. Verify that the docs site builds.

## 5. Integration

- [ ] 5.1 Full gradle build, tests green, and the game launches with all mods. In game:
  - place a Fluid Inbound, feed it from a pump and a Fluid Tank on Pull;
  - set sides with the panel (left and right click) and the carrot;
  - watch a tanker dock and carry the fluid to a Fluid Rack;
  - check the lamps, the sight glasses, the markers on both halves, and flicker from several angles.

## Workflow follow-up

- Stacked after `m13-fluid-side-modes` and `m13-fluid-rack`, and before `m13-fluid-outbound`.
- On archive, add an Added line for the Fluid Inbound under `## Unreleased` in `shuttles/CHANGELOG.md`.
