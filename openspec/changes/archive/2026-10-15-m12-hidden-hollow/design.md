# Design

## Context

See proposal.md for the why. Today `GnomeHollowBlock` extends Foundation's `MachineBlock` (facing, lit, menu on click, carrot side config and dismantle) and `GnomeHollowBlockEntity` extends `MachineBlockEntity` (two slot inventory, side handlers, pull and push every 8 ticks, `MenuProvider`). `ServusAlchemyEvents.registerGnomeHollow` registers the item, fluid and production capabilities. The client reveal (`HollowTracker`, `HollowRevealRenderer`) reads the yield, ripeness and binding from the update tag and stays as it is. Ducts connect to a neighbour only when it exposes the matching capability (`UtilityDuctBlockEntity`, capability lookup), so removing the registrations is enough to keep pipes away.

## Goals / Non-Goals

**Goals:**
- A Hollow that is indistinguishable from stone without the sight: look, Jade, interaction, pick block, drops.
- All handling by hand under the sight, an internal store with a datapack capacity.

**Non-Goals:**
- Gnome gifts (`m12-gnome-gifts`), the Principle Dropper (`m12-principle-dropper`), the homunculus harvest (`m12-receptaculum`), the knocking and dust hints (`m12-hidden-hints`).
- A new look under the sight; the reveal renderer stays.

## Decisions

### D1 A plain block entity instead of a Foundation machine
`GnomeHollowBlock` becomes a `BaseEntityBlock` without block state properties (no facing, no lit) and `GnomeHollowBlockEntity` a plain `BlockEntity` with its own server ticker. It keeps `PrincipleTanks` (no drain), `VeinGrowth.State`, `TeamOwnership` and the client fields. Not being a `MachineBlock` also drops the menu, the carrot side config and the carrot dismantle, which would all betray the block.
- Alternative: keep `MachineBlockEntity` with no slots and no registered capabilities. Rejected: it still is a `MenuProvider`, carries side config and pushes, and the carrot would cycle sides on "stone".
- Stats still come from `MachineStatsManager` with id `servus_alchemy:gnome_hollow` (`fluid_capacity` for the tanks, `item_capacity` for the store), so the docs machine table and datapack overrides keep working. `TeamOwned` is set on placement in `setPlacedBy`, as `MachineBlock` did.

### D2 The store is a count of the vein's yield
A vein yields one item, so the store is a number, not an inventory. Pure `HollowStore` (count, capacity, `hasRoom`, `add`, `takeAll`) is covered by JUnit `HollowStoreTest`. `VeinGrowth.tick` already takes "output has room", which becomes `store.hasRoom()`. Saved as `Stored`. Taking hands out stacks of the yield's max stack size through `Inventory.placeItemBackInInventory`, which drops what does not fit.

### D3 Interactions gated by the sight on both sides
`useItemOn` and `useWithoutItem` check `ElementalSight.active(player)`. The effect is synced to its own client, so client and server agree and the client predicts the same result. Without the sight `useItemOn` returns `PASS` (the held item acts as on stone: blocks place, buckets pour into the world) and `useWithoutItem` returns `PASS`. Under the sight, in order: Salt Crystal binds, a fluid container pours via `FluidUtil.interactWithFluidHandler(player, hand, pos, tanks, null)` (NeoForge 26.1.2.116, returns the empty bucket), any other item tries to seed; empty hand takes the store or shows the status. Every failure under the sight sends an action bar line (`Player.sendOverlayMessage`, as today).

### D4 Drops from the loot table, the store from `playerDestroy`
The loot table (datagen) is one pool with an alternatives entry: the Gnome Hollow when `entity_properties` of `this` has the effect `servus_alchemy:elemental_sight` (`LootItemEntityPropertyCondition.hasProperties(EntityTarget.THIS, EntityPredicate.Builder.entity().effects(MobEffectsPredicate.Builder.effects().and(EFFECT)))`, verified in the 26.1.2 sources), else Stone with Silk Touch, else Cobblestone, all with the explosion condition. Explosions have no breaker and fall to the stone branch. The store drops in `Block.playerDestroy` when the breaker has the sight; that method runs only for a survival player with the right tool, so creative breaking drops nothing, like vanilla.
- Alternative: drops in code. Rejected: datagen owns loot tables.

### D5 Jade: a ray trace callback swaps the accessor
Verified in `jade-26.1.11+neoforge.jar`: `IWailaClientRegistration.addRayTraceCallback(JadeRayTraceCallback)` and `BlockAccessor.Builder.from(accessor).blockState(state).blockEntity(Supplier)`. The plugin already uses this for the Athanor crown. For a Gnome Hollow and a viewer without the sight (`accessor.getPlayer()`), the callback returns an accessor with `Blocks.STONE.defaultBlockState()` and no block entity, so Jade draws Stone's name, icon, mod name and harvest info and asks the server for nothing. With the sight a server data provider adds yield, ripeness, steps, bound, store, capacity and principles, and a client component draws them.

### D6 Pick block
`IBlockExtension.getCloneItemStack(level, pos, state, includeData, player)` returns Stone without the sight and the Gnome Hollow with it.

### D7 What goes
`GnomeHollowMenu`, `GnomeHollowScreen`, `ServusAlchemyMenus.GNOME_HOLLOW`, the screen registration, the three capability registrations, `production()`, the `gui.servus_alchemy.gnome_hollow.*` lang keys that only the screen used. The Hollow's `"crafter"` row in `AlchemyMachineDefaults` stays for the docs table and gets `item_capacity` 64.

## Risks / Trade-offs

- [A player without the sight breaks a Hollow they built and loses vein and store] → This is the plan ("drops stone, the vein is lost"); the journal text warns to drink before moving it.
- [Jade shows Stone but other tooltip mods (WTHIT, TOP) show the Hollow] → Only Jade is supported; Jade is what ATM11 ships.
- [The sight check on the client lags the server by a tick when the effect ends] → At worst one click shows the wrong swing; the server decides.
- [Existing Hollows in dev worlds lose their facing property and sample and output slots] → Unreleased, no migration.

## Assumptions to confirm with the user

- Store capacity 64 items (`item_capacity` in `data/servus_alchemy/machine/gnome_hollow.json`), the tanks stay 4,000 mB per principle.
- Breaking without the sight loses the store as well as the vein (only stone drops); with the sight the store drops beside the fresh Hollow.
- Explosions drop stone or cobblestone like a player without the sight.
- Creative players keep their sample, crystal and full bucket, like the Salt Crystal today; creative breaking drops nothing.
- Action bar hints under the sight: sample that does not take root (unknown ore or chapter not finished), already seeded, tank full or wrong fluid, not ripe, already bound; an empty hand on an empty store shows the vein status (no vein, ripeness x / y, a gnome at work).
- Without the sight a right click shows no message at all, including the old "You cannot see the spirits of the earth" for a Salt Crystal, because a message would betray the block.
- Seeding takes any item that matches an open vein, in the main hand or off hand, one per click.
