# Design

## Context

A CPU step counts outputs only when its own collection packets land (`OrderStep.landed`, from `CpuBlockEntity.Host.collect`). An Access Point's item handler (`handlerFor(side)`, inherited from the Inbound Station) is one intake for every face. Machines push with the standard `Capabilities.ItemHandler.BLOCK` query on the neighbour with the context `direction.getOpposite()`, so the Access Point sees which of its faces an insert comes through. See proposal.md for why.

## Goals / Non-Goals

**Goals:** pushed step outputs count for any producer, native or foreign; no double count; no change for other inserts.

**Non-Goals:** intermediaries between producer and Access Point; changing how machines push.

## Decisions

### Credit at the Access Point face (chosen)
`AccessPointBlockEntity.handlerFor(side)` returns, for a non null side, a wrapper around the intake (cached per side). A real insert (not simulated) through side S credits the inserted count to `CpuOrders.creditPushed(level, accessPoint, accessPoint.relative(S), key, count)`: the CPUs of the Access Point's data line, lowest position first, their open orders oldest first, steps that are active (delivering or producing), run on that neighbour and produce the key, each credited up to `needed - collected` through a new `OrderStep.creditPushed(key, count)`. The null side (internal use) stays the plain intake.

Why: it is the same face rule the old Inbound Station step credit used, it needs no change in any machine, and it covers machines of other mods that push on their own.

No double count: packets enter an Access Point through the face that touches the data duct, whose neighbour is a duct, never the producer; the CPU's own collection keeps counting through `landed` only. A credited step is capped at its need, so a push and a packet stream for the same step can never count more than the step needs.

### Alternative considered: stop native machines from pushing while a step holds them
Like `m8-station-job-collect` for stations. Rejected for now: it needs a Foundation hint every native machine and the adapter must honour, and it cannot reach machines of other mods. The credit rule covers both cases with one change.

### Testing
A Storage GameTest can not place Manufacturing machines, so the push is reproduced with a Crafting Station whose sides are all Input (nothing can extract, so the CPU cannot collect its outputs) beside an Access Point: the test moves the crafted sticks into the Access Point through the face touching the station, exactly what a pushing machine does. A second test inserts through another face and checks the step counts nothing. The existing `stationBesideAccessPointFinishes` and the packet tests guard against double counting.

### PLAN.md
No open decision touched.

## Risks / Trade-offs

- [A player or hopper standing on the producer's face is impossible, but a player using the Access Point GUI could insert items that a producer would also make] -> the GUI intake does not go through a side handler, so it is not credited.
- [Wrapper allocation] -> six wrappers cached per Access Point.
