# Proposal: m6-hanging-rails

## Why

PLAN.md milestone M6 (mid game warehouse), item "Warehouse Rail". The rail is a floor rail today: it needs a solid block under it. Upper rack rows and elevator floors therefore need a scaffold floor under every rail, which is busy work and clutters the aisle. Players want to hang a rail straight on a rack front or an elevator and bridge short gaps between them.

## What Changes

- A rail no longer needs a floor. A rail with a solid block under it is **standing**. A rail without one is **hung** when it touches an **anchor** (a Heavy Duty Rack front, a Rail Elevator segment or a standing rail) and **free** otherwise.
- Standing and hung rails hold on their own, so a rail can hang straight on a rack front and a whole row of rails can hang along an upper rack row.
- A rail can be placed in open air only beside an anchor or another rail.
- Free rails that link to each other form a **gap**. The **span** of a gap is its free rails plus the hung rails linked to them. A gap holds when its span is at most the maximum span (3 by default) and it links to at least two hung rails.
- A gap that does not hold **collapses**: its free rails and the hung rails linked to them drop as items. Placing a fourth rail beside a three rail bridge collapses all four; a four block gap cannot be bridged at all. Breaking a rack, an elevator segment, a rail or the floor under a rail re-checks the gaps beside it.
- Examples that hold: elevator, rail, rail, rail, elevator; rack, rail, rail, rail, rack; floor rail, rail, rail, rail, floor rail. A bridge is built from both ends: one rail on each anchor first, then the middle.
- The maximum span lives in `warehouse.json` as `rail_max_span`.
- PLAN.md section 2.3 drops "flat floor rail" in favour of rails that stand on a floor or hang on racks and elevators.

## Non-goals

- Crates, controllers, terminals, stations and garages do not anchor a rail (they still dock and link as today).
- No ceiling rails, no slopes, no vertical rails. Rails still link sideways only.
- No new model or texture for hanging rails; they render like floor rails.
- No support beams, pillars or other decoration under hanging rails.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `storage/warehouse-rail`: the floor rail requirement becomes a rail that stands or hangs; new requirement for free rail gaps, span limit and collapse; the floor removed scenario changes.

## Impact

- `storage/rail/RailBlock.java`: placement and survival rules, a scheduled tick that checks the gaps beside a rail and collapses them.
- New pure class `storage/rail/RailSpan.java` with JUnit tests.
- `storage/crate/StorageStatsManager.java` and `warehouse.json`: `rail_max_span`.
- Storage GameTests for hanging rails, bridges and collapse.
- `PLAN.md` section 2.3.
