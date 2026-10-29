# Tasks: m13-fluid-rack

## 1. Pure rules

- [x] 1.1 Add `FluidRackSection` in `shuttles/.../fluidrack/FluidRackSection.java`: `portIndex`, `shares`, `canJoin`. Add `FluidRackSectionTest` with these cases:
  - the port is at index 0 for one bay, 0 for two and 1 for three;
  - shares split evenly, with the remainder on the left bays;
  - a zero total gives zero shares;
  - a different fluid cannot join, while empty or the same fluid can.

  Verify with `gradlew :shuttles:test`.
- [x] 1.2 Rework `FluidRouting.Candidate` to `(preferred, holds, accepts, room, cost)` and `FluidRestockPlan.sources` to the same view. Update `FluidRoutingTest` and `FluidRestockPlanTest`, and add these cases:
  - a preferred rack beats an empty tank on Any;
  - a preferred tank and a preferred rack at the same rank are chosen by the shorter way;
  - a non-accepting rack is skipped;
  - a rack source is picked by the most free fluid.

  Verify with `gradlew :shuttles:test`.

  Note: `Candidate` is `(id, preferred, holds, accepts, room, cost)`; the pure factory `Candidate.of` derives holds, accepts and room from the content, the lock or zone and the runs on their way, so that planning stays unit tested. `FluidRestockPlan.Source` already was such a view and stays.

## 2. Data and the fluid store

- [x] 2.1 Add `data/servus_shuttles/shuttles/fluid_rack.json` and `tanker_pump_ticks` in `warehouse.json`. Read them in `ShuttlesStatsManager` (`fluidRackCapacityPerBay`, `fluidRackBaysPerSection`, `fluidRackLevelSteps`, `tankerPumpTicks`). Remove `pump_ticks` from `gantry.json` and `gantryPumpTicks`, and switch `ShuttleFleet.holdForPump` to `tankerPumpTicks`. Verify with `gradlew :shuttles:build`; the existing tanker GameTests stay green.
- [x] 2.2 Rename `TankRef` to `FluidStoreRef(pos, tank)` and update `FleetSaves` (key `"Store"`), `FluidJob`, `Shuttle` and `ShuttleFleet`. Give `TankView` a `kind` and `preferred`/`accepts` derived from gantry locks. Rename `readGantries` to `readFluidStores`. Verify with `gradlew :shuttles:build` and `gradlew :shuttles:runGameTestServer`; the tanker tests stay green.

## 3. The block

- [x] 3.1 Add `NodeKind.FLUID_RACK`. Update the `WarehouseConnectionRule` links (rail only, its own section) and `StorageNodeBlockEntity.docksRail`. Add a `WarehouseConnectionRuleTest` case: a fluid rack links only to rails and to its own section. Verify with `gradlew :shuttles:test`.
- [x] 3.2 Add `FluidRackBlock`, `FluidRackBlockEntity`, `FluidRackItem` and `FluidRackRecipe` in `shuttles/.../fluidrack/`, and register block, block entity, item, recipe serializer and creative tab entry in `shuttles/.../registry/`. Cover placement joins with the fluid rule, the `PORT` flag on section changes, the share rebalance in `onPlace` and on removal, wood memory, the drop with share and zone, sneak carrot dismantle, the ladder sides, and buckets through `FluidUtil.interactWithFluidHandler` with no GUI. Verify with `gradlew :shuttles:build`.
- [x] 3.3 Add `FluidRackSectionHandler`, the `Capabilities.Fluid.BLOCK` registration in `ServusShuttlesEvents` (standalone only), and capability refresh on port rail changes. Add `FluidRackGameTests` for these cases:
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

  Notes:
  - A placed bay settles its section (zone, shares, port flags, links, capabilities) on the block tick right after `onPlace`, not in `onPlace` itself: a placed item's share and zone reach the block entity only after `onPlace`, so a rebalance there would be overwritten. Removal settles at once in `affectNeighborsAfterRemoval`.
  - The drop is made in code like the Fluid Gantry's (`preRemoveSideEffects`, nothing for a creative break), so there is no loot table: a loot table cannot carry the share.
  - The duct case checks the fluid capability on every side, the duct's fluid arm and a fill through the capability; it does not pump through a duct, which would depend on the Fluids side layer changing in `m13-fluid-side-modes`.
  - "Left" means a player's left in front of the rack: `FluidRackBlock.section` lists bays from that left. The block's `LEFT`/`RIGHT` flags keep the Heavy Duty Rack's meaning (counter clockwise and clockwise of the facing).

