# Tasks: m13-fluid-inbound

## 1. Station base

- [x] 1.1 Add `fluid_station_tanks` and `fluid_station_tank_capacity` to `warehouse.json` and `ShuttlesStatsManager`, clamped to 1 to 6. Verify with `gradlew :shuttles:build`.
- [x] 1.2 Add `FluidStationBlock` (abstract, `HALF`, forwarding, two-half removal, `PushReaction.BLOCK`) and `FluidStationItem` (placement check with messages, saved data) in `shuttles/.../fluidstation/`. Add `FluidStationBlockEntity` (abstract): tanks as `FluidSlot`s, a Fluids-only `SidedBlockEntity`, `"TankLabels"` in the update tag, `NodeKind.FLUID_STATION`, and front rail only. Update `WarehouseConnectionRule`, `docksRail` and `WarehouseConnectionRuleTest` (a fluid station links only to rails). Verify with `gradlew :shuttles:test` and `gradlew :shuttles:build`.

## 2. Fluid Inbound

- [x] 2.1 Add `FluidInboundBlock` and `FluidInboundBlockEntity` (`INBOUND_MODES`, Input default, FRONT locked) and register block, block entity, item and creative tab entry. Register `Capabilities.Fluid.BLOCK` per block with the half-and-face to relative-side mapping and `FluidSideHandler`. Call `FluidSideMover.tick` at `push_interval` with both halves' neighbours. Add `FluidInboundGameTests` for these cases:
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
- [x] 2.2 Add `FluidInboundMenu` and `FluidInboundScreen`: gauges, a fluids-only side panel with mode and tank buttons, and the state line through `StationStatePayload`. Register the menu and screen. Add buckets by hand (fill by All order, empty from the first tank with a container's worth) and the Data Potato report `FluidStationReport`. Add GameTests for the bucket into an empty station and the potato report. Verify with `gradlew :shuttles:runGameTestServer`.

## 3. Network and tanker

- [x] 3.1 Add `fluidStations` to `WarehouseNetwork`, and `dockRails` for the front rail.
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
- [x] 3.2 Set the tanker coupling at station stops in `holdForPump` and `endPumping`. Add a GameTest: while pumping at the station, the shuttle view carries the coupling side toward it. Verify with `gradlew :shuttles:runGameTestServer`.

## 4. Look and docs

- [x] 4.1 Add `FluidStationModels` datagen (both halves, teal band, sight glass recesses, lamp sockets, port flange) and blockstates with lamp variants. Add `FluidStationRenderer` for the sight glasses with `FluidColumn`, plus the recipe, loot, lang and pickaxe tag. List every overlapping box pair and its plane check in the task notes. Verify with `gradlew :shuttles:runData` and `gradlew :shuttles:build`.
- [ ] 4.2 Document the Fluid Inbound in `docs/src/content/docs/mods/shuttles.md`. Verify that the docs site builds.
  - Deferred to release: `docs/` describes only released mod versions.

## 5. Integration

- [ ] 5.1 Full gradle build, tests green, and the game launches with all mods. In game:
  - place a Fluid Inbound, feed it from a pump and a Fluid Tank on Pull;
  - set sides with the panel (left and right click) and the carrot;
  - watch a tanker dock and carry the fluid to a Fluid Rack;
  - check the lamps, the sight glasses, the markers on both halves, and flicker from several angles.

## 6. Play check decisions: one tank, the item station on a tank, wood

- [x] 6.1 One tank. Remove `fluid_station_tanks` (`ShuttlesStatsManager` field, clamp, `MAX_FLUID_STATION_TANKS`, `DEFAULT_FLUID_STATION_TANKS`) and raise `fluid_station_tank_capacity` to 16000 in `warehouse.json`. `FluidStationBlockEntity` keeps one `FluidSlot` (`tank()`), one side tank and one shown fluid; buckets use the tank directly (`FluidStationHandler` goes); `FluidStationReport` prints one line; `FluidStoreRef.station(pos)` names it; `WarehouseNetwork.putAwaySources` and the station checks drop the tank loops. `FluidStationMenu`, `FluidInboundMenu`, `FluidStationScreen`, `FluidInboundScreen`: one centred gauge, no tank numbers, no tank count in `writeOpening`. Update `FluidInboundGameTests` (one tank, a second fluid refused, Pull, potato line) and the tanker GameTests. Verify with `gradlew :shuttles:test`, `gradlew :shuttles:build` and `gradlew :shuttles:runGameTestServer`.
- [x] 6.2 Wood. Register both stations with `Material.register(..., MaterialKind.WOOD)` in `ServusShuttles`; give `FluidStationBlockEntity` a `MaterialSlot` (save, update tag, implicit components, model data); add `FluidStationTopBlockEntity` (upper half, a copy of the wood for its model) and register it; `FluidStationItem` extends `MaterialBlockItem`. Add the GameTest: a birch station dropped and placed again is birch on both halves and keeps its water and sides. Verify with `gradlew :shuttles:build` and `gradlew :shuttles:runGameTestServer`.
- [x] 6.3 Recipe and lamps. The recipe is an Inbound Station on top of a Fluid Tank as a wood material recipe (`ServusShuttlesRecipeProvider.fluidStation`), which keeps the station item's wood. `FluidStationBlockEntity.setBlockState` copies `ONLINE` and `ACTIVITY` to the upper half. Add GameTests: a spruce Inbound Station over a Fluid Tank gives a spruce Fluid Inbound (`BlockStyleGameTests`), the upper half shows the lamp state. Verify with `gradlew :shuttles:runData` and `gradlew :shuttles:runGameTestServer`.
- [x] 6.4 Look (concept H). `FluidStationModels`: the shared tank model `fluid_station_tank` (iron frame, floor, glass on four sides), the multipart block state with the Inbound Station models on the upper half by lamp state, and the item model (tank plus station cube). `FluidStationLook` for the tank; `FluidStationRenderer` draws the fluid in the lower half with four sides and top. Drop the window row models and the steel halves. List every overlapping box pair and its plane check in the task notes. Verify with `gradlew :shuttles:runData` and `gradlew :shuttles:build`.

## 7. Swing hose arm (option A)

- [x] 7.1 Add `StationArmLook` (pure) in `shuttles/.../fluidstation/`: the arm's geometry, `swing`, `lower`, `angle`, `headTop`, `hoseDrop`, `capTop` and `endedEarly`. Add `StationArmLookTest` with these cases:
  - swings down, lowers, holds and goes back over a 40 tick stop;
  - the hose moves only while the boom lies level;
  - a short stop never lowers the hose;
  - an early end goes back from where the arm is;
  - the angle runs from upright to level;
  - the docked head sits around the filler cap;
  - the arm stays in front of the station and clear of the stub;
  - the raised arm clears the tanker;
  - no shared planes between the parts.

  Verify with `gradlew :shuttles:test`.
- [x] 7.2 Dock sync. `FluidStationBlockEntity`: `dockArm(ticks)`, `undockArm()`, `armOut()`, `armTicks()`, `armElapsed(partialTick)`, the start and length in the update tag, one block update at the start and at an early end. `ShuttleFleet.Host` gains `dockStationArm` and `undockStationArm` (default no-ops), `WarehouseNetwork` forwards them to the station; `ShuttleFleet.holdForPump` sets the coupling only at racks and docks the station's arm otherwise, `endPumping` undocks it. Update the GameTests: `FluidInboundTankerGameTests.armAtStationCouplingAtRack` (was `couplingAtStation`: the station's arm is out, no coupling while it is, the coupling only toward the rack), `FluidRackTankerGameTests.couplingWhilePumpingAtRack` (coupling only toward the rack, the Inbound's arm docked), and add `FluidInboundGameTests.armDocksAndEndsEarly`. Verify with `gradlew :shuttles:runGameTestServer`.
- [x] 7.3 Look. `FluidStationRenderer` draws the stub, swivel, boom, hose and head from `StationArmLook`, turned by the facing, the boom about the swivel, with steel, copper and `heavy_rack_iron` on every face, lit by the block in front of the upper half; the render box reaches over the rail. List every overlapping box pair and its plane check in design.md. Verify with `gradlew :shuttles:build`.
- [x] 7.4 Specs and plan: the Fluid Inbound and Fluid Outbound look requirements gain the arm, the tanker's `Coupling at a port` keeps the coupling in at stations (both tanker deltas), and the PLAN.md M13 arm point goes. Verify with `openspec validate m13-fluid-inbound` and `openspec validate m13-fluid-outbound`.

## Workflow follow-up

- Stacked after `m13-fluid-side-modes` and `m13-fluid-rack`, and before `m13-fluid-outbound`.
- On archive, add an Added line for the Fluid Inbound under `## Unreleased` in `shuttles/CHANGELOG.md`.

## Implementation notes

- Build, unit tests (foundation 549, shuttles 963, alchemy 267, delicacies 87, brewery 72) and all 1103 shuttles GameTests are green, 20 of them new (`FluidInboundGameTests`, `FluidInboundTankerGameTests`). 5.1 waits for the in game play check.
- Names that differ from the design:
  - The ref kind is `FluidRole` (Import, Storage, Export), which the host reads from the block at the ref (`fluidStoreRole`), since `FluidStoreRef` stays `(pos, tank)` and saved runs keep loading. `FluidJob.sourceRole()`/`targetRole()` replace `sourceMode()`/`targetMode()`; `fluidStoreDocks` tells port stops (rack, station) from spout stops.
  - The `FluidSource` view is a record of ref, fluid and capacity built by `putAwaySources()`; "still a source" is `fluidStoreRole(ref) == IMPORT`.
  - `FluidActivity` and `FluidActivityHolder` live in `network/`; the gantry's `setActivity(...)` became `setFluidActivity(FluidActivity)`.
  - The Inbound's tanks are `TankRole.FILL_ONLY`, so no side can drain them even if a mode allowed it.
  - Breaking either half removes the other through `updateShape`, as vanilla doors do, rather than `affectNeighborsAfterRemoval`.
  - The GUI's waiting reason (no room, no tanker) travels as a data slot like the gantry's; `StationStatePayload` carries only online and the offline reason.
  - The window row of the upper half depends on `fluid_station_tanks`, so it is a standalone model per tank count (`fluid_station_windows_1` to `_6`) drawn by `FluidStationRenderer`, not part of the block model. The sight glass fluid uses `FluidColumn` in `level_steps` of `fluid_rack.json`.
  - `SideMarkerRenderer.State.hide(Direction)` was added in Foundation so the renderer draws the markers on both halves without the faces between them.
  - The lamps sit low on the lower front (connection left, activity right) because the port is at coupling height (12.4 px), near the top of the lower half. Lamp lenses use vanilla concrete textures with full light emission.
- 4.1 overlapping box pairs and their plane check (station built facing north, front at z 0):
  - Lower body (z 1 to 16) and corner posts (z 0 to 1): the posts have no back face, so only the body's front is in the plane z 1; their sides meet the body's sides edge to edge.
  - Lower body and plinth: the plinth has no back face; its bottom meets the body's bottom edge to edge.
  - Corner posts and plinth: the plinth has no side faces; only the post's inner face is in each plane x 1 and x 15.
  - Port flange (z 0 to 2) and lower body: the flange passes through the body's front plane; its back face is left out and no flange face lies in z 1.
  - Lamp sockets (z 0.5 to 1) and lower body: the sockets have no back face. Lenses (z 0.3 to 0.5) and sockets: the lenses have no back face; their sides and top lie 0.5 inside the socket's.
  - Port flange, sockets, posts and plinth do not overlap each other (flange y 10.9 to 13.9, sockets y 4.5 to 7.5, plinth y 0 to 1, posts x 0 to 1 and 15 to 16).
  - Lower and upper half: the tops of all lower parts and the bottoms of the upper body, band box, front band and posts are left out; the window sill keeps its bottom face over the lower front recess, where no lower face lies.
  - Upper body (z 3 to 16, y 0 to 13) and band box (y 13 to 16): the band box has no bottom and no front face; their side and back faces meet edge to edge.
  - Upper posts (z 0 to 3) and the upper body: the posts have no back and no inner side face; sill, header, outer fins and front band have no side face against them.
  - Front band (y 13 to 16) and header (y 12 to 13): the band has no bottom face and the header no top face.
  - Sill, header and fins against the upper body: none has a back face; only the body's front lies in z 3.
  - Fins against sill and header: the fins have no top or bottom face.
  - Glass panes (z 0.45 to 0.55) stand 0.05 off the fins, sill and header; the fluid stands 0.1 off them, from z 0.7 to 2.9, behind the glass and 0.1 in front of the body.
  - Two stations side by side meet only with their outer faces at the block edge, facing opposite ways.
  - Coupling: the tanker's head stops 0.005 short of the block edge where the flange's front lies.
- In game play check for the user: place a Fluid Inbound on a floor and against a wall (refusal message), feed it from an Electric Pump through a duct on the back of either half and from a Fluid Tank on a Pull side; set sides with the panel (left click mode, right click tank) and the carrot on both halves; watch a Tanker dock its coupling into the port and carry the fluid to a Fluid Rack; check both lamps (offline dark, green, orange, red with the GUI naming the reason), the sight glasses following each tank, the markers on both halves, the item model in hand and GUI, and flicker on every part from several angles, also while the coupling moves.

## Implementation notes (play check revision)

- Build, unit tests (foundation 550, shuttles 955) and all GameTests (shuttles 1080, foundation 253) are green after 6.1 to 6.4.
- Several look directions were tried during this revision (a family wood frame with a dye coloured belt, then the tank half telling the stations apart, then a swivel hub); the user settled on concept H, the item station on a glass tank, and dropped the dye. Nothing of the earlier directions is left in code or specs.
- Names and decisions that differ from the tasks:
  - Buckets use the tank itself (`FluidSlot` is a `ResourceHandler<FluidResource>`), so `FluidStationHandler` was deleted.
  - `FluidStoreRef.station(pos)` names the station's tank with index `STATION` (0); the network checks that index.
  - `FluidStationBlockEntity.setBlockState` copies `ONLINE` and `ACTIVITY` to the upper half, because the upper half shows the Inbound or Outbound Station front and the lamp state is set on the lower half only.
  - The recipe needs no class of its own: a plain wood `MaterialRecipe` already takes the wood an ingredient carries (the item station's `servus_foundation:material` component).
  - `TankerRunGameTests.fullInboundTankGoesBelowMinLoad` and `runsOnTheWayCountAgainstRoom` now fill the one tank; the second tank's assertions went. `TankerRestockGameTests.restockLeftoverBackToRack` keeps at the capacity, so 3 B still go back to the rack with 16,000 mB tanks.
  - PLAN.md M13 gained the open point for the swing hose arm.
- 6.4 overlapping box pairs of `fluid_station_tank` (the upper half is the item station's own full cube model, unchanged):
  - post and post: apart (corners 0 to 2 and 14 to 16), no shared plane.
  - post and bar: bars run from 2 to 14 and end at the posts' inner faces; bars have no end faces, so the post face there is the only face. No overlap.
  - bar and bar: bottom and top bars along x and along z meet only at the posts, never each other.
  - bottom bar and floor (2 to 14, y 0 to 2): they meet at the bars' inner sides, which the bars do not show; the floor shows only its top and bottom, at y 2 and 0, next to (not over) the bars' tops and bottoms.
  - glass pane and post, bar or floor: panes stop 0.05 short of every frame part and show only their broad faces in the middle of the frame depth (z or x 0.95 to 1.05 and 14.95 to 15.05). No shared plane.
  - pane and pane: the side panes run z 2.05 to 13.95 and the front and back panes x 2.05 to 13.95, so they never touch.
  - fluid and frame or glass: the fluid stands 1.15 inside the outer faces (0.1 inside the panes), from y 2.1 up to at most 13.9, so its faces lie on no frame, floor or glass plane; its corners pass inside the posts.
  - lower half top and upper half bottom: the tank shows no face at y 16 (posts and top bars show no top), so the upper cube's bottom face is the only face on that plane.
- Tanker coupling: there is no port on a station now. The tanker still stops on the rail in front of the lower half and its side coupling still slides toward the tank while it pumps; the hose arm change replaces that look.
- In game play check for the user:
  - craft a spruce Inbound Station over a Fluid Tank and a birch Outbound Station over a Fluid Tank; the item shows the wood in hand and GUI;
  - place both: the upper half is the item station in its wood, the lower half a glass tank; break one and place it again (wood, tank and sides kept);
  - fill with water and lava: the fluid rises in steps behind the glass on all four sides, seen from every angle; no flicker on the frame, glass or fluid;
  - the lamps on the upper front: dark offline, green, orange while a tanker runs, red with the GUI reason;
  - the GUI with one gauge, the side panel without tank chips (right click on a cell does nothing, tooltip "Side: Mode");
  - a tanker stopping in front of the station, its coupling sliding toward the tank.

## Implementation notes (hose arm)

- The arm is option A of `temp/fluid-station-wood.html`; its final geometry, timing, sync and plane check are in design.md (Hose arm at the station). The coupling note and the last play check line above are outdated: at a station the coupling stays in and the arm docks.
- Differences from the mockup: the pivot is at y 19, not 18, because the real filler cap (15.84) stands higher than the mockup's and the hose drop would be 0.6 px; the swivel is 1.25 out of the front so it does not reach into the upper half; the hose and head turn with the boom while it swings (the head rides under the boom, on the rail side when upright) and the hose only lowers once the boom lies level.
- Everything of the arm is drawn by `FluidStationRenderer`, the stub and swivel included, so the block models and the item model stay unchanged (the item shows no arm).
- The arm's state is not saved: a station unloaded mid stop shows the arm up when it loads again.
- Build, unit tests (foundation 550, shuttles 965, alchemy 267, delicacies 87, brewery 72) and all GameTests (shuttles 1084, foundation 253) are green.
- In game play check for the user:
  - the arm at rest stands upright against the upper half's front of a Fluid Inbound and a Fluid Outbound, facing each of the four ways;
  - a tanker stop: the boom swings down (about 0.4 s), the head lowers onto the filler cap (about 0.3 s), the fluid moves, the head rises and the boom swings up before the tanker drives on; the tanker's side coupling stays in;
  - at a Fluid Rack the coupling still slides into the port;
  - recall the garage during a stop: the arm goes back up from where it is;
  - flicker on the stub, swivel, boom, hose and head from several angles, at rest and while moving; nothing pokes into the station or the tanker.
