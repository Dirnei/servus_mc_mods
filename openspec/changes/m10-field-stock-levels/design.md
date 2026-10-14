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
- `clamp(value)` to 0..99,999
- `keepOrder(limit, level, moved)`: moving one past the other moves the other along

**Stopping harvests.** In `tickFields` the harvest check passes `harvestDue && !paused` to `FieldDue.due`, so a capped field still gets runs for tilling and planting. `HarvestRun` carries a `harvest` flag, set from the head when the run starts and refreshed at each rail, and `PlotWork.plan` gets it as a parameter: with harvesting paused, ripe plots get no action.

**Stopping fertilizing.** `fertilizeDue` and `fertilizeTargets` return nothing while `fertilizeAllowed` is false, so no Spreader is called and a Spreader already out stops dosing at its next plot. `FertilizeStatus` gains `NOT_NEEDED` (not a problem).

**Stock read.** The head reads the produce stock from its warehouse at each field check (no per tick scan) and keeps it for the GUI and the statuses.

**GUI (after the play test).** The first version put a level row with -64, -1, +1, +64 buttons on the Field and Fertilizer tabs, grew the GUI by 36 px and narrowed the pickers to fit a buffer count beside them. The user's play test found it far too crowded and asked for typed amounts with room of their own. So:
- **Panel:** 264 x 236 px (was 176 x 214), painted with `PanelPainter`; the player inventory is centred (x 51, y 154). At GUI scale 3 a 1280 x 720 window is 426 x 240 GUI px, so panel and tabs (28 px left of it) fit with 2 px to spare in height. `init` clamps `leftPos` to at least the tab width and `topPos` to at least 0, so a smaller window cuts the bottom rather than the tabs.
- **Tabs:** Field, Water, Fertilizer, Stock, Upgrades. The Stock tab (icon: a chest) never carries a red marker.
- **Field tab:** the crop picker full width (240 px), the ripe counts and the ripe share bar, the harvest threshold with - and +, the work status (two rows, 240 px wide) including "Harvest paused: 1,024 / 1,000 Wheat". No level row, no buffer count.
- **Water tab:** water, bar, hydration and corrosion. The anti-corrosion count moved to the Stock tab.
- **Fertilizer tab:** the fertilizer picker full width, wider mode buttons, the Until ripe threshold, the status line including "Not needed: 640 Wheat in stock (below 500)". No level row, no reserve, no buffer count.
- **Stock tab:** the produce icon, name and stock ("Wheat in stock: 1,024") or "Produce learned at the first harvest"; three rows "Stop harvesting at", "Fertilize below", "Keep fertilizer in stock", each a label, a vanilla `EditBox` (46 x 14) and a grey hint ("0 = no limit", "0 = always"); under a "Supplies in the head" heading the seeds, fertilizer and anti-corrosion icons side by side with "held/level", each with the buffer tooltip (what it is, filled by supply runs). The label tooltips keep what a level does and the produce stock.
- **Typed entry:** an `EditBox` takes digits only (`setFilter`) and at most 5 characters. A small client helper `AmountField` commits on Enter or when the box loses focus (checked each client tick, as the controller's name field does), and when the screen closes: an empty box counts as 0, the value is clamped on the client too and sent only when it differs from the head's. While a box is focused it is never overwritten; unfocused it shows the head's value from the container data, except for a short grace (20 ticks) after a commit until the server's answer arrives. A click anywhere else in the GUI and a tab change clear the focus. While a box is focused, keys go to it, so E does not close the GUI.
- **Payload:** `SetFieldAmountPayload(pos, which, value)` replaces `SetFieldLevelPayload` and the four reserve menu buttons; `which` is a `FieldAmount` (`HARVEST_LIMIT`, `FERTILIZE_BELOW`, `RESERVE`). `FieldHeadMenu.setAmount` acts only while the player's open menu is that head's menu and still valid (distance check), clamps (levels 0..99,999, reserve 0..`FertilizePlan.MAX_RESERVE`) and, for the levels, keeps the order with `StockLevels.keepOrder`. Registered in `ServusStoragePayloads`.
- **Pure logic:** `FieldAmount.parse(text)` (empty is 0, digits only, saturating) and `FieldAmount.clamp(value)` per kind, with JUnit. `StockLevels.step` and `FertilizePlan.stepReserve` go, nothing steps any more.

**Data.** `harvest_limit` and `fertilize_below` in `storage/field.json`, read by `StorageStatsManager`.

**NeoForge APIs.** Only the existing payload registration (`RegisterPayloadHandlersEvent` in `ServusStoragePayloads`) and vanilla `EditBox` (`setFilter`, `setMaxLength`, `canConsumeInput`), as the controller and terminal screens use them; nothing new.

**PLAN.md.** Settles no open decision.

**Pure logic vs Minecraft.** `Produce` and `StockLevels` are pure and JUnit tested. Runs, statuses and the GUI are covered by GameTests and the play check.

## Risks / Trade-offs

- [A capped field keeps ripe crops forever while stock stays high] → Intended: the field is the store.
- [Two wheat fields count the same stock] → Each stops at its own limit; acceptable, a shared limit is a non-goal.
- [The produce is unknown until the first harvest, so levels do nothing at first] → The GUI says "learned at the first harvest".
- [A rare drop wins one harvest] → Counts are per run; the item that dominates a run is picked, and the next run corrects it.
- [A typed value the server clamps or reorders looks like it was ignored] → The box shows the head's value once the answer arrives, so the player sees 99999 or the moved level.
