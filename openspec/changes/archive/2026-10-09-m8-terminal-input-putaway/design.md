# Design

## Context

`StorageTerminalBlockEntity.storeInput` and `StorageTerminalMenu.store`/`pushCarried` call `WarehouseNetwork.insert`, which only fills crates and foreign inventories. A rack warehouse never takes anything, so the Input row stays full. The terminal is a data member and the Access Point already takes packets on every face into its intake and puts them away by shuttle (`storage/access-point`). See proposal.md.

## Goals / Non-Goals

**Goals:** one store path for the terminal, through packets and an Access Point, that also fills racks; nothing lost.

**Non-Goals:** ledger tracking of input packets; changes to the Access Point or the put-away itself.

## Decisions

### One path: everything goes through the Input row
`StorageTerminalBlockEntity.queueInput(stack)` inserts a stack into the Input row and returns what does not fit. The menu's grid push, the crafting grid clear and close all call it; shift click already moves into the Input row. Automation inserts that are not for a request already land in the input. So `WarehouseNetwork.insert` is no longer used by the terminal. Alternative: send packets straight from each path. Rejected: several places would each need failure handling, and the Input row already gives a visible buffer the player understands.

### Sending
Each store pass (every 20 ticks, or the next tick after something was put in) takes every non-empty input slot and calls `DataPackets.send(level, terminal, accessPoint, stack)`. The slot is emptied first; when the delivery fails at once (no route, no target) the caller keeps its stack, so it goes back into the slot. Once the network owns a stack, packets that cannot finish are settled by the existing rules: into the target, then the source (the terminal's item handler, which routes it back into the Input row), then the ground or the stash. Items in flight are not tracked in the production ledger; they are settled at server stop like every packet (record: a crash loses them, as for all packets).

### Choosing the Access Point
Among the data members of the terminal's data network, the Access Points whose warehouse is online; the nearest to the terminal by squared distance, ties by the lower position. Cheap: only on store passes that have something to send. Alternative: the Access Point with the shortest shuttle trip to free rack space; rejected for now, the put-away already chooses racks.

### Message
The block entity keeps an input refusal (`no_access_point` while input waits and no Access Point is reachable). The menu shows it on the message line when no other message is set, via a new key `gui.servus_storage.terminal.message.input_no_access_point` ("No Access Point on this data line").

### Arrows
`StorageTerminalScreen.tag` draws the Input arrow pointing up and the Output arrow pointing down; colours stay per row. Tooltips stay.

### Testing
GameTests (Storage): rack-only warehouse with an Access Point and a garage, input planks end in a rack; no Access Point keeps the stack and sets the refusal; grid push and shift click go through the Input row; clear goes through the Input row. Existing tests that expected an instant push wait for the put-away.

## Risks / Trade-offs

- [Storing is no longer instant] -> packets plus shuttle trip; the stock updates once items are put away.
- [Access Point intake full] -> packets wait at the target and retry, existing packet behaviour.
- [Many stacks at once] -> one delivery per slot per pass; packet throughput limits apply.
