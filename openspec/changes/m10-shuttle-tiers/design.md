# Design

## Context

Current state, read from the code on 2026-10-11 (after the three applied m10 changes):

- `ShuttleItem` (`shuttle`, stacks to 16) carries `ShuttleModules(Optional<Item> tool, Optional<Item> utility)` as the `shuttle_modules` data component. `ModuleSpec(id, slot, fieldOnly, cargoHold)` sits on `ShuttleModuleItem`; the Cargo Hold and the Harvester are the two module items.
- `ShuttleProfile(modules, fieldOnly, stacks)` is pure; `ShuttleGarageBlockEntity.profiles()` builds one per slot with `cargo_hold_stacks` and `WarehouseNetwork` hands them to `ShuttleFleet.adoptGarage(pos, profiles, active)`.
- `ShuttleFleet` advances every shuttle by `host.shuttleSpeed()` (`warehouse.json` `shuttle_speed` 0.25). Loading and unloading happen in `arrivePick` and `arrivePutAway` on the tick of arrival. Field runs carry `max(profile.stacks, harvester_stacks)` and wait `field.json` `work_ticks` per worked plot (`beginStop`, `work`).
- `ShuttleView(path, segment, progress, speed, cargo, lift)` is synced per garage; `ShuttleGarageRenderer` draws `new ItemStack(SHUTTLE)` for every cart.
- `ShuttleWorkshopBlock` has no block entity; `ShuttleWorkshopMenu` is a crafting table style menu with a shuttle slot and two module slot views over the component.
- Foundation has `ServusFoundationItems.SPEED_UPGRADE`, an `UpgradeItem` with a block speed multiplier from `upgrades.json`.

## Goals / Non-Goals

**Goals:**
- Tiers as items, counts and tool on the component, all effects resolved into the pure profile once per garage change.
- Per shuttle speed and stop times without touching the leg state machine more than needed.
- A stateful workshop with an install slot, removal buttons, supply slots and Build now, with a clear seam for the build by hand.
- The shuttle's item in the view, so `m10-shuttle-looks` only needs the renderer.

**Non-Goals:**
- Rendering parts, the minigame, name plates, field range (later M10 changes).

## Decisions

### D1. Tiers are three items, Mk1 keeps the id `shuttle`
`ServusStorageItems.SHUTTLE_MK1` (`shuttle`), `SHUTTLE_MK2` (`shuttle_mk2`), `SHUTTLE_MK3` (`shuttle_mk3`), each a `ShuttleItem(int tier, Properties)` stacking to 16. Keeping the old id keeps the shuttles in the user's dev world. Every former `stack.is(SHUTTLE.get())` check becomes `ShuttleItem.isShuttle(stack)` (`stack.getItem() instanceof ShuttleItem`). `ShuttleItem.tier(stack)` gives 1 to 3, `ShuttleItem.ofTier(int)` the item. Alternative considered: one item with a tier component. Rejected because the user asked for three base models and separate items give them for free in `m10-shuttle-looks`.

### D2. Component: tool plus counts
`ShuttleModules(Optional<Item> tool, int speed, int handling)` replaces the old record, codec fields `tool`, `speed`, `handling` (optional, default 0), same component id `shuttle_modules`. An old `utility` field is ignored by the record codec, so Cargo Hold shuttles in dev worlds load as plain shuttles. `EMPTY` removes the component as before so plain shuttles stack. `ModuleSlot` and the `cargoHold` and `slot` fields of `ModuleSpec` go away: `ModuleSpec(id, fieldOnly)`, every `ShuttleModuleItem` is a tool.

### D3. Pure stats: `ShuttleTiers`
New pure record `ShuttleTiers(double baseSpeed, double speedPerCount, int stopTicks, int stackTicks, double handlingFactor, List<Tier> tiers)` with `Tier(int stacks, int maxSpeed, int maxHandling, List<Supply> build)` and `Supply(String id, int count)` (an item id string, so the class stays free of Minecraft). Methods, all clamping counts to the tier's max:
- `tier(int)` (1 based, clamped), `speed(tier, count)` = base + step * count,
- `factor(tier, handling)` = handlingFactor ^ count,
- `stopTicks(factor, stacks)` = max(1, round((stopTicks + stackTicks * stacks) * factor)),
- `workTicks(base, factor)` = max(1, round(base * factor)),
- `room(tier, kind, current)` for the install slot.
`DEFAULT` holds the example numbers. `ShuttleProfile` becomes `(Set<String> modules, boolean fieldOnly, int stacks, double speed, double handling)` built by `ShuttleProfile.of(ShuttleTiers, int tier, Optional<ModuleSpec> tool, int speed, int handling)`. `JobNeeds` and `CargoBatch` stay.

