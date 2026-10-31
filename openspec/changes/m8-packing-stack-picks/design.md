# Design

## Context

See proposal.md for the bug. Current code:

- `WarehouseNetwork.readyToSend(PickJob)` gates every pick, shuttle or instant, before it is sent: a job that already holds locks at the destination goes; otherwise a Packing Station first asks `admits(key, amount)` (demand: `PickAdmission` over grants and open shipments), then the destination `DeliveryTarget.lockDelivery(jobId, requestId, key, amount)` must lock room for the whole amount, or the partial locks are released and the pick waits. The default `lockDelivery` calls `deliveries().lockIn(jobId, key, amount, true)`, empty slots only. `deliverTo` puts the cargo in with `deliveries().insertAs(jobId, stack)` and the rest with `insertItem`.
- The Packing Station's To network buffer (`PackingStationBlockEntity.outgoing`) is a `LockedSlots` in `Mode.CLOSED` and does not override `lockDelivery`. A slot with an IN lock is closed to every other job and to the station's own sending: `DataPackets.move(..., new ItemView(outgoing), ...)` extracts through the handler and is refused there.
- So each pick takes a fresh empty slot. Even with `lockIn(..., false)` (partial stacks first), picks running in parallel would still split, because `SlotFill.in` skips locked slots and a slot under an IN lock cannot take a second pick in CLOSED mode.
- `ParcelLockerBlockEntity` (unarchived change `m11-handheld-link`) already solves the same problem: its boxes are `Mode.COUNTED`, and its `lockDelivery` plans with `SlotFill.in` over a "reserved view" of each slot (key from the stack or from the IN locks, count = stack + IN amounts, never locked).
- Other users of the buffer: `admitted(key)` counts items plus IN reservations; `returnOrphans()` skips locked slots; `keep()`, relay arrivals and `outgoingRoom` use `insertItemStacked`; `takeOutgoing` (wireless) uses `extractItem`. The menu is read-only and the station registers no item capability.

## Goals / Non-Goals

**Goals:**
- Picks of one item into the To network buffer share slots, also while several are on their way at once.
- The station keeps sending from a slot while more picks of its item are on their way into it.
- A reserved pick always finds its room when it arrives.
- No pick is ever sent into the To network buffer for more than fits there; picks on their way count against the room.

**Non-Goals:**
- No change to pick sizes, admission (`admits`, `PickAdmission`), grants or rates.
- No change to the terminal Output row, Outbound Station or bay targets (they keep the empty slot default).

## Decisions

### D1. The To network buffer becomes a counted `LockedSlots`

