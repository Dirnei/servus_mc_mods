# Tasks: m13-fluid-rack

## 1. Pure rules

- [ ] 1.1 Add `FluidRackSection` in `shuttles/.../fluidrack/FluidRackSection.java`: `portIndex`, `shares`, `canJoin`. Add `FluidRackSectionTest` with these cases:
  - the port is at index 0 for one bay, 0 for two and 1 for three;
  - shares split evenly, with the remainder on the left bays;
  - a zero total gives zero shares;
  - a different fluid cannot join, while empty or the same fluid can.

  Verify with `gradlew :shuttles:test`.
- [ ] 1.2 Rework `FluidRouting.Candidate` to `(preferred, holds, accepts, room, cost)` and `FluidRestockPlan.sources` to the same view. Update `FluidRoutingTest` and `FluidRestockPlanTest`, and add these cases:
  - a preferred rack beats an empty tank on Any;
  - a preferred tank and a preferred rack at the same rank are chosen by the shorter way;
  - a non-accepting rack is skipped;
  - a rack source is picked by the most free fluid.

  Verify with `gradlew :shuttles:test`.

## 2. Data and the fluid store

- [ ] 2.1 Add `data/servus_shuttles/shuttles/fluid_rack.json` and `tanker_pump_ticks` in `warehouse.json`. Read them in `ShuttlesStatsManager` (`fluidRackCapacityPerBay`, `fluidRackBaysPerSection`, `fluidRackLevelSteps`, `tankerPumpTicks`). Remove `pump_ticks` from `gantry.json` and `gantryPumpTicks`, and switch `ShuttleFleet.holdForPump` to `tankerPumpTicks`. Verify with `gradlew :shuttles:build`; the existing tanker GameTests stay green.
- [ ] 2.2 Rename `TankRef` to `FluidStoreRef(pos, tank)` and update `FleetSaves` (key `"Store"`), `FluidJob`, `Shuttle` and `ShuttleFleet`. Give `TankView` a `kind` and `preferred`/`accepts` derived from gantry locks. Rename `readGantries` to `readFluidStores`. Verify with `gradlew :shuttles:build` and `gradlew :shuttles:runGameTestServer`; the tanker tests stay green.

## 3. The block

- [ ] 3.1 Add `NodeKind.FLUID_RACK`. Update the `WarehouseConnectionRule` links (rail only, its own section) and `StorageNodeBlockEntity.docksRail`. Add a `WarehouseConnectionRuleTest` case: a fluid rack links only to rails and to its own section. Verify with `gradlew :shuttles:test`.
- [ ] 3.2 Add `FluidRackBlock`, `FluidRackBlockEntity`, `FluidRackItem` and `FluidRackRecipe` in `shuttles/.../fluidrack/`, and register block, block entity, item, recipe serializer and creative tab entry in `shuttles/.../registry/`. Cover placement joins with the fluid rule, the `PORT` flag on section changes, the share rebalance in `onPlace` and on removal, wood memory, the drop with share and zone, sneak carrot dismantle, the ladder sides, and buckets through `FluidUtil.interactWithFluidHandler` with no GUI. Verify with `gradlew :shuttles:build`.
- [ ] 3.3 Add `FluidRackSectionHandler`, the `Capabilities.Fluid.BLOCK` registration in `ServusShuttlesEvents` (standalone only), and capability refresh on port rail changes. Add `FluidRackGameTests` for these cases:
  - three bays form one section with the port in the middle;
  - the port of two is on the left;
  - the port moves when a bay breaks;
  - lava does not join water;
  - stacked rows stay apart;
  - a standalone bay is filled by a duct;
  - a rail at the port closes pipe access;
  - a rail at another bay does not join;
  - a bucket fills and empties;
  - an empty hand opens nothing;
  - dismantle keeps the share;
  - breaking a bay drops its share.

  Register them in `ShuttlesGameTests`. Verify with `gradlew :shuttles:runGameTestServer`.

## 4. Network, zones and tanker

- [ ] 4.1 Wire rack sections into `WarehouseNetwork`:
  - the `fluidRacks` set and `FluidChanged` for sections;
  - `readFluidStores` for racks and `dockRails` to the port rail;
  - `routeFluid` and `restockSources` candidates;
  - `storageFill` tanks and the `fluids` command listing.

  Add GameTests for these cases:
  - the section is stock only with a port rail;
  - a bucket change reaches the index;
  - the command lists racks;
  - the tanker puts away from an Import gantry into a rack;
  - a restock goes from a rack to an Export gantry;
  - a run whose target rack's port bay breaks re-routes;
  - storage fill counts racks.

  Verify with `gradlew :shuttles:runGameTestServer`.
- [ ] 4.2 Zones:
  - `ZoneLabelItem.useOn` stamps and clears whole sections;
  - joining bays copy the zone;
  - `ZoneFilter.fluids` reads container contents;
  - the section handler uses the zone filter as its validator;
  - routing marks zoned racks listing the fluid as preferred.

  Add GameTests for these cases:
  - a stamp zones all bays;
  - a sneak stamp clears;
  - a water-zoned rack refuses lava by bucket and by tanker;
  - a zone listing bread takes any fluid;
  - water prefers the water-zoned rack.

  Verify with `gradlew :shuttles:runGameTestServer`.
- [ ] 4.3 Tanker coupling:
  - `shuttle.coupling` set and cleared in `holdForPump` and `endPumping` for rack stops;
  - `ShuttleView` sync;
  - `TankerLook` coupling boxes and `ShuttleModels.tanker()` recesses;
  - the `ShuttleGarageRenderer` animation toward the rack side.

  Add a GameTest: while a tanker pumps at a rack, its shuttle view carries the coupling side toward the rack, and it is cleared after. List every overlapping box pair of the tanker and the coupling with its plane check in the task notes. Verify with `gradlew :shuttles:runData`, `gradlew :shuttles:build` and `gradlew :shuttles:runGameTestServer`.

## 5. Look, controller, docs

- [ ] 5.1 Add `FluidRackModels` datagen (posts, beams, boards with the wood skin, glass pane, port flange) and the `FluidRackRenderer` (fluid level in steps, zone name on the port bay's top beam). Move the gantry column drawing to a shared `FluidColumn` helper used by both renderers. Add lang, loot and the pickaxe tag. List the overlapping box pairs and their plane checks in the task notes. Verify with `gradlew :shuttles:runData` and `gradlew :shuttles:build`.
- [ ] 5.2 Controller and Production Terminal: `StorageRow` counts `fluidRacks`; the tooltips name gantries and racks; rename `storage.no_gantry` to `storage.no_fluid_storage`. Update `StorageFillGameTests` with the rack case. Verify with `gradlew :shuttles:runGameTestServer`.
- [ ] 5.3 Document the Fluid Rack, the zone fluid filter and the tanker coupling in `docs/src/content/docs/mods/shuttles.md`. Run `gradlew :alchemy:runData` to refresh `docs/src/data/gamedata.json`, then verify the docs site builds.

## 6. Integration

- [ ] 6.1 Full gradle build, tests green, and the game launches with all mods. In game:
  - build a three bay section and a stacked row;
  - stamp a zone;
  - fill one rack by bucket and one by tanker from an Import gantry;
  - watch the coupling and the glass level from several angles for flicker;
  - break the port bay during a run;
  - read the potato report and the controller Fluids line.

## Workflow follow-up

- Stacked after `m13-fluid-side-modes` and before `m13-fluid-inbound` and `m13-fluid-outbound`.
- On archive, add an Added line for the Fluid Rack and a Changed line for zones and the tanker under `## Unreleased` in `shuttles/CHANGELOG.md`.
