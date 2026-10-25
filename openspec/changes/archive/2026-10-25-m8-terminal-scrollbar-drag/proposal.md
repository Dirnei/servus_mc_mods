# Proposal

## Why

The Storage Terminal's item grid shows a scrollbar, but its thumb cannot be grabbed: the grid only moves with the mouse wheel. Players expect to drag a scrollbar, and with a large stock the wheel is slow.

Milestone M8 (Storage Terminal). PLAN.md has no item for it: it is a bug found in play.

## What Changes

- Pressing the left button on the grid's scrollbar jumps the grid there, and dragging moves the grid with the mouse until the button is released, also when the mouse leaves the bar.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/storage-terminal`: the item grid's scrollbar can be dragged with the mouse.

## Non-goals

- The Foundation item lists of the amount panel (From stock, To craft, Missing) and the Production Terminal keep scrolling by wheel only; they can follow later if needed.
- No change to the thumb's look or size.

## Impact

- `foundation/ui/ListScroll` gains a pure helper from a position on the track to a scroll offset, with JUnit tests.
- `shuttles/client/StorageTerminalScreen` handles press, drag and release on the scrollbar.
