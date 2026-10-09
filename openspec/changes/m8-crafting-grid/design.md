# Design

## Context

The Counter Terminal menu (`menu/CounterTerminalMenu`) holds the output slots, then the player inventory and hotbar; the screen (`client/CounterTerminalScreen`) is laid out from `terminal/TerminalLayout.Counter`, whose areas `TerminalLayoutTest` keeps apart and inside the panel (max 266 px high for 1080p at GUI scale 4). Pushes into the warehouse use `WarehouseNetwork.insert` and instant pulls use `WarehouseNetwork.extract`, both skipping racks. Warehouses are found with `TerminalWarehouses.reachable` / `TerminalRequests.first`. JEI already has a transfer handler for the Production Terminal. See proposal.md for why.

## Goals / Non-Goals

**Goals:** a vanilla correct crafting grid, refill from instant stock, a way to pull stock to the cursor, clear, close, JEI fill.

**Non-Goals:** rack stock, reservations, persistence of grid contents (see proposal).

## Decisions

### Layout
The panel grows from 222 to 260 px; the right column keeps requests, message and output, and gains below the output a "Crafting" label with a clear button at its right end, the 3x3 grid at x 190, an arrow and the result slot at its right. The left column is unchanged (stock, inventory, hotbar). `TerminalLayout.Counter` gets `CRAFT_LABEL`, `CLEAR`, `CRAFT_GRID`, `CRAFT_RESULT`; `areas()` lists them so the existing overlap and fit tests cover them. A mode toggle was not needed because 260 fits under 266.

### Menu
- `TransientCraftingContainer` (3x3) and `ResultContainer` with a `ResultSlot` subclass; slot order: outputs, inventory (36), grid (9), result (1). Inventory indices stay where they were, so existing quick moves keep working.
- `slotsChanged` recomputes the result on the server like vanilla `CraftingMenu.slotChangedCraftingGrid`, which is `protected static` there, so the same steps are written out with the result slot's own index (`RecipeManager.getRecipeFor(RecipeType.CRAFTING, input, level, last)`, `ResultContainer.setRecipeUsed`, `assemble`, `ClientboundContainerSetSlotPacket`). Source: NeoForge 21.1.255 sources, `net.minecraft.world.inventory.CraftingMenu`.
- Refill: the result slot remembers the grid's item keys before `super.onTake` (vanilla removes one per slot and places remainders); afterwards every slot whose count dropped and whose old key is known gets one more of that key from `extract` over the reachable warehouses, if the slot is empty or still holds that key. Remainders stay where vanilla puts them.
- Shift click on the result follows vanilla `CraftingMenu.quickMoveStack` for index 0 (move to inventory, `onQuickCraft`, `onTake`); `AbstractContainerMenu.doClick` repeats quick move while the result stays the same item, which gives "craft until stock runs out". Shift click on a grid slot moves it to the inventory; shift click on an inventory slot keeps storing it in the warehouse as today.
- `removed`: grid contents go to the warehouse, remainder to the player (`moveItemStackTo` or `player.getInventory().placeItemBackInInventory`).

### Payloads (client to server)
- `GridPullPayload(item)`: Alt click; `pullToCursor(key)` extracts up to a stack when the cursor is empty.
- `GridClearPayload`: clear button.
- `GridFillPayload(List<List<ItemStack>> slots, boolean max)`: JEI; each of the 9 entries lists the acceptable stacks for that grid slot (empty for none). The server clears the grid, then per slot takes the first option available from instant stock, else from the player inventory; with max it takes as many per slot as fit for the smallest stack size.
All registered in `ServusStoragePayloads` with `CounterTerminalMenu` checks.

### Alt click
Alt is unused by the stock grid today (plain opens the request prompt, shift requests a stack, Ctrl or middle opens the craft dialog). `Screen.hasAltDown()` in `mouseClicked` over a stock entry sends `GridPullPayload`.

### JEI
`CounterTerminalRecipeTransferHandler implements IRecipeTransferHandler<CounterTerminalMenu, RecipeHolder<CraftingRecipe>>` reads the recipe slot views (`IRecipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT)`), maps each to its grid index (3 columns, shaped recipes keep their position as JEI lays them out in the 3x3 view), and sends `GridFillPayload` on `doTransfer`. It returns null (no error) so the button is always usable while a warehouse or the inventory may hold the items.

### Testing
JUnit: layout areas through the existing `TerminalLayoutTest`. GameTests (`CraftingGridGameTests`): craft sticks from planks pulled from a crate, shift craft refill empties the crate, rack planks are not used, clear returns to storage, close returns to storage, fill payload logic fills from the crate first. Screen and JEI checked in game.

### PLAN.md
No open decision touched.

## Risks / Trade-offs

- [Grid takes stock reserved for a running order] -> recorded non-goal; the order reports missing and retries as with other instant pulls.
- [Vanilla crafting logic copied, not called] -> kept to the same steps as `CraftingMenu`, covered by the sticks GameTests.
