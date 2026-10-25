## Why

A player built a farm in the basement with the racks on another floor. Harvesters and Spreaders are wide and never ride a lift, so racks they reach only by lift are out of reach: the harvest goes to some rack on the field's floor, ignoring zones, or, with none there, the harvester parks with its cargo in the garage and the field stops. Fields belong where the player wants them, not on the storage floor.

This change belongs to PLAN.md milestone M10 (Field work, Field Head, Harvester and Spreader).

## What Changes

- **To storage buffer on the Field Head**: each head gets a second buffer (`harvest_buffer_slots` in `field.json`, example 9) for items leaving the field. Players, hoppers and pipes can neither put items in nor take them out; it is saved with its locks and drops when the head is broken.
- **Field shuttles unload at the head**: a Harvester puts its harvest, and a Spreader its leftover fertilizer, into that buffer at the farm rail beside the head, after spare seeds and fertilizer have filled the supply buffer as today. They never drive to racks.
- **Plain shuttles put the buffer away**: the warehouse puts the To storage buffer away like an Inbound Station, by the zone rules, with slot locks; plain shuttles fetch at the head's farm rail and may ride lifts to the rack.
- **Full buffer**: no field job starts while the To storage buffer has no free slot. A shuttle whose cargo does not fit parks in its garage with the rest as a shuttle with cargo that has nowhere to go (the garage says the head's To storage is full), and unloads it at the head once the buffer has room again.
- **GUI and status**: the head GUI shows the To storage buffer as a read-only row, and the field status says when the head waits for a shuttle to collect.
- The harvest limit stays a trigger that stops new harvests, not a hard cap; the To storage buffer does not count toward it.
- A garage on another floor than its field stays unsupported: the harvester's garage must stand on the field's floor.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `shuttles/harvester`: "Harvest put-away" is replaced by "Harvest unloads at the head"; "Field jobs" waits for a free To storage slot; "Field cargo" unloads at the head; "Runs that cannot go on" without a head drops the cargo at the garage.
- `shuttles/spreader`: "Leftover goes back to a rack" is replaced by "Leftover goes to the head".
- `shuttles/field-head`: new requirements "To storage buffer", "To storage buffer is put away" and "To storage row in the GUI".

## Impact

- Shuttles mod: `field/FieldHeadBlockEntity` gets the To storage buffer (`LockedSlots`, saved, dropped, in its `lockedSlots()`), `field/` a pure unload plan; `shuttle/FieldRuns` replaces `continueStore` rack trips for Harvester and Spreader with an unload at the head and parks with the rest; `network/WarehouseNetwork` puts the head's buffer away through the same drain as Inbound Stations (generalised to a put-away source) with the pickup at the head's farm rail, and checks the free slot before queueing field jobs; `menu`/`client` Field Head screen row; `field.json` `harvest_buffer_slots`; lang via datagen.
- Pure logic with JUnit: the unload plan (supply parts first, then To storage, what is left).
- GameTests: harvester and spreader tests that expected rack put-aways; new tests for a rack reached only by lift.
- Saves: heads saved without the buffer load with an empty one.

## Non-goals

- Harvesters or Spreaders riding lifts, or a garage on another floor than its field.
- Data line access to the To storage buffer (the head stays off the data line).
- Counting the To storage buffer toward the harvest limit.
