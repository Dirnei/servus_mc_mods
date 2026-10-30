# Design

## Context

`WarehouseNetwork.restockFluids` runs every `fluid_check_interval` ticks. For each Fluid Outbound it computes what is missing to `FluidKeep.target()` (`FluidRestockPlan.missing`) and queues one run of `FluidRestockPlan.amount(missing, tankerMinLoad, capacity, sourceFree)`, which takes the exact missing millibuckets. The keep amount itself already steps in whole buckets (`FluidOutboundMenu.KEEP_STEPS`), but the tank drains in any amount, so runs carry odd amounts and start as soon as anything is missing. PLAN.md leaves no open decision for this; it refines the built M13 restock runs.

## Goals / Non-Goals

**Goals:**
- Restock runs carry whole buckets only.
- A per station restock level with a start and stop rule (hysteresis), set in the GUI.

**Non-Goals:**
- Whole buckets for put-away runs from Fluid Inbounds, and the Foundation amount widgets for the steps.

## Decisions

### Restock level lives in `FluidKeep`
`FluidKeep<K>` gets a third field `restockAt` (mB), always within 0 and `amount`. `clamp(capacity)` limits the amount, then the restock level to the amount. Stepping the keep amount moves the restock level along while the two are equal, so a new keep and a station that never touched its restock level restock at the keep amount, as today. A lower keep clamps it. `stepRestock(delta)` steps only the restock level within 0 and the amount. `withFluid` keeps both amounts. Pure Java, covered by `FluidKeepTest`.

Saved as `RestockAt` beside `KeepAmount`; a save without it loads with the restock level at the keep amount. The item keeps it through the same block entity data as the keep. *Alternative:* a separate field on the block entity. Rejected: the keep is the unit the GUI, the report and the item already carry.

### Start and stop rule
"At or below" rather than "below", so a restock level of 0 means "when empty" and the default (level = keep) starts as soon as one bucket is missing. The station gets a `restocking` flag, saved as `Restocking`. Pure rule in `FluidRestockPlan.restocking(boolean was, int have, int restockAt, int keep)`: true when `have <= restockAt`, or when `was` and at least one bucket is missing (`keep - have >= BUCKET`). `restockFluids` updates the flag every check before it plans; a station that is not restocking queues nothing and its lamp is green. The flag is saved so a reload in the middle of a refill goes on refilling. *Alternative:* no flag, start below the level and stop there. Rejected: a tanker smaller than the gap would leave the tank between the level and the keep.

### Whole buckets
`FluidRestockPlan.BUCKET = 1000` (the pure class has no Minecraft imports; it matches `FluidType.BUCKET_VOLUME`). `amount` takes the least of missing, capacity and source free, rounds it down to a multiple of `BUCKET`, returns 0 when that is 0, and lets a run below `minLoad` wait unless it equals the whole buckets missing (`missing / BUCKET * BUCKET`). A shelf with less than a bucket free yields 0 and the next ranked source is not tried, as today.

### GUI
Under the keep amount and its four buttons, a second label "Restock at N B" with its own four buttons (ids `KEEP_STEPS.length + step`, `FluidOutboundMenu.restockButton(step)`), both rows inactive without a keep fluid. The gauge draws the restock mark in orange (`0xFFFFA040`) under the white keep mark. The menu syncs the restock level as two more data halves (`EXTRA_COUNT` 3 to 5). `IMAGE_HEIGHT` grows by one label and one button block. *Alternative:* one row of buttons with a Keep/Restock toggle. Rejected: hides which value the buttons change.

### Report
`FluidStationReport.keeps` uses a new key `message.servus_shuttles.potato.fluid_station_keeps_restock` ", keeps %s mB of %s, restocks at %s mB" in place of `fluid_station_keeps`, in the style of the other potato lines.

### NeoForge APIs
No new APIs. Existing: `ValueInput.getIntOr` / `getBooleanOr`, `ValueOutput.putInt` / `putBoolean` (26.1 block entity save, already used in `FluidOutboundBlockEntity`), `AbstractContainerMenu.clickMenuButton`, `ContainerData`.

### Pure and Minecraft parts
Pure (JUnit): `FluidKeep` (restock level clamp, follow, step), `FluidRestockPlan.amount` (whole buckets, min load) and `FluidRestockPlan.restocking`. Minecraft (GameTest): restock start at the level, refill above it, whole bucket runs, save and load of the level and flag, menu buttons. GUI layout and marks: play check.

## Risks / Trade-offs

- A keep amount that is not a whole bucket cannot happen from the GUI, but a hand-filled tank can sit at 7,250 of 8,000 mB forever. That is intended: less than a bucket is not worth a trip.
- The lamp is green between the restock level and the keep amount while the tank drains. That reads as "fine", which it is.
