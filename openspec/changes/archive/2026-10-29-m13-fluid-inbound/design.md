# Design: m13-fluid-inbound

## Context

After `m13-fluid-side-modes`, Foundation has a Fluids layer with modes and tank choice. It also has `FluidSideHandler` and `FluidSideMover`, but no block uses them yet. After `m13-fluid-rack`:
- the network has `FluidStoreRef`, fluid stores (Storage gantry tanks, rack sections) and `NodeKind.FLUID_RACK`;
- the tanker has a coupling for rack ports.

Put-away sources are still Import gantry tanks only, read in `WarehouseNetwork.tickFluids()` and planned with `FluidPutAwayPlan` and `FluidRouting`. Lamps are set through `gantry.setActivity(noRoom, missing, noTanker)`.

The item stations are single blocks (`StationBlock`, `StationBlockEntity`). The only two-high block in Shuttles is the Fluid Gantry, which keeps its block entity on the master part, forwards clicks, and registers its capability with `registerBlock` for every part.

## Goals / Non-Goals

**Goals:**
- A two-high station base shared by Fluid Inbound and Fluid Outbound.
- The Inbound as a put-away source with the same rules and lamp as an Import gantry tank.
- The first real user of the Fluids side layer, with GameTests for every mode and the tank choice.

**Non-Goals:**
- The Outbound and the removal of the gantry.

## Decisions

### Block: one block with `HALF`, entity on the lower half

`FluidStationBlock` (abstract, in `shuttles/.../fluidstation/`) has these state properties: `FACING`, `HALF` (LOWER, UPPER), `StorageNodeBlocks.ONLINE` and `ACTIVITY`.
- **Block entity:** only the lower half has one. The upper half forwards `useWithoutItem`, buckets, the potato and the carrot to it, in the same way `FluidGantryBlock.master` does.
- **Placement:** `FluidStationItem.useOn` checks both spaces (`canBeReplaced`, build height) and a sturdy top below. Otherwise it refuses with a message, as `FluidGantryItem.placeAround` does. It then places the lower half and applies the item's saved block entity data (`BlockEntityItems`), and then places the upper half.
- **Breaking:** the upper half turns to air in `updateShape` when the lower half is gone. `affectNeighborsAfterRemoval` on the lower half removes the upper one. `playerWillDestroy` on either half routes the drop through the lower half, and creative breaks drop nothing.
- **Pistons:** `PushReaction.BLOCK`.

`FluidInboundBlock` extends it. The Outbound comes in the next change.

### Sides on both halves

`FluidStationBlockEntity implements SidedBlockEntity`:
- `hasItemsLayer()` is false;
- `fluidSides()` is `new FluidSides(tanks, INBOUND_MODES, INPUT)` with FRONT locked;
- `sideTanks()` is one `SideTank`;
- `sideTankFluid(0)` is the tank's fluid.

With one tank, the Fluids layer offers no tank choice (`m13-fluid-side-modes`, No tank choice with one tank): every side reaches the tank.

The capability is registered with `event.registerBlock(Capabilities.Fluid.BLOCK, ...)` for the block, so it works for both halves. It maps the face to the station's relative side with the lower half's facing:
- the top of the lower half and the bottom of the upper half are internal and return null;
- both halves' sides map to LEFT, RIGHT and BACK;
- the upper half's top maps to TOP and the lower half's bottom to BOTTOM.

It then returns `FluidSideHandler.of(station, side)`. On the Inbound that handler's drain always returns nothing, because the modes allow no drain.

`serverTick` (lower half) calls `FluidSideMover.tick` at `push_interval`. That needs a neighbour position per relative side: for LEFT, RIGHT and BACK, the mover tries the neighbour of the lower half first and then of the upper half, so a pipe on either half works.

The marker renderer (`SideMarkerRenderer`) draws on both halves. The upper half uses the lower half's layer through the forwarding.

### Network: `NodeKind.FLUID_STATION`

- `FluidStationBlockEntity extends StorageNodeBlockEntity` with `kind() = FLUID_STATION`.
- `acceptsRail(side)` is the front only, on the lower half (the only half with a block entity).
- `WarehouseConnectionRule`: `railOnly` and linked only to `RAIL`. It is added to `docksRail`.
- `onNodeAdded` puts it in `fluidStations`, which is never part of the fluid index.
- `dockRails(pos)` returns `pos.relative(facing)`.

