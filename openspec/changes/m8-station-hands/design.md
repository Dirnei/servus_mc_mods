# Design

## Context

`CraftingStationBlockEntity` holds a list of `StationRecipe`s, a ghost pattern grid for its editor and crafts round robin between them. Its production capability reports those recipes. `PatternRunners.canRun` lets the CPU use a pattern only on a station whose own list holds the same recipe. The CPU's `OrderBook` drives steps through `OrderBook.Host` (`feed`, `collect`, `busyElsewhere`), implemented by `CpuBlockEntity`. The Production Terminal and the station share `OpenStationEditorPayload` for the JEI transfer, and `CraftingStationBlockEntity.match/templateOf` serve the terminal editor too. See proposal.md for why.

## Goals / Non-Goals

**Goals:**
- Any Crafting Station on a data line crafts any pattern the CPU hands it, one job at a time.
- No configured recipes left on the station, no duplicated editor.
- Several idle stations take steps in parallel.

**Non-Goals:**
- Persisting jobs; returning leftover inputs to storage.

## Decisions

### Job on the station
`CraftingStationBlockEntity` gets a transient job: the `EncodedPattern`, crafts left and an owner object. `assign(pattern, crafts, owner)` succeeds when the station has no job, the same owner, or a job with no crafts left; `release(owner)` clears the job only for its owner, so a late release from a cancelled step cannot clear a newer job. The owner is the `OrderStep` object. The job is not saved, like the CPU's in-memory orders; after a restart stations are idle. Leftover inputs stay in the station (the player can take them; a later job can use them). Alternative considered: send leftovers back to an Access Point. Rejected for now: it needs a delivery per item kind and leftovers are rare because steps ship exact amounts.

Crafting reuses the existing plan and finish code with the job's template and recipe id instead of a configured recipe; each finished craft counts one off. Automation insertion (`SideHandler`, `pullFrom`, shift click in the menu) accepts only items the job's pattern uses while crafts are left. The production capability reports an empty recipe list and the job's recipe as active while crafts are left. The update tag carries the job result and crafts left for the GUI; the block sends an update when they change.

### CPU hands out jobs
`OrderBook.Host` gains `boolean start(OrderStep step)` and `void release(OrderStep step)`. A step leaving WAITING_STATION calls `start` first; `false` keeps it waiting with the reason "station busy". `release` is called when a step is done and for every step of a cancelled order (cancel, cancelAll). `CpuBlockEntity` implements them: for a Crafting Station producer it finds the pattern of the line with the step's recipe id and the same inputs (`PatternLibrary.entries`) and calls `assign`/`release`; other producers return true and do nothing. A missing pattern (removed meanwhile) returns false with the reason "pattern missing".

`PatternRunners.canRun` becomes "the producer is a Crafting Station". So that two orders spread over two stations, `OrderBook.recipes` orders producers by whether an open order of this CPU has an unfinished step planned on them or a step of another CPU runs there, then by whether they report an active recipe. Planned steps count, not only running ones, because two orders are often made in the same tick.

### GUI
The station menu keeps inputs, outputs, progress, work ticks and the side panel; the ghost grid, editor state and recipe cell buttons are removed. The screen draws the job area in the middle: an 18 by 18 frame with the job result and "N left", or "Idle". The texture keeps its size; the area formerly used by cells is drawn over.

### Machines tab
`ProductionRows.MachineRow` gains `left` (crafts left). `ProductionView` builds rows for Crafting Stations with state `station_crafting` (working item and crafts left) or `station_idle` and no recipe rows; the screen prints "crafting X, N left" or "idle", then "crafts any pattern".

### Shared editor helpers
`match` and `templateOf` move to a new `pattern/CraftingGrid`. `OpenStationEditorPayload` becomes `pattern/OpenRecipeEditorPayload`, handled only by the Production Terminal menu; the station's JEI transfer handler is deleted.

### Data and lang
`crafting_station.json` keeps `work_ticks` only; `StorageStatsManager` drops `craftingStationRecipes`. Editor lang keys of the station are removed; `gui.servus_storage.crafting_station.idle`, `.left` and the Machines tab strings are added.

### Testing
GameTests (Storage): a new `StationHandsGameTests` with a pattern order crafted on a station without configuration, two stations crafting two orders in parallel, no job refuses hopper input, cancel returns the station to idle and keeps its inputs, a second job is refused. Existing tests that configured station recipes switch to a sticks pattern in a Pattern Storage (CPU tests) or to a sticks job set directly (packet and data line tests that need the station to accept planks); tests of the removed editor are deleted. No new pure logic, so no new JUnit tests.

### Found during apply
A station whose Auto side touches an Access Point pushes its outputs straight into it; they are put away but never counted for the step, so the order stays running. This was true before this change as well. Tests keep stations away from Access Points; the play check covers it and a later change may stop stations from pushing outputs while they have a job.

### PLAN.md
Item 8 is implemented as written. PLAN.md section 2.4 already says stations craft what the CPU assigns; no change needed.

## Risks / Trade-offs

- [A station whose CPU vanished without release keeps a finished job] -> assign accepts a new owner once the old job has no crafts left; a job with crafts left from a vanished CPU blocks the station until restart. Cancel paths release explicitly, so this needs a crash or a forced unload.
- [Leftover inputs block slots] -> rare, visible in the GUI, the player can take them.
- [Players lose configured recipes in dev worlds] -> unreleased, accepted.
