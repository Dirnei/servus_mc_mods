# Design

## Context

Today a terminal's output is one stacked inventory (`StorageTerminalBlockEntity.output`, an `ItemSlots` of `terminal_output_slots`). A request (`StorageTerminalBlockEntity.Request`) has no slot: packets for an item a request waits for go through `RoutingHandler` into any output slot, and `credit` gives them to the oldest request of that item. The GUI gets requests as `RequestRow`s in `StockSyncPayload` and draws them as the first cells of the grid; `StorageTerminalScreen.clickGrid` cancels a request cell on any click, opens the amount prompt on a click and requests a stack on a shift click. See proposal.md for the motivation.

## Goals / Non-Goals

**Goals:**
- One request owns one output slot for its whole life, saved with it.
- The grid click becomes a one item request that grows an open request of the same item.
- The Output row draws the requests and is where they are cancelled.

**Non-Goals:**
- No change to how requests are shipped, booked, retried or crafted (`TerminalRequests.ship`, `book`, `retry`, `CpuOrders`).
- No change to the amount prompt, craft dialog or order grid screens beyond which click opens them and the new refusal.

## Decisions

### A slot index on the request
`Request` gets `int slot`, saved as `Slot` next to `Delivered`. `newRequest` takes the slot chosen by the pure `RequestSlots.choose` and the caller refuses with `output_full` when it returns none. A request loaded from an older save without `Slot` gets one assigned on load with the same helper (nothing is released yet, so no data fixer).

Alternative: keep the shared inventory and only draw requests over the slots they happen to fill. Rejected: two requests of different items would race for slots, and an empty request has no slot to show its ghost on.

### Pure slot choice
`terminal/RequestSlots` (no Minecraft imports) takes, per slot, the owning request id or none, the item id it holds or none, and whether it has room, plus the new request's item id. It returns the slot of the same item with room that no open request owns, else the leftmost slot that is empty and unowned, else `-1`. A slot owned by an open request is never chosen, even for the same item, so two requests never share a slot. Unit tests in `RequestSlotsTest`: same item first, owned skipped, ended item stays taken for other items, full row gives -1, same item without room is skipped.

### Routing into the owner's slot
`RoutingHandler.insertItem` for an output slot accepts an item only when the slot is owned by an open request waiting for that item; `credit` books the inserted count on the owner of the slot instead of the oldest request of the item. `fits` (waiting for room) checks the request's own slot. When the slot is full the packet is refused and waits at its source as today, which gives the refill behaviour for requests over a stack with no extra code.

### Merging clicks
`TerminalRequests.request` first looks for an open request of this site (`remote == null`) for the key whose `orderCpu` has no open order. If one exists, it ships the extra amount on that request (`ship(...)` adds a `Tracked`) and raises `requested` by what was shipped; it needs no slot. Otherwise it makes a new request in a chosen slot. Transfers (`RemoteTransfers`) and craft requests (`craft`, `craftOneOff`) always make a new request and take a slot. Merging into a producing request was left out: its missing count and work order are linked and growing it would mean re-planning the order.

### Click mapping in the screen
`clickGrid` becomes: carried stack pushes; not stocked craftable opens the craft dialog; craftable with middle or Ctrl opens the craft dialog; shift requests a stack (unchanged count rule); right button opens the amount prompt; left button sends `RequestItemPayload(stack, 1)`. Request cells and `Cell.isRequest` go away from the grid.

On the Output row, `mouseClicked` checks `hoveredSlot` before `super`: a middle click, or a left click with Ctrl down, on an output slot that a request owns sends `CancelRequestPayload` and returns true, so the vanilla slot click (pickup or the creative clone) does not run. Other clicks fall through to the normal slot handling, so left and shift click take items.

### Drawing the requests
`RequestRow` gains the `slot` index. `StorageTerminalScreen.extractSlots` draws, for each row, `graphics.fakeItem` when the slot is empty, a thin progress bar of delivered against requested under the slot, and a small state mark in the slot's corner with the state colours the grid cells used. `extractTooltip` shows the item, "delivered of requested", the state words and "Middle click or Ctrl click to cancel" for a hovered owned slot, as `LockOverlay` does for locked slots (same `GuiGraphicsExtractor.fakeItem` and `setComponentTooltipForNextFrame` calls, see `client/LockOverlay.java` and `client/HeavyRackScreen.java`).

### Refusal text
New key `output_full` in the refusal map of the terminal menu, lang "Output row full, take your items", shown on the message line for requests and in the craft dialog and order grid for Start.

### APIs
- `AbstractContainerScreen.mouseClicked(MouseButtonEvent, boolean)`, `hoveredSlot`, `extractSlots`, `extractTooltip` (26.1 names, already overridden in `StorageTerminalScreen` and `HeavyRackScreen`).
- `GuiGraphicsExtractor.fakeItem`, `fill`, `setComponentTooltipForNextFrame` (used by `LockOverlay`).
- `Minecraft.hasControlDown()` / `hasShiftDown()` (used by `clickGrid`), GLFW button constants.
- `ValueOutput`/`ValueInput` save of the request (existing `saveAdditional`/`loadAdditional` of the terminal).

### Pure and Minecraft parts
Pure: `RequestSlots` (JUnit). Minecraft: request slot ownership, routing, merging, refusal, reload (GameTests in `TerminalRequestGameTests`); click mapping and drawing (play check).

### PLAN.md
This settles no open decision of PLAN.md; it adds and then implements one new M8 point under "Open: Shuttles".

## Risks / Trade-offs

- [A player fills all nine slots with finished deliveries and wonders why requests are refused] -> the refusal text says to take the items; finished slots look like normal stacks.
- [Ctrl click on a request slot is also Ctrl's "move all" in some inventory mods] -> the screen consumes the click on owned output slots before `super`, so those mods do not see it there.
- [Old saves with requests without a slot] -> slots assigned on load; requests that do not fit a slot (more open requests than slots) keep running and get a slot when one frees up, drawn nowhere meanwhile.
