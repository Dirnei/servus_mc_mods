# Proposal

## Why

In a play test a Packing Station's To network buffer (9 slots) held only 14 coal, spread over all 9 slots in stacks of 1 to 3, and so counted as full: further picks waited and shipments stalled. Every pick into the To network buffer locks only empty slots, a rule meant for the terminal's Output row (one request per slot) that the Packing Station inherited. Small picks of one item, as upkeep and CPU feeds make them, each take a whole slot.

This is a fix to M8 On-demand autocrafting (Packing Station, slot locks) in `PLAN.md` section 4, found in the play test.

## What Changes

- A pick into a Packing Station's To network buffer joins a stack of the same item that has room (counting what earlier picks on their way already bring there) and only then uses empty slots. Several picks of one item share one slot. A pick larger than the room left on a stack fills that room and puts the rest into an empty slot (60 coal plus a pick of 10 gives 64 and 6).
- No pick is ever sent into the To network buffer for more than fits there: a pick goes only once room for its whole amount is reserved, picks already on their way count against the room, and the rest waits until room frees. Demand from shipments and target grants never bypass that reservation.
- To network slots reserve instead of closing, like a Field Head buffer part: an IN lock reserves room for its amount of its item, the station keeps sending the items a slot holds while later picks still have room reserved there, and every other insert (wireless arrivals, items put back after a failed send) sees the slot's room less the reservations.
- A counted slot with room reserved for one item takes no other item, so a reserved pick always fits when it arrives. This holds for every counted slot (Packing Station To network buffer, Field Head buffer parts, Parcel Locker boxes), since it is what keeps the room guarantee above.
- The terminal's Output row, the Outbound Station and rack bays keep their rules.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `shuttles/slot-locks`: the To network buffer joins the counted slots; a pick locks room on top of its own item there before empty slots; picks of one item may share a To network slot; counted slots refuse other items while room is reserved for one item.
- `shuttles/packing-station`: new requirements that picks into the To network buffer join stacks of the same item, that picks never ask for more than fits in the To network buffer, and that the station sends from slots that still have picks on their way.

## Impact

- `shuttles/.../packing/PackingStationBlockEntity.java`: To network buffer in `LockedSlots.Mode.COUNTED`, overrides `DeliveryTarget.lockDelivery`.
- `shuttles/.../lock/LockedSlots.java` and `lock/SlotFill.java`: a shared "join incoming" planner for counted slots, and the other item refusal in counted mode.
- Saves stay compatible: locks are saved per slot already and load unchanged; a buffer already split into many small stacks drains by sending as before.
- GameTests in `PackingStationGameTests` and `SlotLockGameTests`, JUnit in `SlotFillTest`, including tests that picks never reserve more than the buffer's room.

## Non-goals

- Compacting stacks already split in a saved world; they leave by sending as before.
- Changing the terminal Output row, the Outbound Station feed or rack bay picks.
- Moving the Parcel Locker onto the new shared planner: the locker belongs to the unarchived change `m11-handheld-link`, so it keeps its own code here and can switch once that change is archived.
- Any change to how many items a pick carries or to pick admission (`admits`). Admission stays a demand gate in front of the room reservation, never in place of it.
