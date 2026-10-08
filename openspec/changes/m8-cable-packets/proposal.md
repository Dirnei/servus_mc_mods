# Proposal

## Why

PLAN.md M8 moves items over the data line as packets with a size and a speed, so production on demand reaches machines far from the warehouse without a pipe. This change implements M8 item 2, `m8-cable-packets`: the transport layer that the Access Point, the Counter Terminal on the cable and the Crafting CPU build on. The data line still never moves anything on its own; packets exist only when a mod asks for a delivery.

## What Changes

- A Foundation API on the data network to **send** a stack to a member and to **move** items from one member to another. A delivery is split into packets of `packet_size` items; each packet travels the shortest path of data ducts at `packet_speed` blocks per second. One packet of a delivery is in flight at a time, so throughput is packet size times speed over distance, as PLAN.md section 2.4 states.
- Packet size, speed and the retry interval come from datapack JSON `data/<ns>/data/packets.json` (examples 8 items, 4 blocks per second, 20 ticks).
- A packet is delivered into the target member's item handler on the face that touches the data duct, or into its stock sink. What does not fit waits at the target and is retried; nothing is voided.
- A source member gives items through its stock provider, or through its item handler on the touching face; items leave the source only when their packet leaves.
- Every delivery has an id and a state (running, done, failed with a reason) that callers can poll or listen to, so later changes can track their deliveries.
- A packet whose route breaks takes a new route from where it is; when there is none, its items go to the target, else back to the source, else drop as an item entity at a loaded place. When the server stops, packets in flight are settled the same way.
- An op command `/servus_foundation data move <from> <to> <item> <count>` starts a delivery for testing.
- Pure Java route and flight logic with JUnit tests.

## Capabilities

### New Capabilities
- `foundation/data-packets`: deliveries and packets on the data network, their limits from datapack JSON, delivery into and out of members, broken routes and the test command.

### Modified Capabilities
None. The data line still moves nothing on its own (`foundation/data-cable`); packets only run for a delivery a mod or an op starts.

## Non-goals

- Client rendering of packets (a moving glow along the duct): later, when players see deliveries (Access Point, CPU).
- Saving packets in flight across a server restart beyond settling them at stop.
- Access Point, Counter Terminal on the cable, Crafting CPU: their own M8 changes.
- High-Speed Data Cable (bigger, faster packets).

## Impact

- `foundation`: new pure classes in `foundation.data.packet`, packet ticking in `DataNetwork`, a reload listener, a stash for items that cannot be placed yet, a command, `ServerStoppingEvent` hook, datapack JSON.
- `storage`: GameTests that move items between a Warehouse Controller and a Crafting Station over a data duct.
