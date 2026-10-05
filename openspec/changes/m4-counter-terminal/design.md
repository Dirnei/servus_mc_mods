# Design

## Context

`WarehouseNetwork` offers `index()`, `status()`, `extract(key, amount)` and `insert(stack)`. Manufacturing shows the payload pattern (`RegisterPayloadHandlersEvent`, `PayloadRegistrar.playToClient` and `playToServer`, handlers checking `player.containerMenu`). See proposal.md.

APIs verified: `EditBox` (`setResponder`, `getValue`, `setHint`), `ByteBufCodecs.list()`, `VAR_LONG`, `StreamCodec.composite`, `ItemStack.STREAM_CODEC`, `GuiGraphics.renderItem`, `renderItemDecorations`, `renderTooltip(Font, List<Component>, Optional, int, int)`, `Screen.getTooltipFromItem`, `AbstractContainerMenu.setCarried`, `GameTestHelper.makeMockPlayer`.

## Decisions

### D1: Menu
`CounterTerminalMenu extends AbstractContainerMenu` with the 36 player slots at (8 + c * 18, 112 + r * 18) and the hotbar at y 170. It keeps the terminal block entity. `broadcastChanges()` on the server computes the stock every 10 ticks: status plus entries from the index as `StockEntry(ItemStack one, long count)`; a signature (status, entry count, sum of counts, hash of keys) decides whether `StockSyncPayload` is sent. `pull(ItemKey, toInventory)` and `pushCarried()` and `quickMoveStack` call the network; all refuse when offline.

### D2: Payloads
`StockSyncPayload(boolean online, String reason, List<StockEntry>)` server to client, stored in `ClientStock`. `PullItemPayload(ItemStack item, boolean toInventory)` and `PushCarriedPayload` client to server, handled against `player.containerMenu instanceof CounterTerminalMenu`. Registered in `ServusStoragePayloads` with registrar version "1".

### D3: Screen
`CounterTerminalScreen` 176 by 194: title, `EditBox` at (8, 18) width 160, grid 9 by 4 at (8 + c * 18, 34 + r * 18), scroll with the mouse wheel by rows. Filtering is client side on the hover name. Click on a cell sends pull (shift for inventory) when the cursor is empty, push when carrying. Hover draws a highlight and a tooltip with the stored count. Counts above 999 are shown abbreviated with k. `keyPressed` routes to the search box first so typing "e" does not close the screen.

### D4: Block and block entity
`CounterTerminalBlockEntity extends StorageNodeBlockEntity implements MenuProvider`. `CounterTerminalBlock` mirrors the controller block (facing, wrench, neighbour reporting) and opens the menu on right click.

### D5: GameTest
`ServusStorageGameTests.terminalPullsAndPushes` in the shared `empty_12x6x12` template: crate with 50 cobblestone, controller, terminal, mock survival player; after 10 ticks a menu built directly on the server pulls to the inventory and asserts 50 cobblestone in the inventory and an empty crate, then pushes 20 from the inventory and asserts the crate holds 20. Registered through `RegisterGameTestsEvent` like Manufacturing.

## Pure Java vs Minecraft

All Minecraft. Verified by the Storage GameTest, `gradlew :storage:runGameTestServer`, and the client launch.

## Risks / Trade-offs

- [Stock payload size for huge warehouses] -> Entries are one stack plus a long each; thousands of types stay well under the packet limit. Paging can come with the wireless terminal.
