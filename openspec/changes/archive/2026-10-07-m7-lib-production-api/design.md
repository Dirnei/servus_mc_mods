# Design

## Context

See proposal.md for motivation. Lib has no cross-mod discovery today: the only shared extension points are `Wrenchable`, the creative tab, `Teams.install`, `NetworkTypes` and the goggles helper. Content mods reach other mods' inventories only through NeoForge's `Capabilities.ItemHandler.BLOCK` with a `BlockCapabilityCache`. The Assembler keeps `List<ConfiguredRecipe>` where each has `id()`, `template()` (concrete stacks with counts), `assembling(level)` and `displayResult(level)`, and `activeIndex()`. The warehouse network has `index().count(key)`, `extract(key, amount)` and `insert(stack)`. The repo has no `BlockCapability.create` call yet.

## Goals / Non-Goals

**Goals:**
- One small Lib package that both content mods can implement or query without knowing each other.
- A planner that is pure and generic over the item key so it is tested with strings and used with Storage's `ItemKey`.

**Non-Goals:**
- Any live reservation or job creation; that is `m7-work-orders`.

## Decisions

### Settles no PLAN.md open decision
Section 3's API sketch names these types; section 5 has no related bullet.

### Block capabilities, sided, in Lib
Package `io.github.dirnei.mc.servus.lib.production`:
- `record ProductionRecipe(ResourceLocation id, List<ItemStack> inputs, List<ItemStack> outputs, int ticks)`.
- `interface ProductionProvider { List<ProductionRecipe> recipes(); Optional<ResourceLocation> active(); }`.
- `interface StockProvider { long count(ItemStack like); ItemStack extract(ItemStack like, int amount); }`.
- `interface StockSink { ItemStack insert(ItemStack stack); }`.
- `ServusLibCapabilities` with `BlockCapability.createSided(ResourceLocation, Class)` for `PRODUCTION`, `STOCK_PROVIDER` and `STOCK_SINK`. `BlockCapability.createSided` is the NeoForge 1.21.1 factory for a `BlockCapability<T, @Nullable Direction>`, the same shape as `Capabilities.ItemHandler.BLOCK`; providers register with `RegisterCapabilitiesEvent.registerBlockEntity` exactly as the mods do for item handlers today. Lib owns the capability objects so both content mods compile against Lib only. Alternative rejected: a static service registry in Lib, which would need manual lifecycle handling that capabilities already provide.

### Pure planner
`OrderPlanner` in the same package, no Minecraft imports:
- `record Recipe<K>(Object id, Map<K, Integer> inputs, Map<K, Integer> outputs, int ticks)`.
- `record Step<K>(Recipe<K> recipe, int batches)`.
- `record Plan<K>(List<Step<K>> steps, Map<K, Integer> reserved, Map<K, Integer> missing)` with `complete()` true when nothing is missing.
- `static <K> Plan<K> plan(K target, int amount, Map<K, Long> stock, List<Recipe<K>> recipes, int maxDepth)`.
The algorithm is a post-order walk: take from available stock (stock minus already reserved plus surplus) first; for the rest pick the first recipe in list order whose outputs contain the key; batches is the ceiling of rest over output count; recurse into every input before adding or growing the recipe's step; surplus outputs go into a surplus map for later needs; past `maxDepth` the rest is missing. Steps are kept in a `LinkedHashMap` keyed by recipe so repeated needs add batches to the existing step, which already sits before every dependent step. `OrderPlannerTest` covers the six spec scenarios plus a recipe with two outputs.

### Assembler provider
`AssemblerBlockEntity` gets `ProductionProvider production()` returning a view: `recipes()` maps each `ConfiguredRecipe` to a `ProductionRecipe` with inputs merged by item and components (`ItemStack.isSameItemSameComponents`), output `displayResult(level)` and ticks from `assembling(level).map(AssemblingRecipe::time)` or `stats().workTicks()`; `active()` is the id at `activeIndex()`. `ServusManufacturingEvents.ModBus.onRegisterCapabilities` registers it for `ASSEMBLER` on every side.

### Warehouse provider and sink
`WarehouseControllerBlockEntity` gets `StockProvider stock()` and `StockSink sink()` views that use `network()` filtered to `ONLINE`: count from `index().count(ItemKey.of(like))`, extract through `extract`, insert through `insert`; offline they answer zero and return the stack. `ServusStorageEvents.ModBus.onRegisterCapabilities` registers both for `WAREHOUSE_CONTROLLER`.

### Minecraft side verification
Manufacturing GameTest `assemblerReportsItsRecipes`: assembler with the iron gear recipe, query `level.getCapability(ServusLibCapabilities.PRODUCTION, pos, null)`, check inputs, output and ticks, then no recipes after `removeRecipe(0)`. Storage GameTest `controllerExposesStock`: crate with 80 cobblestone and an empty crate; provider count 80, extract 20 leaves 60, sink insert of 20 bread lands in the empty crate.

## Risks / Trade-offs

- [Capability objects must exist before registration] → they are static finals in Lib, which loads before every content mod (`ordering="AFTER"` in each mods.toml).
- [Planner picks the first matching recipe] → callers order the recipe list; `m7-work-orders` will put recipes of idle producers first.
- [Exact stacks hide tag flexibility] → accepted, it matches what the Assembler actually consumes.

## Open Questions

None.
