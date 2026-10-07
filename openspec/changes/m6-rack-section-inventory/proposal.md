# Proposal

## Why

Bays of a Heavy Duty Rack section look like one rack but each keeps its own 27 slots. During the play check of the wood material the user asked for joined bays to behave like one rack: one shared inventory for a section, while stacked bays stay separate. A shuttle should serve exactly the rack its rail docks to. Today a shuttle reaches every storage block that touches a member rail on any side, so a rail along the side or back of a rack still counts as its dock.

Milestone: M6 (Storage automation), a follow up to the Heavy Duty Rack (PLAN.md section 2.3 item 1 and the M6 list).

## What Changes

- Bays that are joined side by side form a section that shares one inventory. Opening any bay of a section shows the slots of every bay in the section, from left to right, in one GUI. The GUI grows with the section (9 rows for a full section of 3 bays, no scrolling).
- Each bay still stores its own part of the slots. Breaking a bay spills only that bay's items; the rest of the section keeps theirs. Stacked bays are never part of the same section.
- A shuttle reaches a rack section through a rail at the front of any of its bays. One front rail is enough for the whole section: a shuttle parks at that rail to pick from or put into any bay of the section.
- A rail touching a rack's side or back never counts as the rack's dock, even when it is part of the warehouse through some other route. A rack section without a front rail stays out of shuttle reach.
- PLAN.md section 2.3 item 1 and the M6 rack line describe sections sharing one inventory (and the wood rule from m6-rack-wood-material).

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/heavy-duty-rack`: the GUI shows the whole section; shuttles serve a section from any front rail; side and back rails never dock.

## Non-goals

- Item handler capability for the whole section. Hoppers and other mods keep seeing one bay's slots on that bay, so the warehouse index keeps counting each item once, per bay.
- Joining stacked bays into one inventory.
- Scrolling or paging in the rack GUI.
- Changing how crates, stations and terminals find their rail.

## Impact

- `storage`: `HeavyRackBlock` gets a public section walk, `HeavyRackMenu` and `HeavyRackScreen` show every bay of the section, `ShuttleFleet` asks its host for dock rails instead of every touching rail, `WarehouseNetwork` answers with the front rails of a rack's section, GameTests.
- `PLAN.md` wording for the rack.
