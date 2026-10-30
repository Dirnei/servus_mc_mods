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
`FluidKeep<K>` gets a third field `restockAt` (mB), always within 0 and `amount`. `clamp(capacity)` limits both to the capacity, then the restock level to the amount. `withAmount(mB, capacity)` sets the keep amount and drags a higher restock level down with it; `withRestockAt(mB, capacity)` sets the restock level and pushes a lower keep amount up with it, both within the capacity. `withFluid` keeps both levels when a fluid was kept; from none it starts at the full capacity with a restock level of `BUCKET` (`FluidKeep.started(fluid, capacity)`). Pure Java, covered by `FluidKeepTest`. The earlier follow-while-equal rule and the step methods go. Pure Java, covered by `FluidKeepTest`.

Saved as `RestockAt` beside `KeepAmount`; a save without it loads with a restock level of 1,000 mB, limited to the keep amount, like a new keep. The item keeps it through the same block entity data as the keep. *Alternative:* a separate field on the block entity. Rejected: the keep is the unit the GUI, the report and the item already carry.

### Start and stop rule
"At or below" rather than "below", so a restock level of 0 means "when empty"; the default of 1 B starts when at most one bucket is left. The station gets a `restocking` flag, saved as `Restocking`. Pure rule in `FluidRestockPlan.restocking(boolean was, int have, int restockAt, int keep)`: true when `have <= restockAt`, or when `was` and at least one bucket is missing (`keep - have >= BUCKET`). `restockFluids` updates the flag every check before it plans; a station that is not restocking queues nothing and its lamp is green. The flag is saved so a reload in the middle of a refill goes on refilling. *Alternative:* no flag, start below the level and stop there. Rejected: a tanker smaller than the gap would leave the tank between the level and the keep.

### Whole buckets
`FluidRestockPlan.BUCKET = 1000` (the pure class has no Minecraft imports; it matches `FluidType.BUCKET_VOLUME`). `amount` takes the least of missing, capacity and source free, rounds it down to a multiple of `BUCKET`, returns 0 when that is 0, and lets a run below `minLoad` wait unless it equals the whole buckets missing (`missing / BUCKET * BUCKET`). A shelf with less than a bucket free yields 0 and the next ranked source is not tried, as today.

### GUI
The step button rows go. The gauge grows from 52 to 80 px (`FluidOutboundMenu` passes its own gauge height; the Fluid Inbound keeps 52), so one bucket of a 16 B tank is 5 px. A white arrow (6x8 px) left of the gauge points at the keep amount and an orange one right of it at the restock level; the marks across the gauge stay. Dragging an arrow maps the mouse y to whole buckets of the capacity; the wheel over an arrow steps 1 B, shift 10 B. Fixed fields at the gauge's middle height, "Keep" left and "Restock" right, are digits-only `EditBox`es in buckets that send on Enter or focus loss, and follow the synced value while not focused. Without a keep fluid arrows and fields are inactive. `IMAGE_HEIGHT` drops by the removed button blocks and grows by the taller gauge. *Alternative:* steppers or buttons. Rejected in the play check: too small and set away from the tank.

### Setting a level
A new payload `SetFluidLevelPayload(boolean restock, int millibuckets)` (client to server, registered beside `SetFluidKeepPayload`) carries every drag end, wheel step and typed value as an absolute amount; the server checks the menu belongs to a Fluid Outbound the player has open and applies `withAmount` or `withRestockAt`. Dragging sends once on release and shows the dragged value locally meanwhile, so a drag does not flood the server. The `restockButton`/`keepButton` ids and `KEEP_STEPS` go.

### Report
`FluidStationReport.keeps` uses a new key `message.servus_shuttles.potato.fluid_station_keeps_restock` ", keeps %s mB of %s, restocks at %s mB" in place of `fluid_station_keeps`, in the style of the other potato lines.

### NeoForge APIs
No new APIs. Existing: `ValueInput.getIntOr` / `getBooleanOr`, `ValueOutput.putInt` / `putBoolean` (26.1 block entity save, already used in `FluidOutboundBlockEntity`), `AbstractContainerMenu.clickMenuButton`, `ContainerData`.

### Pure and Minecraft parts
Pure (JUnit): `FluidKeep` (clamp, push up, drag down, start full), `FluidRestockPlan.amount` (whole buckets, min load) and `FluidRestockPlan.restocking`. Minecraft (GameTest): restock start at the level, refill above it, whole bucket runs, save and load of the level and flag, the level payload with push and drag. GUI layout and marks: play check.

## Risks / Trade-offs

- A keep amount that is not a whole bucket cannot happen from the GUI, but a hand-filled tank can sit at 7,250 of 8,000 mB forever. That is intended: less than a bucket is not worth a trip.
- The lamp is green between the restock level and the keep amount while the tank drains. That reads as "fine", which it is.
