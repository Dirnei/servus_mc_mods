# Design

## Context

The endpoint GUI is opened from `UtilityDuctBlock.onWrench`: it finds the clicked arm with `hitArm` and opens only when `duct.isEndpoint(side)`. `UtilityDuctBlockEntity.computeArm` returns false for a `SideMode.DISABLED` side, so a disabled side has no arm, no endpoint bit and no way back into the GUI.

`UtilityDuctMenu` is built for one fixed `side` (sent in the open buffer). Its nine `GhostSlot`s read `duct.filter(side)` through a supplier, the priority is a `ContainerData` slot refreshed in `broadcastChanges`, and the cable channels are sent once a second by `sendCableChannels`. `UtilityDuctScreen` draws line tabs left of a 176 by 204 page: mode button (full width) at y 18, channel button at y 42, filter at y 72, priority at y 94, inventory at y 122.

The line networks (`LineNetwork` and subclasses) keep one endpoint per non-disabled side, rebuilt on `DuctEvent.ConfigChanged`. `ItemChannelNetwork.Endpoint` holds its duct; `FluidChannelNetwork.Endpoint` does not. Fluid and items pushed into a duct go through `DuctFluidHandler` and `DuctItemHandler`, which know their side.

The Chimney already has a redstone mode (`ChimneyRedstone` over pure `ChimneyRules.Mode`, read with `level.hasNeighborSignal` in `ChimneyBlock`), with the lang names "Always", "While powered", "While unpowered".

## Goals / Non-Goals

**Goals:**
- Every side that touches a block can always be configured, whatever its flags.
- One redstone setting and one On/Off switch per line per side, at no tick cost while nothing changes.
- A page layout that the fluid filter and keep amounts can extend.

**Non-Goals:**
- Fluid filter, fluid priority, keep amounts (later changes).
- Redstone per side, redstone output, comparator output from the duct.

## Decisions

### D1 Open from anywhere, start side
`onWrench` (not sneaking) no longer requires `isEndpoint`. It picks the start side: the clicked arm when `isEndpoint(side)`, else the first side in `Direction.values()` order (down, up, north, south, west, east) that is selectable. A side is selectable when the neighbour block is not air and is not a `UtilityDuctBlockEntity`; a pure helper takes six flags and the clicked side and returns the start side or none. Panels keep their own sneak behaviour; a click on a panel still passes.

The selectable rule is checked on the server when opening and when a side is picked, and on the client to draw the side tabs, from the same block states.

