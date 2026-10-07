# Proposal

## Why

Play test (2026-10-07): two shuttles froze in front of a rack and could not be taken out of their garage. The fleet stops ticking altogether when no garage has a home rail, so shuttles that are already out stand still forever, and the garage GUI keeps their slots locked because they count as out. The garage had been built before the rail moved to its back and lost its home that way, but any rail break has the same effect. A stuck fleet must never need a world reload: the garage gets a recall, a pause switch, and the fleet keeps moving and recalls by itself when a home disappears.

## What Changes

- **Shuttles always finish.** The fleet ticks motions and legs whether or not any garage has a home. Only job assignment needs a home.
- **Losing the home recalls.** When a garage's home rail disappears, its shuttles out on jobs are recalled at once: cargo drops at the garage, jobs not yet loaded go back to the queue, loaded picks count as missing, and the shuttles sit inside the garage.
- **Recall button.** The garage GUI gets a Recall button that does the same for every shuttle of the garage on demand, so a confused fleet can always be reset and the shuttle items taken out.
- **Pause switch.** The garage GUI gets an Active / Paused toggle. A paused garage hands out no shuttles for new jobs; shuttles already out finish their job and park. The state persists with the block and shows in the GUI line and the warehouse summary.

## Capabilities

### Modified Capabilities
- `storage/shuttle-garage`: recall, pause, and the home loss rule.

## Non-goals

- A watchdog for shuttles whose path is physically blocked. Recall covers it by hand.
- A redstone input for the pause switch.

## Impact

- `storage`: `ShuttleFleet` (tick without homes, `recall`, `setActive`, recall in `refreshHome`), `ShuttleGarageBlockEntity` (`active` flag, NBT), `ShuttleGarageMenu` and `ShuttleGarageScreen` (two buttons, a data slot), `WarehouseNetwork` pass through, lang, GameTests.
