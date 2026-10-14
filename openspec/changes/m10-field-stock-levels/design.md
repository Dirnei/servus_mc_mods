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

**Anti-corrosion item and reserve (play test).** The user asked for a minimum stock of honeycomb, to choose the anti-corrosion item on the Water tab, and never to call it wax in the UI.
- The head keeps `antiCorrosionChosen` and `antiCorrosion` (null for Off), saved and synced as `AntiCorrosion` (an empty id for Off, absent until chosen, so a new head follows the first item of the tag). `supplyItem(ANTI_CORROSION)` is that item, its level 0 while Off; a held item that is no longer it is stale like an old crop, and `age` only counts the chosen item. The client reads the item from the update tag, which replaces the buffer item sync (`AntiCorrosionItem`).
- `ChooseAntiCorrosionPayload` and `FieldHeadMenu.chooseAntiCorrosion` work like the fertilizer choice. `FieldFertilizerStockPayload` becomes `FieldSupplyStockPayload` with the stock of both tags, and the client `FertilizerPickerEntries` becomes `TagPickerEntries`, shared by both pickers.
- The Water tab puts the picker at the top across the page; the water lines move down by 22 px.
- The head keeps `antiCorrosionReserve` (-1 follows `anti_corrosion_reserve`, example 0, saved as `AntiCorrosionReserve`), clamped like the fertilizer reserve. `FieldHeadBlockEntity.supplyReserve(part)` gives each part's reserve (seeds 0); `WarehouseNetwork.supplyDue` and the supply run (`SupplyRun.reserves`, fixed when it plans) use it, so `SupplyPlan` keeps its shape.
- `FieldAmount.ANTI_CORROSION_RESERVE` is a fourth typed amount. The field command prints the reserve on the corrosion line.

**Stock table (play test).** The user wanted the Stock tab structured: first the warehouse stock of produce, seeds, fertilizer and anti-corrosion item, then the head's buffer, then the settings, and the explanations only in tooltips. The tab is a table: row labels in x 8 to 84, then four 43 px columns (produce, seeds, fertilizer, anti-corrosion) headed by the item icon at y 18. Rows: Warehouse (y 40), Field Head (y 54), a rule at y 67, Keep at least (boxes at y 72 under fertilizer and anti-corrosion), Harvest up to (y 89) and Fertilize below (y 106), both boxes under produce; every box 38 x 14, centred in its column. The warehouse counts come from what the client already has: the produce stock from the container data, the seed stock from the crop picker's stock, the fertilizer and anti-corrosion stock from the supply stock payload. Pure `CompactCount` (client, JUnit) shortens counts from 100,000 up (123k, 1.2M). The hints ("0 = no limit") and the item names beside the reserves move into the tooltips.

**Ripe slider, water use, aging estimate and axe cost (play test).**
- The ripe share bar on the Field tab is a slider too. A left click within 3 px of the bar, or a drag that started there, sets the threshold to `FieldThreshold.fromFraction` of the mouse position (pure, JUnit), sent as menu button `BUTTON_HARVEST_PERCENT + percent / STEP` only when the step changes; the mark and the "Harvest at" line show the dragged value until the mouse is released. The - and + buttons stay.
- The Water tab drops "Hydrated": it adds nothing beside the water bar. In its place the field's use per minute while watered (`FieldWater.perMinute`, pure), or "Dry" in orange. Below the corrosion stage, the watering time to the next aging step (`FieldWater.ticksToUse` of the water left to the step, whole intervals, pure) as a rough real time (`FieldDuration`, pure: "~50m"), saying when an anti-corrosion item in the buffer will hold the step. The head computes both on the server from `field.json`, so a client never needs the server's datapack; they travel as container data (split ints, the time as seconds plus one, zero while unknown).
- The estimate showed how slow aging was: 64000 mB made a stage of a five rail field take almost 18 hours of watering. The user wants 1 to 2 hours, so `corrosion_water` drops to 6000: about 1h 40m for five rails, 50m for ten, 25m for twenty. Larger fields age faster and use more anti-corrosion items, one per held step, which is the intended upkeep.
- The axe cost per rail moves into `field.json` as `axe_damage_per_rail` (2, was a fixed 1). `FieldCorrosion.axeDamage` (pure) multiplies, saturating.

**Data.** `harvest_limit`, `fertilize_below`, `anti_corrosion_reserve` and `axe_damage_per_rail` in `storage/field.json`, and `corrosion_water` lowered to 6000, read by `StorageStatsManager`.

**NeoForge APIs.** Only the existing payload registration (`RegisterPayloadHandlersEvent` in `ServusStoragePayloads`) and vanilla `EditBox` (`setFilter`, `setMaxLength`, `canConsumeInput`), as the controller and terminal screens use them; nothing new.

**PLAN.md.** Settles no open decision.

**Pure logic vs Minecraft.** `Produce` and `StockLevels` are pure and JUnit tested. Runs, statuses and the GUI are covered by GameTests and the play check.

## Risks / Trade-offs

- [A capped field keeps ripe crops forever while stock stays high] → Intended: the field is the store.
- [Two wheat fields count the same stock] → Each stops at its own limit; acceptable, a shared limit is a non-goal.
- [The produce is unknown until the first harvest, so levels do nothing at first] → The GUI says "learned at the first harvest".
- [A rare drop wins one harvest] → Counts are per run; the item that dominates a run is picked, and the next run corrects it.
- [A typed value the server clamps or reorders looks like it was ignored] → The box shows the head's value once the answer arrives, so the player sees 99999 or the moved level.
