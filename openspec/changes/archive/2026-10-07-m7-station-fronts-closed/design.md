# Design

## Context

Both station block entities build their Lib `SideConfig` in the constructor; the Outbound Station sets its front to Push and reads the machine at `facingPos()` in front. `SideMarkerRenderer` draws every face while configuring. The station screens build the Lib panel without a locked predicate.

## Goals / Non-Goals

**Goals:**
- Same closed front everywhere; the Outbound Station serves the block behind it.

**Non-Goals:**
- See proposal.

## Decisions

### Settles no PLAN.md open decision
Section 2.4 says the Outbound Station stands with its back to the machine.

### Stations
`InboundStationBlockEntity`: `sides.lock(FRONT, DISABLED)`. `OutboundStationBlockEntity`: `sides.lock(FRONT, DISABLED)` and `sides.set(BACK, PUSH)`; `facingPos()` becomes `machinePos()` = the block behind, and `producer()` asks that block through the side that faces the station. Both station screens pass `side -> side == FRONT` as the locked predicate.

### Renderer
`SideMarkerRenderer` skips a face whose relative side `isLocked`.

### Minecraft side verification
GameTests: the outbound stations in `outboundSetup`, `outboundServesCraftingStation` and `orderSetup` face away from their machine; `outboundSideModes` asserts the back is Push and the front stays Off; `inboundSideOff` asserts the front stays Off.

## Risks / Trade-offs

- [Placed outbound stations in the test world face their machine] → nothing is released; turn them around by placing anew.

## Open Questions

None.
