# Design: m13-fluid-side-modes

## Context

Foundation's side configuration has two layers. The Items layer (`SideConfig`, `SideMode`) gives every side one mode from a block's supported list. The Fluids layer (`FluidSides`) stores one int per side: Off, All, or a tank index. Tanks are fixed `SideTank(key, color, input)` records returned by `SidedBlockEntity.sideTanks()`, which "must not depend on instance state". The panel (`SideConfigPanel.enableFluids`), the markers (`SideMarkerRenderer.extractFluids`), the carrot (`SideConfigWrench`, `ConfigurationCarrotItem` layer component) and the machine menu data slots (`MachineBlockEntity` `DATA_FLUID_SIDE_FIRST`) all support it.

No block uses the layer today. The Calciner and the Coagulator used it (commit 713b729bc) and moved to fixed ports later. `MachineBlockEntity.fluidSideExposes` has no callers. No fluid capability reads the layer, and nothing pushes or pulls fluid by side. The layer can therefore change shape without a migration (see the "nothing is BREAKING before a first release" rule of this repo).

The first users come in `m13-fluid-inbound` (Input, Pull, Off) and `m13-fluid-outbound` (Push, Output, Off), each with three tanks that hold any fluid.

## Goals / Non-Goals

**Goals:**
- One Fluids layer model that carries a mode and a tank choice per side, in pure Java.
- One shared fluid side handler and one shared pull and push routine, so a block only declares its tanks, modes and labels.
- Tank labels that follow the content, synced without a new payload.
- Panel, carrot and markers that read the same model.

**Non-Goals:**
- A block that uses the layer (see proposal).
- Per side fluid filters.
- Choosing the tank with the carrot.

## Decisions

### Model: `FluidSides` holds a `SideMode` and a tank choice per side

`FluidSides` keeps its role but stores two things per side:
- a `SideMode` limited to a supported list, cycled like `SideConfig`;
- a tank choice, `ALL` (-1) or a tank index.

It is created with `(int tanks, List<SideMode> supported, SideMode fallback)`. It keeps `lock(side)` (fixes the side to Off and All), `isLocked`, `isDefault` and the byte encoding.

Methods:
- `mode(side)`, `setMode`, `cycleMode` (wraps through the supported list);
- `tank(side)` (-1 for All), `setTank`, `cycleTank` (All, 0 .. n-1, All);
- `reaches(side, tank)`.

Encoding: one byte per side, the mode ordinal in the low 4 bits, the tank choice plus 1 in the high 4 bits (0 = All). This allows up to 15 tanks. Unknown or unsupported values decode to the fallback on All, and locked sides stay Off. The NBT key `"FluidSides"` stays; no saved data uses it today.

`SideConfig` and `FluidSides` stay separate classes rather than one generic one. The Items layer has no tank choice, and the Items layer API is used by every station and machine.

*Alternative:* flatten mode and tank into one cycle (Input All, Input tank 1, ..., Pull All, ...). One button would do it all, but a block with three tanks and three modes needs 12 steps per side, and the carrot would pass through every tank to reach a mode.

### Tanks: `SideTank` gets a direction, labels come from the block entity

`SideTank` becomes `record SideTank(TankRole role)` with `TankRole` FILL_ONLY, DRAIN_ONLY or ANY. The fixed name and colour move out of the record. The block entity answers per tank:
- `SidedBlockEntity.sideTankLabel(int tank)` returns a `TankLabel(Component name, int color)`;
- the default builds it from `sideTankFluid(tank)` (the fluid held, else the kept or set fluid, else empty);
- an empty, unset tank becomes "Tank N" in `TankLabel.NEUTRAL` (a grey).

A block with fixed names overrides `sideTankLabel`.

Client sync: `sideTankFluid` per tank is written into the block entity's update tag as fluid ids under `"TankLabels"`. Stations and machines already send update tags for their lamps and markers. Labels change only when a fluid starts or ends in a tank, or a keep fluid changes, so the update is cheap. The panel reads labels from the client block entity, as `MachineScreen` already reads `sideTanks()` from it today. This keeps menus free of fluid id data slots. Those are 16 bit on the wire, which fits vanilla but not every pack.

### The rules are pure: `FluidSideRules`

`FluidSideRules` (in `foundation/.../side/`, no Minecraft imports) answers, for one side:
- `fillOrder(fluidSides, side, tanks, content)`: the tanks to try for an offered fluid key. Tanks holding that key come first, then empty tanks that accept it, in tank order. Tanks holding another fluid are never included. Tanks the side does not reach, and FILL-refusing roles, are left out.
- `drainOrder(fluidSides, side, tanks, content)`: non-empty tanks the side reaches whose role drains, in tank order.
- `allowsFill(mode)`, `allowsDrain(mode)`, `pushes(mode)`, `pulls(mode)`: these read the `SideMode` flags.

