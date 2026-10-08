# Proposal

## Why

PLAN.md M8 item 8, `m8-station-hands`. Patterns (`m8-pattern-storage`) are the knowledge of the autocrafting system and the Crafting CPU does the planning, but a Crafting Station still only crafts the recipes a player configured in its own list. Until stations craft whatever pattern the CPU sends them, patterns change no orders and every station needs the same recipe twice. This change makes Crafting Stations the CPU's hands.

## What Changes

- The Crafting Station loses its configured recipe list, its recipe editor popup, its JEI transfer handler and the saved recipes. No migration (unreleased).
- The CPU assigns a pattern step to a Crafting Station as a job: the pattern and the number of crafts. The station crafts that pattern while its inputs are present, puts results and remainders into its outputs, and returns to idle when the step is done or its order is cancelled.
- Any Crafting Station on the data line can run any crafting pattern of the line; the planner prefers stations that are not busy, so several stations craft in parallel.
- The station reports no fixed recipes through the production capability, only the recipe it is crafting as active.
- Automation inserts into the station only items the current job uses; without a job nothing goes in.
- The station GUI keeps inputs, outputs, progress and side panel and shows the job in the middle: the result and how many crafts are left, or idle.
- The Production Terminal's Machines tab shows a Crafting Station as idle or crafting an item with the crafts left, and says it crafts any pattern.
- The recipe viewer transfer stays only on the Production Terminal; its payload moves from the station to the pattern package.
- `crafting_station.json` drops `recipes`.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/crafting-station`: pattern jobs replace configured recipes; sides, production report and GUI follow; configured recipes and the station's recipe viewer transfer are removed.
- `storage/patterns`: any Crafting Station can execute any pattern.
- `storage/crafting-cpu`: steps from patterns hand the station a job and release it.
- `storage/production-terminal`: Machines tab rows for Crafting Stations.

## Non-goals

- Machine Adapter and foreign machines (`m8-machine-adapter`).
- Upkeep and native Manufacturing machines (`m8-native-machines`).
- Saving jobs or orders across restarts (orders are still in memory).
- Sending leftover inputs back to storage when a job ends.

## Impact

- `storage/crafting`: `CraftingStationBlockEntity`, `CraftingStationBlock`, `OpenStationEditorPayload` (moved and renamed).
- `storage/menu/CraftingStationMenu`, `storage/client/CraftingStationScreen`, `storage/integration/jei`.
- `storage/pattern/PatternRunners`, a new `pattern/CraftingGrid` with the grid matching helpers, `storage/order/OrderBook`, `storage/cpu/CpuBlockEntity`.
- `storage/production/ProductionView`, `ProductionRows`, `client/ProductionTerminalScreen`, `menu/ProductionTerminalMenu`.
- `StorageStatsManager`, `crafting_station.json`, lang, docs Storage page.
- GameTests that configured station recipes.
