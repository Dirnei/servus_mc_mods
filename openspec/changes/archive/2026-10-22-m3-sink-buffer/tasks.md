# Tasks: m3-sink-buffer

## 1. Pure logic

- [x] 1.1 Add `SinkBatch.plan(int[] amounts, double budget)` in `foundation/.../machine/sink/SinkBatch.java` (no Minecraft imports). Add `SinkBatchTest` with these cases:
  - budget 8 over 3, 64, 10 gives 3, 5, 0;
  - an unlimited budget takes everything;
  - budget 0 takes nothing;
  - empty indices are skipped;
  - a budget larger than the contents takes all of it.

  Verify with `gradlew :foundation:test`.
- [x] 1.2 Add `SinkCapacity` in `foundation/.../machine/sink/SinkCapacity.java`: open slots and open tanks for an upgrade count, and `canRemoveUpgrade(upgrades, lastRowEmpty, lastTankEmpty)`. Add `SinkCapacityTest` with these cases:
  - 0 to 3 upgrades give 7 to 28 slots and 1 to 4 tanks;
  - removal is refused with items in the last row;
  - removal is refused with fluid in the last tank;
  - removal is allowed when both are empty;
  - removal is refused with 0 upgrades.

  Verify with `gradlew :foundation:test`.
- [x] 1.3 Change `SinkRates` to `fluid_per_second` and add `itemsPerBatch(height)` and `fluidPerBatch(height)` (per 20 ticks, `UNLIMITED` from the unlimited height). Update `SinkRatesTest` for the per second fluid table and the batch values. Verify with `gradlew :foundation:test`.

## 2. Data

- [x] 2.1 Extend `SinkStats` with `slots_per_row` (clamped 1..7), `tank_capacity` and `upgrade_slots`, and rename `fluid_per_tick` to `fluid_per_second` in its codec and `DEFAULTS`. Update `foundation/src/main/resources/data/servus_foundation/sink/sink.json` to the format in design.md and the log line in `SinkStatsManager`. Verify with `gradlew :foundation:build` and check the loaded stats in the server log.

## 3. Buffer and heartbeat

- [x] 3.1 In `SinkBlockEntity`, add the item buffer (`ItemSlots` sized for the maximum, closed slots refuse) and the fluid buffer (a `FluidStacksResourceHandler` with one tank per row, closed tanks refuse). Both insert through `ResourceHandlerUtil.insertStacking`, and `onContentsChanged` calls `wake()`. Add the `CapacityUpgradeSlots`. Save and load all three plus `lastBatch` with `ValueOutput` and `ValueInput`. Verify with `gradlew :foundation:build`.
- [x] 3.2 Replace the handlers in `SinkHandlers`:
  - insert-only `DelegatingResourceHandler` views for items and fluid, with `extract` returning 0;
  - keep `Energy` with its `VoidBudget`;
  - drop `Items`, `Fluid` and the item and fluid budgets.

  Route `itemHandler(side)` and `fluidHandler(side)` to the views. Verify with `gradlew :foundation:build`.
- [x] 3.3 Build the heartbeat from design.md:
  - `wake()` on the controller;
  - `SinkBlock.tick` voids one batch through `SinkBatch.plan` when 20 ticks have passed since the last batch;
  - it reschedules in 20 while the buffer holds something, or else in the remaining active time, or else turns idle;
  - `markVoided` and `onLoad` call `wake()`.

  Verify with `gradlew :foundation:build`.
- [x] 3.4 In `SinkBlock`:
  - buckets fill the tanks through the normal fluid handler, so a full container stays full when there is no room, and `manualFluidHandler` goes away;
  - unforming or removing the controller clears the buffer and drops the Capacity Upgrades at the controller position.

  Verify with `gradlew :foundation:build`.
- [x] 3.5 Rework `SinkGameTests` for the new spec, adapting `hopperEmptiesIntoSink`, `sinkBaseRateTakesFourItems`, `sinkWithFiveChimneysIsUnlimited`, `sinkRateGrowsWithChimney`, `sinkGivesNothing`, `sinkActiveWhileVoiding` and `sinkEmptiesLavaBucket`, and removing `sinkTrashSlotVoidsStack`. Add tests for:
  - a full buffer refusing;
  - two fluids in two tanks;
  - four items per batch;
  - upgrade removal refused and allowed;
  - the bucket refused without room;
  - breaking dropping the upgrades and no items;
  - the buffer kept after save and load, via block entity save and load.

  Verify with `gradlew :foundation:runGameTestServer` (or the project's GameTest task) and `gradlew :foundation:test`.

## 4. Menu

- [x] 4.1 Rewrite `SinkMenu` from the trash slot to:
  - 4 rows of 7 `ResourceHandlerSlot`s on the real item buffer, where closed slots are inactive;
  - 3 upgrade slots on the side tab whose `mayPickup` asks `SinkCapacity`, with the refusal time recorded;
  - the player inventory at x 8;
  - shift click both ways;
  - `ContainerData` for the height, the rates (items/s, mB/s, FE/t), the open count, each tank's fluid id and amount and the tank capacity in 16 bit halves, the ticks to the next batch and whether an upgrade may come out; the refusal time is recorded in the menu on each side, as in `FluidGantryMenu`.

  Verify with `gradlew :foundation:build` and adjust `sinkGuiShowsUnlimited` to read the new data.

## 5. GUI

- [x] 5.1 Write `tools/sink-textures/gen.py`, which generates `foundation/src/main/resources/assets/servus_foundation/textures/gui/sink.png`. It holds:
  - the soot and brick panel 176 wide and the side tab for 3 slots;
  - the slot and tank wells and the black iron gate tile for closed slots and tanks;
  - the ember overlay, the batch bar, the chimney pips and two fan frames.

  Follow `temp/sink-mockup.html`. Verify that the script runs and that the PNG has the expected size.
- [x] 5.2 Rewrite `SinkScreen`:
  - draw the background and the gate over closed slots and tanks;
  - draw the sideways tanks with fluid sprites (add a horizontal mode to `FluidGauge` or draw them here), with an amount and capacity tooltip;
  - draw the ember glow on the slots `SinkBatch.plan` marks, the batch bar, the chimney pips and the fan icon while active;
  - show the rates line and the red refusal message.

  Update the lang strings in `MachineDataParts`: remove `trash` and `hint`, and add the rate units per second, the refusal message and the tooltips. Then run datagen. Verify with `gradlew :foundation:runData` and `gradlew :foundation:build`.
- [x] 5.3 Play check by the user once the change is merged to main; the client is launched from the main checkout, never from a worktree:
  - fill the Sink with a hopper and with pipes;
  - watch the batches at chimney heights 0, 2 and 5;
  - add and remove upgrades, including the refusal;
  - take items back;
  - empty a bucket;
  - break the Sink and reload the world;
  - check the gate, the ember glow, the bar and the fan in the GUI at GUI scales 2 and 3.

## 6. Docs

- [x] 6.1 Update the Sink entry in `docs/src/content/docs/mods/foundation.md`: the buffer, batches every second, Capacity Upgrades, taking items back and the new `sink.json` fields. Verify that the docs build still passes (`npm run build` in `docs/`).

## 7. Integration

- [x] 7.1 Full `gradlew build` and `gradlew test` green, GameTests green, and the game launches with all mods.

## Workflow follow-up

- Archive with `/opsx:archive` after the play check; no PLAN.md item to remove, since the Sink rework was not listed.
