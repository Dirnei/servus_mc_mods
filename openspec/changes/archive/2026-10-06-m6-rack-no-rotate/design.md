# Design

## Context

See proposal.md for motivation. `HeavyRackBlock.onWrench` dismantles on sneak and otherwise rotates the facing clockwise, clears the joins and calls `HeavyRackBlockEntity.facingChanged()` to refresh the rail links. `ConfigurationCarrotItem.useOn` hands the click to `Wrenchable.onWrench` on both sides.

## Goals / Non-Goals

**Goals:**
- A plain carrot click on a rack is harmless.

**Non-Goals:**
- Any other way to turn a rack. Break and place is the way.

## Decisions

### No rotate branch
`onWrench` keeps the sneak dismantle branch and returns `PASS` otherwise, so the click falls through like on a block the carrot does not know. `HeavyRackBlockEntity.facingChanged()` is removed; the facing never changes after placement, so the rail links never need a refresh.

## Pure versus Minecraft logic

No pure logic. The GameTest `carrotDoesNotTurnRack` uses the carrot through `ConfigurationCarrotItem.useOn` on a north facing rack with a joined neighbour and asserts the facing and the join are unchanged.

## Risks / Trade-offs

- [Player placed a rack the wrong way] -> Sneak dismantle keeps the contents, so breaking and placing again costs nothing.
