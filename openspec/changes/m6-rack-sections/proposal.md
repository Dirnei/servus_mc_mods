# Proposal

## Why

The rack posts are placed by counting bays from the row's left end, so adding a bay at that end shifts every post along the row. The user saw the joins move when placing a fourth rack on 2026-10-06 and wants existing joins to stay put: a fourth rack beside a full section of three stands alone. This polishes the PLAN.md M4 line "Heavy Duty Rack".

## What Changes

- Joins between bays are decided once, when a bay is placed, and stored on the block. A new bay joins the neighbouring section on a side only while that section has fewer than `bays_per_section` bays (from `rack.json`, example 3). Otherwise it starts its own section. Existing joins never move when bays are added anywhere.
- Breaking a bay only removes the joins towards it, which may split a section into two smaller ones. Rotating a bay with the Configuration Carrot leaves its sections.
- `posts_every` in `rack.json` is renamed to `bays_per_section`.
- Stacking stays geometric: a rack directly above or below with the same facing still continues the posts. A bay placed on top of a row copies the section boundaries of the row below, so a shifted upper row splits above the same post. The user asked for this on 2026-10-06 while testing.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/heavy-duty-rack`: "Bays connect visually" describes sections fixed at placement time.

## Non-goals

- Merging two short sections when a bay between them is placed beyond the cap. The bay joins whichever side has room, left first.

## Impact

- `servus_storage`: `HeavyRackBlock` join logic and neighbour mirroring on placement, `RackRow` becomes the pure section join rule with new tests, `StorageStatsManager` and `rack.json` key rename, the GameTests gain a placement sequence test.
