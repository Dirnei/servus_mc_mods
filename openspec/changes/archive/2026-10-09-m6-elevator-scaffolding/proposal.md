# Proposal

## Why

Play test feedback on the Rail Elevator (PLAN.md M6, item "Rail Elevator for multi level warehouses"): it does not behave like scaffolding. A player cannot stand on a level of the shaft, and a plain click with an elevator on a segment places it against the clicked face instead of growing the shaft. Only the collapse from the bottom and climbing already feel like scaffolding.

## What Changes

- A player above a segment stands on it, like on vanilla scaffolding: every level of a shaft can be stood on. Sneaking lets the player sink down through it instead of holding them in place.
- Climbing inside with jump stays; walking along a rail into a ground floor segment stays possible because the walkable top only exists for an entity that is already above the segment.
- A plain click (not sneaking) with an elevator on any face of a segment places the new segment on top of the shaft, walking up the column to the first free spot, like scaffolding. A blocked top places nothing. There is no reach limit for this walk; the build height and the support rules still apply.
- A sneak click keeps using the placement assist (same target on top of the shaft, with the ghost preview).
- The collapse from the bottom, the lift car, decks, flaps and shuttles are unchanged.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/rail-elevator`: segments are walkable from above and sneaking descends; the sneak placement requirement is replaced by one where any click with an elevator builds the shaft up.

## Non-goals

- Sideways extension like scaffolding (segments need support from below).
- Any change to the lift car, decks, flaps, shuttles or the warehouse network.
- A ghost preview for the plain click.

## Impact

- `storage/.../rail/ElevatorBlock.java` (collision shape, scaffolding hook), a new elevator item class, `ServusStorageItems`.
- Pure helper and JUnit test in `storage/.../rail`.
- Storage GameTests for elevators, docs Storage page.
