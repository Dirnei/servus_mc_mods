# Design

## Context

- **Field checks.** `WarehouseNetwork.tickFields` queues a `FieldJob` when `FieldDue.due(harvestDue, plantMode, hydrated, tillable, plantable, seeds)` holds. It queues a `FertilizeJob` when `head.fertilizeDue(now, stock)` holds (after `m10-field-head-buffer`: when the head's buffer holds fertilizer).
- **Plot work.** In a run, `FieldRuns` plans each plot with `PlotWork.plan(state, plants, haveSeed)`, whose `Action(harvest, till, plant)` decides the work. Harvest drops are collected per plot into `drops` before they go into the cargo (`addOrDrop`).
- **Stock.** The warehouse's stock of an item is `available(ItemKey)`.

## Goals / Non-Goals

**Goals:** fields that produce on demand, with no settings needed for players who do not care (0 means off).

**Non-Goals:** shared limits across fields, a hand-picked produce.

## Decisions

**Produce learning.** `HarvestRun` gets a per run tally of harvested drops (item key to count), added where the drops of a harvested plot are collected, before the seed for replanting is taken out. When a run ends having harvested, the head gets the tally and pure `Produce.pick(tally, crop)` returns the item with the highest count, leaving out the crop item unless it is the only one. Ties are broken by registry id. The head saves the produce (an item id) and clears it in `setCrop` when the crop changes. In harvest only mode (no crop) the plain highest count wins.

**Levels.** The head saves `harvestLimit` and `fertilizeBelow` (ints, -1 = follow `field.json`), with getters that fall back to `harvest_limit` and `fertilize_below` (example 0). Pure `StockLevels` holds the rules (JUnit):
- `harvestPaused(produceKnown, stock, limit)`: a limit above 0 and stock at or above it
- `fertilizeAllowed(produceKnown, stock, level, limit)`: no level, or stock below the level, and harvesting not paused
- `step(value, delta)` clamped to 0..99,999
- `keepOrder(limit, level, moved)`: moving one past the other moves the other along

**Stopping harvests.** In `tickFields` the harvest check passes `harvestDue && !paused` to `FieldDue.due`, so a capped field still gets runs for tilling and planting. `HarvestRun` carries a `harvest` flag, set from the head when the run starts and refreshed at each rail, and `PlotWork.plan` gets it as a parameter: with harvesting paused, ripe plots get no action.

**Stopping fertilizing.** `fertilizeDue` and `fertilizeTargets` return nothing while `fertilizeAllowed` is false, so no Spreader is called and a Spreader already out stops dosing at its next plot. `FertilizeStatus` gains `NOT_NEEDED` (not a problem).

**Stock read.** The head reads the produce stock from its warehouse at each field check (no per tick scan) and keeps it for the GUI and the statuses.

**GUI.**
- **Field tab:** a row "Stop harvesting at" with the produce icon (or "learned at the first harvest"), the value or "no limit", and -64, -1, +1, +64 buttons, where shift moves ten times as far.
- **Fertilizer tab:** the same row for "Fertilize below".
- One payload `SetFieldLevelPayload(pos, which, delta, shift)`, checked on the server like the reserve buttons.
- The work status line shows "Harvest paused: 1,024 / 1,000 Wheat", the Fertilizer status "Not needed: 640 Wheat in stock (below 500)". Neither is a problem: no red marker, no red bar.

**Data.** `harvest_limit` and `fertilize_below` in `storage/field.json`, read by `StorageStatsManager`.

**NeoForge APIs.** Only the existing payload registration (`RegisterPayloadHandlersEvent` in `ServusStoragePayloads`); nothing new.

**PLAN.md.** Settles no open decision.

**Pure logic vs Minecraft.** `Produce` and `StockLevels` are pure and JUnit tested. Runs, statuses and the GUI are covered by GameTests and the play check.

## Risks / Trade-offs

- [A capped field keeps ripe crops forever while stock stays high] → Intended: the field is the store.
- [Two wheat fields count the same stock] → Each stops at its own limit; acceptable, a shared limit is a non-goal.
- [The produce is unknown until the first harvest, so levels do nothing at first] → The GUI says "learned at the first harvest".
- [A rare drop wins one harvest] → Counts are per run; the item that dominates a run is picked, and the next run corrects it.
