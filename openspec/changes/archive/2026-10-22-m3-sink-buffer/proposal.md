# Proposal: m3-sink-buffer

## Why

The Sink holds nothing, so senders such as hoppers, pipes and the Utility Duct are refused most of the time and keep retrying. It only ever shows one empty slot to automation. Its GUI is a vanilla grey panel with one trash slot and four lines of text. This change belongs to the Sink that lived on from the M2/M3 factory milestones (PLAN.md section 4, "what lived on: pumps, Sink, ..."). PLAN.md lists no open point for it; the request comes from play.

## What Changes

- The Sink gets a buffer: rows of item slots and fluid tanks that hoppers, pipes, the Utility Duct, buckets and the player fill. Senders are refused only when the buffer is full.
- The buffer is voided in batches once every 20 ticks at the chimney rate. An item rate of 4 per second means 4 items every 20 ticks. The first slots are voided first and the first tanks are drained first. The Sink does work only while it holds something or is active, through scheduled block ticks. There is no ticker.
- Fluid rates are given in mB per second (`fluid_per_second` in `sink.json`, example 1,000 at height 0) and voided in the same batches.
- The base Sink has one row of 7 slots and one tank of 8,000 mB. Each Capacity Upgrade (the existing Foundation item) opens one more row and one more tank, up to 3 upgrades. Taking an upgrade out is refused while the row and tank it opened hold anything. Slots per row, tank size and upgrade slot count are in `sink.json`.
- The player can take items back out of the buffer in the GUI. Automation still extracts nothing.
- Breaking a Sink block voids the buffer and drops the Capacity Upgrades.
- The trash slot goes. A bucket used on the Sink fills a tank like any other sender, and is refused when the tanks have no room.
- Energy stays as it is: accepted at once within the FE per tick rate, with a carry over of one second.
- A new GUI in soot and brick at the vanilla width:
  - each row of slots has its tank beside it;
  - locked rows and tanks are drawn as a closed black iron gate;
  - a bar fills toward the next batch, and the slots the next batch will void glow like embers;
  - chimney pips, a fan that spins while the Sink is active, and the three rates;
  - a side tab with the 3 Capacity Upgrade slots.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/sink`: the void rules change from refusing what exceeds the rate to a buffer voided in batches; the trash slot GUI becomes the buffer GUI with upgrades; buckets fill the tanks instead of skipping the rate; the "never holds or gives back" rule now allows the player to take items back by hand; the balance data gains the buffer fields and fluid rates in mB per second.

## Impact

- `foundation/.../machine/sink/`: `SinkBlockEntity`, `SinkHandlers`, `SinkBlock`, `SinkStats`, `SinkRates`, `VoidBudget` (kept only for energy), `SinkGameTests`, plus new pure classes for batch draining and capacity.
- `foundation/.../machine/menu/SinkMenu.java`, `foundation/.../client/SinkScreen.java`, new GUI texture `textures/gui/sink.png` and its generator script under `tools/sink-textures/`.
- `data/servus_foundation/sink/sink.json` and the lang strings in `MachineDataParts`.
- Docs: the Sink entry in `docs/src/content/docs/mods/foundation.md`.
- Senders: no code changes. Hoppers, pipes and the Utility Duct see a normal inventory and a normal tank.

## Non-goals

- A buffer for energy. FE stays immediate.
- Filters, or a Sink that keeps some items and voids the rest.
- Changes to forming, the chimney, the fan, the smoke or the recipe.
- Speed Upgrades for the Sink. The chimney stays the only way to raise the rate.
- A GUI to choose which slot or tank is voided first.
