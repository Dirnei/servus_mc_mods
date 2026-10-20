# Design: m3-sink-buffer

## Context

Today the Sink controller (part 0 of the cube) owns three `VoidBudget` token buckets and hands out handlers that hold nothing: one `LegacyItemHandler` slot, one endless `LegacyFluidHandler` tank and a `LegacyEnergyHandler`. Each insert asks the bucket on the spot and refuses the rest. The block entity has no ticker. The active state is cleared by a scheduled block tick in `SinkBlock.tick`, and nothing is saved except the chimney height in the client update tag. The GUI (`SinkMenu`, `SinkScreen`, `textures/gui/sink.png`) is one trash slot plus text.

PLAN.md leaves nothing open for the Sink. The decisions below were settled in explore mode with the user (see proposal.md) and are recorded here with how they are built.

## Goals / Non-Goals

**Goals:**
- A buffer in the controller, filled through the standard NeoForge transfer capabilities and drained in batches every 20 ticks.
- No per tick work: one scheduled block tick every 20 ticks while the Sink holds something or is active, none when idle.
- The batch maths and capacity rules in pure Java with JUnit tests.
- A GUI in soot and brick that shows the buffer, the locked rows as a closed iron gate and the next batch.

**Non-Goals:**
- Changing how energy is voided.
- A shared "buffered machine" base class for other blocks. The Sink is the only user for now.

## Decisions

### Buffer storage: `ItemSlots` and `FluidStacksResourceHandler` sized for the maximum

The controller holds one `ItemSlots` of `slots_per_row * (upgrade_slots + 1)` slots (example 28) and one `FluidStacksResourceHandler` with `upgrade_slots + 1` tanks of `tank_capacity`, both sized from `sink.json` when the block entity is created. Slots and tanks beyond the open count stay in the handler but are closed:
- `isValid(index, resource)` returns false for them;
- the item slots report `getCapacity` 0 for them.

Sizing for the maximum keeps indices stable when upgrades come and go, so a menu slot always maps to the same handler index.

*Alternative:* resize the handlers on every upgrade change. That means rebuilding menu slots and copying contents, and there is no gain.

Inserts go through `ResourceHandlerUtil.insertStacking` (NeoForge `transfer/ResourceHandlerUtil.java`). It fills matching stacks first and then empty ones. The default `ResourceHandler.insert` is first fit by index, which would scatter one item type over several slots.

### Automation sees an insert-only view

The capabilities (`Capabilities.Item.BLOCK`, `Capabilities.Fluid.BLOCK` registered in `MachineEvents` and routed through `SinkBlockEntity.itemHandler(side)` and `fluidHandler(side)`, unchanged) return a `DelegatingResourceHandler` subclass whose `extract` returns 0 (NeoForge `transfer/DelegatingResourceHandler.java`). The menu works on the real handlers, so the player can take items out. Energy keeps `SinkHandlers.Energy` and its `VoidBudget`.

### One heartbeat tick, 20 ticks apart

Vanilla keeps one scheduled block tick per position and block, and a second `scheduleTick` for the same pair is dropped. The Sink therefore has a single heartbeat on the controller position that serves both the batch and the active state:

```
wake():  if no tick is scheduled at the controller -> schedule in 20
tick():  if buffer holds something and now - lastBatch >= 20 -> void one batch, lastBatch = now
         if buffer holds something -> schedule in 20
         else if active time left > 0 -> schedule in min(that time, 20)
         else -> ACTIVE = false
```

`wake()` runs from `onContentsChanged` of both handlers. In NeoForge 26.1 it fires on the root transaction commit (`StacksResourceHandler`, `onRootCommit`), so simulated and aborted inserts do not wake the Sink. `wake()` also runs from `markVoided()` for energy and on load when the buffer is not empty. The `lastBatch` guard keeps batches at least 20 ticks apart even when a tick that was scheduled for the active state fires early. The tick for the active state is capped at 20 ticks, so items that arrive while the Sink is active and empty wait at most one batch.

*Alternative:* a block entity ticker returned only for `ACTIVE=true` states. That still costs a call every tick while active, and the user asked for nothing per tick that is not needed.

### Batch planning in pure Java

`SinkBatch.plan(int[] amounts, long budget)` returns how much to take from each index, walking from index 0 and leaving the rest at 0. An infinite budget (`SinkRates.UNLIMITED`) takes everything. The same method serves item counts and fluid amounts. `SinkCapacity` answers:
- the open slot count and open tank count for an upgrade count;
- whether an upgrade may come out, given whether the last open row and tank are empty.

Both classes have no Minecraft imports. `SinkRates` stays pure and switches its fluid table to mB per second.

### Rates and data

`sink.json` becomes:

```json
{
  "items_per_second": [4, 8, 16, 32, 64],
  "fluid_per_second": [1000, 2000, 4000, 8000, 16000],
  "energy_per_tick": [100, 200, 400, 800, 1600],
  "active_ticks": 40,
  "slots_per_row": 7,
  "tank_capacity": 8000,
  "upgrade_slots": 3
}
```