## 4. Network, zones and tanker

- [x] 4.1 Wire rack sections into `WarehouseNetwork`:
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
- [x] 4.2 Zones:
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
- [x] 4.3 Tanker coupling:
  - `shuttle.coupling` set and cleared in `holdForPump` and `endPumping` for rack stops;
  - `ShuttleView` sync;
  - `TankerLook` coupling boxes and `ShuttleModels.tanker()` recesses;
  - the `ShuttleGarageRenderer` animation toward the rack side.

  Add a GameTest: while a tanker pumps at a rack, its shuttle view carries the coupling side toward the rack, and it is cleared after. List every overlapping box pair of the tanker and the coupling with its plane check in the task notes. Verify with `gradlew :shuttles:runData`, `gradlew :shuttles:build` and `gradlew :shuttles:runGameTestServer`.

  Notes:
  - The sync carries `Coupling(side, ticks, elapsed)` like `Felling`: clients ease the coupling out over `COUPLING_TICKS`, hold it and ease it back over the last ticks of the stop (`TankerLook.extension`, unit tested), so only the stop's start and end are sent.
  - The coupling sits at model y 13, z 5.5, between the front band and the sight glass frame: the tank side has no free spot at its middle (z 8 is the sight glass, below it the chassis plate at y 10 to 11). So it is 2 px off the bay centre along the rail, either way by the tanker's heading, and the rack's port flange is 6 px wide (x 5 to 11) to meet it both ways. Port centre height is `TankerLook.couplingYInBlock()` = 12.4 px.
  - Overlapping boxes, model pixels, left side (the right side mirrors it at x = 16 - x):
    - collar (x 3.9 to 4.7, y 12.3 to 13.7, z 4.8 to 6.2) and tank box 1 (x 4.5 to 11.5, y 11.25 to 14.75, z 2.6 to 13.4): planes x 3.9/4.7, y 12.3/13.7, z 4.8/6.2 against x 4.5/11.5, y 11.25/14.75, z 2.6/13.4, none shared.
    - collar and front band (x 4.3 to 11.7, y 11.05 to 14.95, z 4 to 5): against x 4.3/11.7, y 11.05/14.95, z 4/5, none shared. The inner band boxes start at x 4.8 and 5.8 and do not overlap the collar.
    - collar and sight glass frame (x 4.2 to 4.6, y 11.75 to 14.25, z 6 to 10): against x 4.2/4.6, y 11.75/14.25, z 6/10, none shared. The collar ends at z 6.2, short of the drawn sight glass fluid at z 6.4.
    - collar and tank boxes 2 and 3, saddles, cap, chassis and hold: no overlap (they start at x 5 or higher, or lie below y 11 or outside z 4.8 to 6.2).
    - stowed head (x 3.95 to 4.65, y 12.35 to 13.65, z 4.85 to 6.15) inside the collar: every face 0.05 inside the collar's, so it is hidden and not drawn while stowed.
    - head and tube (y 12.55 to 13.45, z 5.05 to 5.95, x from head x0 + 0.3 to 4.4): the tube's y and z faces are 0.2 inside the head's and 0.25 inside the collar's; its ends lie inside the head and inside the collar (x 4.4 matches no other face).
    - while sliding, the head's two x faces pass the collar's x 3.9 plane, the band's x 4.3 plane and the frame's x 4.2 and 4.6 planes for a single frame each; they never rest there. Extended, the head's outer face ends 0.005 px short of the block edge, the rack's front plane.