`outgoing` is created with `LockedSlots.Mode.COUNTED`. IN locks then only reserve room, several jobs may hold IN locks on one slot (`lock()` does not clear in counted mode), inserts by anyone but the acting job see `SlotFill.sharedRoom` (room less other jobs' IN reservations), and extracts see the items less OUT reservations. The To network buffer holds no OUT locks, so the station's data line sending (`ItemView` extract) and wireless sending (`takeOutgoing`) take every item a slot holds, also with picks on their way into it.

Alternatives considered:
- Stay CLOSED and call `lockIn(..., false)`: joins only slots no pick is on its way into, so parallel picks still split, and a slot under a lock blocks sending its items.
- `Mode.TOP_UP`: opens slots that hold only OUT locks to more of their item; the To network buffer has IN locks, so it does not help.
- `extendIn`: grows one job's own IN locks; every pick is its own job, so it does not apply.

COUNTED is already proven by the Field Head buffer and the Parcel Locker.

### D2. A shared joining planner

New pure function in `SlotFill`: a slot "as incoming picks see it", built from the stack's key and count, the slot limit and the slot's IN reservations (key and amount each). Its key is the stack's key or else the reservations' key, its count adds the reserved amounts, and it is never locked; a slot whose stack and reservations name different keys is seen as locked (takes nothing). `SlotFill.in` then plans over these views: partial room first, then empty slots, each up to the stack size.

`LockedSlots` gets `lockInJoining(jobId, key, amount)` that builds the views from its stacks and locks, plans with `SlotFill.in` and places IN locks with `lock()`. `PackingStationBlockEntity` overrides `lockDelivery` to call it. The Parcel Locker keeps its private `reservedView` in this change (it belongs to `m11-handheld-link`); switching it to the helper is a small follow-up once that change is archived.

### D3. "Joins a stack with enough room" splits a pick

Confirmed by the user. The user's decision: a pick joins an existing stack of its item when it has room, then uses empty slots. A pick larger than the room left on the stack fills that room and puts the rest in an empty slot (`SlotFill.in` as today; 60 coal plus a pick of 10 locks 4 and 6). Taking an empty slot for the whole pick instead would leave one more partial stack and never use fewer slots, so the split is chosen. It also matches the Parcel Locker.

### D4. Counted slots take only their reserved item

Confirmed by the user, for every user of `LockedSlots` counted mode (Packing Station To network buffer, Field Head buffer parts, Parcel Locker boxes), because it is what keeps D6. In counted mode, `LockedSlots.insert` and `isValid` refuse a resource that differs from the key of another job's IN lock on that slot. Without it, an empty slot reserved for 8 coal could take iron from a wireless arrival or `keep()`, and the coal pick would not fit when it arrives. This is shared code, so it also guards Field Head parts and Parcel Locker boxes, where it is correct as well (nothing should fill a box reserved for another item).

### D6. Picks never reserve more than the To network buffer holds

The user's rule: it must never be possible to pick more into the To network buffer than fits there. The design keeps it by making the reservation the only way in:

- `readyToSend` sends a pick only when `lockDelivery` (now `lockInJoining`) locked its whole amount; a short lock is released and the pick waits unsent. Picks are not resized, so a pick bigger than the room waits whole until room for it frees (by the station sending).
- `lockInJoining` plans over views whose count adds every IN reservation of the slot, so room is the slot limit less items held less what picks on their way reserved; `SlotFill.in` never plans past a view's limit, so the sum of planned parts never exceeds that room and a call returns less than `amount` when room is short.
- `admits()` and `PickAdmission` are a demand gate in front of the reservation, never in place of it: their "one pick through when nothing of the item is admitted" rule only lets the pick try `lockDelivery`. They stay unchanged.
- A job that already holds locks goes without a new reservation, but it only holds locks that `lockDelivery` placed for its whole amount.
- Inserts that are not picks (`keep()`, relay arrivals, items put back after a failed send) see `SlotFill.sharedRoom` and D4, so they can never take room a pick reserved; they are not picks and do not fall under the rule.
- `deliverTo` inserts the rest of a cargo with `insertItem` after `insertAs`; with the whole amount reserved, nothing is left for it, and a held load retries rather than overfilling.

### D5. Nothing else in the station changes

- `admitted(key)` already counts items plus IN reservations: right for shared slots.
- `returnOrphans()` keeps skipping locked slots: a slot with a pick on its way holds an item a shipment wants, and an orphan pick's slot is returned once it lands.
- `ship()` computes `ready` from `outgoingCount` (all items held) less `claimed`; unchanged.
- Saves: locks are saved per slot; an old save holds at most one IN lock per To network slot and loads unchanged in counted mode.
- The GUI tooltip already lists every lock of a slot (`MenuLocks`), so a shared slot names each pick.

### APIs

No new NeoForge API. `LockedSlots` extends Foundation `ItemSlots` on NeoForge's `ItemStacksResourceHandler` (`net.neoforged.neoforge.transfer.item`) and already overrides `insert(int, ItemResource, int, TransactionContext)`, `extract(...)` and `isValid(int, ItemResource)`; D4 only adds a check in those overrides.

### Pure Java vs Minecraft

- Pure, JUnit (`SlotFillTest`): the joining view and planning: a sequence of small picks of one item ends in one slot, a pick joins a pick on its way, a nearly full stack splits, a full stack spills to an empty slot, a slot reserved for another item takes nothing, planning never exceeds the room left after reservations.
- Minecraft, GameTest: `PackingStationGameTests` (picks share a slot through `lockDelivery` and `deliverTo`, sending while a pick is on its way, picks wanted beyond the room wait at `readyToSend` and go once room frees), `SlotLockGameTests` (a counted slot reserved for one item refuses another).
- Manual play check: the play test case (coal upkeep through a Packing Station).

### PLAN.md open decisions

None: this is a bug fix inside M8 (Packing Station, slot locks). The decisions taken came from the user: picks join a stack, then empty slots, and split over the room left (D3); counted slots take only their reserved item (D4); picks never ask for more than fits (D6). No question is open.

## Risks / Trade-offs

- [The station sends items out of a slot a pick is still on its way into] → that is intended; the reservation keeps the room, and `admitted` still counts the pick so admission does not overbook.
- [D4 changes shared counted behavior for the Field Head and the Parcel Locker] → only refuses inserts that would break a reservation; the existing `FieldLockGameTests` and `ParcelLockerGameTests` run in the verify task.
- [Existing GameTests assume an empty slot per pick in the To network buffer] → run `PackingStationGameTests` and `EgressGameTests`; adjust a test only where it asserted the old empty slot rule.
- [A world saved with a split buffer] → no compaction; the items leave by sending as before.

## Apply notes

- Applied on top of `m8-upkeep-slot-fuel` (refusal clock, `endShort`, saved RequestId and RefusedSince) and the Parcel Locker. Neither needed a change here: `endShort` drops queued picks and leaves what waits to `returnOrphans`, which still skips slots with a pick on its way; the refusal clock does not look at the To network buffer. `admitted`, `keep`, `outgoingRoom` and `takeOutgoing` read correctly under counted semantics and stay unchanged (task 2.2).
- `readyToSend` is unchanged: a Packing Station pick goes only when `lockDelivery` (now `lockInJoining`) locked its whole amount, after `admits` as a demand gate.
- The D4 guard lives in `LockedSlots.insert` and `isValid` and so also covers Field Head buffer parts and Parcel Locker boxes; `FieldLockGameTests` and `ParcelLockerGameTests` stay green.
- GameTests: `sendsWhileAPickIsOnItsWay` and `picksNeverExceedBufferRoom` use oak planks instead of coal, since the test Crafting Station takes planks. `picksNeverExceedBufferRoom` holds demand open with a wireless shipment (no target grant needed) and runs inside one callback, taking 10 planks out of the slot with `extractItem` the way the station's own sending does, instead of waiting for a real send. Its room for 20 is a partial stack of 54 plus an empty slot a pick on its way already reserved 54 of.