`fluid_per_tick` goes away without a fallback, because the mods are unreleased. `SinkStats` reads the new fields with defaults. `SinkRates` gets `itemsPerBatch(height)` and `fluidPerBatch(height)`, both per 20 ticks. The GUI shows items per second, mB per second and FE per tick.

The GUI has room for exactly 7 slots per row next to a tank, so `slots_per_row` is read as data but the screen lays out 7 columns. A datapack value above 7 would not fit on screen, so `SinkStats` clamps it to 1..7.

### Capacity Upgrades

The controller owns a `CapacityUpgradeSlots(upgrade_slots, this::onUpgradesChanged)`. `onUpgradesChanged` saves, syncs and wakes the Sink. The menu uses an upgrade slot like `FluidGantryMenu.UpgradeSlot`:
- `mayPickup` asks `SinkCapacity.canRemoveUpgrade`;
- on a refusal it records the game time, so the GUI shows "Empty the last row and tank first" for a few seconds.

Upgrades drop at the controller position and the buffer is cleared in `SinkBlockEntity.dissolve()`. It runs from two places:
- `SinkBlock.affectNeighborsAfterRemoval` (through `unformRest`) when another block of the cube is removed and the controller turns back into a casing; the controller's block entity stays, since the block does not change;
- `SinkBlockEntity.preRemoveSideEffects` when the controller block itself is removed, because its block entity is already gone by the time `affectNeighborsAfterRemoval` runs.

### Saving and sync

The controller saves the item slots, the tanks, the upgrades and `lastBatch` with `ValueOutput` (`ItemSlots.save`, the fluid handler's `serialize`). `onLoad` wakes the Sink when the buffer holds something. The menu syncs through `ContainerData`, because only the controller's GUI needs these values:
- the chimney height and the three rates;
- per tank, the fluid id and the amount as two 16 bit halves, as in `FluidGantryMenu`, and the tank capacity the same way;
- the open count;
- the ticks until the next batch;
- whether an upgrade may come out, which the client's upgrade slot asks.

The refusal time is not synced: as in `FluidGantryMenu`, the upgrade slot records it in the menu on each side when `mayPickup` refuses, and the client's own click runs that check. The item slots sync as normal menu slots. The client menu builds its own handlers from the slots per row and upgrade slot count that the server writes when it opens the menu, because the client never loads `sink.json`.

### Menu and screen

`SinkMenu` lays out 4 rows of 7 `ResourceHandlerSlot`s at x 8, y 18, plus 3 upgrade slots on the side tab and the player inventory at x 8. Closed slots return false from `mayPlace` and `isActive`, so they are neither drawn nor hoverable. Shift click moves between buffer and inventory; Capacity Upgrades from the inventory go only to the upgrade slots, never into the buffer. The next batch marks come from `SinkBatch.plan` over the client's slot counts and the item rate.

`SinkScreen` draws in this order:
1. the background texture, with the closed gate drawn over closed slots and tanks;
2. the tanks with `FluidGauge` laid sideways (a horizontal gauge 30 by 18), or a small horizontal variant of it;
3. the ember glow over marked slots, the batch bar, the chimney pips and a two frame fan icon while the Sink is active.

### Texture

`tools/sink-textures/gen.py` writes `foundation/src/main/resources/assets/servus_foundation/textures/gui/sink.png`, which holds:
- the soot and brick panel at 176 wide and the side tab;
- the slot and tank wells and the gate tile;
- the ember overlay, the bar, the pips and the fan frames.

The script is the source of the PNG, as for the duct and gantry textures. The mockup `temp/sink-mockup.html` is the visual reference.

### Pure Java versus Minecraft

- Pure, JUnit: `SinkBatch`, `SinkCapacity`, `SinkRates`.
- GameTest: the buffer, the batches, the upgrades, extraction, buckets, breaking and reload, by reworking `SinkGameTests`.
- Manual play check: the GUI, the gate, the ember glow, the bar and the fan icon.

## Risks / Trade-offs

- [A sender inserts between batches faster than the rate] → It fills the buffer and is then refused. That is the intended back pressure.
- [Shrinking `upgrade_slots` or `slots_per_row` in a datapack while Sinks hold items] → Indices beyond the new size are lost when loaded into the smaller handler. Accepted: it is a Sink, and datapack changes of this kind happen rarely.
- [`FluidGauge` is vertical only] → Add a horizontal mode, or draw the sideways tank in `SinkScreen` with `FluidSprites`. Settled during apply without changing the spec.
- [Players lose items they put in by mistake] → They can take them back until their batch runs, and the ember glow shows what burns next.

## Migration Plan

None. The mods are unreleased, so old Sinks load with an empty buffer and no upgrades, and the old `fluid_per_tick` field is dropped.
