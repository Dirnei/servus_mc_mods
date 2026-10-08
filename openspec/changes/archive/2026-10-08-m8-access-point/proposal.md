# Proposal

## Why

PLAN.md M8 item 3 (`m8-access-point`). The data line can move packets (`m8-cable-packets`), but a warehouse has no door to it: racks are served only by shuttles, and nothing turns a shuttle load into packets or packets into put-away. The Access Point is that door, and later M8 changes (Counter Terminal on the cable, Crafting CPU) order items through it. Terminals, the CPU and Pattern Storage will be data members without production or stock, so Foundation also needs a capability that just says "I am on the data line".

## What Changes

- Foundation: a sided capability `servus_foundation:data_link` and a data role `link`. A block exposing it on a side touching a data line is a data member with that role, like producers and stock members.
- Storage: new block **Access Point**. It sits on a rail like a station (shuttles reach it) and touches a data line as a link member. It has an intake buffer and an outgoing buffer.
- Inbound: packets delivered into an Access Point land in its intake buffer and are put away by the warehouse like items in an Inbound Station.
- Outbound: a Storage API asks a warehouse to ship items to a member of the data line. The warehouse picks one of its Access Points on that data line, the one with the shortest shuttle trip from the racks holding the item (any one on a tie), plans the picks like a terminal request (crates and foreign inventories instantly, racks by shuttle into the Access Point's outgoing buffer), and the Access Point sends what arrives as packets to the requester, one packet stream at a time. Several Access Points add throughput.
- The Warehouse Controller stays the stock member of the warehouse on the data line; shipping through it is the new API.
- An op command `/servus_warehouse ship <controller> <target> <item> <count>` to try it in game.
- Datagen, texture, recipe, lang, connection lamp; docs Storage page.

## Capabilities

### New Capabilities
- `storage/access-point`: the Access Point block, its intake and outgoing buffers, shipping to data members and the choice of Access Point.

### Modified Capabilities
- `foundation/data-cable`: members by capability gain the link role through `data_link`.

## Non-goals

- Counter Terminal on the data line and terminal deliveries as packets: `m8-item-terminal-cable`.
- Orders from the Crafting CPU: `m8-crafting-cpu`.
- A GUI for the Access Point (it shows its state with the lamps and the summary only), and client rendering of packets.
- Persisting a shipment's tracking object across a restart: the Access Point saves what it still has to send, but callers holding a ticket lose it.

## Impact

- `foundation`: `ServusFoundationCapabilities`, `DataRole`, `DataMembership`, `DataNetwork` probes.
- `storage`: new `access` package (block, block entity, shipping), `WarehouseNetwork` (choice and planning), registries, capabilities, datagen, commands, GameTests, docs.
