# Proposal

## Why

Storage knows what the warehouse holds and Manufacturing knows what its machines can make, but neither can talk to the other: content mods never reference each other's classes, and Lib has no shared vocabulary for production or stock. Autocrafting (M7) needs that bridge first. This change implements the M7 item "Lib API for production and stock" from `PLAN.md`: a small Lib API that Manufacturing implements for its Assembler and Storage implements for its warehouse, plus the pure order planner later changes build on.

## What Changes

- **Production capability in Lib.** A block capability `servus_lib:production` whose provider lists the recipes a block can run right now: for each one an id, the exact input stacks with counts, the output stacks, and the time in ticks. It also says which recipe it is working on. Any mod can implement or query it.
- **Stock capabilities in Lib.** Block capabilities `servus_lib:stock_provider` (how many of an item are available, take some) and `servus_lib:stock_sink` (hand a stack in). Any mod can implement or query them.
- **Pure order planner in Lib.** Given a wanted item and amount, the stock on hand and the known recipes, it explodes the bill of materials into production steps, leaves first, reserving stock before producing, reusing surplus from earlier steps, and reporting raw materials that are missing. Plain Java, unit tested.
- **Assembler implements production.** An Assembler exposes its configured recipes through the production capability, with the template stacks as inputs, the recipe result as output and the time it takes.
- **Warehouse implements stock.** A Warehouse Controller exposes its online warehouse through the stock capabilities: counts from the index, extraction and insertion through the warehouse's own rules (racks only by shuttle, zone routing).

## Capabilities

### New Capabilities
- `lib/production-api`: the production and stock block capabilities, their records, and the pure order planner.

### Modified Capabilities
- `manufacturing/assembler`: the Assembler reports its configured recipes through the production capability.
- `storage/warehouse-controller`: the controller exposes the warehouse through the stock capabilities.

## Non-goals

Deferred to later changes:
- Work orders, reservations in the live warehouse, docks, stock rules, the order board, the Crafting Station (the remaining M7 items).
- Production capability on machines other than the Assembler. Smelters, crushers and extractors can follow once an order needs them.
- Tag aware inputs. The Assembler already collapses a tag ingredient to one concrete item; the API carries exact stacks.
- Persisting planner results. The planner is a pure function; callers decide what to keep.

## Impact

- `lib` gains the package `io.github.dirnei.mc.servus.lib.production` with `ProductionProvider`, `ProductionRecipe`, `StockProvider`, `StockSink`, `ServusLibCapabilities` and the pure `OrderPlanner` with JUnit tests.
- `manufacturing`: `AssemblerBlockEntity` gets a production provider; `ServusManufacturingEvents` registers the capability; a GameTest queries it.
- `storage`: `WarehouseControllerBlockEntity` gets stock provider and sink views over its network; `ServusStorageEvents` registers them; a GameTest queries them.
- No datapack changes. No new blocks or items. `PLAN.md` needs no change; section 3 already sketches these names.
