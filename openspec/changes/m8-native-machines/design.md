# Design

## Context

`m8-machine-adapter` added `servus_foundation:supplier` (Warehouse Controller and Access Point ship items to a data member as packets) and an adapter that checks its machine's upkeep side every 40 ticks itself, asks the lowest positioned supplier on its line, routes arriving upkeep through `expected` and gives up after 1200 ticks. Data roles are found per duct side through `BlockCapabilityCache`s in `DataNetwork` (`Probe`) and by `DataMembership.isMember` for the duct's arms. Manufacturing burner machines share `BurnerMachineBlockEntity` (fuel slot, `MachineStats` from `data/<ns>/machine/<id>.json` with built in defaults in `MachineDefaults`). Only the Assembler exposes the production capability today. The Production Terminal's `ProductionView.machines` lists producer members and asks `MachineAdapterBlockEntity` directly for upkeep. See proposal.md for why, and the specs for the behaviour.

## Goals / Non-Goals

**Goals:**
- One upkeep mechanism in Foundation that any block of any mod can join through a capability.
- Native burner machines kept fuelled from the warehouse; Burner Smelter and Crusher usable by the CPU.
- The adapter keeps its observable behaviour and its GameTests.

**Non-Goals:**
- Editing upkeep item or level in Manufacturing GUIs (datapack only).
- Extractors as order producers; energy as upkeep.

## Decisions

### Upkeep capability (Foundation `production`)
`UpkeepHolder` with `ItemStack upkeepItem()` (empty for none), `int upkeepLevel()`, `int upkeepCount()` and `default void upkeepRequested(int amount, long until)`. `ServusFoundationCapabilities.UPKEEP` is a sided `BlockCapability<UpkeepHolder, Direction>` named `servus_foundation:upkeep`. A holder with an empty item or level 0 is skipped by the keeper. Alternative: put the counting into the holder (like the adapter's `expected`). Rejected: every holder would repeat it; counting arrivals from `upkeepCount()` works for any slot.

### Upkeep role and arms
`DataRole.UPKEEP`; `DataNetwork.Probe` gets a fifth cache for `UPKEEP`; `DataMembership.isMember` includes it, so the duct's data arm appears through the existing path. The capability is queried with the side facing the duct, like the other roles.

### Bookkeeping (pure, `foundation.data.UpkeepBook`)
Per member key: `asked`, `remaining`, `lastCount`, `until`. `ask(key, count, level, now)` returns the amount to request (0 while a request is open or nothing is missing) after first crediting `count - lastCount` when positive against `remaining` and ending the request when `remaining <= 0` or `now > until`. `asked(key, amount, count, now, timeout)` opens a request. `retain(keys)` forgets members that left. Consumption between checks hides arrivals, which only delays the next request until the timeout; recorded as a trade-off. Keys are generic so the class has no Minecraft import. JUnit: `UpkeepBookTest`.

### Keeper (Foundation `DataNetwork`)
`DataNetwork.onTick` counts ticks; every `UPKEEP_INTERVAL` (40) it walks `dataMembers(UPKEEP)` that are loaded, reads the holder (side null), asks `UpkeepBook`, and for a positive amount asks the network's suppliers (members with `SUPPLIER`, sorted by position, the member itself skipped) until one answers more than 0; then `asked(...)` with `UPKEEP_TIMEOUT` (1200) and `holder.upkeepRequested(answer, until)`. Merges and splits start a fresh book for the new network; at worst one extra request is asked, which the supplier serves only when stock exists. Interval and timeout stay code constants like the adapter's (they are pacing, not balance).

### Machine Adapter
The adapter's own `serverTick` loop and ticker are removed. It exposes `UPKEEP` through an `UpkeepHolder` view: the item and level apply the profile defaults lazily on first read (same rule as before), the count is read from the upkeep side, and `upkeepRequested` sets `expected` and the deadline that `receiveUpkeep` uses for routing (expected is dropped once the deadline passed). The package private `expect(int)` stays for its GameTests and calls `upkeepRequested`.

### Manufacturing upkeep
`MachineStats` gains `String upkeepItem` and `int upkeepLevel` with optional JSON fields `upkeep_item` and `upkeep_level`; the factories for burner, burner pump and generator default to `minecraft:coal` and 16, the others to "" and 0. `BurnerMachineBlockEntity.upkeep()` returns a holder over its fuel slot, or null when the stats name no upkeep; registered for Burner Extractor, Burner Smelter, Burner Pump and Coal Generator in `ServusManufacturingEvents`. A stats reload does not re-register the capability: a machine whose upkeep is switched on or off by a reload changes after it is reloaded or replaced (recorded). Arriving coal enters through the machine's normal side handler into the fuel slot, so the side mode rules apply. The game data export writes both stats, and the docs `gamedata.ts` schema reads them with defaults.

### Smelter and Crusher as producers
The adapter's single ingredient recipe listing moves to Foundation as `production.SingleInputRecipes.list(ServerLevel, RecipeType<?>, ticks)` with a per recipe ticks function and a cache keyed by the `RecipeManager` instance; the adapter uses it with the cooking time, the Burner Smelter with its own work time for `minecraft:smelting`. The Crusher builds its own list from `CrushingRecipe` (`input().ingredient().getItems()`, `input().count()`, `result()`, `time()`). Both report the recipe of their input slot as active. The CPU needs no change: producers come from the producer role.

### Production Terminal
`ProductionView.machines` walks members with the producer or the upkeep role. Upkeep for any member comes from the `UPKEEP` capability (adapter included), so the `MachineAdapterBlockEntity` special case only keeps the "(adapter)" name. A member without the producer role gets the state `upkeep_only` (lang "upkeep only"). `MachineRow` already carries upkeep.

### PLAN.md
M8 is complete with this change; the M8 heading gets a short "done" note matching how the plan describes finished milestones, if it does so; otherwise the list stays.

### NeoForge APIs
`BlockCapability.createSided`, `RegisterCapabilitiesEvent.registerBlockEntity`, `BlockCapabilityCache.create` (all used already in this repo for NeoForge 21.1). `RecipeManager.getAllRecipesFor`, `AbstractCookingRecipe.getCookingTime` as the adapter uses them.

### Testing
- Pure: `UpkeepBookTest` (first ask, open request blocks, arrival completes, timeout reopens, retain forgets, level met asks nothing).
- Foundation GameTests: adapter tests stay green; a holder probe through the capability on an adapter.
- Manufacturing GameTests: each burner machine exposes upkeep for its fuel slot with coal 16; Burner Smelter and Crusher report production recipes and the active one; electric extractor has no upkeep.
- Storage GameTests: Burner Smelter on a data line gets coal from the warehouse; an order for iron ingots runs on a Burner Smelter; Coal Generator is an upkeep member and listed upkeep only in the terminal view; adapter refill still works.

## Risks / Trade-offs

- [The keeper polls members every 40 ticks] -> only known members, a handful per network; no block scanning.
- [Consumption hides arrivals] -> the request ends by timeout at the latest; the next check asks the right amount again.
- [Burner Smelter on Auto sides pushes outputs into neighbours, for example an adjacent Access Point, so they are put away uncounted] -> same limitation the Crafting Station had before `m8-station-job-collect`; placements in tests avoid it; recorded for a follow up.
- [Many smelting recipes per producer] -> same as the adapter; the list is cached per recipe manager.
