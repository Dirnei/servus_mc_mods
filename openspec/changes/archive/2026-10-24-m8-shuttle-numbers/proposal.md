## Why

Shuttles of one tier look alike, so a player cannot tell which shuttle is out, stuck or carrying what. The next changes of the shuttle job series (saved jobs, slot locks, following) show which shuttle holds a lock or waits in a queue, so every shuttle needs a stable name first.

## What Changes

- Every shuttle item gets a number the first time it is put into a Shuttle Garage. The number counts up per world, starts at 1 and is never given twice.
- The number is stored on the item, so it stays when the shuttle is taken out, built into a higher tier in the workshop or put into another garage.
- Shuttles that already sit in garages of an existing world get their number when their garage loads. Old saves load without errors.
- A shuttle label names a shuttle everywhere: "Shuttle #12" without a name plate, "Berta (#12)" with one. The numbered shuttle item is named by its label and its tooltip names the tier. The garage GUI shows it through the item, and the client gets it for every drawn shuttle through the shuttle item it already syncs.
- PLAN.md lists the rest of the shuttle job series as open points: saved jobs, slot locks, following and Field Head locks.

## Capabilities

### New Capabilities
- `shuttles/shuttle-numbers`: per world shuttle numbers, given in the garage, kept on the item, and the shuttle label built from number and plate.

### Modified Capabilities
- `shuttles/shuttle-hand-build`: "The plate shows" now names a numbered shuttle by its label ("Berta (#12)") instead of "Berta (Shuttle Mk2)".

## Impact

- Shuttles mod: new data component `shuttle_number`, a world SavedData counter, numbering in `ShuttleGarageBlockEntity`, label helper on `ShuttleItem`, lang entries, GameTests and JUnit tests.
- No change to the fleet, jobs or network sync format; `ShuttleView` already carries the shuttle item.
- PLAN.md: four new open points under Shuttles.
