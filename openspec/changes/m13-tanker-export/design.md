# Design

## Context

From `m13-tanker-import`: `FluidJob`, the fleet legs and stops for fluid, `fluidQueuedFrom` and `fluidIncomingTo`, `FluidRouting` (target order) and `FluidPutAwayPlan`, `WarehouseNetwork.tickFluids`, gantry lamps and state line. The item counterpart is `WarehouseNetwork.restock` for Outbound Stations: `have = bufferCount + pendingTo`, request the difference.

## Decisions

### D1. Keep levels on the gantry

`FluidGantryBlockEntity` keeps `int keep[2]` in mB, saved, synced to the menu through data slots, and added to `GantryContents` (default 0 for older items). The screen shows four buttons under each gauge only in Export mode and a thin mark on the gauge at the keep level; buttons go through `clickMenuButton` ids (tank times 4 plus button). Values clamp to the tank capacity; a tank on Any shows them greyed out.

### D2. Same job type, other direction

`FluidJob` gets a `Purpose { PUT_AWAY, RESTOCK }` field. The legs and stops are the same; only the leftover rule differs (restock leftovers go to a Storage tank by `FluidRouting`, never back to the Export source). This keeps one fluid run in the fleet.

### D3. Restock planning is pure

`gantry/FluidRestockPlan`: `missing(keep, amount, incoming)` and `amount(missing, minLoad, shuttleCapacity, sourceFree)` (0 to wait; a run below the min load goes when it fills the missing amount), and `source(candidates)` ordering Storage tanks holding the key by free fluid descending, then distance. `tickFluids` runs restock after put-aways, so fresh imports are counted first.

### D4. Missing stock

When no Storage tank holds the fluid, the network sets the Export gantry BLOCKED with the fluid name for the state line, like the no room case of Import gantries.

## NeoForge APIs

- None beyond those of `m13-tanker-import`.

## Pure and Minecraft

- Pure, JUnit: `FluidRestockPlan`.
- Minecraft, GameTests: keep level set and saved, restock run, runs on the way, below min load waits, missing stock red lamp, stock appears, partial pick, leftover back to storage, keep on Any ignored.
- Manual: GUI buttons and keep mark.
