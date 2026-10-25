# Design

## Context

The endpoint GUI is opened from `UtilityDuctBlock.onWrench`: it finds the clicked arm with `hitArm` and opens only when `duct.isEndpoint(side)`. `UtilityDuctBlockEntity.computeArm` returns false for a `SideMode.DISABLED` side, so a disabled side has no arm, no endpoint bit and no way back into the GUI.

`UtilityDuctMenu` is built for one fixed `side` (sent in the open buffer). Its nine `GhostSlot`s read `duct.filter(side)` through a supplier, the priority is a `ContainerData` slot refreshed in `broadcastChanges`, and the cable channels are sent once a second by `sendCableChannels`. `UtilityDuctScreen` draws line tabs left of a 176 by 204 page: mode button (full width) at y 18, channel button at y 42, filter at y 72, priority at y 94, inventory at y 122.

The line networks (`LineNetwork` and subclasses) keep one endpoint per non-disabled side, rebuilt on `DuctEvent.ConfigChanged`. `ItemChannelNetwork.Endpoint` holds its duct; `FluidChannelNetwork.Endpoint` does not. Fluid and items pushed into a duct go through `DuctFluidHandler` and `DuctItemHandler`, which know their side.

The Chimney already has a redstone mode (`ChimneyRedstone` over pure `ChimneyRules.Mode`, read with `level.hasNeighborSignal` in `ChimneyBlock`), with the lang names "Always", "While powered", "While unpowered".

## Goals / Non-Goals

**Goals:**
- Every side that touches a block can always be configured, whatever its modes.
- One Control per line per side, with redstone, at no tick cost while nothing changes.
- A page layout that the fluid filter and keep amounts can extend.

**Non-Goals:**
- Fluid filter, fluid priority, keep amounts (later changes).
- Redstone per side, redstone output, comparator output from the duct.

## Decisions

### D1 Open from anywhere, start side
`onWrench` (not sneaking) no longer requires `isEndpoint`. It picks the start side: the clicked arm when `isEndpoint(side)`, else the first side in `Direction.values()` order (down, up, north, south, west, east) that is selectable. A side is selectable when the neighbour block is not air and is not a `UtilityDuctBlockEntity`; a pure helper takes six flags and the clicked side and returns the start side or none. Panels keep their own sneak behaviour; a click on a panel still passes.

The selectable rule is checked on the server when opening and when a side is picked, and on the client to draw the picker, from the same block states.

