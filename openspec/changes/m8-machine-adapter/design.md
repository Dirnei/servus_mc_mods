# Design

## Context

Producers on the data line expose `servus_foundation:production`. The Crafting CPU (Storage) plans with the reported recipes, ships a step's inputs with `WarehouseNetwork.ship(target, ...)` as packets that `PacketIo` inserts into the target's `IItemHandler` on the face touching the duct, and collects outputs with `DataPackets.move(producer, accessPoint, ...)`, which extracts through the producer's `IItemHandler` (`CpuBlockEntity.collect`, `extractable`). Steps on producers other than Crafting Stations need no `start` hook. Foundation must not import Storage. See proposal.md and the delta specs.

Vanilla furnaces expose `SidedInvWrapper` per face (NeoForge `CapabilityHooks`, 21.1.255 sources): `UP` maps to the ingredient slot 0, `DOWN` to the result slot 2 (and fuel slot 1 for empty buckets), every horizontal face to the fuel slot 1, which only accepts fuel (`canPlaceItem`).

## Goals / Non-Goals

**Goals:** a furnace, smoker or blast furnace becomes a CPU producer through an adapter, fuel is refilled from the warehouse without being part of any recipe, and packs can add profiles.

**Non-Goals:** carrot cycling of side roles, adapter patterns, multi ingredient machines (profiles only support recipes with exactly one ingredient), fluids and energy.

## Decisions

### Package and registration
New package `foundation.adapter`: `MachineAdapterBlock` (`DirectionalBlock`-like with `FACING` in six directions, toward the machine: `getStateForPlacement` uses the clicked face's opposite), `MachineAdapterBlockEntity`, `AdapterItemHandler`, `MachineProfile` and `ProfileSides` (pure), `MachineProfiles` (reload listener, `SimpleJsonResourceReloadListener` on directory `machine_profiles`), `AdapterMenu`, `AdapterScreen` (client), `AdapterContent` with its own `DeferredRegister`s (block, item, block entity, menu) registered from `ServusFoundation` like `ServusLogisticsContent`. Capabilities and the reload listener are registered in an `AdapterEvents` subscriber; the item joins the Servus creative tab through `BuildCreativeModeTabContentsEvent`.

### Profiles (pure parsing)
`MachineProfile(List<String> blocks, String recipeType, Map<RelativeSide, Role> sides, String upkeepItem, int upkeepLevel)` with `Role { INGREDIENTS, UPKEEP, OUTPUT, OFF }`, parsed from a Gson `JsonObject` without Minecraft types so JUnit can cover it: `blocks` (array of ids, `#tag` entries allowed and resolved by the manager), `recipe_type`, `sides` object with keys `front`, `back`, `left`, `right`, `top`, `bottom`, optional `upkeep: {item, level}`. Missing sides are off. `ProfileSides.firstOf(role, facing)` returns the absolute side for a role: the first relative side in the order top, bottom, back, left, right, front that has the role, turned into an absolute side with `RelativeSide.toSide(facing)`; pure and unit tested. The machine's facing comes from its `HORIZONTAL_FACING` or `FACING` property, north when it has none.

### Recipes
For the profile's recipe type, the adapter lists every recipe with exactly one non empty ingredient; each item of that ingredient gives one `ProductionRecipe` (id `<recipe id>/<item path>` made unique per provider), one of the item in, the result out, `getCookingTime()` for cooking recipes and 200 otherwise. The list is cached per `RecipeManager` instance (a datapack reload makes a new one). `active()` reports the recipe whose ingredient matches the item in the machine's ingredient slot.

### Item handler
One `AdapterItemHandler` on every face of the adapter, with three virtual slots: 0 the machine's ingredient side (insert only), 1 the machine's output side (extract only, reads the output side handler), 2 the upkeep side (read only view). `insertItem` on any slot routes the stack: while the adapter is expecting upkeep and the stack matches the upkeep item, into the upkeep side (up to the expected amount, the rest to ingredients); otherwise into the ingredient side. The machine's own handlers decide what fits, so a furnace refuses non fuel in its fuel slot.

### Upkeep
The block entity ticks every 40 ticks on the server: count = upkeep items in the machine's upkeep side; when the ghost item is set, the count is below the level and nothing is expected, it asks the suppliers on its data line (members whose block exposes `servus_foundation:supplier`, lowest position first) for `level - count` and remembers the answer as `expected` with a deadline 1200 ticks ahead; arriving upkeep items lower `expected`; past the deadline `expected` resets. Ghost item and level are saved; `expected` is not. Level range 1 to 64, default from the profile when the adapter first meets its machine.

### Supplier capability
`ItemSupplier { int supply(BlockPos target, ItemStack like, int amount); }` as `servus_foundation:supplier` in `ServusFoundationCapabilities`. Storage registers it on the Warehouse Controller and the Access Point: it finds the warehouse network, caps the amount at `available(key)` and answers `ship(target, key, amount)`'s planned count, or 0 on a refusal.

### GUI
`AdapterMenu` with a `GhostSlot` (reused from `foundation.logistics.menu`) over a one slot ghost handler, the player inventory, and menu buttons (`clickMenuButton`) for level minus and plus; machine name, profile state and the count in the machine travel in `ContainerData`. Opening: right click with an empty hand or any non carrot item.

### Storage Machines tab
`ProductionView` checks for `MachineAdapterBlockEntity` and names the row after the faced machine plus "(adapter)", and adds the upkeep item, count and level; `MachineRow` gets an `upkeep` stack plus `upkeepLevel` with codec support; the screen prints "Upkeep: Coal 12 of 16" under the name.

### Look, recipe, datagen
Placeholder textures from a new helper `gen_machine_adapter.py` (copper frame, teal port on the front, hopper funnel), a `directionalBlock` model, loot drop self, pickaxe tag, recipe `C H C / R D R / C C C` (copper ingot, hopper, redstone, Data Cable) giving one, lang. Docs Foundation page gets a section; game data regenerated.

### Testing
- JUnit: `MachineProfileTest` (parse, missing sides off, upkeep default), `ProfileSidesTest` (role to absolute side for each facing).
- Foundation GameTests (`AdapterGameTests`): adapter facing a furnace reports smelting recipes with stone from cobblestone; insertion goes to the ingredient slot; expected upkeep goes to the fuel slot; output is extractable through the adapter; facing a chest reports nothing.
- Storage GameTest: warehouse with cobblestone and coal, Access Point, CPU, adapter on a furnace on the data line; upkeep is refilled to the level and an order for stone finishes.

### PLAN.md
PLAN.md 2.4 already describes this; the supplier capability is added to the section 3 API list.

## Risks / Trade-offs

- [An item that is both upkeep and an ingredient, for example logs as fuel and as charcoal input] -> routed to upkeep only while upkeep is expected; documented.
- [Large recipe lists, every smelting recipe item] -> cached per recipe manager; the Production Terminal already caps rows at 64 recipes.
- [Machines that consume fuel from their own GUI by hand] -> the adapter just refills on the next check.
- [A supplier answer that never arrives (shuttle trouble)] -> deadline resets the request.