### Put-away source

- `tickFluids()` gains a loop over Inbound tanks next to the Import gantry tanks. Both feed the same planner through `FluidStoreRef(stationPos, tank)`.
- The gantry-specific checks (`gantryMode`) are replaced by a `FluidSource` view with `still(ref)`: "the source is still there and still a source".
- `FluidJob` keeps `Purpose.PUT_AWAY`. Its `sourceMode()`/`targetMode()` (`GantryMode`) are replaced by a `kind` of the ref. That generalisation is needed for the Outbound and the gantry removal anyway.
- The lamp states reuse the gantry's three reasons (no room, missing, no tanker). They move to a shared `FluidActivity` record set on any `FluidActivityHolder`, which both the gantry and the station implement.

### Hose arm at the station

`holdForPump` sets the tanker's coupling only for rack stops (`store.isRack()`). For a station stop it calls the new `ShuttleFleet.Host.dockStationArm(store, ticks)` with the stop's length, and `endPumping` calls `undockStationArm(store)` with the store it remembered in `shuttle.pumping`. Both are default no-ops on `Host`; `WarehouseNetwork` forwards them to the loaded `FluidStationBlockEntity` (`station(store)`), on the server thread inside the fleet tick. The shuttle view carries no coupling at a station.

Sync, like the tanker coupling: the station keeps `armStart` (game time) and `armTicks`, not saved. `dockArm` sets both, `undockArm` shortens `armTicks` with `StationArmLook.endedEarly` when the stop ends before its time (recall, rail or garage loss), so the arm goes back from where it is as fast as it came out. Each sends one block update (`sendBlockUpdated`); `getUpdateTag` carries `ArmStart` and `ArmTicks`, `loadAdditional` reads them. A stop that ends on time changes nothing and sends nothing. `armOut()` (server and client) is true from the dock until the arm stands up again. Clients compute the pose in `FluidStationRenderer.extractRenderState` from `level.getGameTime() + partialTick - armStart`; there is no client tick logic.

Timing (`StationArmLook`, pure, JUnit `StationArmLookTest`): with `r` the ticks to the nearer end of the stop, the boom's swing is `ease(r / 8)` (0.4 s), the hose's drop `ease((r - 8) / 6)` (0.3 s), with smoothstep easing. The hose moves only while the boom lies level. A stop of 16 ticks or less (Handling can shorten `tanker_pump_ticks` of 40) swings down and up without lowering the hose fully.

Geometry in block pixels of the lower half for a station facing north, `out` from the front face toward the rail; the renderer turns it by the facing (`Axis.YP` by `180 - facing.toYRot()`):
- copper stub: x 7.4 to 8.6, y 15 to 18, out 0.65 to 1.85;
- copper swivel: x 6.7 to 9.3, y 17.8 to 20.2, out 0.05 to 2.45 (pivot y 19, out 1.25);
- steel boom, turned about the pivot (`Axis.XP` by 90 upright to 0 level): x 7.3 to 8.7, 1.4 px square, from 0.5 behind the pivot to 7.45 in front of it;
- dark hose (`heavy_rack_iron`): x 7.55 to 8.45, 0.9 px square, at 6.75 along the boom, from 0.5 under the boom's axis (inside the boom) down 0.3 into the head;
- copper head: x 6.9 to 9.1, 2.2 px square, 1.2 high, 0.05 under the boom when raised.
Level, the hose hangs over the rail block's centre (out 8), where the tanker's filler cap is (x and z 7.2 to 8.8, top at `TankerLook.FLOOR + SCALE * CAP_TOP` = 15.84). The hose drops 1.61 px, so the head's bottom ends at 15.44, around the cap and 0.24 above the tank's top (15.2). The pivot sits at y 19 rather than the mockup's 18: at 18 the drop would be 0.61 px, too small to see, since the real cap stands higher than the mockup's. Upright, the boom reaches y 26.45 against the upper half's front and the head sits 2.0 to 3.2 out, on the rail side. Nothing reaches into the station: the nearest point during the swing is 0.40 out (the boom's back corner at 45 degrees), and the boom's lowest point, 18.15, stays above the stub. Raised, the head's bottom (17.05) clears the cap, so the swing never touches the tanker.

