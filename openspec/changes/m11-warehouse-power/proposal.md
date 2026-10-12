# Proposal

## Why

During the 2026-10-12 play test the user noted that the warehouse "is all powered out of thin air": shuttles drive, sites wake and Crafting Stations craft for free, while the Chunk Loader and every Manufacturing machine pay FE. `m11-controller-wireless` gives each site one FE buffer in its Warehouse Controller for wireless; the user decided the same day that everything in the warehouse draws from that buffer, so a site needs one power connection and a generator, and a site without power stops its automation in a clear state without locking the stock.

This change belongs to `PLAN.md` M11 (Wireless and scale) as a new item after `m11-controller-wireless`: warehouse power.

## What Changes

- **One buffer per site.** Every warehouse consumer draws FE from its site's Warehouse Controller buffer (introduced by `m11-controller-wireless`), the only power connection of a site. No consumer has a buffer of its own and nothing costs FE while idle.
- **Consumers and costs** (numbers in `storage/power.json`, examples):
  - Wireless: per item and distance, unchanged from `m11-controller-wireless`.
  - Shuttles: `shuttle_fe_per_block` (example 20) for every block a shuttle drives, on rails, farm rails and lift rides, paid when it starts that block.
  - Waking: `wake_fe_per_chunk_per_tick` (example 2) for every chunk of the site while a wake holds it loaded, paid by the woken site.
  - Crafting: `crafting_station_fe_per_tick` (example 5) for every tick a Crafting Station works on a step. CPU steps on Crafting Stations therefore pay per duration; steps on Machine Adapters and Manufacturing machines keep their own fuel and power and draw nothing from the controller. The CPU itself draws nothing.
- **No power state.** A controller that cannot pay a draw, or holds no FE at all, is in a "no power" state: its lamp is red, its GUI and every terminal on the site show a no power line, and automation stops: no new shuttle jobs, shuttles on a trip stop at the next block and wait, Crafting Stations pause with their progress kept, no wake starts, a running wake ends. Everything resumes by itself when FE arrives. Stock stays visible in the terminal and can still be taken by hand from racks and crates.
- **Wake needs power.** A sleeping site wakes only when its controller held enough FE for at least one second awake when it went to sleep; the site record keeps the controller's FE for that.
- **Power readout.** The controller GUI shows the FE used in the last second next to the FE stored, so players can size their generators.
- Pure logic without Minecraft imports and with JUnit tests: the draw and power state accounting of the buffer, the cost of a wake per tick and the FE needed to start one, the shuttle segment gate decision, and the pause and resume decision of a Crafting Station step.
- `PLAN.md`: M11 gets the item; section 2.3 says the warehouse runs on FE through its controller; the M11 item 2 note "cost no FE" is updated.

## Non-goals

- Idle upkeep per block, per rack or per stored item (decided: none).
- Power for Manufacturing machines through the controller (they keep their own fuel and power).
- Priorities between consumers when FE is short, or a reserve per consumer; first come, first served within a tick.
- Shuttle tiers costing different FE per block (one number now; can follow with the balance pass).
- A power line through rails or data cable; the controller is the only connection.
- The Balance lab page for warehouse power is an optional task, not a requirement.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `storage/warehouse-controller`: draws from the buffer, the no power state, lamp and GUI line, FE use readout.
- `storage/shuttles`: FE per block driven, stop and resume without power, no new jobs without power.
- `storage/crafting-station`: FE per working tick, pause and resume without power, status.
- `storage/crafting-cpu`: steps on Crafting Stations wait while the warehouse has no power; other producers are not affected.
- `storage/site-wake`: FE per chunk while awake, no wake without power, the record keeps the controller's FE.
- `storage/storage-terminal`: a no power line, requests wait for power.

## Impact

- New: pure `storage/.../power/PowerBudget.java`, `WakeCost.java`, `StepPower.java`; JUnit tests; `gametest/WarehousePowerGameTests.java`.
- Changed: `storage/power.json` and `StorageStatsManager` (three numbers), `controller/ControllerEnergy.java` (draws, state, use per second), `WarehouseControllerBlockEntity` (lamp, state), `SiteStatusPayload`, `WarehouseControllerScreen`, `network/WarehouseNetwork.java` (power state for the fleet and others), `shuttle/ShuttleFleet.java` (segment gate, ride cost, no new jobs), `crafting/CraftingStationBlockEntity.java`, `site/SiteWakes.java`, `site/WakeAllowance.java`, `site/SiteRecord.java`, `site/SiteRegistry.java`, `terminal/StockSyncPayload.java`, `terminal/TerminalRequests.java`, `client/StorageTerminalScreen.java`, Production Terminal step state, lang.
- Existing GameTests that run shuttles, wakes or Crafting Stations need FE in the controller: a shared GameTest helper fills it.
- Docs page `docs/src/content/docs/mods/storage.md`, `PLAN.md`, optionally the Balance lab.
