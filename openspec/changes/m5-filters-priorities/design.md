# Design

## Context

The utility duct has per side modes, a plain configuration screen and two payloads. Manufacturing shows the ghost slot pattern (`PatternSlot`, `clicked` override) and menu button ids. See proposal.md.

## Decisions

### D1: Block entity state
`UtilityDuctBlockEntity` gains `ItemStackHandler[6] filters` (nine slots each) and `int[6] priorities`. `filterAllows(side, stack)` is true for an empty list or any `ItemStack.isSameItem` match. `setPriority` clamps to -9..9. Both are saved under `Filters.<side>` (only non empty) and `Priorities.<side>` (only non zero), included in the update tag. Any change calls `changed()` and posts `DuctEvent.ConfigChanged` (renamed from ModesChanged) to the channel networks. The block entity implements `MenuProvider`.

### D2: Menu
`UtilityDuctMenu extends AbstractContainerMenu`: nine `GhostSlot`s backed by a `SelectedSideHandler` that delegates to the duct's filter handler of the currently selected side, plus the player inventory. `clickMenuButton` ids: 0 to 5 select a side; 10 plus channel ordinal cycles that channel's mode on the selected side; 20 plus channel ordinal removes the channel; 30 and 31 change the priority. The selected side lives in the menu on both sides, kept in step because both run `clickMenuButton`. `clicked` on a ghost slot copies the carried stack; `quickMoveStack` from the inventory fills the first free ghost slot. A `ContainerData` of two values syncs the selected side and its priority to the client.

### D3: Screen
`UtilityDuctScreen extends AbstractContainerScreen`, 176 by 212: six side cells at y 18 (selected drawn brighter), one mode button per present channel at y 38 (labels read the client block entity), the filter row at y 64, priority minus and plus with the value at y 86, remove buttons at y 106, player inventory from y 130. Mode and remove buttons are `Button`s calling `handleInventoryButtonClick`.

### D4: Network
`ItemChannelNetwork.Endpoint` carries the duct, the side, the mode and the priority. `flat()` sorts by priority descending; `route` walks the distinct priorities in order and round robins inside each group with a cursor per priority; it skips endpoints whose duct filter rejects the stack. `pull` only extracts stacks the pull side's filter allows.

### D5: Removals
`ClientHooks`, `SetDuctModePayload`, `RemoveChannelPayload` and `ServusLogisticsPayloads` are deleted. `UtilityDuctBlock.useWithoutItem` calls `player.openMenu(duct, pos)`.

## Pure Java vs Minecraft

All Minecraft. Verified over RCON on `runServerAll`: a filtered pull leaves dirt behind; two outputs with priorities 5 and 0 receive 20 and 0; filling the priority 5 chest overflows to the other.

## Risks / Trade-offs

- [Sorting endpoints on every rebuild] -> Lists are tiny (six per duct).
- [Ghost slot handler swaps when the side changes] -> The slots re-read through the delegate each tick; the menu's normal slot sync carries the new contents to the client.