Textures: steel `servus_foundation:block/electric_pump_steel` for the boom, vanilla `copper_block` for stub, swivel and head (as the tanker's cap and coupling head), `heavy_rack_iron` for the hose, on every face. Each part is drawn as a `FluidBox` from its own corner with `Sheets.cutoutBlockSheet()`, lit by the block in front of the upper half. The render box reaches 0.75 block over the rail.

Plane check (no two faces share a plane where they overlap):
- stub and swivel: x 7.4/8.6 against 6.7/9.3, y 15/18 against 17.8/20.2, out 0.65/1.85 against 0.05/2.45; the stub's top lies inside the swivel;
- boom and swivel: x 7.3/8.7 against 6.7/9.3; upright, the boom's sides at out 0.55/1.95 and its back end at y 18.5; level, its sides at y 18.3/19.7 and its back end at out 0.75; in between its faces are not axis aligned; its back end lies inside the swivel;
- boom and stub: x 7.3/8.7 against 7.4/8.6, and they never overlap (boom lowest 18.15 above the stub's top 18);
- hose and boom: x 7.55/8.45 against 7.3/8.7; the hose's top lies inside the boom; its ends along the boom (6.3/7.2) against the boom's ends (-0.5/7.45);
- head and boom: x 6.9/9.1 against 7.3/8.7, a 0.05 gap between the head's top and the boom's bottom, the head's ends along the boom (5.65/7.85) against the boom's end at 7.45;
- head and hose: x differ, the hose's bottom lies inside the head;
- arm and station: the front face is at out 0; the nearest arm face is the swivel's back at 0.05;
- head and tanker cap: x and z 6.9/9.1 against 7.2/8.8, the head's bottom 15.44 against the cap's 15.04 and top 15.84; the cap's top lies inside the head;
- no part is drawn more than once.

### GUI

- **Menu:** `FluidInboundMenu` syncs per tank the amount, the capacity and the fluid id. Like `FluidGantryMenu` does for its gauges, it uses data slots for the amounts and the update tag for fluid ids and labels. It also syncs the Fluids side data (modes and tanks, six bytes each, through `ContainerData`) and the state line through the existing `StationStatePayload`.
- **Screen:** `FluidInboundScreen` draws one centred gauge with the Foundation gauge widget used by the gantry screen, and `SideConfigPanel` with `enableFluids` and fluids only. Clicks send `SideConfigMenus` mode and tank buttons.

### Look

Decided in the play check (mockup `temp/fluid-station-wood.html`, concept H, after concepts F and G were dropped): the station is the item station on a tank.

- **Upper half:** the item station's own block models, reused as they are: `inbound_station`, `inbound_station_idle`, `inbound_station_working` and `inbound_station_blocked` on the Fluid Inbound, the `outbound_station` ones on the Fluid Outbound. The multipart block state picks them by the upper half's `ONLINE` and `ACTIVITY`, rotated by the facing. Those models draw their boards on the wood placeholder, so `MaterialModels` shows the station's wood.
- **Lamps:** the lamp state is set on the lower half (the network's `pushLamps` and `setFluidActivity`). `FluidStationBlockEntity.setBlockState`, which the chunk calls whenever the lower half's state changes with the block entity kept, copies `ONLINE` and `ACTIVITY` up to the upper half (`UPDATE_CLIENTS | UPDATE_KNOWN_SHAPE`), so the item station front shows them.
- **Lower half:** `fluid_station_tank`, shared by both stations: iron posts and bars (`heavy_rack_iron`, 2 px) on the twelve edges, an iron floor, a glass pane on each side in the middle of the frame depth, open at the top to the bottom of the upper half. No board shows through the glass. `FluidStationRenderer` draws the fluid inside the panes with the shared `FluidColumn` helper (`m13-fluid-rack`) in `level_steps` from `fluid_rack.json`, with four sides and its top.
- **Item:** one item model per station with the tank boxes and the item station cube on top (board layer under the station's overlay textures, like `LayeredModels`), as a material item model of the wood kind.
- **No port, a hose arm:** neither station has a port. A tanker stops on the rail in front of the lower half and pumps as before, its side coupling stays in, and the station's swing hose arm (option A of the mockup, see Hose arm at the station) docks onto its filler cap. The tanker and its coupling stay unchanged at Fluid Racks.

Plane check of `fluid_station_tank`: posts span y 0 to 16 and show their outer and inner sides and their bottom, never their top (the upper half's bottom face lies there). Bars meet the posts at x or z 2 and 14 and show no end faces; bottom bars show their side, bottom and top, top bars their side and bottom. The floor (2 to 14, y 0 to 2) shows up and down and meets the bottom bars at their inner sides, which they do not show. Panes stand 0.05 off posts, bars and floor and show only their broad faces; the fluid stands 1.15 inside the outer faces, 0.1 inside the panes, and above the floor. No two faces share a plane where they overlap.

### Wood

The stations reuse the item stations' wood mechanism: `Material.register(block, MaterialKind.WOOD)` in `ServusShuttles`, a `MaterialSlot` on the block entity with `getModelData`, `MaterialBlockItem` for the item, and the `servus_foundation:material` item model type.

Model data is per position and only the lower half had a block entity. The upper half, which shows the boards, gets a small `FluidStationTopBlockEntity` that only holds a copy of the wood, set when the station is placed, saved and sent in its update tag. It has no ticker, no capability and no network role; the station is still the lower half's block entity.

### Recipe

Shaped, one column: the item station on top of a Fluid Tank (`S / T`). The Fluid Inbound takes an Inbound Station, the Fluid Outbound an Outbound Station, so the item stations tell them apart. The recipe is a plain `MaterialRecipe` of the wood kind (`MaterialRecipes.output(..., MaterialKind.WOOD)`): its pick already counts an ingredient that carries a material itself (the station item's `servus_foundation:material` component, as the rail does), so the result takes the station's wood with no recipe class of its own. A station item without a wood gives the default oak.

### Data

`warehouse.json` gains `fluid_station_tank_capacity`, 16000. There is no tank count: the station has one tank.

## PLAN.md decisions settled

This settles the M13 fluid inbound dock:
- two blocks high;
- one tank;
- the item station on a glass tank, in the item station's wood;
- the item station side modes on the Fluids layer;
- a rail only at the front of the lower half;
- no port: tankers stop in front of the lower half and the station's swing hose arm docks onto their filler cap.

## NeoForge APIs

- `RegisterCapabilitiesEvent.registerBlock(Capabilities.Fluid.BLOCK, provider, block)`, as the gantry uses it today.
- `Level.getCapability(Capabilities.Fluid.BLOCK, pos, side)` in `FluidSideMover`.
- `FluidUtil.interactWithFluidHandler` for buckets.
- `BlockBehaviour.updateShape` and `affectNeighborsAfterRemoval` for the two-half removal, as `FluidGantryBlock` does.

## Pure and Minecraft parts

- **Pure:** `StationArmLook` with `StationArmLookTest` (swing and hose timing, early end, the docked head around the cap, clearances and plane gaps). The side rules are covered by `FluidSidesTest` and `FluidSideRulesTest` from `m13-fluid-side-modes`, and the planner by `FluidPutAwayPlanTest`.
- **GameTests (`FluidInboundGameTests`):**
  - placement, the no-room refusal, and breaking either half;
  - the drop keeping the tank and sides;
  - Input filling the tank, and the tank refusing a second fluid;
  - Pull from a Fluid Tank;
  - Off staying connected;
  - the front with no endpoint;
  - no drain through any side;
  - joining only through the front rail;
  - not counting as stock;
  - a pump to rack run by tanker;
  - the red lamp for no room and for no tanker;
  - buckets;
  - a source broken mid run.
- **GameTests for the play check decisions:** the drop keeping the wood on both halves, the recipe taking the station's wood, the one tank refusing a second fluid, and the upper half showing the lamp state.
- **Manual play check:** the glass and fluid from every side, the wood on both halves and the item, the lamps on the upper front, the tanker stop in front of the station with the hose arm docking onto its cap, markers on both halves, and flicker.

## Risks / Trade-offs

- [Two neighbours per relative side for pull and push] A Pull side with a tank on both halves pulls from both in one tick. Acceptable, and documented in the GUI tooltip.
- [Lamp reasons shared with gantries until the gantry goes] The shared `FluidActivity` keeps the gantry working, and the gantry's part is deleted in `m13-fluid-outbound`.