### D4. `shuttle.json`
`data/servus_storage/storage/shuttle.json`, read by `StorageStatsManager` like the other files of the `storage` folder into a `ShuttleTiers` (`shuttleTiers()` plus a test setter). Shape:
```
{ "speed": { "base": 0.15, "per_upgrade": 0.04 },
  "stops": { "stop_ticks": 10, "stack_ticks": 4, "handling_factor": 0.8 },
  "tiers": [ { "stacks": 2, "max_speed": 2, "max_handling": 2 },
             { "stacks": 4, "max_speed": 4, "max_handling": 4, "build": [ { "item": "minecraft:gold_ingot", "count": 16 }, ... ] },
             { "stacks": 8, "max_speed": 8, "max_handling": 8, "build": [ ... ] } ] }
```
The build of tier N is the sheet that makes tier N from tier N-1. Removed keys: `cargo_hold_stacks` and `shuttle_speed` from `warehouse.json`, `harvester_stacks` from `field.json`, with their `StorageStatsManager` fields and the `Host.shuttleSpeed()` method.

### D5. Fleet: profile from the stack, stack kept for the view
`ShuttleGarageBlockEntity.profiles()` becomes `slots()`, the list of slot stacks (empty stack for an empty slot). `adoptGarage(pos, List<ItemStack> slots, active)` builds each profile with `ShuttleModules.profile(stack, StorageStatsManager.INSTANCE.shuttleTiers())` and keeps a copy of the stack on the `Shuttle`. A slot whose profile changed recalls its shuttle as before; a stack that changed with the same profile only replaces the kept copy. `tick` advances by `shuttle.profile.speed()`, `views` sends that speed.

### D6. Stops as a countdown in front of `arrive`
`Shuttle` gains `int stop` (ticks left) and `ShuttleMotion stopAt` (the leg whose stop has started). `arrive` for a pick on TO_SOURCE or TO_TERMINAL, and for a put-away on TO_SOURCE or TO_TARGET, first checks `stopAt`: when it is not the current motion it sets `stop = tiers.stopTicks(profile.handling, stacks)` (stacks = the loads it holds), remembers the motion, and returns; when it is, the stop is over and the arrival goes on. `tick` counts `stop` down before anything else for that shuttle and calls `arrive` when it reaches zero. Every new leg is a new `ShuttleMotion`, so a recall or a new leg ends a running stop without extra bookkeeping. The view of a stopped shuttle is a one node path or an arrived path, so it stands still on the client. Field work: `beginStop` and `work` use `tiers.workTicks(fieldWorkTicks, profile.handling)`. `startRun` uses `profile.stacks()`.

### D7. Speed Upgrade reuse
The shuttle Speed count uses the Foundation Speed Upgrade item (`ServusFoundationItems.SPEED_UPGRADE`), the same item that doubles a Packing Station. Storage already depends on Foundation, and one item fewer to craft is what the user asked for with the Mekanism style. The block multiplier from `upgrades.json` does not apply to shuttles; shuttle Speed comes from `shuttle.json` only. The Handling Upgrade is a new plain Storage item `handling_upgrade` (stacks to 64) with a shaped recipe of iron ingots, a piston and redstone. Neither item is a `ShuttleModuleItem`; the install slot asks `ShuttleUpgrades.kind(stack)` (SPEED, HANDLING, TOOL or none).

