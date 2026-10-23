## Why

A player of Shuttles 0.3.0 could request a single item twice from the Storage Terminal: the count in the grid did not drop, both picks were planned for the same item, the second found nothing and its request waited as missing forever. `m8-slot-locks` makes a pick lock its items so a second pick is refused, but the grid still shows raw stock, the request dialog accepts any amount and a short request still waits as missing. The terminal's crafting popup is also a hand crafting table bolted onto a warehouse terminal; with a Crafting CPU on the line it should order instead.

This change belongs to PLAN.md milestone M8 (On-demand autocrafting, Storage Terminal and Crafting CPU) and closes the request bug reported on 0.3.0 together with the shuttle job series (`m8-job-persistence`, `m8-slot-locks`, `m8-shuttle-following`).

## What Changes

- **Requestable stock in the grid**: the Storage Terminal grid counts what can still be requested, the stock less the items picks have locked. An item with nothing left to request leaves the stock entries; when it is craftable it shows as a craftable entry like any craftable out of stock. Items count once they are in a rack. A lock or release shows in the grid within half a second.
- **Requests never wait for stock that is not there**: asking for more than the requestable stock is allowed only when the rest can be crafted on the line (a Crafting CPU and a producer or pattern for the item, nothing missing); the request dialog then says how many come from stock and how many are crafted, with the craft preview. Otherwise the dialog starts at one stack or what is requestable, whichever is smaller, and the amount cannot go above what is requestable. The server checks again: it ships what is there and the request is for that amount; with nothing there it is refused as out of stock. A request waits as missing only when a shipment in flight comes up short. Shift click stays: one stack, or all there is.
- **Order grid instead of the crafting popup**: the Craft button opens a one-off order grid and is there only while a formed Crafting CPU and a Crafting Station are on the terminal's data line. The 3x3 grid takes ghost items (click with an item sets one, an empty click clears it; JEI's transfer sets ghosts and takes nothing from the inventory), shows the result of the matching vanilla recipe, an amount with the step buttons, and the planner preview (from stock, crafted, missing). Start makes a terminal request with a work order for that recipe and those items; ingredients come from the warehouse and the results land in the output slots like a craft request. No real items live in the grid any more, so the old hand crafting, Clear, store on close and repeated crafting go.
- **Save as pattern**: when no pattern of the line has that recipe with those items, Start asks whether to save it: "Save as pattern" stores it in the first Pattern Storage of the line with room, like the Production Terminal's editor, and starts the order; "Only this once" starts the order without saving. A refused save (no Pattern Storage, full) still starts the order once and says why. With a matching pattern Start just orders.
- The craft dialog (Ctrl or middle click on a craftable entry) stays as it is.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `shuttles/storage-terminal`: "Stock view with search" counts requestable stock; "Live stock while open" refreshes on locks; "Request items" limits the amount; "Open requests" names when a request waits as missing; "Craftable entries" shows Craft when nothing is requestable; "Screen layout" places the order grid; "Crafting popup", "Crafting grid" and "JEI fills the grid" become the order grid; "Clear the grid", "Grid empties on close" and "Repeated crafting" are removed; new requirements "Request beyond stock is crafted", "Order grid start" and "Save the order grid as a pattern".
- `shuttles/patterns`: "Encoding from the terminal" also saves from the Storage Terminal's order grid.

## Impact

- Shuttles mod: `lock/LockedSlots` reports its OUT locked amounts; `rack/HeavyRackBlockEntity` tells its warehouse when its locks change; `network/WarehouseNetwork` keeps a running tally of OUT locked items per item with a version; `terminal/TerminalWarehouses` merges stock less that tally; `terminal/TerminalRequests` caps requests and makes one-off orders; `order/OrderBook` and `order/WorkOrder` plan and keep an order's one-off pattern; `cpu/CpuOrders` and `cpu/CpuBlockEntity` hand it to Crafting Stations; `pattern/PatternEditing` encodes a grid for both terminals; `menu/StorageTerminalMenu` swaps the crafting grid for nine ghost slots; `client/StorageTerminalScreen` and `client/ClientStock` get the order dialog and the request split; payloads in `terminal/` change; JEI `StorageTerminalRecipeTransferHandler` sets ghosts and refuses without a CPU and station; `TerminalLayout.Storage` gets the order grid parts; lang via datagen.
- Pure logic with JUnit: the reservation tally, the request split, the requestable stock merge, the order grid layout.
- GameTests: `CraftingGridGameTests` is replaced by order grid tests; request tests change where a short request used to wait as missing.
- Saves: work orders save their one-off pattern; orders saved by 0.3.0 load unchanged. Rack locks already save with their rack, so the tally rebuilds on load.

## Non-goals

- Fluids in requests and the terminal (M13).
- CPU reservations of open work orders do not lower the grid count; only locked picks do.
- Hand crafting in the Storage Terminal; a Crafting Table next to it does that.
- A per item "requestable" for other sites: the stock of another site stays its last known stock.
