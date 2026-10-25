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
- `sideTanks()` is three `SideTank(TankRole.ANY)`;
- `sideTankFluid(i)` is the tank's fluid.

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

### Coupling at the station

`holdForPump` sets `shuttle.coupling` for station stops as for rack stops (`m13-fluid-rack`). The side is the direction from the rail to the station. The station's port sits at the same height as a rack port.

### GUI

- **Menu:** `FluidInboundMenu` syncs per tank the amount, the capacity and the fluid id. Like `FluidGantryMenu` does for its gauges, it uses data slots for the amounts and the update tag for fluid ids and labels. It also syncs the Fluids side data (modes and tanks, six bytes each, through `ContainerData`) and the state line through the existing `StationStatePayload`.
- **Screen:** `FluidInboundScreen` draws three gauges with the Foundation gauge widget used by the gantry screen, and `SideConfigPanel` with `enableFluids` and fluids only. Clicks send `SideConfigMenus` mode and tank buttons.

### Look

**Datagen** (`FluidStationModels`):
- a lower and an upper half model in steel with the teal band on the upper top edge;
- recesses for three sight glasses on the upper front;
- lamp sockets, and the copper port flange on the lower front at coupling height.

Lamp states use the blockstate variants the item stations use (`station(block, name)` builds dark, idle, working and blocked models).

**`FluidStationRenderer`** draws each tank's fluid in its sight glass with the shared `FluidColumn` helper from `m13-fluid-rack`, in `level_steps` from `fluid_rack.json`.

### Data

`warehouse.json` gains:
- `fluid_station_tanks`, 3, clamped to 1 to 6 so the GUI fits;
- `fluid_station_tank_capacity`, 8000.

## PLAN.md decisions settled

This settles the M13 fluid inbound dock:
- two blocks high;
- three tanks;
- the item station side modes on the Fluids layer;
- a rail only at the front of the lower half;
- the tanker's coupling into a fixed port.

## NeoForge APIs

- `RegisterCapabilitiesEvent.registerBlock(Capabilities.Fluid.BLOCK, provider, block)`, as the gantry uses it today.
- `Level.getCapability(Capabilities.Fluid.BLOCK, pos, side)` in `FluidSideMover`.
- `FluidUtil.interactWithFluidHandler` for buckets.
- `BlockBehaviour.updateShape` and `affectNeighborsAfterRemoval` for the two-half removal, as `FluidGantryBlock` does.

## Pure and Minecraft parts

- **Pure:** nothing new. The side rules are covered by `FluidSidesTest` and `FluidSideRulesTest` from `m13-fluid-side-modes`, and the planner by `FluidPutAwayPlanTest`.
- **GameTests (`FluidInboundGameTests`):**
  - placement, the no-room refusal, and breaking either half;
  - the drop keeping tanks and sides;
  - Input on All filling two fluids into two tanks;
  - Pull on one tank;
  - Off staying connected;
  - the front with no endpoint;
  - no drain through any side;
  - joining only through the front rail;
  - not counting as stock;
  - a pump to rack run by tanker;
  - the red lamp for no room and for no tanker;
  - buckets;
  - a source broken mid run.
- **Manual play check:** the sight glasses, lamps, port, the coupling at the station, markers on both halves, and flicker.

## Risks / Trade-offs

- [Two neighbours per relative side for pull and push] A Pull side with a tank on both halves pulls from both in one tick. Acceptable, and documented in the GUI tooltip.
- [Lamp reasons shared with gantries until the gantry goes] The shared `FluidActivity` keeps the gantry working, and the gantry's part is deleted in `m13-fluid-outbound`.
