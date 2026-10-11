# Design

## Context

Current state, read from the code on 2026-10-11:

- `ShuttleGarageBlockEntity` keeps shuttles as `Shuttle` items in an `ItemStackHandler`, one per slot, and calls `WarehouseNetwork.garageChanged`, which calls `ShuttleFleet.adoptGarage(pos, count, active)`. The fleet only adds or removes anonymous `Shuttle` objects until the count matches.
- `ShuttleFleet.Shuttle` holds one `ShuttleJob job` and one `ItemStack cargo`. `assignJobs` polls the queue in order and gives each job to the free shuttle with the shortest path. `canReach` and `hasShuttles` look at any shuttle of a homed garage.
- `ShuttleGarageMenu.locked(slot)` locks the first `out` filled slots, because the fleet does not know which slot left.
- The Shuttle item is a plain `Item` stacking to 16. Storage registers data components in `ServusStorageComponents`.
- Requests are split by `RequestPlan` into `PickJob`s of up to a stack, all queued at once. Put-aways are queued one buffer slot at a time, at most `put_away_per_tick` per station per tick.

## Goals / Non-Goals

**Goals:**
- Shuttle identity in the fleet, one shuttle per garage slot, carrying a profile derived from its modules.
- A module concept with slot kinds and a field only flag that `m10-harvester` can use without touching the matching code.
- Cargo Hold batching that needs no change to the leg state machine.

**Non-Goals:**
- Field jobs, the Harvester, multi stop routes (see the proposal).

## Decisions

### D1. Pure module model (no Minecraft imports)

Package `io.github.dirnei.mc.servus.storage.shuttle.module`:

- `ModuleSlot` enum `TOOL`, `UTILITY`.
- `ModuleSpec(String id, ModuleSlot slot, boolean fieldOnly, boolean cargoHold)`: what a module is. Structural traits stay in code; the only balance number, the stack count, lives in `warehouse.json`.
- `ShuttleProfile(Set<String> modules, boolean fieldOnly, int stacks)` with `ShuttleProfile.of(Collection<ModuleSpec> fitted, int cargoHoldStacks)` (stacks is `max(1, cargoHoldStacks)` with a Cargo Hold, else 1; field only when any module is) and `PLAIN`.
- `JobNeeds(Set<String> modules, boolean field)` with `WAREHOUSE` (no modules, not field) and `fits(ShuttleProfile)`: the profile has every needed module, and a non field job refuses a field only profile.
- `CargoBatch.take(List<T> queue, Predicate<T> sameWay, int room)`: indexes of the first `room` queued entries that share the way, in queue order.

Alternatives: a capability or tag lookup per module at match time. Rejected: the fleet runs every tick and must not touch items; a resolved profile per shuttle is cheap and testable.

### D2. Modules as a data component

`ShuttleModules(Optional<Item> tool, Optional<Item> utility)`, a record in `shuttle.module`, registered as `ServusStorageComponents.SHUTTLE_MODULES` (`shuttle_modules`) with a `RecordCodecBuilder` codec of two `optionalFieldOf` fields over `BuiltInRegistries.ITEM.byNameCodec()` and a stream codec of two `ByteBufCodecs.optional(ByteBufCodecs.registry(Registries.ITEM))`. `Item` fields compare by identity, so the record's `equals` and `hashCode` are correct for component comparison (unlike `ItemStack` fields). Writing an empty set removes the component, so a plain shuttle is again equal to a new one and stacks.

Module items are `ShuttleModuleItem extends Item` holding a `ModuleSpec`. The Cargo Hold is `ServusStorageItems.CARGO_HOLD` (`cargo_hold`, stacks to 16) with `new ModuleSpec("cargo_hold", UTILITY, false, true)`. The Shuttle becomes `ShuttleItem` with `appendHoverText(ItemStack, Item.TooltipContext, List<Component>, TooltipFlag)` (signature checked in `net/minecraft/world/item/Item.java` of the 21.1.255 sources) listing the modules. `ShuttleModules.profile(ItemStack, int cargoHoldStacks)` resolves the specs of the fitted module items and calls `ShuttleProfile.of`.

### D3. Workshop without a block entity

`ShuttleWorkshopBlock` works like the vanilla crafting table: `useWithoutItem` opens a `SimpleMenuProvider` from `getMenuProvider` (as `CraftingTableBlock` does), and the menu keeps a `SimpleContainer(1)` for the shuttle that `removed` hands back with `clearContainer(player, container)` through the `ContainerLevelAccess`, like `CraftingMenu.removed`. `stillValid` uses `AbstractContainerMenu.stillValid(access, player, block)`.

The two module slots are a view: `ModuleContainer implements Container` with two slots whose `getItem` returns a one item stack of the module in the shuttle's component and whose `setItem` / `removeItem` rewrite the component of the stack in the shuttle slot. A `ModuleSlotView extends Slot` allows placing only a `ShuttleModuleItem` of its slot kind while a shuttle is present, with a max stack size of 1. The shuttle slot's max stack size is 1, so `moveItemStackTo` (which caps at `slot.getMaxStackSize(stack)`, checked in `AbstractContainerMenu.java`) moves one shuttle. The menu type is a plain `new MenuType<>(ShuttleWorkshopMenu::new, FeatureFlags.DEFAULT_FLAGS)`, no extra data. The screen reuses the garage GUI background and draws its own slot frames, as `ShuttleGarageScreen` does.