- [x] 4.4 Rails along the rack:
  - add `StorageNodeBlockEntity.holdsRail(side)` (default `acceptsRail`), override it in `FluidRackBlockEntity` for the front of any bay, and use it in `RailBlock.isAnchor`;
  - in `RailSpan.check`, let a gap of a single free rail hold with one hold.

  Add `RailSpanTest` cases: a lone free rail beside one hung rail holds; two free rails with one hold fall; a four block gap between two hung rails still falls. Add GameTests: rails hang in front of all three bays of a raised fluid section with one dock; a rail one past a raised Heavy Duty Rack's end stays; a second rail beyond it drops all three. Verify with `gradlew :shuttles:build`, `gradlew :shuttles:test` and `gradlew :shuttles:runGameTestServer`.

## 5. Look, controller, docs

- [x] 5.1 Add `FluidRackModels` datagen (posts, beams, boards with the wood skin, glass pane, port flange) and the `FluidRackRenderer` (fluid level in steps, zone name on the port bay's top beam). Move the gantry column drawing to a shared `FluidColumn` helper used by both renderers. Add lang, loot and the pickaxe tag. List the overlapping box pairs and their plane checks in the task notes. Verify with `gradlew :shuttles:runData` and `gradlew :shuttles:build`.

  Notes (block pixels, built facing north; no loot table, see 3.3):
  - Frame parts only meet at their edges: back board z 15 to 16 against back posts (z 13 to 15), floor board and top plate (both end at z 15); floor board (z 2 to 15) against the front floor beam (z 0 to 2); side beams and rung (z 2 to 13) against the posts; core parts (x 2 to 14) against posts or extensions (x 0 to 2, 14 to 16). Touching faces face each other and never show.
  - Front glass (z 0.9 to 1.1, y 1.05 to 14.95, front and back faces only) is 0.05 off the floor beam (y 1), the top plate (y 15) and the posts (x 2, 14); a joined bay's glass runs on from x 2.05 to the block edge, meeting the core pane only at an edge.
  - Side glass (x 0.9 to 1.1 or 14.9 to 15.1, side faces only) is 0.05 off the side beams, the floor beam and the posts; where the rung (y 8 to 9) crosses it, its faces lie inside the rung (rung faces at x 0 and 2).
  - Port flange (x 5 to 11, y 10.9 to 13.9, z 0 to 1.5) passes through the front glass: the glass faces at z 0.9 and 1.1 lie inside the flange, whose faces are at z 0 and 1.5.
  - Fluid drawn by the renderer: x 2.1 to 13.9 (to the block edge where a bay joins, with no side face there), y 1.1 to at most 14.9, z 1.2 to 14.9: 0.1 off the posts, floor, top, back board and back posts and 0.1 behind the glass; the flange's back face (z 1.5) lies inside the fluid box, not on a face.
- [x] 5.2 Controller and Production Terminal: `StorageRow` counts `fluidRacks`; the tooltips name gantries and racks; rename `storage.no_gantry` to `storage.no_fluid_storage`. Update `StorageFillGameTests` with the rack case. Verify with `gradlew :shuttles:runGameTestServer`.
- [ ] 5.3 Document the Fluid Rack, the zone fluid filter and the tanker coupling in `docs/src/content/docs/mods/shuttles.md`. Verify that the docs site builds.

  Deferred to release: `docs/` describes only released mod versions.

## 6. Integration

- [ ] 6.1 Full gradle build, tests green, and the game launches with all mods. In game:
  - build a three bay section and a stacked row;
  - stamp a zone;
  - fill one rack by bucket and one by tanker from an Import gantry;
  - watch the coupling and the glass level from several angles for flicker;
  - break the port bay during a run;
  - read the potato report and the controller Fluids line.

  Automated part done: `gradlew build`, the unit tests and all 1083 Shuttles GameTests are green. The in-game launch and play check are left for the user. Runs need `-Pfoundation_min_version=0.4.0` until Foundation's dev `mod_version` (0.4.0) is raised to the 0.5.0 that `gradle.properties` now requires.

## Workflow follow-up

- Stacked after `m13-fluid-side-modes` and before `m13-fluid-inbound` and `m13-fluid-outbound`.
- On archive, add an Added line for the Fluid Rack and a Changed line for zones and the tanker under `## Unreleased` in `shuttles/CHANGELOG.md`.
