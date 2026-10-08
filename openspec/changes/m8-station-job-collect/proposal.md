# Proposal

## Why

Since `m8-station-hands` a Crafting Station crafts only CPU jobs, and the CPU counts a step as done when the step's outputs arrive back at an Access Point as packets. The station still pushes its outputs into adjacent inventories through Auto and Push sides. A station placed right beside an Access Point therefore pushes its sticks straight in: they are put away, but never counted, and the order stays running forever. This is a follow up fix inside PLAN.md M8 (between items 8 and 9), found while archiving `m8-station-hands`.

## What Changes

- While a Crafting Station holds a job (from the moment a CPU hands it one until the CPU ends it), it SHALL NOT push its outputs into neighbours. The CPU alone collects a job's outputs.
- Without a job the station pushes leftover outputs through Auto and Push sides as before, so items left after a cancelled job still leave the station.
- Pulling job inputs through Pull sides is unchanged.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/crafting-station`: the push rule of "Machine like sides" depends on the job.

## Non-goals

- Counting outputs that leave a station another way (a player taking them, a hopper pulling them): the step still waits, as recorded in `m8-crafting-cpu`.
- Changing how the CPU collects outputs.

## Impact

- `storage/.../crafting/CraftingStationBlockEntity.java` (push rule).
- GameTests: a new test with the station directly beside an Access Point; the dock test that relied on pushing during a job now releases the job first.
