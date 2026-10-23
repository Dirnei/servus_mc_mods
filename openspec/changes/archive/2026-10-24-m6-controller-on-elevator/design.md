# Design

## Context

Links in the warehouse network are decided by `WarehouseConnectionRule.accepts(from, side, to)`, a pure Java rule over node kinds. The controller's kind is `STOCK`; the elevator's `ELEVATOR` case refuses `STOCK` on every side and the `STOCK` case accepts only `STOCK` and `RAIL`. Both ends must accept for a link. See proposal.md for why.

## Goals / Non-Goals

**Goals:**
- Controller and segment link on the four horizontal sides.

**Non-Goals:**
- No change to block state, models or renderers.

## Decisions

- **Horizontal sides only.** `STOCK` accepts `ELEVATOR` and `ELEVATOR` accepts `STOCK` when the side is not up or down, matching how a segment takes a rail. A controller above or below a shaft would sit in the car's path, so it stays unlinked. Alternative: every side, like controller to rail; rejected because the shaft's ends are where the car travels.
- **No visual link.** `ElevatorBlock.connectsTo` decides decks and rail pieces from neighbouring `RailBlock` and `ElevatorBlock` blocks only, not from network links, so it needs no change.
- **Shuttles stay out of the controller.** The controller holds stock (`holdsStock()` is true for `STOCK`), so `WarehouseNetwork.onNodeAdded` never adds it to `connectors` and `isTravelNode` stays false. Route searches through `travelNeighbours` therefore never step onto it. No change needed.
- **PLAN.md decisions settled:** none open; this corrects an outdated link rule.

## NeoForge APIs

None new. The rule runs inside the existing Foundation network graph (`NetworkType` connection predicate in `WarehouseNetwork.TYPE`).

## Testing

- Pure Java: `WarehouseConnectionRuleTest` (JUnit).
- Minecraft: a GameTest in `ElevatorGameTests` for a controller beside a shaft, and one on top of it.

## Risks / Trade-offs

- [An existing GameTest builds a controller beside a shaft and expects it apart] → Run the full GameTest suite; adjust that fixture if one turns up.
- [Two controllers on one shaft now make the network offline with "more than one controller"] → Same as two controllers on one rail run today; intended.