Alternative: a block entity holding shuttle and modules separately and packing on extraction. Rejected: more persistence and drop handling for no gain; nothing in the workshop needs to tick.

### D4. Fleet identity and profiles

`ShuttleFleet.adoptGarage(BlockPos pos, List<ShuttleProfile> slots, boolean active)` takes one entry per garage slot, `null` for an empty slot. Each `Shuttle` gets `final int slot` and a `ShuttleProfile profile`. Adopting keeps the shuttle of a slot whose profile is unchanged, removes one whose slot emptied (lift released, cargo dropped as today), creates a parked one for a filled slot, and for a changed profile recalls that shuttle (cargo dropped, unloaded jobs queued) before taking the new profile. Shuttles stay sorted by slot. `ShuttleGarageBlockEntity.profiles()` builds the list from its slots with `ShuttleModules.profile`; `WarehouseNetwork` passes it on join and on `garageChanged`.

`ShuttleFleet.outSlots(pos)` returns a bit mask of the slots whose shuttle is out. `ShuttleGarageMenu` syncs it in a `DataSlot` next to `out` and locks by bit. Container data travels as a short (`ClientboundContainerSetDataPacket` writes `writeShort`), so the mask covers 15 slots; `garage_capacity` above 15 would lock nothing past slot 15, which is noted and acceptable for a balance value of 4.

### D5. Jobs carry needs, matching in the assigner

`ShuttleJob` gets `default JobNeeds needs()` returning `JobNeeds.WAREHOUSE`, and `default boolean sameWay(ShuttleJob other)`: picks with equal source and terminal, put-aways with equal station and target. `m10-harvester` adds a field job type that overrides `needs()`.

`assignJobs` walks the queue in order. For each job it collects free shuttles whose profile fits its needs; the one with the shortest path takes it. When none is free but some homed, active shuttle fits, the job stays queued and the walk goes on (needs that failed once in a tick are skipped for the rest of it). When no shuttle of the fleet fits at all, the job is served instantly as without shuttles. `hasShuttles()` and `canReach(pos)` count only shuttles that fit `JobNeeds.WAREHOUSE`, so a warehouse with only field only shuttles behaves like one without shuttles.

### D6. Several loads per shuttle

`Shuttle.job` and `Shuttle.cargo` become `List<Load> loads`, `Load { ShuttleJob job; ItemStack cargo; }`; a shuttle is busy while it has loads. All loads of a shuttle share its way, so the legs (`TO_SOURCE`, `TO_TARGET`, `TO_STATION`, `TO_TERMINAL`, `WAITING_ROOM`, `HOME`) stay as they are and each arrival handles every load: extract, deliver, insert or return each one, drop loads that came out empty, and go home when none is left. `dropCargo` handles each load as today and puts unloaded jobs back at the front of the queue in their original order.

Batching happens twice, both through `CargoBatch.take` with `sameWay` and `room = profile.stacks() - loads`: when a shuttle takes its first job, and again when it arrives at the source or station (`TO_SOURCE` arrival, before loading). The second pass matters for put-aways, which are queued one per tick, so the rest of a station's stacks are in the queue by the time the shuttle gets there.

`ShuttleView` keeps one cargo stack; the cart shows the first non empty load.

### D7. Data

`warehouse.json` gains `"cargo_hold_stacks": 4`, read by `StorageStatsManager.cargoHoldStacks()` (default 4). Recipes: Shuttle Workshop from planks, iron ingots and a crafting table; Cargo Hold from iron ingots and a chest. Datagen adds blockstate and block model (cube bottom top), item models, loot (drops itself), lang, recipes and the pickaxe tag. Placeholder textures via the png helper.

### PLAN.md open decisions

None of section 5 is settled here. The field work paragraph in section 2.3 is implemented as written; the "two module slots" there are the tool slot and the utility slot.

## Pure logic and tests

- JUnit (`storage/src/test/.../shuttle/module/`): `ShuttleProfileTest` (plain one stack, Cargo Hold stacks from the argument, field only from any module, modules set), `JobNeedsTest` (warehouse job fits plain and Cargo Hold, refuses field only; a needed module must be present), `CargoBatchTest` (queue order, room limit, skips other ways, zero room).
- GameTests in `ServusStorageGameTests`: workshop fitting and removal through the menu, one shuttle per slot, closing returns the shuttle; Cargo Hold pick of 100 in one trip versus one job at a time; Cargo Hold put-away loading three stacks at once; garage lock by slot.

## Risks / Trade-offs

- [Batching jumps the queue] later jobs that share the way ride along ahead of earlier jobs for other ways → acceptable, the spec states it; they would have been served by the same trip order otherwise.
- [Refactor of the fleet's job handling could break pick and put-away flows] → every existing shuttle GameTest stays and must pass.
- [Lock mask limited to 15 slots] → documented in D4.

## Migration Plan

Unreleased: no migration. Existing garages load their Shuttle items without the component, which are plain shuttles.
