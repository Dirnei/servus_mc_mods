# Tasks: m13-fluid-side-modes

## 1. Pure model

- [x] 1.1 Rewrite `FluidSides` in `foundation/.../side/FluidSides.java`:
  - per side, a `SideMode` from a supported list and a tank choice (All or an index);
  - `mode`, `setMode`, `cycleMode`, `tank`, `setTank`, `cycleTank`, `reaches`, `lock`, `isLocked`, `isDefault`;
  - the byte encoding from design.md.

  Rewrite `FluidSidesTest` with these cases:
  - defaults are the fallback on All;
  - the mode cycle wraps and keeps the tank;
  - the tank cycle runs All, 0, 1, 2, All and keeps the mode;
  - an unsupported mode falls back;
  - a locked side stays Off and All through set, cycle and decode;
  - the encoding round trip;
  - an unknown tank decodes to All.

  Verify with `gradlew :foundation:test`.
- [x] 1.2 Add `TankRole` and change `SideTank` to `SideTank(TankRole role)`. Add `TankLabel(name, color)` with `NEUTRAL` in `foundation/.../block/`, since its name is a Minecraft `Component`. Verify with `gradlew :foundation:build`.
- [x] 1.3 Add `FluidSideRules` in `foundation/.../side/FluidSideRules.java`: `fillOrder`, `drainOrder` and the mode flag helpers, over plain tank records (key, amount, capacity, role) and an accepts predicate. Add `FluidSideRulesTest` with these cases:
  - a holding tank comes before an empty tank;
  - a tank holding another fluid is never filled;
  - a side on one tank reaches only it;
  - a fill-only tank is never drained;
  - a drain-only tank is never filled;
  - Off reaches nothing;
  - the predicate refuses a kept fluid mismatch;
  - drain order is tank order and skips empty tanks.

  Verify with `gradlew :foundation:test`.

## 2. Shared handler and mover

- [x] 2.1 Change `SidedBlockEntity` (`foundation/.../block/SidedBlockEntity.java`):
  - add `hasItemsLayer()`;
  - add `sideTankFluid(int)` and `sideTankLabel(int)` with the default label rule;
  - rename the cycle to `cycleFluidMode` and add `cycleFluidTank`;
  - add the `"TankLabels"` write and read helpers for update tags.

  Change `MachineBlockEntity`, `MachineMenu` and `MachineScreen` to the new names and to `fluidModes()`. Verify with `gradlew build`; the existing machine GameTests stay green.
- [x] 2.2 Add `FluidSideHandler` in `foundation/.../transfer/`. It is a per-side `LegacyFluidHandler` over a block's `List<FluidSlot>` using `FluidSideRules`, nested in `Transfer.current()`; on Off it returns a handler that moves nothing. Add `FluidSideMover.tick(...)` for Push and Pull sides through `Capabilities.Fluid.BLOCK` in one transaction per move. Verify with `gradlew :foundation:build`. Behaviour is covered by the GameTests of `m13-fluid-inbound` and `m13-fluid-outbound`.

## 3. Client and carrot

- [x] 3.1 Add `FLUID_TANK_BUTTON_FIRST` (1012 to 1017) to `SideConfigMenus`, with `fluidTankSideOf` and `buttonOfTank`. Update the station and machine menu `clickMenuButton` paths that route Fluids buttons. Verify with `gradlew :foundation:build`.
- [x] 3.2 Change `SideConfigPanel` (`foundation/.../client/SideConfigPanel.java`):
  - Fluids cells in the mode colour with the tank chip ("A" or the label colour);
  - right click sends the tank button;
  - tooltip "Side: Mode, Tank";
  - no tabs for a block with one layer.

  Change `SideMarkerRenderer` to draw the mode glyph with the label colour frame, and the Fluids layer for goggles on fluids-only blocks. Verify with `gradlew :foundation:build`.
- [x] 3.3 Change `SideConfigWrench` to cycle the mode of the block's only layer when it lacks the carrot's layer, and to report mode and tank on the Fluids layer. Add lang strings for "Tank %s" and the "Side: Mode, Tank" line in the Foundation language provider. Verify with `gradlew :foundation:runData` and `gradlew :foundation:build`.

## 4. Docs and integration

- [ ] 4.1 Update the side configuration section of `docs/src/content/docs/mods/foundation.md`: Fluids sides have modes and a tank, a right click picks the tank, and the carrot cycles the mode. Verify that the docs site builds (`npm run build` in `docs/`). (Deferred to release: docs describe released versions only.)
- [ ] 4.2 Full gradle build, tests green, and the game launches with all mods. Open a machine GUI and check that the Items side panel looks and works as before. (Build, unit tests and Foundation GameTests green; game launch and machine GUI check left for the user play check.)

## Workflow follow-up

- Stacked before `m13-fluid-rack`, `m13-fluid-inbound` and `m13-fluid-outbound`. Apply and archive in that order.
- On archive, add a Changed line under `## Unreleased` in `foundation/CHANGELOG.md`.
