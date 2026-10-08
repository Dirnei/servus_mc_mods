# Design

## Context

`CounterTerminalMenu` sends a `StockSyncPayload` from `broadcastChanges` every 10 ticks when its signature changed; the first send therefore comes 10 ticks after opening. `ProductionTerminalMenu` does the same with `ProductionPayload`. The Counter Terminal panel is 176x256 from a texture: the inventory label (y 165) sits on the output slots (y 154 to 172) and the request message line runs into the Output label. The Production Terminal draws the title at y 6 and right aligns three 56 px tabs on the same row, so the tabs cover the title. Requests are only possible for stocked items: `TerminalRequests.request` ships stock and orders the shortfall. `OrderBook.create` already plans the full amount without counting stock of the item itself. `InboundStationMenu.stillValid` checks the Inbound Station block, so the Access Point (a subclass block entity with its own block) closes the menu at once.

See proposal.md for why and the delta specs for the behaviour.

## Goals / Non-Goals

**Goals:** a player facing crafting entry point like Refined Storage, no overlapping GUI areas guarded by a test, content on the first frame after the server answers.

**Non-Goals:** a new Access Point GUI; previews across CPUs; art.

## Decisions

### Craftables
`CpuOrders.producers(level, network)` is extracted from the CPU host so the terminal and the CPU list the same producers, patterns included (`PatternRunners.withPatterns`). The craftable items are the first outputs of their recipes, deduplicated by `ItemKey`, sorted by name. Only runnable recipes count: a pattern without a Crafting Station on the line is not craftable (recorded as a non-goal).

`StockSyncPayload` gains `craftables` (list of one item stacks). The payload has seven fields, past `StreamCodec.composite`'s six, so it gets a hand written codec.

### Preview and start
`OrderBook.preview(key, amount)` shares the planning code with `create` (same stock, reservations, recipe order) and returns `Preview(fromStock, toCraft, missing, refusal)` without creating an order. `toCraft` sums each step's outputs times batches. `CpuOrders.preview(level, network, key, amount)` asks the CPU `CpuChoice` would pick; with no CPU the refusal is `no_cpu` and the lists are empty. `TerminalRequests.craft(level, terminal, key, amount)` makes a request with the whole amount missing and places the order with `CpuOrders.create`; it refuses `no_cpu`, `no_producer` or `missing` (checked with a preview first, so Start never creates a blocked order). The request then behaves like a shortfall request: state producing, retried shipping once crafted items are in storage, cancel cancels the order. If some of the item was already in stock, the retry may ship that first; the request is complete either way (recorded).

Payloads: `CraftPreviewRequestPayload(item, amount)` client to server, answered at once by `CraftPreviewPayload(item, amount, fromStock, toCraft, missing, refusal)`; `CraftStartPayload(item, amount)` client to server. The client asks again whenever the amount changes; Start is enabled only when the shown preview matches the amount in the field, has no refusal and nothing missing.

### Click rules
Normal click on a stocked entry opens the request prompt as before; Shift click requests a stack. A click on a craft only entry, a Ctrl click or a middle click on a craftable entry opens the craft dialog. The filter button cycles All, Stock, Craftable.

### Layout
A pure class `terminal/TerminalLayout` (no Minecraft imports) holds `Rect(x, y, w, h)` constants for both screens and lists the areas per screen. Menus take slot positions from it, screens draw and hit test with it. `TerminalLayoutTest` (JUnit) asserts that the areas of each screen are pairwise disjoint and lie inside the panel, and that the panel height is at most 266 (1080p at GUI scale 4 is 270 high).

Counter Terminal, 290x222: left column search (8,18,112,12) and filter (124,18,46,12), grid 9x5 (8,34,162,90) with scrollbar (174,34,8,90), inventory label (8,128), inventory (8,139) and hotbar (8,197); right column requests label (190,18), request list (190,30,92,72) with 6 rows, message (190,104,92,10), output label (190,118), output slots 3x3 at (199,129). Dialogs (amount prompt, craft dialog) cover the grid and request areas (8,30,274,94).

Production Terminal, 240x222: title row (8,6), tab bar (8,18,224,12) with left aligned tabs, list (8,34,224,180); editor popup at (40,34), inventory at (40,122) and hotbar (40,180).

Panels are painted from the layout with fills (vanilla grey panel, bevel, insets, slot frames) instead of fixed size textures, so the art cannot drift from the layout. The old `counter_terminal.png` and `production_terminal.png` GUI textures are deleted.

### Instant content
Both menus override `broadcastFullState()`, which `ServerPlayer.initMenu` calls right after opening, to send their payload at once and remember its signature; `broadcastChanges` keeps the half second change check. The menu records the game time of its first send (`firstSyncTick`) so a GameTest can check it with a mock server player. The client keeps a `loaded` flag (cleared when the screen opens) and shows "Loading" until the first payload.

### Access Point GUI
`InboundStationMenu.stillValid` accepts the Inbound Station and the Access Point block.

### APIs
NeoForge 1.21.1: `AbstractContainerMenu.broadcastFullState` (called by `ServerPlayer.initMenu`), `PacketDistributor.sendToPlayer`, `StreamCodec.of`, `GuiGraphics.fill/renderOutline`, `GameTestHelper.makeMockServerPlayerInLevel`.

### Pure and Minecraft side
Pure: `TerminalLayout` with `TerminalLayoutTest`. Minecraft side (GameTests): craftables listed, preview lists, Start creates a producing request and the sticks land in the output slots, refusal without CPU, first sync sent on open, Access Point menu still valid.

## Risks / Trade-offs

- [Craftable list on a big line costs a recipe walk per sync] -> computed only every sync tick while a screen is open and capped like the stock list.
- [A craft request may be filled from stock that arrived meanwhile] -> the player gets the items either way; crafted extras go into storage.
- [Painted panels look plainer than hand drawn textures] -> accepted until art direction is settled (PLAN.md section 5).
