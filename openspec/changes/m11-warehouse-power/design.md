# Design

## Context

Archive order: this change is archived after `m0-port-minecraft-26-1-2`, `m11-controller-site`, `m11-chunk-loader-upgrade`, `m11-wireless-packing-station`, `m8-cpu-buffer` and `m11-controller-wireless`, in that order. It modifies `Over capacity is an error` (added by `m11-controller-site`) and `Controller shows the wake` (added by `m11-chunk-loader-upgrade`), adds requirements to `storage/site-wake` (created by `m11-chunk-loader-upgrade`) and to `storage/crafting-cpu` (rewritten by `m8-cpu-buffer`, so the new requirement uses that change's wording: buffered intermediates), and builds on the controller FE buffer of `m11-controller-wireless`. Until those are archived, `openspec validate` reports that the archive would refuse the modified headers, which is expected.

Code this change works with (main, plus `m11-controller-wireless` once applied):
- `ControllerEnergy extends SimpleEnergyHandler` (`net.neoforged.neoforge.transfer.energy`) on the controller, exposed as `Capabilities.Energy.BLOCK` in `ServusStorageEvents`, max extract 0, fed by the Utility Duct's `EnergyChannelNetwork` through a `BlockCapabilityCache` (`foundation/.../logistics/network/EnergyChannelNetwork.java`). This is the same path the Foundation Chunk Loader uses: `PoweredMachineBlockEntity` holds a `MachineEnergyStorage extends SimpleEnergyHandler` registered in `MachineEvents` (line 62) and spends `energy_per_tick` (10, `servus_foundation/machine/chunk_loader.json`) with `consume(int)` each working tick. No new Foundation or NeoForge API is needed; the controller keeps owning its buffer and spends from it directly.
- `ShuttleFleet.tick()` moves shuttles with `ShuttleMotion.advance(blocks, mayStart)`: the gate `mayStart(segment)` is asked before each one block segment starts and a refusal leaves the shuttle on the segment's first node with `held()` true (used today for the lift car). Lift rides go through `rideOver` and `handover`.
- `CraftingStationBlockEntity.tick()` increments `progress` up to `workTicks()` while `working()`.
- `SiteWakes.step` runs `WakeMachine.tick(wanted, busy, allowance, grace)` per wake each server tick; `WakeAllowance.check` gives the reason a wake may not load.
- `TerminalWarehouses.reachable(level, pos)` lists the online warehouses of a data line, lowest controller position first.

## Goals / Non-Goals

**Goals:**
- One buffer, one draw method, one power state per site; every consumer asks the controller.
- Pause and resume everywhere; nothing is cancelled, dropped or locked for lack of power.
- All costs in `storage/power.json`.

**Non-Goals:**
- Priorities or reserves between consumers.
- Changing how Manufacturing machines or Foundation machines are powered.

## Decisions

### Draws and the power state (pure)
`storage/.../power/PowerBudget.java`, no Minecraft imports: `draw(stored, amount)` returns whether a draw is paid (all or nothing); `observe(tick, paid)` remembers the tick of the last refused draw; `state(tick, stored)` gives `POWERED` or `NO_POWER` (`stored == 0` or a refusal within the last 20 ticks; the window is a UI hold so the lamp does not flicker, a code constant, not a balance number); a ring of 20 per tick sums gives `usedLastSecond()`. `ControllerEnergy` wraps it: `draw(long amount)` spends through the handler and records use or refusal, `noPower()` for everyone else. A state flip refreshes the controller lamp and marks the site status payload dirty (event driven, no scan).

Consumers draw when they act, in the order the server ticks them: no priority. Rejected: a per tick budget shared out by priority (more state, and the user decided no priorities for now), reserving FE for wireless (a player who wants transfers to keep going adds power).

Wireless keeps its own rule from `m11-controller-wireless` (capable while the buffer holds one item at the smallest cost); its spend now goes through `draw` so it counts in the use readout and can set the no power state.

### Shuttles: pay per block at the segment gate
`ShuttleFleet` gets the power gate in the existing `mayStart` predicate: before a horizontal segment starts, `controller.draw(shuttle_fe_per_block)`; refused means the gate refuses, so the shuttle stands on the node where the block starts with its cargo and jobs, exactly like a shuttle waiting for the lift car, and retries next tick. A ride is paid as a whole (height times `shuttle_fe_per_block`) in `handover` before the car takes the shuttle; refused, the shuttle waits beside the shaft. A paid ride finishes. The lift car's empty trips to answer a call cost nothing (the car is part of the shaft; only shuttles pay). Field runs drive the same motion, so they pay the same. `assignJobs()` and field run starts are skipped while the controller is in the no power state, so parked shuttles stay in their garages. The client draws a held shuttle standing, as today.

Pure helper `power/StepPower.java`: `segmentCost(boolean vertical, int height, double perBlock)` and `mayStart(boolean paid)`; tested without Minecraft. Rejected: paying per tick of driving (speed upgrades would make trips cheaper per block for no reason; per block is what the user decided), or stopping mid block (shuttles would hang between nodes and the lift and crossing logic assumes nodes).

### Crafting Stations: pay per working tick
`CraftingStationBlockEntity.tick()` asks before `progress++`: the controller of `TerminalWarehouses.reachable(level, worldPosition)` first entry pays `crafting_station_fe_per_tick`; refused, progress stays and a `waitingForPower` flag drives the GUI status line and the front (working face off, online lamp stays lit). The lookup is cached per second like the station's lamp refresh, not done per tick over the data line. A station on a line without an online warehouse cannot pay and does not craft; a CPU needs a warehouse to ship inputs anyway. `StepPower.advance(progress, paid)` keeps the pause and resume decision pure.

CPU: no draw. `CpuOrders` asks the station's `waitingForPower` when it builds the step text for the Production Terminal ("waiting for power"); nothing is cancelled or replanned, so `m8-cpu-buffer`'s buffer and reservations stay as they are. Machine Adapters and Manufacturing producers are never asked.

### Waking: pay per chunk per tick, start only with one second in hand
`power/WakeCost.java` (pure): `perTick(chunks, perChunk)` and `toStart(chunks, perChunk)` = one second of it. `SiteWakes.step` charges the woken controller each tick the wake holds tickets (`WakeMachine` states awake and grace); a refused tick, or the controller entering the no power state for another consumer, releases at once with the new `WakeAllowance.Reason.NO_POWER` (a site that cannot drive its shuttles should not keep paying to stay loaded). `WakeAllowance.check` gains the stored FE and the FE to start: from the loaded controller while it is loaded, else from the site record. `SiteRecord` gains `energy` (saved as `Energy`), written by `SiteRegistry.update` when the controller unloads (`onChunkUnloaded`) and with the stock snapshot every `site_snapshot_interval` while online. A sleeping controller's FE does not change, because unloaded block entities neither tick nor are reached by duct capability caches, so the recorded value is the real one.

A site loaded by a player or a Chunk Loader without a wake pays nothing for being loaded; it pays only for what runs. Holds stay registered when a wake ends for no power: the transfer shows waiting for power and wakes again only once the record allows it (after the site was visited and powered).

### Terminal and controller GUI
`StockSyncPayload` gains `noPower` (any reached warehouse in the no power state); `StorageTerminalScreen` draws the alert line. `TerminalRequests` adds the row state `NO_POWER` (exists since `m11-wireless-packing-station`) for a request whose shuttle jobs wait while its warehouse has no power. `SiteStatusPayload` gains `usedLastSecond` and `noPower`; `WarehouseControllerScreen` draws the use next to the FE line and the red no power line; the summary tooltip adds it. Lang keys for all lines, no em dashes, no "team".

### Lamp
`WarehouseControllerBlockEntity.lampActivity()`: `BLOCKED` (red) when over capacity or no power, `WORKING` (orange) while awake, `IDLE` (green) otherwise. The GUI says which red applies. The modified requirement also drops the line "its GUI SHALL say the site cannot wake without a Chunk Loader Upgrade", which the play test removed from the GUI (commit `8033edbe`); the delta records the code as it is.

### Numbers
`storage/power.json` (created by `m11-controller-wireless`) gains `shuttle_fe_per_block` (example 20), `wake_fe_per_chunk_per_tick` (example 2) and `crafting_station_fe_per_tick` (example 5), read by `StorageStatsManager` with defaults. For scale: a Coal Generator makes 40 FE per tick (`coal_generator.json`), a Chunk Loader spends 10 FE per tick for one chunk; a Mk1 shuttle at 0.15 blocks per tick spends 3 FE per tick while driving, a 9 chunk wake 18 FE per tick, a working Crafting Station 5 FE per tick. One generator runs a small warehouse; a busy one with many shuttles needs several.

### Balance risk and the Balance lab
Risk: the numbers are guesses; too high and an early warehouse stalls before the player has power (the first shuttle setup should stay affordable, see the crate decision), too low and power is decoration. GameTests pin the mechanics, not the numbers. Optional docs task: export `storage/power.json` and the Coal Generator's output into `docs/src/data/gamedata.json` through the Manufacturing data run (`GameDataExportProvider`) and add a Balance lab page "Warehouse power": inputs for shuttles and blocks per trip, trips per minute, Crafting Stations working, wake chunks and seconds; output FE per tick and the number of Coal Generators, with the numbers from the export.

### PLAN.md
M11 gets the item `m11-warehouse-power` after `m11-controller-wireless`; section 2.3 says the warehouse runs on FE through its controller (one power connection per site, no idle upkeep); the M11 item 2 note "cost no FE" becomes "cost FE per chunk while awake since m11-warehouse-power". No section 5 open decision is settled.

### Pure logic and tests
Pure, JUnit: `PowerBudget`, `WakeCost`, `StepPower`, `WakeAllowance` (new reason). Minecraft side: `WarehousePowerGameTests` and the existing GameTests (a shared helper `PowerFixtures.fill(controller)` gives every existing test that runs shuttles, wakes or Crafting Stations a full buffer, so they keep testing what they tested).

## Risks / Trade-offs

- [Existing GameTests start without FE] Every test that moves a shuttle or crafts would stall. Mitigation: the shared helper fills the controller in the test setup; a grep for `ShuttleGarage` and `CraftingStation` placements in `gametest/` lists them.
- [No priority between consumers] Shuttles can starve a Crafting Station when FE is short. Accepted for now; the no power state and the use readout show the player that more power is needed.
- [A woken outpost runs dry] Its wake ends once the wake's own cost cannot be paid (a refused shuttle draw alone never ends it); its shuttles freeze mid trip until the next visit or wake. Mitigation: nothing is lost, everything resumes; the terminal shows waiting for power.
- [Recorded FE of a sleeping site] Correct only because sleeping controllers neither gain nor spend FE; a mod that pushes FE into unloaded block entities would break it. Mitigation: while loaded the live value is used; at worst a wake starts and ends at once for no power.
- [Balance] See the Balance lab section.

## Answered Questions

Answered by the user on 2026-10-12:
- The example costs (shuttle 20 FE per block, wake 2 FE per chunk per tick, Crafting Station 5 FE per working tick, buffer 100,000 FE) are fine as starting values.
- The no power state is a warning only: each consumer stops only on its own refused draw, so a failed shuttle draw never ends a running wake.
- `PLAN.md` gets both an M11 item and a sentence in section 2.3 that the warehouse runs on FE through its controller.

## Open Questions

- Final numbers in `storage/power.json` are tuned in the play check and the M3 balance pass; they do not change the specs.
