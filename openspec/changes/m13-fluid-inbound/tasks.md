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
