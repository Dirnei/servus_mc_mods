# Design

## Context

The endpoint GUI (`UtilityDuctScreen`, `UtilityDuctMenu`) shows nine `GhostSlot`s that read and write the selected side's `ItemSlots` filter on the duct block entity. `UtilityDuctMenu.clicked` copies the cursor into a slot (an empty cursor clears it) and `quickMoveStack` copies a shift clicked inventory item into the first free slot. Slot contents reach the client through the normal menu slot sync. `UtilityDuctBlockEntity.filterAllows` matches by `ItemStack.isSameItem`, so only the item counts, never its components.

Storage already has a search picker: `storage/client/ItemPicker` (button plus popup with search field, scroll list, icons, "none" entry) on the pure `storage/picker/PickerList` and `PickerStock`, used by the Field Head. Foundation cannot use it while it lives in Storage. Foundation's JEI plugin (`integration/jei/ServusFoundationJeiPlugin`) only registers subtypes and example recipes so far; JEI is a compile only dependency.

This is the first of three changes; `m5-named-channels` and then `m5-data-potato` build on it and are applied after it.

## Goals / Non-Goals

**Goals:**
- Set a filter slot from JEI or from a search over every item, without owning the item.
- One shared picker in Foundation for all Servus GUIs.

**Non-Goals:**
- No change to how filters match or to routing.
- No picker button in the duct GUI; the slots themselves open the popup.

## Decisions

### D1: The picker moves to Foundation, unchanged in behaviour

`ItemPicker` moves to `foundation/client/ItemPicker`, `PickerList` and `PickerStock` to `foundation/picker/` with their JUnit tests (`PickerListTest`, `PickerStockTest`) to `foundation/src/test/.../picker/`. Storage imports them from Foundation; Storage already depends on Foundation, so no new dependency appears. A plain move keeps the Field Head behaviour and the tests as the proof.

The duct GUI opens the popup without its button: `ItemPicker` gains a way to open for a caller supplied target (the slot index is remembered by the screen), and `renderButton` is simply not called. Alternative: a new popup class in Foundation. Rejected, it would copy the search, scroll and list code.

### D2: Every registered item, one entry each

The entries are `BuiltInRegistries.ITEM` without air, one entry per item, id `namespace:path`, icon `new ItemStack(item)`, name the item's name. The filter ignores components, so creative tab variants (enchanted books, potions) would only add duplicates. The list is built once per client session on first open and sorted by name with `PickerList.sorted`; typing filters with `PickerList.filter`. Alternative: the creative search tab contents. Rejected: duplicates per component, and the tab is only filled after the creative screen ran once.

### D3: Slot clicks

| Click on a filter slot | Effect | Where |
|---|---|---|
| Left, empty cursor | open the search popup for this slot | client only, nothing is sent |
| Left, item on cursor | set the slot to one copy, cursor kept | server, as today |
| Right | clear the slot | server, `UtilityDuctMenu.clicked` with button 1 |
| Shift click inventory item | first free slot, as today | server |

The screen intercepts the empty cursor left click in `slotClicked` before it reaches the server, because the server would otherwise clear the slot (today's behaviour for an empty cursor). While the popup is open the screen passes the mouse to the popup first, like the Field Head.

### D4: Setting a slot by id goes through a payload

`SetDuctFilterPayload(int slot, Optional<Identifier> item)`, client to server, in `logistics/menu/`, registered with `playToServer` in `data/ServusFoundationPayloads`. The handler runs on the server thread like Storage's `ChooseFieldCropPayload` handler, which touches the menu and world directly, and only acts when `player.containerMenu` is a `UtilityDuctMenu` whose duct still carries the Item Pipe and the slot index is in range; an unknown item id or an empty optional clears the slot. It calls a new `UtilityDuctMenu.setFilter(int slot, ItemStack stack)`, which uses `GhostSlot.setGhost` so the copy rule stays in one place. Both the picker and the JEI drop use this payload. Alternative: encode the item in a `clickMenuButton` id. Rejected, ids are ints and registry ids are not stable numbers.

### D5: JEI ghost ingredient handler

`ServusFoundationJeiPlugin.registerGuiHandlers` adds `IGuiHandlerRegistration.addGhostIngredientHandler(UtilityDuctScreen.class, handler)`. The handler (`integration/jei/DuctFilterGhostHandler`) returns one `IGhostIngredientHandler.Target` per filter slot when the screen shows the Item Pipe page, the popup is closed and the ingredient is an item stack (`ITypedIngredient.getItemStack()`); `Target.getArea()` is the slot's 16 by 16 screen rect from a new `UtilityDuctScreen.filterSlotArea(int)`, `accept` sends `SetDuctFilterPayload`. `shouldHighlightTargets` keeps its default (true). API verified in `jei-26.1.2-common-api-29.43.0.107.jar`: `IGhostIngredientHandler<T extends Screen>` with `getTargetsTyped(T, ITypedIngredient<I>, boolean)` and `onComplete()`, `Target<I>` with `getArea()` returning `Rect2i` and `accept(I)`, and `IGuiHandlerRegistration.addGhostIngredientHandler(Class<T>, IGhostIngredientHandler<T>)`.

### APIs

- NeoForge: `RegisterPayloadHandlersEvent`, `PayloadRegistrar.playToServer`, `ClientPacketDistributor.sendToServer` (as in Storage's `ChooseFieldCropPayload`).
- Minecraft: `BuiltInRegistries.ITEM`, `AbstractContainerScreen.slotClicked`, `AbstractContainerMenu.clicked`.
- JEI API 29.43 as listed in D5.

### Pure logic and tests

- Pure: `PickerList`, `PickerStock` keep their JUnit tests after the move.
- Minecraft: `UtilityDuctMenu.setFilter` and the right click clear get GameTests in `ServusLogisticsGameTests` (mock player, menu built directly). The popup and the JEI drag are checked in game.

### PLAN.md

No open decision in `PLAN.md` covers this. "Open: Foundation" lists this change, `m5-named-channels` and `m5-data-potato` since the proposal; archiving removes this one.

## Risks / Trade-offs

- [The list holds tens of thousands of entries in a big pack] → built once, filtered on typing only, and the popup draws only its seven visible rows.
- [A modded item whose name throws or is empty] → fall back to the registry id as name.
- [JEI renamed the ghost API in a later 26.1 build] → the handler is isolated in one class under `integration/jei`, loaded only with JEI.
