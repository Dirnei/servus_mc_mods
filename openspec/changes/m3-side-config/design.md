# Design

## Context

Machines expose one item handler on every side and push outputs to all six neighbours. The user wants per side configuration in the GUI, not through the wrench. See proposal.md and the delta specs.

APIs verified: `BlockEntity.saveToItem(ItemStack, HolderLookup.Provider)` writes block entity data into the item so `BlockItem` restores it on placement; `Direction.getClockWise/getCounterClockWise`; vanilla `clickMenuButton` as used by the assembler.

## Decisions

### D1: Pure types in Lib
`SideMode` enum with `allowsInsert()`, `allowsExtract()`, `next()`. `RelativeSide` enum (FRONT, BACK, LEFT, RIGHT, TOP, BOTTOM) with `toSide(Side facing)` and `fromSide(Side facing, Side absolute)` on the graph `Side` type; left is the facing rotated counter clockwise, right clockwise, so "left" is the machine's own left. `SideConfig` holds six modes, `mode`, `set`, `cycle`, `toBytes`/`fromBytes` for persistence.

### D2: Machine wiring
`MachineBlockEntity` owns a `SideConfig`. `sideHandler(Direction)` returns a cached per side view: insert allowed when the relative side allows insertion and the slot role allows it; extract allowed when the side allows extraction and the slot is an output. A null side uses the Auto rules. Output push iterates only directions whose relative side allows extraction. Container data indices 7 to 12 carry the six modes; `DATA_COUNT` becomes 13. NBT key `Sides` as a byte array. `MachineMenu.clickMenuButton` handles ids 1000 to 1005 as "cycle relative side", and the assembler menu defers unknown ids to the base.

### D3: GUI panel
`MachineScreen` draws a 3 by 3 grid of 14 pixel cells left of the GUI (x from `leftPos - 46`, y from `topPos + 17`): top row holds TOP, middle row LEFT, FRONT, RIGHT, bottom row BACK below FRONT and BOTTOM in the corner... kept simple: row 1 TOP centre, row 2 LEFT FRONT RIGHT, row 3 BACK centre and BOTTOM right. Colors: Auto grey, Input blue, Output orange, Both green, Disabled dark red. Hover shows "Side: Mode"; click sends the button id. Lives in Manufacturing for now; moved to Lib when Logistics needs it.

### D4: Dismantle keeps data
`MachineBlock.onWrench` dismantle drops the contents first, then `saveToItem` on the machine item, so the item carries side config, selected recipe, owner and energy. The loot table drop on tool break stays a fresh item.

## Pure Java vs Minecraft

- Pure Java, JUnit tested: `SideMode`, `RelativeSide`, `SideConfig`.
- Minecraft, verified over RCON: side modes set through `data merge` of the `Sides` byte array, hoppers and a chest as neighbours, `data get` of the results. The panel click is a manual check.

## Risks / Trade-offs

- [Auto keeps pushing into every neighbour] -> Intended default; players restrict sides where it matters.
- [saveToItem also copies energy and progress] -> Harmless and arguably fair for a dismantle.
