# Proposal: m13-fluid-side-modes

## Why

The Fluid Gantry is being replaced by a Fluid Inbound, a Fluid Outbound and a Fluid Rack built like the item side (PLAN.md M13 Fluid logistics). The fluid stations need the same side configuration players know from the item stations (Input, Pull, Off and Push, Output, Off), but per fluid tank: "push the water tank out of the back and the lava tank out of the left". Foundation's Fluids layer only assigns a tank to a side (Off, All or one tank), has no modes, and names its tanks with fixed labels. No block uses the layer today, so it can be reshaped now and stay reusable for later fluid blocks in every mod.

## What Changes

- Each side of the Fluids layer gets a mode from the block's supported list, with the same meaning as in the Items layer: Input, Output, Both, Pull, Push, Auto and Off. It also keeps its tank choice: All, or one tank. The mode decides what may move and whether the block moves fluid itself. The tank choice decides which tanks the side reaches.
- A tank may still declare a direction (fill only or drain only). A side then allows only what both the mode and the tank allow.
- Foundation gets a shared fluid side handler and a shared pull and push routine that every block with a Fluids layer uses. A side set to Off offers nothing, and a fixed side offers no fluid capability.
- Tank labels follow the tank: its name and colour come from the fluid it holds or is set to keep, and from "Tank 1", "Tank 2" in a neutral colour while it is empty and unset.
- The side panel draws a Fluids cell in the mode's colour with a small corner chip for the tank (an "A" chip for All, otherwise the tank's colour). A left click cycles the mode and a right click cycles the tank. The tooltip names the side, the mode and the tank.
- The Configuration Carrot on the Fluids layer cycles the mode of a face. The tank is chosen in the GUI, because sneak with the carrot stays dismantle.
- A block may have only a Fluids layer. Its panel then shows no tabs, and the carrot cycles that layer whatever layer it is set to.
- Fluids markers on the block use the mode's glyph and the tank's colour for the frame.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/side-config`: the Fluids layer gains modes and a tank choice per side, a tank direction rule, dynamic tank labels, a shared pull and push behaviour, the panel cell look with right click for the tank, the carrot rule for blocks with only a Fluids layer, and markers that show the mode. The layer requirements lose their Calciner examples, because the Calciner has fixed ports now.

## Impact

- `foundation/.../side/`: `FluidSides` (mode plus tank per side, new encoding), `SideTank` (direction and label source), and a new pure `FluidSideRules` that picks the tanks for a fill or a drain.
- `foundation/.../block/`: `SidedBlockEntity` (an optional Items layer, tank labels, a mode cycle and a tank cycle), `SideConfigWrench`, `SideConfigMenus` (buttons for the tank cycle), and a new `FluidSideHandler` and `FluidSideMover` in `foundation/.../transfer/`.
- `foundation/.../client/`: `SideConfigPanel` (cell look, right click, single layer) and `SideMarkerRenderer`.
- `foundation/.../machine/`: `MachineBlockEntity`, `MachineMenu` and `MachineScreen` follow the new layer API. No machine declares fluid tanks today, so nothing changes in play.
- Lang strings for modes on the Fluids layer and for the tank names.
- JUnit: `FluidSidesTest` rewritten, new `FluidSideRulesTest`.

## Non-goals

- Using the layer on any block. The Fluid Inbound (`m13-fluid-inbound`) and the Fluid Outbound (`m13-fluid-outbound`) are the first users.
- Bringing the layer back to alchemy machines. They keep their fixed ports.
- Choosing a tank with the carrot.
- Fluid filters per side.