`content` is a list of nullable keys with amounts, passed as plain records, so the class stays testable. "Accepts" (for example a keep fluid on a tank) is a `Predicate` the block passes in.

### Shared Minecraft side: `FluidSideHandler` and `FluidSideMover`

In `foundation/.../transfer/`:
- `FluidSideHandler extends LegacyFluidHandler`. It is a per side view over the block's `List<FluidSlot>` that fills by `fillOrder` and drains by `drainOrder`, inside the caller's transaction (`Transfer.current()`). A side in Off returns a handler that moves nothing, so pipes keep their connection (as the Items layer does). A locked side gets `null` from the capability, so no pipe arm shows.
- `FluidSideMover.tick(level, pos, block, interval)`. At the block's push interval it walks the six sides:
  - **Push:** for each tank in `drainOrder`, it fills the neighbour's `Capabilities.Fluid.BLOCK` handler (`level.getCapability(Capabilities.Fluid.BLOCK, neighbourPos, oppositeFace)`) as far as it takes, then drains that amount from the tank, in one `Transaction`.
  - **Pull:** for each fluid the neighbour offers, it fills by `fillOrder` and drains the same amount from the neighbour, in one `Transaction`.

  No fluid is created or lost. A failed fill rolls back with the transaction. There is no per side rate limit, matching the fluid pipe decision (no rate limit).

APIs: NeoForge `Capabilities.Fluid.BLOCK`, `ResourceHandler<FluidResource>`, and `Transaction.open(TransactionContext)` in `net.neoforged.neoforge.transfer.transaction`, reached through the repo's `Transfer.open()` and `Transfer.run(...)`. `FluidSlot`, `Transfer.fill`/`drain` and `LegacyFluidHandler` already use them against the NeoForge 26.1 transfer package.

### `SidedBlockEntity`: optional Items layer, two cycles

- `hasItemsLayer()` defaults to true. A fluids-only block returns false, and its `sideConfig()` may return a config whose sides are all locked Off.
- `cycleFluidSide(side)` becomes `cycleFluidMode(side)`, and `cycleFluidTank(side)` is added. Both mark changed, invalidate capabilities and send a block update, like `cycleSide`.
- `fluidSides()` and `sideTanks()` stay. `sideTanks()` may depend on instance state now, because labels moved out.

### Menu buttons, panel, carrot, markers

- `SideConfigMenus`: Fluids mode buttons stay at `FLUID_BUTTON_FIRST` (1006 to 1011). Tank buttons are added at 1012 to 1017 (`FLUID_TANK_BUTTON_FIRST`).
- `SideConfigPanel.enableFluids(modes, tanks, labels, locked, modeClick, tankClick)` draws each cell in `color(mode)` and adds a 5 px chip in the bottom right: "A" on the All colour, or the label colour. A right click (`mouseClicked` button 1) sends the tank button. A panel with only the Fluids layer skips the tabs (`fluidsOnly`).
- `SideConfigWrench`: on a block with only one layer it uses that layer. On the Fluids layer it calls `cycleFluidMode` and reports `side_line` with mode and tank label. Sneak stays dismantle.
- `SideMarkerRenderer.extractFluids` draws the mode glyph (the same glyph rules as the Items layer) and frames the face in the tank label colour, or the All colour.

### Machines follow the API

`MachineBlockEntity`, `MachineMenu` and `MachineScreen` compile against the new names. A machine declares Fluids modes through a new `fluidModes()` (default `SideConfig.MACHINE_MODES` with Auto). No machine declares tanks today, so they create no layer and nothing changes in play.

## PLAN.md decisions settled

PLAN.md leaves the fluid side configuration open only through the M13 rewrite of the gantry. This change settles that fluid sides use the same modes as item sides, per tank, and that the tank is picked in the GUI.

## Pure and Minecraft parts

- Pure, JUnit: `FluidSides` (modes, tank choice, cycling, lock, encoding), `FluidSideRules` (fill and drain order, roles, modes).
- Minecraft: `FluidSideHandler`, `FluidSideMover`, the panel, the markers and the carrot. They are covered by GameTests in `m13-fluid-inbound` and `m13-fluid-outbound`, the first blocks with a Fluids layer, and checked in game there.

## Risks / Trade-offs

- [No in-game user in this change] The Minecraft side is only proven by the next two changes. Mitigation: they are stacked right after it, and their GameTests cover every mode and the tank choice.
- [Labels in the update tag lag one block update] A label changes when a block update is sent, which stations do on content start and end anyway. Acceptable for a colour chip.
- [15 tank limit in the encoding] It is far above any planned block (3 tanks).
