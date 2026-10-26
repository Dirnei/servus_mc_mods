# Proposal

Milestone M8 (warehouse logistics), follow up to the Inbound Station (M6) and the WTP handshake (`m8-wtp-ingress`). PLAN.md has no item for it: it comes from a play test. The finding is a bug in how the station buffer and the Item Pipe work together, so no PLAN.md open point is closed. One new open point is added to PLAN.md (Open: Shuttles): a minimum wait before a pipe grant from a slow source ends.

## Why

A Utility Duct Item Pipe that pulls from a chest into an Inbound Station moves `items_per_pull` (8) items every `pull_interval` (8) ticks. Each batch lands in its own buffer slot, because the warehouse plans a put-away for the batch within a tick and OUT locks its slot, and a locked slot refuses every insert. One stack of 64 cobblestone takes 8 of the 9 buffer slots, the station looks full, and hoppers or a second pipe find no room. A shuttle may also leave with a part stack while the rest is still in the pipe. The player wants a pipe trickle to fill one slot per item and a station that knows how much is coming, so a stack goes into storage as one stack and one trip.

## What Changes

- **Top up a planned slot.** An Inbound buffer slot whose put-away no shuttle has loaded yet SHALL take more of its own item from every inserter (pipe, hopper, the station's Pull side). The surplus joins the open put-away in the same slot, up to a stack per job, so the trickle fills one slot. Once a shuttle has loaded the slot, what is left or arrives after becomes a new put-away.
- **Stacked inserts.** The station's own Pull side and the Item Pipe put items into slots that already hold the item before they use an empty slot.
- **The pipe announces what it pulls.** When an Item Pipe pulls an item toward a block that answers offers (the Foundation `PacketEndpoint` capability, the WTP handshake from `m8-wtp-ingress`), it first offers what its source can give of that item, up to a stack. The Inbound Station answers with a grant: one slot reserved for that amount. The pipe puts the item only into that slot, and the slot is put away once the grant is filled or ends, so one shuttle trip takes the whole stack. Throughput stays at `items_per_pull` and `items_per_tick`; the pipe does not move more per tick than today.
- **Inbound Station as packet endpoint.** The station implements `PacketEndpoint` (Shuttles side). Foundation keeps knowing nothing about Shuttles; the duct only looks up the existing `servus_foundation:packet_endpoint` capability.
- **Cap counts the announced amount.** An Inbound grant is clamped by the cap of the item's stock rule, and what open grants still expect counts as held for every later insert, pull or offer, the same as buffered stacks.
- **The GUI shows the announced amount.** A slot reserved by a pipe grant shows the IN wash and the faded item with the tooltip "Reserved: pipe brings 56 more Cobblestone".
- The grant handling of the Packing Station's To storage moves into a shared Shuttles helper that both stations use.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `shuttles/inbound-station`: the buffer takes top ups into slots whose put-away is not loaded yet, puts stacked, answers pipe offers with grants, and the GUI shows grant slots.
- `shuttles/slot-locks`: a put-away OUT lock on an Inbound buffer slot no longer closes the slot to more of its own item; more of an item joins the open put-away in the same slot.
- `foundation/utility-duct`: the Item Pipe inserts stacked and offers its pulls to packet endpoints before it sends.
- `foundation/data-packets`: a packet endpoint grant may be held by a sender that is not a data line delivery (the Item Pipe), which ends it itself.

## Impact

- Foundation: `logistics/network/ItemChannelNetwork` (stacked insert, grants per target and item on pulls, closing them), a pure `logistics/duct/PipeOffer` for the offered amount.
- Shuttles: `lock/LockedSlots` (top up mode), `station/InboundStationBlockEntity` (endpoint, top up, stacked pull, held counts grants), `packing/PackingStationBlockEntity` (moves to the shared grant helper), new `station/BufferGrants`, `network/WarehouseNetwork` (`drain`, `joinOpenPutAway`, `capRoom`, `grantLock`), `ServusShuttlesEvents` (capability registration), the Inbound Station screen tooltip, lang.
- Data: no new JSON keys. `items_per_pull`, `items_per_tick`, `pull_interval` and `grant_hold_ticks` keep their values.
- Apply order: after `m8-rule-cap`, `m5-duct-endpoint-gui` and `m5-duct-keep-amounts` are archived, because it edits the same intake clamp and the same `ItemChannelNetwork.pull` and `route`.
- Compatibility: no saved format changes. Grants are not saved; an Inbound buffer saved with put-away locks loads as before.

## Non-goals

- Pushed items (a machine or another mod pushing into a duct) are not announced: the duct does not know how much more will come. They profit from the top up only.
- Packing Stations have no pipe access, so nothing changes for them except the shared helper. Outbound Stations need no reservation.
- No change to pipe rates or to how many items a pull takes.
- A Data Potato report on an Inbound Station (buffer, locks, incoming grants) is left to the Data Potato open point in PLAN.md ("Deeper inspection with the Data Potato").
- Other mods' pipes and hoppers are not asked for amounts; they get the top up behaviour.
