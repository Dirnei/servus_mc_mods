# Design

## Context

- Manufacturing machines extend `MachineBlock` and `MachineBlockEntity`, which own an `ItemStackHandler`, work cycles, output push and `MachineStats` from `data/servus_manufacturing/machine/*.json`. The Sink has no inventory and no work cycle, so it does not fit `MachineBlockEntity`; it reuses the Lib side pieces instead: `SidedBlockEntity`, `SideConfig(supported, fallback)` with `lock`, `SideConfigWrench.use` for carrot cycling and dismantling with the configuration kept, `SideConfigMenus` button ids, client `SideConfigPanel` and `SideMarkerRenderer`.
- Capabilities are registered in `ServusManufacturingEvents` on `RegisterCapabilitiesEvent`.
- NeoForge 21.1.255 sources (checked in `build/moddev/artifacts/neoforge-21.1.255-sources.jar`): `Capabilities.ItemHandler.BLOCK`, `Capabilities.FluidHandler.BLOCK`, `Capabilities.EnergyStorage.BLOCK` (sided `BlockCapability`); `ModelEvent.RegisterAdditional.register(ModelResourceLocation)` and `ModelResourceLocation.standalone(ResourceLocation)` for an extra model, read back with `Minecraft.getInstance().getModelManager().getModel(...)`; `ModelBlockRenderer.renderModel(...)` to draw it from a `BlockEntityRenderer`; `Block.animateTick` for client particles; `ParticleTypes.CAMPFIRE_COSY_SMOKE`; `FluidUtil.interactWithFluidHandler(Player, InteractionHand, IFluidHandler)` for buckets, called with the Sink's unlimited manual handler (the overload taking a position would look up the rate limited capability).
- The fluid changes `m5-fluid-tank`, `m5-fluid-pipe` and `m3-pumps` are open. The Sink depends on none of them.

## Goals / Non-Goals

**Goals:**
- Zero work per tick for an idle Sink; no ticker at all.
- Chimney height known without scanning: recounted only when the column changes.
- Rate logic pure and unit tested.

**Non-Goals:**
- A shared "heat" budget across resources: each resource has its own budget so voiding water never starves item voiding.
- Rendering smoke as a model or entity; vanilla particles only.

## Decisions

### D1. Separate block family, not a `MachineBlockEntity`
`SinkBlock extends BaseEntityBlock implements Wrenchable` with `FACING` (horizontal, faces the player) and `ACTIVE` (boolean). `SinkBlockEntity extends BlockEntity implements MenuProvider, SidedBlockEntity`. Its `SideConfig` is `new SideConfig(List.of(SideMode.INPUT, SideMode.DISABLED), SideMode.INPUT)` with `lock(RelativeSide.FRONT, SideMode.DISABLED)`. Carrot: `SideConfigWrench.use(context, this, sink, () -> {})` since there is nothing to drop. NBT holds the side config only; chimney height is recounted on load.
Alternative: extend `MachineBlockEntity` with zero slots (rejected: it brings a ticker, work progress and output push the Sink never uses).

### D2. Pure rate logic
- `SinkRates`: three `int` lists (items per second, mB per tick, FE per tick) indexed by chimney height. `unlimitedHeight()` is the shortest list's length; `items(height)`, `fluid(height)`, `energy(height)` return the per tick rate as a `double` (items divided by 20) or `UNLIMITED`. JUnit `SinkRatesTest`: height 0, last limited height, unlimited height, heights above it, lists of different length.
- `VoidBudget`: a token bucket with `take(long requested, long gameTime, double ratePerTick)` returning the granted amount. Tokens refill lazily from the elapsed game time since the last call, capped at 20 ticks of rate (one second carry over). `UNLIMITED` grants everything. Fractional tokens stay in a `double`, so 4 items per second grant one item every 5 ticks on average. JUnit `VoidBudgetTest`: idle bucket grants one second of rate, a second take in the same tick gets the remainder only, steady 1 per tick requests settle at the rate, fractional item rate, unlimited, a rate drop caps stored tokens to the new capacity, game time going backwards (world reload edge) treated as zero elapsed.
Lazy refill from `level.getGameTime()` is what lets the Sink run without a ticker.

### D3. Capability handlers
Three handlers on the block entity share its three `VoidBudget`s, so all sides draw from one budget per resource. The capability provider returns them for every side whose mode is Input.
- Items: `IItemHandler` with one slot, `getStackInSlot` empty, `isItemValid` true, `getSlotLimit` 64, `insertItem` grants `min(count, budget)` (budget only consumed when not simulating), `extractItem` empty.
- Fluid: `IFluidHandler` with one tank of `Integer.MAX_VALUE` capacity, `getFluidInTank` empty, `fill` grants from the fluid budget, both `drain` methods return empty.
- Energy: `IEnergyStorage` with `receiveEnergy` granting from the energy budget, `extractEnergy` 0, `getEnergyStored` 0, `getMaxEnergyStored` `Integer.MAX_VALUE`, `canReceive` true, `canExtract` false.
A side whose mode is Off (including the locked front) returns no capability (`null`), so cables and pipes do not connect there. Changing a side invalidates capabilities with `level.invalidateCapabilities(pos)`, as `SidedBlockEntity.cycleSide` already does.
Every grant greater than zero calls `markVoided()`.

### D4. Active state without a ticker
`markVoided()` stores `lastVoided = gameTime`. If the state is not `ACTIVE`, it sets `ACTIVE` true (`Block.UPDATE_CLIENTS`) and schedules a block tick after `active_ticks` with `level.scheduleTick`. `SinkBlock.tick` (server scheduled tick) checks `gameTime - lastVoided >= active_ticks`: then it sets `ACTIVE` false, otherwise it reschedules for the remaining ticks. So a busy Sink costs one scheduled tick per active period, an idle one nothing. `lastVoided` is not saved; after a reload an `ACTIVE` state found in `onLoad` schedules one tick to clear it.

