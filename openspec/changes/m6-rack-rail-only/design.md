# Design

## Context

The warehouse graph links nodes by `WarehouseConnectionRule.accepts(kind, side, kind)` plus `WarehouseNetwork.railSideAccepted`, which lets a rack take rails on its front only. Racks report `NodeKind.STOCK`, so they link to every touching stock node, and as stock holders they pull neighbouring inventories in through `addCandidatesAround` / `CandidateNeighbours`. Sections come from the rack block states `LEFT` / `RIGHT`, set on placement and mirrored onto the neighbour in `HeavyRackBlock.onPlace`. See proposal.md.

## Decisions

### A RACK node kind
`NodeKind.RACK` for `HeavyRackBlockEntity`. Rule table:
- RACK links to RAIL (front only, through the existing rail side check) and to RACK when both bays join each other.
- STOCK, STATION, GARAGE no longer accept RACK; ELEVATOR never did.
- RAIL accepts RACK like STOCK.
`docksRail` and `holdsStock` include RACK, so racks dock rails and stay indexed sources.

### Section links
The graph asks both nodes. For RACK to RACK the network's connection lambda additionally asks `HeavyRackBlockEntity.joinedTowards(side)`: true when the side is the rack's left or right and that join flag is set and the neighbour is a rack of the same facing. The flags only change on placement (mirror) and removal, so after `onPlace` mirrors a join onto the neighbour, both racks refresh their links (`StorageNodeBlockEntity.refreshLinks`, a public wrapper around `refreshNetworkConnections`).

### No pulling around racks
A new `pullsNeighbours()` (STOCK only) gates `addCandidatesAround`, and rack positions count as connectors for `CandidateNeighbours.compute`. `addSource(pos, false)` skips positions holding a `StorageNodeBlockEntity`, so a crate never indexes an unlinked rack (or any other storage block) as a foreign inventory.

### Pure parts
The rule table stays pure and gets JUnit tests (`WarehouseConnectionRuleTest`).

## Risks / Trade-offs

- [Existing builds where racks touched the controller go dark in the index] -> intended; the user asked for it. Unreleased, no migration.
- [A section whose bays did not mirror their join (older worlds)] -> those bays connect only when each has its own front rail; acceptable.

## Testing

JUnit for the rule table; GameTests for rack beside controller not listed, front rail connects the whole section, side rail does not, breaking the rail disconnects, crate behind rack not linked; existing tests rewired with front rails.