### D8. Stateful workshop
`ShuttleWorkshopBlock` becomes a `BaseEntityBlock` with `ShuttleWorkshopBlockEntity`: an `ItemStackHandler` of 1 for the shuttle, an `ItemStackHandler` of 6 for supplies, and `int buildStep` saved but always 0 here (the seam for `m10-shuttle-build-minigame`). `onRemove` pops every stack of both handlers with `Block.popResource`, as the garage does. The menu opens with `player.openMenu(provider, buf -> ...)`, writing the block pos and `ShuttleTiers` (a `StreamCodec` on the pure record lives in the menu) so the client knows maxes and sheets; the menu type uses `IMenuTypeExtension.create(ShuttleWorkshopMenu::fromNetwork)` like the other Storage menus. Slots: shuttle (`SlotItemHandler`, `ShuttleItem.isShuttle`, max 1), install (a slot over an always empty one slot container), six supply slots, inventory. The install slot overrides `mayPlace(stack)` (room above 0), `getMaxStackSize(stack)` (the room: max minus count for Speed and Handling, 1 for a tool) and `set(stack)`, which installs the stack into the shuttle and leaves the container empty; on the server a swapped out tool goes to the player with `Inventory.placeItemBackInInventory`. Vanilla's `Slot.safeInsert` and `AbstractContainerMenu.moveItemStackTo` both cap by `getMaxStackSize(ItemStack)` before `setByPlayer`, so a stack installs as many as fit and the rest stays on the cursor (checked in the 1.21.1 sources from `neoforge-21.1.255-sources.jar`).
Buttons go through `clickMenuButton(player, id)` (sent by `MultiPlayerGameMode.handleInventoryButtonClick`): 0 takes a Speed out, 1 a Handling, 2 the tool, 3 is Build now. `ShuttleBuild` (pure, in `shuttle/module`) checks a sheet against supply counts by id and says what to take; the block entity resolves ids to items and `buildNow()` takes the supplies and puts `stack.transmuteCopy(nextTier, 1)` (1.21.1 `ItemStack.transmuteCopy`, copies all components, so counts, tool and custom name stay) into the shuttle slot. Tag supplies are left out; packs name items.

### D9. Views carry the shuttle item
`ShuttleView` gains `ItemStack shuttle` (count 1, `ItemStack.OPTIONAL_STREAM_CODEC`). `ShuttleGarageRenderer` draws that stack instead of a fixed Shuttle item. `m10-shuttle-looks` reads tier, counts and tool from it, and the name plate change reads the custom name.

### D10. Datagen and textures
Item models for `shuttle_mk2`, `shuttle_mk3` and `handling_upgrade` (basic items), lang for the three items, the GUI strings and tooltip lines, the Handling Upgrade recipe; the Cargo Hold model, lang and recipe go away. Placeholder textures: Mk2 and Mk3 are recolours of `shuttle.png` (iron grey and gold, then diamond cyan), the Handling Upgrade reuses the Cargo Hold texture file renamed. Creative tab: Mk1 to Mk3, Handling Upgrade, Harvester.

### PLAN.md open decisions
None of section 5 is settled here. This change settles the tier numbers (examples 2, 4, 8 stacks and maxes), the stop time model (stop plus per stack, Handling as a factor) and that the Foundation Speed Upgrade is the shuttle Speed item.

## Pure logic and tests

- JUnit in `storage/src/test/.../shuttle/module/`: `ShuttleTiersTest` (speed per count, clamping to the tier max, factor, stop ticks with the example numbers 18 and 12, work ticks 6, at least one tick, room per kind, tier clamp), `ShuttleProfileTest` (rewritten: stacks from the tier, field only from the tool, speed and handling from counts), `ShuttleBuildTest` (complete sheet, missing counts, extra supplies untouched, no sheet for the top tier), `JobNeedsTest` updated.
- GameTests (`ShuttleModuleGameTests`, rewritten): install a stack of Speed upgrades up to the max with the rest kept, tool swap returns the old tool, remove buttons, install refused without a shuttle, Build now with and without supplies keeps counts and tool, workshop keeps and drops its contents, a Mk1 shuttle takes two pick jobs of one way, a Mk2 put-away loads three stacks, a stop delays the delivery by the stop time, Speed shortens a trip. Existing storage and harvester GameTests move to `SHUTTLE_MK1` and get their timeouts checked against the slower base speed.
- Manual: the play check at the end of M10.

## Risks / Trade-offs

- Slower shuttles and stop times make every existing shuttle GameTest slower; timeouts may need raising. Tests can set `shuttleTiers` through the test setter for fast runs where timing is not the point.
- Reusing the Speed Upgrade means its tooltip speaks of block speed. The workshop GUI names the effect on shuttles; a shuttle specific tooltip line can follow.
- Tool swap on the client: the client predicts the install but not the returned tool; the server's inventory sync corrects it on the next tick.

## Migration Plan

Unreleased: none. Dev worlds keep Mk1 shuttles (same id); Cargo Hold items disappear and shuttles that had one lose it.

### Play check fixes
- Fast shuttles jumped at the lift: the client extrapolated them up the shaft between syncs and the next sync pulled them down onto the car. `ShuttleGarageRenderer.positionAfter` now stops at the start of a vertical step, and `ShuttleFleet` pauses `lift_handover_ticks` (`warehouse.json`, 10) after boarding and after the car arrives (`Shuttle.handover`, `handedOver`; the RIDE gate and `rideOver` ask `handover(shuttle)`). The storage GameTest `shuttlesQueueForCar` checks its first state 20 ticks later because of the two pauses.
