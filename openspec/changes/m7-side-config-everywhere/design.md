# Design

## Context

See proposal.md for motivation. Lib has `lib/side`: `RelativeSide` (front, back, left, right, top, bottom with `toSide(facing)` and `fromSide(facing, absolute)`), `SideMode` (Auto, Input, Output, Both, Disabled with insert and extract flags and `next()`), `SideConfig` (six modes, cycle, bytes). Manufacturing machines keep a `SideConfig`, map absolute directions through the facing, sync it through `ContainerData` slots `DATA_SIDE_FIRST` to `DATA_SIDE_FIRST + 5`, cycle it through menu button ids `SIDE_BUTTON_FIRST + ordinal`, and draw the 3 by 3 panel in `MachineScreen` with the cells `SIDE_CELLS`. Storage stations use the new `SideModes` with world directions, a six button row and a symbol string in the state payload. The Utility Duct uses the Lib enum in a switch in `UtilityDuctBlockEntity.cycle` and is being reworked by `m5-duct-rework`.

## Goals / Non-Goals

**Goals:**
- One model, one widget, one gesture, with each block choosing its modes.
- Keep the sync and button mechanics the machines already use; move them into Lib so stations reuse them.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision
Section 2.1's side configuration bullet is reworded.

### Modes
`SideMode`: `AUTO(insert, extract, push)`, `INPUT(insert)`, `OUTPUT(extract)`, `BOTH(insert, extract)`, `PULL(insert, pull)`, `PUSH(extract, push)`, `OFF()`. Flags `allowsInsert`, `allowsExtract`, `pushes`, `pulls`. `DISABLED` is renamed `OFF`; the byte ordinal order stays Auto, Input, Output, Both, Off, Pull, Push so saved machines load unchanged. `next()` goes away; cycling lives in the config.

### Config with a supported set
`SideConfig(List<SideMode> supported, SideMode fallback)`: every side starts at the fallback; `set` of an unsupported mode falls back; `cycle` advances within the list. `SideConfig.machine()` is the full list with Auto; `SideConfig.inbound()` is Input, Pull, Off; `SideConfig.outbound()` is Push, Output, Off. NBT stays a byte array `Sides`; `isDefault` compares with the fallback. `SideConfigData implements ContainerData` with six entries reading and writing the config, for menus to `addDataSlots`.

### Widget
`lib/.../client/SideConfigPanel`: constructed with the menu's `IntFunction<SideMode>` (mode by side ordinal), a `Consumer<RelativeSide>` click handler and the anchor (GUI left and top). `render(graphics, font, mouseX, mouseY)`, `renderTooltip`, `mouseClicked`. Colours: Auto grey, Input blue, Output orange, Both green, Pull light blue, Push dark orange, Off dark red. The panel is 46 by 46 pixels at `leftPos - 50`, `topPos + 17`. Lang keys `gui.servus_lib.side.<side>` and `gui.servus_lib.side_mode.<mode>` in the Lib language provider; the Manufacturing keys are removed.

### Menu contract
`SideConfigMenus.BUTTON_FIRST = 1000`. A menu with sides adds `new SideConfigData(config)` through `addDataSlots` and handles `clickMenuButton(id)` by calling the block entity's `cycleSide(RelativeSide)` for `id - BUTTON_FIRST`. Screens build the panel with `menu::sideMode` and `side -> handleInventoryButtonClick(BUTTON_FIRST + side.ordinal())`. The Storage `CycleSidePayload`, `SideButtons` and the `sides` field of `StationStatePayload` are removed.

### Block entities
- `MachineBlockEntity`: `pushOutputs` only on `pushes()` sides; a new `pullInputs` every `PUSH_INTERVAL` on `pulls()` sides takes from the neighbour's handler whatever `acceptsInput` accepts, one stack move per side per interval; `sideMode(Direction)` unchanged.
- `InboundStationBlockEntity`: `SideConfig.inbound()`, `handlerFor(Direction)` through `RelativeSide.fromSide(facing, side)`, a `serverTick` (new ticker on the block) that pulls on Pull sides while a buffer slot is free.
- `OutboundStationBlockEntity`: `SideConfig.outbound()` with the front set to Push at creation; push on `pushes()` sides; handler on `allowsExtract()` sides.
- `CraftingStationBlockEntity`: `SideConfig.machine()`; the side handler checks `allowsInsert` and `allowsExtract`; outputs pushed on `pushes()` sides; inputs pulled on `pulls()` sides.
- Every one gets `cycleSide(RelativeSide)` and `cycleFace(Direction)` that map through the facing, mark changed, invalidate capabilities and return the new mode.

### Carrot
A Lib helper `SideConfigWrench.cycle(level, pos, face, player, blockEntityWithSides)` used by `MachineBlock`, `InboundStationBlock`, `OutboundStationBlock` and `CraftingStationBlock`: sneak dismantles with `saveToItem`, otherwise cycles the clicked face and sends the action bar message. Rotation code is removed from these blocks.

### Logistics
`UtilityDuctBlockEntity.cycle` gets `case PULL, PUSH -> SideMode.BOTH` so Logistics compiles; its behaviour is unchanged. The rename Disabled to Off touches its references.

### Minecraft side verification
GameTests: Manufacturing `machinePullsFromNeighbour` (assembler with a recipe, chest on a Pull side with ingredients and cobblestone: ingredients move, cobblestone stays) and `outputSideDoesNotPush` (Output side chest stays empty, Push side chest fills). Storage `inboundPullsFromChest`, `craftingStationSideOff`, and the existing side tests rewritten for relative sides and the new mode names.

## Risks / Trade-offs

- [Existing worlds with Output sides that pushed] → nothing is released; placed machines in Auto keep pushing.
- [Merge with `m5-duct-rework`] → both touch `UtilityDuctBlockEntity.cycle`; the merge keeps the duct rework's version plus the two cases.
- [Relative sides on stations] → a station placed with its front to the aisle has its back on the machine side; the panel letters and tooltips name front and back, which matches what the player sees.

## Open Questions

None.