### D2 Mutable side in the menu
`UtilityDuctMenu.side` becomes mutable. New button ids `BUTTON_SELECT_SIDE + direction.ordinal()` (40 to 45) set it on both sides (the screen's `press` already runs `clickMenuButton` locally after `MenuButtons.press`). The server refuses a side that is not selectable. On a change the server refreshes the priority data slot and calls `sendCableChannels(true)`; the ghost slots follow by themselves because their supplier reads the current side, and `broadcastChanges` sends their new contents. The menu title stays the duct's name; the screen draws the selected side and neighbour name at the top of the page instead, so it can change without reopening.

### D3 Side tabs
`logistics/client/DuctSideTabs` draws six fixed tab places (26 by 24 px, stride 28, starting 4 px in from the page's left edge) on top of the page in the order down, up, north, south, west, east. Fixed places keep every side where the player expects it; a side with nothing beside it gets no tab and leaves its place empty (chosen over an empty inert tab, which read as clickable). The selected tab is drawn after the page background, 3 px over the page's top border, so it joins the page; the others are drawn behind it. Icons: the screen asks the client level for each neighbour's block state and draws `block.getCloneItemStack(level, pos, state, false, player)` (the NeoForge overload `UtilityDuctBlock` and `GnomeHollowBlock` already override, so disguised blocks stay disguised), falling back to `state.getBlock().asItem()`. Duct sides draw the duct's clone item under a grey fill and cannot be picked. Tooltip: side name plus the block's name. No server packet is needed.

### D4 `DuctRedstone` and On/Off, pure
New pure enum `logistics/duct/DuctRedstone { IGNORE, POWERED, UNPOWERED }` with `met(boolean powered)`, `active(boolean on, boolean powered)` (on and met), `next()`, `id()` and `byId(int)` for storage. It does not reuse `ChimneyRules.Mode`, but uses the same lang names for While powered and While unpowered, plus "Ignore redstone".

On/Off is its own boolean per line per side, default on, so pausing a side never loses its redstone setting. The block entity stores `redstone[line][side]` and `switchedOn[line][side]` for item, power and fluid, saved under `"Redstone"` (line id to side name to id, only non-Ignore) and `"SwitchedOff"` (line id to side name to true, only off sides). Both are part of the update tag like the flags, so the GUI and the potato read them on the client.

Older saves of this change's first version stored `"Controls"` with ids 0 Always, 1 While powered, 2 While unpowered, 3 Off. They load with the same redstone ids, and id 3 (`DuctRedstone.legacyOff`) as switched off with Ignore redstone; the new keys win when both exist.

### D4a Flags instead of the mode cycle
The duct's single mode (`SideMode` BOTH, INPUT, OUTPUT, DISABLED, shared with machines in `foundation/side`) becomes a duct only pure enum `logistics/duct/DuctMode` over three flags: `NONE`, `IN`, `OUT`, `IN_OUT`, `IN_PULL`, `IN_OUT_PULL`. An enum of the six legal combinations (Pull only with In) keeps illegal states out, compares with `==` and saves as one name per side. `SideMode` stays for machines: its In/Out/Push/Pull meaning is the machine's view, not the duct's, and mapping In+Out+Pull onto it had no value.

`DuctMode` has `in()`, `out()`, `pull()`, `connects()` (not `NONE`), `of(in, out, pull)` (drops Pull without In), `toggleIn()` (drops Pull with In), `toggleOut()`, `togglePull()` (switching it on also switches In on), `forLine(line)` and `bySaved(name)`. `forLine` keeps Pull only on the Item Pipe and turns the data line into `IN_OUT` or `NONE`; `UtilityDuctBlockEntity.setMode` and loading apply it.

Meaning per line:
- Item Pipe: In accepts what the block pushes into the duct (`DuctItemHandler`), Out makes the block a routing target, Pull makes the network extract from the block every pull interval. In + Out + Pull pulls and receives; `route` already skips the origin inventory, so items never go back to their source side. In only accepts pushed items and never pulls (new).
- Fluid Pipe: In makes the block a source (drained) and lets it fill into the duct, Out a sink, In + Out keeps the existing source or sink rule. Pull has no extra meaning and is not shown.
- Power Cable: In draws energy from the block, Out delivers. Pull is not shown. The request named Pull as "drawing energy out of the block" for power, but In and Out already draw today (the default side of a generator gives power), and requiring Pull there would cut every existing generator off in old worlds; so power keeps In as the drawing flag.
- Data line: connected or not, through its on and off button.

Saving writes `DuctMode.key()` (`in_out` is the default and not written). `bySaved` also reads the old `SideMode` names: `both` and `auto` as In + Out, `input` and `pull` as In + Pull, `output` and `push` as Out, `disabled` as none, unknown as In + Out; `forLine` then drops Pull on fluid and power sides. Old worlds behave as before.

The networks read the flags instead of `SideMode.allowsInsert/allowsExtract`: `LineNetwork` builds endpoints for `connects()`, `ItemChannelNetwork` targets `out()` and pulls `pull()`, `FluidChannelNetwork` and `EnergyChannelNetwork` use `in()` and `out()`, `EndpointTally` counts `in()` and `out()`.

Menu buttons per line: `BUTTON_TOGGLE_IN` (10), `BUTTON_TOGGLE_OUT` (60), `BUTTON_TOGGLE_PULL` (70), `BUTTON_CYCLE_REDSTONE` (50), `BUTTON_TOGGLE_ON` (80), each plus `line.ordinal()`. On the data line `BUTTON_TOGGLE_ON` connects or disconnects the side.

### D5 Powered flag on the duct
`UtilityDuctBlockEntity.powered` is set from `level.hasNeighborSignal(worldPosition)` in `onLoad` and in `neighbourChanged` (called from `UtilityDuctBlock.neighborChanged`, which vanilla fires on redstone changes of adjacent blocks). It is saved and synced. Changing it triggers nothing else; the networks read it.

`isActive(line, side)` = `redstone[line][side].active(switchedOn[line][side], powered)`. The networks ask it when they use an endpoint, a field read per endpoint per tick, instead of rebuilding endpoints on every signal change, so a redstone clock causes no rebuild churn:
- `ItemChannelNetwork`: `pull` skips inactive sources, `capacity` returns 0 for inactive targets.
- `FluidChannelNetwork`: `Endpoint` gets its duct and side; `tickChannel` skips inactive endpoints; `fill` skips inactive sinks.
- `EnergyChannelNetwork`: skips inactive storages on both ends.
- `DuctItemHandler` and `DuctFluidHandler` accept nothing on an inactive side.

A lever on the ground beside the duct, dust pointing into it or a powered solid block next to it all power the duct through `hasNeighborSignal`. A lever cannot be placed on a duct itself (no sturdy face), which is fine.

### D6 Layout
The page stays 176 by 204 px with the original `textures/gui/utility_duct.png`, because the side tabs sit above it. Coordinates relative to the page:
- y 6: the side and neighbour name ("West: Chest"), in place of the menu title.
- y 17, 20 px high: the icon row from `logistics/client/DuctIconButton`. In (hopper) at x 8, Out (dispenser) at x 29, Pull (sticky piston, Item Pipe only) at x 50, all 20 by 20; the redstone button at x 80 (gunpowder for Ignore redstone, redstone torch item for While powered, the vanilla `textures/block/redstone_torch_off.png` for While unpowered); On/Off at x 101, 28 by 20, labelled "On" or "Off". The data page has only its 28 by 20 on and off button at x 8. Toggles show on with a light face and a green bar under the icon, off darker with the icon dimmed. Pull is never disabled: pressing it while In is off switches In on with it. The screen draws the tooltips itself.
- y 41: the channel button, full width.
- y 63 filter label, y 73 filter or fluid filter slots, y 93 priority (minus at x 8, label, plus at x 84).
- Trash can: 12 by 12 at x 156, y 108, right of the inventory label and away from the settings row, drawn as a small can; tooltip "Remove Item Pipe" (per line) plus a grey line that it takes the line out of the whole duct and drops it, and that On/Off pauses instead.
- y 111 inventory label, y 122 inventory.
Line tabs on the left edge start at y 17.

Every icon button explains itself in its tooltip: In, Out and Pull name their state and what they mean on that line (a side with neither In nor Out says it does not connect), the redstone button names its setting and that it follows a signal into the duct, On/Off says that off pauses and keeps connection and settings.

### D7 Potato
`DuctReport` puts the flags (`gui.servus_foundation.mode.<key>`: "Not connected", "In", "Out", "In, Out", "In, Pull", "In, Out, Pull") and the redstone setting by name into the item, power and fluid side lines; a switched off side reads "<setting>, switched off", an active one whose setting is not met "<setting>, paused". The centre report names the duct "powered" or "unpowered".

## Settled PLAN.md decisions

PLAN.md has no open decision for the endpoint GUI. The change adds the duct overflow work to "Open: Foundation" (fluid filter, keep amounts, a possible Overflow Valve block, warehouse excess on stock rules) and settles: In, Out and Pull flags, a redstone setting and On/Off per line per side, redstone read per duct.

## APIs

- `Level.hasNeighborSignal(BlockPos)` and `Block.neighborChanged(BlockState, Level, BlockPos, Block, Orientation, boolean)`, both already used (`ChimneyBlock`, `UtilityDuctBlock`) in this code base on 26.1.2.
- `Block.getCloneItemStack(LevelReader, BlockPos, BlockState, boolean, Player)` (NeoForge overload), as overridden in `UtilityDuctBlock` and `GnomeHollowBlock`.
- `AbstractContainerMenu.clickMenuButton` and the existing `MenuButtons.press` for the side, flag, redstone and On/Off buttons.

## Pure vs Minecraft

- Pure, JUnit: `DuctMode` (flag rules, toggles, line rules, saved and older names), `DuctRedstone` (activity table with On/Off, cycle, id round trip, older Control ids), the start side helper (`DuctSides.startSide(selectable[], clicked)`).
- Minecraft, GameTest: redstone toggling a pull side, a switched off side not accepting pushed fluid and keeping its redstone setting, In only not pulling, In + Out + Pull not routing back, reopening an unconnected side through the menu, settings surviving save and load, older saves loading with the flags.
- Manual: side tab icons, greyed duct tabs, icon buttons, trash can, layout, tooltips.

## Risks / Trade-offs

- [Redstone dust next to a duct does not point into it] -> dust only powers what it points at; the tooltip of While powered says "a redstone signal into the duct", and a block or lever beside it works.
- [Selectable rule differs between client and server for an unloaded or changing neighbour] -> the server re-checks on every pick and ignores invalid ones; the client redraws from the current state every frame.
- [Trash can clicked by mistake] -> it is small, sits in the bottom right corner away from the settings row, and its tooltip says it removes the line from the whole duct and that On/Off pauses instead.

## Decisions made while applying

- The flag, On/Off, redstone and side tab GameTests live in a new `logistics/gametest/DuctControlGameTests` instead of the already long `ServusLogisticsGameTests`.
- The independent lines GameTest turns the machine around: a Coal Generator on a side with item redstone setting While unpowered and power Ignore redstone under a powered duct gives its energy to an Electric Pump while no coal reaches it. Foundation has no machine that both stores received energy and takes items on one face.
- A floor or any other block under a duct is a selectable side, so the side picker GameTests place their ducts in the air.
- The potato puts the redstone setting as one argument after the flags: its name, "<setting>, switched off" while the side is off, or "<setting>, paused" while the setting is not met. The centre header names the duct "powered" or "unpowered".
- The page draws "West: Chest" at its top in place of the menu title; the old "Utility Duct, West: Chest" title key is gone.
- The duct reads its redstone signal only while all six neighbours are loaded, like its resting pose, so a load at a chunk edge never pulls in a neighbour chunk.
- After the first play check the mode cycle and the Control button were reworked (design D3, D4, D4a, D6): the mode became the In, Out and Pull flags, Control became a redstone setting on an icon button plus a separate On/Off, the side picker row became side tabs on top, and the remove button became a trash can in the bottom right corner. Pull shows only on the Item Pipe page.
- `DuctControlGameTests` keeps its name; it now covers flags, On/Off and redstone.
