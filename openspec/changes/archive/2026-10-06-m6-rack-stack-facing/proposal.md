# Proposal

## Why

Stacking racks is the normal way to build a tall bay, and a rack placed on top of another one should face the same way without the player turning around first. The user asked for this rule on 2026-10-06. This polishes the PLAN.md M4 line "Heavy Duty Rack".

## What Changes

- A Heavy Duty Rack placed directly on top of another Heavy Duty Rack takes the facing of the rack below instead of the player's look direction. Placement anywhere else keeps the current behaviour.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/heavy-duty-rack`: "Bays connect visually" gains the placement rule for stacked racks.

## Non-goals

- Copying the facing from a rack beside the placed one. Rows are laid out by the player.

## Impact

- `servus_storage`: one branch in `HeavyRackBlock.getStateForPlacement`, a GameTest that places a rack through its item.