### D2 Mutable side in the menu
`UtilityDuctMenu.side` becomes mutable. New button ids `BUTTON_SELECT_SIDE + direction.ordinal()` (40 to 45) set it on both sides (the screen's `press` already runs `clickMenuButton` locally after `MenuButtons.press`). The server refuses a side that is not selectable. On a change the server refreshes the priority data slot and calls `sendCableChannels(true)`; the ghost slots follow by themselves because their supplier reads the current side, and `broadcastChanges` sends their new contents. The menu title stays the duct's name; the screen draws the selected neighbour's name under the picker instead, so it can change without reopening.

### D3 Picker icons
The screen asks the client level for each neighbour's block state and draws `block.getCloneItemStack(level, pos, state, false, player)` (the NeoForge overload `UtilityDuctBlock` and `GnomeHollowBlock` already override, so disguised blocks stay disguised), falling back to `state.getBlock().asItem()` and then to a plain frame when both are empty. Duct sides draw the duct's clone item under a grey fill; air draws an empty frame. Tooltip: side name plus the block's name. No server packet is needed.

### D4 `DuctControl`, pure
New pure enum `logistics/duct/DuctControl { ALWAYS, POWERED, UNPOWERED, OFF }` with `active(boolean powered)`, `next()` and `byId(int)` for storage. It does not reuse `ChimneyRules.Mode`, because Off is a duct concept, but uses the same lang names for the first three plus a new "Off".

The block entity stores `controls[line][side]` for item, power and fluid, saved under `"Controls"` as a compound of line id to a compound of side name to id, written only for non-default values. Missing entries load as Always, so old worlds behave as before. Controls are part of the update tag like the modes, so the GUI and the potato read them on the client.

### D5 Powered flag on the duct
`UtilityDuctBlockEntity.powered` is set from `level.hasNeighborSignal(worldPosition)` in `onLoad` and in `neighbourChanged` (called from `UtilityDuctBlock.neighborChanged`, which vanilla fires on redstone changes of adjacent blocks). It is saved and synced. Changing it triggers nothing else; the networks read it.

`isActive(line, side)` = `controls[line][side].active(powered)`. The networks ask it when they use an endpoint, a field read per endpoint per tick, instead of rebuilding endpoints on every signal change, so a redstone clock causes no rebuild churn:
- `ItemChannelNetwork`: `pull` skips inactive sources, `capacity` returns 0 for inactive targets.
- `FluidChannelNetwork`: `Endpoint` gets its duct and side; `tickChannel` skips inactive endpoints; `fill` skips inactive sinks.
- `EnergyChannelNetwork`: skips inactive storages on both ends.
- `DuctItemHandler` and `DuctFluidHandler` accept nothing on an inactive side.

A lever on the ground beside the duct, dust pointing into it or a powered solid block next to it all power the duct through `hasNeighborSignal`. A lever cannot be placed on a duct itself (no sturdy face), which is fine.

### D6 Layout
The picker is a row of six 20 by 20 buttons at the top of the page (y 16), the neighbour's name below it (y 39). The page content moves down by 34 px: mode and Control share the old mode row as two half width buttons (mode left, Control right), then channel, filter and priority as before. The page grows from 204 to 238 px high, and `textures/gui/utility_duct.png` gets the taller background (it stays within 256). Tabs start under the picker. The Control button shows "Control: Always" and its tooltip explains that Off keeps the connection.

### D7 Potato
`DuctReport` adds the Control name and, when inactive, "paused" to the item, power and fluid side lines, and "powered" or "unpowered" to the centre report. The lang key `message.servus_foundation.potato.item_side` and its fluid and power siblings get the extra arguments.

## Settled PLAN.md decisions

PLAN.md has no open decision for the endpoint GUI. The change adds the duct overflow work to "Open: Foundation" (fluid filter, keep amounts, a possible Overflow Valve block, warehouse excess on stock rules) and settles: Control per line per side, redstone read per duct.

## APIs

- `Level.hasNeighborSignal(BlockPos)` and `Block.neighborChanged(BlockState, Level, BlockPos, Block, Orientation, boolean)`, both already used (`ChimneyBlock`, `UtilityDuctBlock`) in this code base on 26.1.2.
- `Block.getCloneItemStack(LevelReader, BlockPos, BlockState, boolean, Player)` (NeoForge overload), as overridden in `UtilityDuctBlock` and `GnomeHollowBlock`.
- `AbstractContainerMenu.clickMenuButton` and the existing `MenuButtons.press` for the side and Control buttons.

## Pure vs Minecraft

- Pure, JUnit: `DuctControl` (activity table, cycle, id round trip), the start side helper (`DuctSides.startSide(selectable[], clicked)`).
- Minecraft, GameTest: redstone toggling a pull side, Off side not accepting pushed fluid, reopening a disabled side through the menu, Controls surviving save and load.
- Manual: picker icons, greyed duct buttons, layout, tooltips.

## Risks / Trade-offs

- [Redstone dust next to a duct does not point into it] -> dust only powers what it points at; the tooltip of While powered says "a redstone signal into the duct", and a block or lever beside it works.
- [Selectable rule differs between client and server for an unloaded or changing neighbour] -> the server re-checks on every pick and ignores invalid ones; the client redraws from the current state every frame.
- [Taller GUI] -> 238 px fits at GUI scale 3 on a 1080p screen; smaller screens fall back to a lower scale as with other tall Servus screens.

## Decisions made while applying

- The Control, redstone and side picker GameTests live in a new `logistics/gametest/DuctControlGameTests` instead of the already long `ServusLogisticsGameTests`.
- The independent lines GameTest turns the machine around: a Coal Generator on a side with item Control While unpowered and power Control Always under a powered duct gives its energy to an Electric Pump while no coal reaches it. Foundation has no machine that both stores received energy and takes items on one face.
- A floor or any other block under a duct is a selectable side, so the side picker GameTests place their ducts in the air.
- The potato puts the Control as one argument after the mode: the Control's name, or "<Control>, paused" while it blocks the side. The centre header names the duct "powered" or "unpowered".
- The menu title is the plain "Utility Duct"; the old "Utility Duct, West: Chest" title key is replaced by the neighbour line "West: Chest" under the picker.
- The duct reads its redstone signal only while all six neighbours are loaded, like its resting pose, so a load at a chunk edge never pulls in a neighbour chunk.