### D5. Chimney column counting
`ChimneyBlock` (full cube, brick sound and strength) has `TOP` (boolean), set in `getStateForPlacement` and `updateShape` from whether the block above is a Chimney. `ChimneyBlock.onPlace` and `onRemove` walk down through Chimney blocks (bounded by a cap of 32) and, if they end on a Sink, call `SinkBlockEntity.recountChimney()`. `SinkBlock.updateShape` (or `neighborChanged`) on the `UP` side recounts as well, which covers the lowest block. `recountChimney()` walks up counting Chimney blocks, capped at the walk cap of 32 rather than at the unlimited height so a datapack reload that raises the unlimited height needs no recount (heights at or above the unlimited height are unlimited anyway), and syncs the height to the client in the update tag so the fan speed and the GUI know it. `onLoad` recounts once on the server.
Alternative: count on every insertion (rejected: block lookups on the hot path of a pipe pushing every tick).

### D6. Fan rendering
The Sink's block model has a brick body, a brick frame around a recess three pixels deep with a sooty back wall (`sink_recess`) and a flat grate plane (`sink_front`, cutout) half a pixel behind the front. The Sink block uses `noOcclusion()` so the recess and the fan get the light of the block position instead of rendering black. The fan blades are a separate standalone model `servus_manufacturing:block/sink_fan`, registered in `ModelEvent.RegisterAdditional` with `ModelResourceLocation.standalone`. `SinkRenderer` (BER) translates to the recess, rotates about the facing axis by `(gameTime + partialTick) * speed` while `ACTIVE` (speed grows with the synced chimney height, example `6 + 3 * height` degrees per tick, no faster above height 8), keeps the last angle when idle, and draws the model with `ModelBlockRenderer.renderModel` in the cutout render type. It then delegates to a `SideMarkerRenderer` instance so the carrot markers still show.
Alternative: animated texture (rejected: cannot change speed with height and cannot stop on a frame cleanly).

### D7. Smoke
`SinkBlock.animateTick` (client) returns when not `ACTIVE`; otherwise it walks up through Chimney blocks (cap 32) to the column top and spawns `CAMPFIRE_COSY_SMOKE` above it, one or two particles per call with small random offsets, like `CampfireBlock.makeParticles`. Without a chimney the smoke starts above the Sink. The walk is client side and only happens for Sinks near the player.

### D8. GUI and manual voiding
`SinkMenu` has one `TrashSlot` backed by a one slot container whose `setItem` discards the stack (and calls `markVoided()` on the server), plus the player inventory; `quickMoveStack` from the inventory moves the whole stack into the trash slot. `SinkScreen` draws the slot, text lines for chimney height and the three rates (or Unlimited) from `ContainerData`, and the `SideConfigPanel`. Rates come from the menu data: height plus three values encoded per resource as an int (`-1` for Unlimited; items as per second). `SinkBlock.useItemOn` first tries `FluidUtil.interactWithFluidHandler` on a handler that ignores the budget; when that succeeds it returns success, otherwise the carrot skips and anything else falls through to `useWithoutItem`, which opens the menu.

### D9. Data
`SinkStatsManager` (`SimpleJsonResourceReloadListener`, registered like `MachineStatsManager`) reads `data/servus_manufacturing/sink/sink.json`:
```json
{
  "items_per_second": [4, 8, 16, 32, 64],
  "fluid_per_tick": [50, 100, 200, 400, 800],
  "energy_per_tick": [100, 200, 400, 800, 1600],
  "active_ticks": 40
}
```
Missing file or fields fall back to these code defaults. With five entries the unlimited height is 5.

### D10. Recipes and look
Shaped recipes by datagen: Sink from 6 `minecraft:bricks`, 2 `minecraft:iron_bars` and 1 `minecraft:iron_ingot` (bars around the ingot as the fan); Chimney ×2 from 6 `minecraft:brick` in two columns. Textures reuse vanilla `minecraft:block/bricks` for the bodies; new textures only for the Sink front grate, its recess, the fan blades and the Chimney cap top. The fan model `block/sink_fan` is generated by datagen like the other models. Loot tables drop the block with `dropSelf`, like machines; the side configuration travels with the item only when the Sink is dismantled with the sneak Configuration Carrot, which saves the block entity data to the item, as the spec requires.

### Settled PLAN.md items
- None of section 5. Section 2.1 gains a line on the Sink; M3 gains the item "Sink and Chimney".

### Pure Java vs Minecraft
- Pure, JUnit: `SinkRates`, `VoidBudget`.
- GameTest: hopper into an unlimited Sink empties; base rate refuses the excess of a 64 item insert; chimney of five gives unlimited for items, fluid and energy; breaking the middle chimney block lowers the height; front and Off sides return no capability; `ACTIVE` turns on when voiding and off after the active time; a bucket use leaves an empty bucket.
- Manual: fan spin and speed, smoke position, chimney cap, GUI, carrot markers.

## Risks / Trade-offs

- [An unlimited Sink on a Power Cable network drains every generator] → Intended; the rate tables and side modes let the player limit it, and the energy network already splits supply among receivers.
- [Shift click voids a stack by accident] → Accepted for a trashcan; the GUI says what it does. An undo slot can come later.
- [Mods that probe `getSlotLimit` or tank capacity may assume a huge buffer] → The handlers report honest refusals through `insertItem` and `fill`, which is what callers act on.
- [Recounting relies on Chimney `onPlace`/`onRemove`; pistons moving chimney blocks] → Pistons call `onRemove` and `onPlace` too, so the count stays right.

## Migration Plan

None, nothing is released.
