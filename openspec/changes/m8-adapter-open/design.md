# Design

## Context

See proposal.md. Every Servus block decides in `useItemOn` whether the held item or the block reacts. Returning `ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION` lets vanilla call `useWithoutItem` (the block's GUI or summary); `SKIP_DEFAULT_BLOCK_INTERACTION` goes straight to the item's `useOn`.

## Decisions

### Vanilla rule, verified in source
`ServerPlayerGameMode.useItemOn` (NeoForge 21.1.255 sources) skips the block entirely when the player is sneaking with an item in either hand (`isSecondaryUseActive() && flag`), unless the item bypasses sneaking. Otherwise it calls `BlockState.useItemOn`, and on `PASS_TO_DEFAULT_BLOCK_INTERACTION` with the main hand calls `useWithoutItem`. The Configuration Carrot only overrides `useOn` (not `onItemUseFirst`), so a sneak click with the carrot always reaches the block's `Wrenchable.onWrench`.

So a block that opens a GUI should return `PASS_TO_DEFAULT_BLOCK_INTERACTION` for every item it does not handle itself; sneaking then gives the held item its normal use, like chests.

### Changed blocks
- **Machine Adapter**: the `useItemOn` override that skipped the block for any non empty hand is removed. Any item, the carrot included, opens the GUI without sneaking; the adapter has no side modes, so a plain carrot click has nothing else to do. Sneak plus carrot still dismantles through `onWrench`.
- **Crafting CPU**: returns `SKIP_DEFAULT_BLOCK_INTERACTION` only for the carrot (it forms the cube through `onWrench`), `PASS_TO_DEFAULT_BLOCK_INTERACTION` for everything else, so the slot summary shows with any held item.

### Audited, already following the rule
Warehouse Controller, Crafting Station, Shuttle Garage, Outbound Station, Inbound Station and Access Point (shared `InboundStationBlock`), Pattern Storage, Production Terminal, Counter Terminal: they skip only for the carrot (side modes or dismantle) and open their GUI or summary with any other item. Manufacturing `MachineBlock` and `SinkBlock` were not changed by this change (out of the M8 Storage and Foundation scope of the request; they keep their existing carrot handling).

### Deliberate exceptions
- **Utility Duct** (data summary): its summary needs an empty hand. Ducts are thin connectors that players build around; a plain click with a block in hand must keep placing the block against the duct without sneaking, and line items, facades and the carrot have their own duct actions.
- **Fluid Tank**: fluid containers fill or empty it, other items act normally; an empty hand reads it out.
- **Crate** and **Heavy Duty Rack**: a held item is inserted (crate) or acts outside the front (rack); the Zone Label stamps them.
- **Rails and elevators**: no GUI.

## Testing
GameTests call `BlockState.useItemOn` with a held stack and check the result: the adapter returns `PASS_TO_DEFAULT_BLOCK_INTERACTION` for cobblestone and the carrot, the CPU returns it for cobblestone and `SKIP_DEFAULT_BLOCK_INTERACTION` for the carrot. The sneaking path is vanilla and is checked in game. No pure logic.

## Risks / Trade-offs
- [A player who wants to place a block against an adapter or CPU now has to sneak] -> same as chests, the expected convention.
