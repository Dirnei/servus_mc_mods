# Design

## Context

See proposal.md for the why. `MachineBlockEntity.sideHandler(Direction)` returns a `SideHandler` for every side; on a side whose mode allows nothing it refuses every insert and extract, but it is still a capability. The Utility Duct draws an item arm wherever `Capabilities.Item.BLOCK` returns a handler, so it connects to a fixed Off front. The Sink already returns no handler on its front faces, which is the pattern to follow. Fixed sides come from `SideConfig.lock(...)` and are read with `SideConfig.isLocked(RelativeSide)`.

The Receptaculum's `acceptsInput` takes any non-Elixir item into the gift slots; the screen and automation share it. Gnome gifts are `GnomeGiftData` (`forItem(stack)`), a server reload listener.

## Goals / Non-Goals

**Goals:**
- No capability on sides fixed to Off, for every machine that registers through the shared side handler.
- Automation fills the gift slots only with liked gifts.

**Non-Goals:**
- Hiding capabilities on sides a player sets to Off (they can change at any time and would need `invalidateCapabilities` on every change).

## Decisions

### `itemCapability(Direction)` on `MachineBlockEntity`
Returns `null` when the side is fixed (`sideConfig().isLocked(relativeSide(side))`) and its mode is `DISABLED`; otherwise `sideHandler(side)`. A null side (the automation view) keeps `sideHandler(null)`. Every `Capabilities.Item.BLOCK` registration that used `sideHandler(side)` switches to it: `MachineEvents` (Coal Generator, Burner Pump) and `ServusAlchemyEvents` (Salamander Forge, Receptaculum, Prelum, the shared machine registration). Fixed sides never change at runtime, so no capability invalidation is needed.
Alternative: return null from `sideHandler` itself. Rejected because GameTests and screens call `sideHandler(side)` on every side and expect a handler.

### Gift filter for automation only
`MachineBlockEntity` gets a hook `acceptsAutomationInput(int slot, ItemStack stack)` that defaults to `acceptsInput(slot, stack)` and is checked by `SideHandler.insertItem` and `isItemValid`. The Receptaculum overrides it: a gift slot additionally needs `GnomeGiftData.INSTANCE.forItem(stack).isPresent()`. The screen's slots keep `acceptsInput`.
Why not filter the screen too: `GnomeGiftData` lives on the server; on a dedicated server the client would refuse every item in its slot prediction.

### Pure Java and GameTests
- No new pure logic.
- GameTests in `HomunculusGameTests`: `pipeTakesTheYieldFromTheSide` (Receptaculum with Raw Iron in a yield slot, an Item Pipe on its east side, a chest behind the pipe: the chest receives it), `noPipeArmAtTheFront` (a pipe north of a north-facing Receptaculum has no arm south and the front has no item capability), `automationRefusesJunkGifts` (the side handler refuses Cobblestone and takes a cookie into a gift slot), `handTakesAnyGift` (the screen's gift slot still takes Cobblestone).
- Foundation GameTest `fixedFrontOffersNoItems` for a Coal Generator front, next to the existing side tests.

### APIs used
`RegisterCapabilitiesEvent.registerBlockEntity` with a provider returning `null` (supported: no capability on that side), as the Sink already does.

## Risks / Trade-offs

- [A hopper below a machine's fixed front side no longer sees it] → intended; fixed Off sides never moved anything.
- [Packs that relied on any item in the gift slots by pipe] → unreleased project; gifts are data, so a pack adds its items to the gift data.
