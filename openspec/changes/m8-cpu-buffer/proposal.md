# Proposal

## Why

With one Crafting Station, a chain like oak logs to planks to a crafting table sends the planks to storage and back: the spec's "Same producer" rule puts an intermediate away at once, and a later step whose producer stays busy longer than `handoff_wait_ticks` does the same. The play test of the 26.1.2 port showed that this wastes Packing Station rate and shuttle trips and lets other orders take the planks, while the player wants intermediates to stay with the order. At the same time the fixed 3x3x3 cube makes the Crafting CPU something you place, not something you design.

## What Changes

This change implements a follow-up to PLAN.md milestone **M8 On-demand autocrafting**, item **Crafting CPU** (section 2.4). It is decided in the memory note "cpu-buffer-decisions" (2026-10-12).

- The Crafting CPU is no longer a fixed 3x3x3 cube. It is a solid box with no gaps, every side at least 2 blocks (smallest 2x2x2), built from Crafting CPU blocks and a new **CPU Buffer** block. Each CPU block adds order slots (`orders_per_cpu_block`, example 1) and each Buffer block adds buffer room (`stacks_per_buffer_block`, example 1 stack), both from `data/<ns>/storage/cpu.json`. These replace the fixed `order_slots`. A box needs at least one CPU block.
- Forming stays a Configuration Carrot click. A box that is not right does not form and the action bar says why: a gap (with its position), not a box, too small, no CPU block, or too big.
- Intermediates of an order wait in the CPU's buffer instead of storage and go back to the producer of the later step as packets when that step runs. Only when the buffer is full do they go to storage as today. The direct handoff to a different producer that is delivering stays.
- Breaking a block of a formed CPU still unforms it and cancels its orders; whatever waits in its buffer is sent as packets to a Packing Station of the line, whose warehouse puts it away by shuttle (what the station cannot take drops beside it), or dropped at the broken block when no Packing Station is on the line, so nothing is lost.
- The buffer and its contents are saved with the CPU and survive reloads.
- The right click summary of a CPU block adds the buffer fill.
- The Production Terminal shows, per job, what waits in the CPU buffer under the step that will use it and a note when a full buffer sent items to storage, and gets a new CPUs tab with one row per CPU: its size, order slots in use and buffer fill as bars, and what the buffer holds. No own CPU GUI.
- `PLAN.md` section 2.4 and the docs site describe the box CPU and the buffer instead of the 3x3x3 cube.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `storage/crafting-cpu`: forming as a box of CPU and Buffer blocks with reasons, unforming without losing buffered items, order slots from CPU blocks, the buffer for intermediates (including same producer and busy producer), buffer saved across reloads, formed box look for any size.
- `storage/production-terminal`: a CPUs tab in the tab bar and screen layout, the Crafting tab shows buffered intermediates per step and the overflow note.

## Non-goals

- Balance of CPU and Buffer block values and recipes (a 2x2x2 of CPU blocks runs 8 orders where a 3x3x3 cube ran 2 before); left to the open M9 balance pass. The CPU block recipe may have to cost more.
- An own GUI for the CPU, buffer upgrade items, automatic forming, free shapes and flat CPUs (rejected in the decision note).
- Buffering raw inputs from storage ahead of time; the buffer only holds what the order's own steps made.
- Moving orders or buffered items between CPUs.
- Fluids in the buffer.

## Impact

- Code: `storage/cpu/*` (forming, box geometry, new `CpuBufferBlock`, buffer state on the master, the CPU's `ServusFoundationCapabilities.PACKET_INLET`, summary), `storage/order/*` (handoff split with the buffer, feeding from the buffer, saving), `storage/stats/StorageStatsManager` (new `cpu.json` keys; moved there by `m11-drop-crates-pipe-access`), `storage/production/*` and `storage/client/ProductionTerminalScreen` plus `ClientProduction` (payload rows, CPUs tab), `storage/terminal/TerminalLayout` (four tabs), registries, `ServusStorageEvents` (capability registration), datagen (blockstates, models, loot, recipe, lang, tags), textures for the Buffer block.
- Data: `data/servus_storage/storage/cpu.json` drops `order_slots` and gains `orders_per_cpu_block`, `stacks_per_buffer_block` and `max_blocks`.
- Tests: new and changed JUnit tests in `storage/src/test/.../cpu` and `.../order`, changed and new GameTests in `CraftingCpuGameTests`, `ChainHandoffGameTests` and the other GameTests that build a 3x3x3 cube.
- Specs: overlaps the unarchived `m0-port-minecraft-26-1-2` delta of `storage/crafting-cpu` ("Order slots"); this change builds on its wording and archives after it. It also archives after `m11-drop-crates-pipe-access`, which brings the packet inlet and removes the instant put-away (design.md D13).
- Docs: `PLAN.md` section 2.4, `docs/src/content/docs/mods/storage.md`.
