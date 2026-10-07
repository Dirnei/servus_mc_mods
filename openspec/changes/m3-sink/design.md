# Design

## Context

- Manufacturing machines extend `MachineBlock` and `MachineBlockEntity`, which own an `ItemStackHandler`, work cycles, output push and `MachineStats` from `data/servus_manufacturing/machine/*.json`. The Sink has no inventory and no work cycle, so it does not fit `MachineBlockEntity`.
- The first implementation (task groups 1 to 6) built a 1x1 Sink with Lib side configuration. After the play test the user asked for a 2x2x2 multiblock with one big fan, formed by the carrot on the front, with no side configuration. Task group 7 reworks the code; the pure rate logic, data file, Chimney block, trash menu and bucket handling stay.
- Capabilities are registered in `ServusManufacturingEvents` on `RegisterCapabilitiesEvent`.
- NeoForge 21.1.255 sources (checked in `build/moddev/artifacts/neoforge-21.1.255-sources.jar`): `Capabilities.ItemHandler.BLOCK`, `Capabilities.FluidHandler.BLOCK`, `Capabilities.EnergyStorage.BLOCK` (sided `BlockCapability`); `ModelEvent.RegisterAdditional.register(ModelResourceLocation)` and `ModelResourceLocation.standalone(ResourceLocation)` for an extra model, read back with `Minecraft.getInstance().getModelManager().getModel(...)`; `ModelBlockRenderer.renderModel(...)` to draw it from a `BlockEntityRenderer`; `BlockEntityRenderer.getRenderBoundingBox` for a box larger than one block; `Block.animateTick` for client particles; `ParticleTypes.CAMPFIRE_COSY_SMOKE`; `FluidUtil.interactWithFluidHandler(Player, InteractionHand, IFluidHandler)` for buckets, called with the Sink's unlimited manual handler (the overload taking a position would look up the rate limited capability); `Block.getPistonPushReaction` via `BlockBehaviour.Properties.pushReaction`.

## Goals / Non-Goals

**Goals:**
- Zero work per tick for an idle Sink; no ticker at all.
- Chimney height known without scanning: recounted only when a column changes.
- Forming, part layout and controller lookup as pure logic with unit tests.
- No saved multiblock bookkeeping that can go stale: the block states alone describe a formed Sink.

**Non-Goals:**
- A shared "heat" budget across resources: each resource has its own budget so voiding water never starves item voiding.
- Rendering smoke as a model or entity; vanilla particles only.
- Side configuration, side markers or a side panel.

## Decisions

### D1. Multiblock in block states, controller found by layout
`SinkBlock extends BaseEntityBlock implements Wrenchable` has `FORMED` (boolean), `FACING` (horizontal front of the formed cube), `PART` (integer 0 to 7) and `ACTIVE` (boolean). An unformed block has `FORMED` false and ignores the other properties (defaults `NORTH`, 0, false). `PART` encodes the block's place in the cube as three bits: right (1), up (2), back (4), seen from the front. Part 0, the front lower left block, is the controller. Every Sink block has a `SinkBlockEntity`, but only the controller's is used: it holds the three `VoidBudget`s, the chimney height and `lastVoided`, and its state carries `ACTIVE`. Any part finds the controller with `SinkLayout.controllerOf(pos, facing, part)`, so nothing about the multiblock is saved in NBT.
`SinkBlockEntity` no longer implements `SidedBlockEntity`; the side config, its NBT and its menu data are removed.
Alternatives: a master block entity storing member positions (rejected: stale lists after chunk unloads are the classic multiblock bug); keep side modes (rejected by the user: input only, no configuration).

### D2. Pure logic
- `SinkRates` and `VoidBudget` unchanged (see tasks 1.1 and 1.2).
- `SinkLayout` (no Minecraft imports, plain int coordinates and a facing given as the front's unit vector): `offset(part, facing)` from the controller to a part; `controllerOf(x, y, z, facing, part)`; `candidates(x, y, z, facing)` returning the four controller positions of the 2x2x2 cubes whose front plane contains the clicked block (the clicked block as any of the four front parts, the cube extending away from the clicked face); `topParts()` (2, 3, 6, 7) and `frontParts()` (0, 1, 2, 3). Right is the player's right when looking at the front, as decided for side config. JUnit `SinkLayoutTest`: offsets for all four facings, controllerOf inverts offset for every part and facing, candidates for each front part position, the cube lies behind the clicked face, top and front part sets.

### D3. Forming and unforming
Carrot on an unformed Sink block (`onWrench`, not sneaking) with a horizontal clicked face (a top or bottom face gets an action bar message asking for a side): for each of the four `SinkLayout.candidates` it checks that all eight positions hold unformed Sink blocks; the first match is formed by setting `FORMED`, `FACING` and each block's `PART` (`Block.UPDATE_ALL`), then the controller recounts the chimney and capabilities are invalidated on all eight positions. No match: action bar message. Carrot on a formed block does nothing; sneak carrot dismantles that one block (drops a plain Sink block, which unforms the rest through `onRemove`).
`SinkBlock.onRemove` of a formed block (when the new state is not a Sink block) walks the seven other positions from the layout and sets those that are still formed Sink blocks of the same controller back to unformed defaults, then invalidates their capabilities. Formed blocks use `PushReaction.BLOCK` (state dependent via `getPistonPushReaction` override), unformed ones push normally. The loot table drops a plain Sink block (`dropSelf` with no state copy).
Alternative: form automatically when the cube completes (rejected by the user: forming is an explicit carrot action on the front).

### D4. Capability handlers
The controller's three handlers share its budgets. The capability provider, registered for the Sink block entity type, returns them for any formed part on every face except the front face of front parts; unformed blocks and front faces return `null`, so cables and pipes do not connect there.
- Items: `IItemHandler` with one slot, `getStackInSlot` empty, `isItemValid` true, `getSlotLimit` 64, `insertItem` grants `min(count, budget)` (budget only consumed when not simulating), `extractItem` empty.
- Fluid: `IFluidHandler` with one tank of `Integer.MAX_VALUE` capacity, `getFluidInTank` empty, `fill` grants from the fluid budget, both `drain` methods return empty.
- Energy: `IEnergyStorage` with `receiveEnergy` granting from the energy budget, `extractEnergy` 0, `getEnergyStored` 0, `getMaxEnergyStored` `Integer.MAX_VALUE`, `canReceive` true, `canExtract` false.
Every grant greater than zero calls `markVoided()` on the controller.

### D5. Active state without a ticker
`markVoided()` stores `lastVoided = gameTime` on the controller. If the controller state is not `ACTIVE`, it sets `ACTIVE` true (`Block.UPDATE_CLIENTS`) and schedules a block tick after `active_ticks` with `level.scheduleTick`. `SinkBlock.tick` (server scheduled tick, controller only) checks `gameTime - lastVoided >= active_ticks`: then it sets `ACTIVE` false, otherwise it reschedules for the remaining ticks. `lastVoided` is not saved; after a reload an `ACTIVE` controller found in `onLoad` schedules one tick to clear it. Unforming clears `ACTIVE`.

### D6. Chimney columns
`ChimneyBlock` (full cube, brick sound and strength) has `TOP` (boolean), set in `getStateForPlacement` and `updateShape` from whether the block above is a Chimney. `ChimneyBlock.onPlace` and `onRemove` walk down through Chimney blocks (cap 32) and, if they end on a formed Sink block, call `recountChimney()` on its controller. `SinkBlock.updateShape` on the `UP` side of a formed top part recounts as well, which covers the lowest Chimney block. `recountChimney()` walks up from each of the four top parts, counting Chimney blocks (cap 32 each), keeps the maximum as the height and the column position of that maximum for the smoke, and syncs both to the client in the controller's update tag.

### D7. Look and fan rendering
- Unformed: a brick casing model with an iron trim (`sink_casing`), the same on all sides, so players see the parts are not working yet.
- Formed: datagen writes one model per `PART`, rotated by `FACING`. Outer faces show bricks; the four front parts together show one large grate over a dark recess (the front texture split in four quarters `sink_front_0` to `sink_front_3`, a fan guard of rings and spokes; recess three pixels deep on the front parts as in the first version, brick bars only on the outer edges of the 2x2 front); faces between parts are left out of the part models entirely, since the block does not occlude. The blockstate turns the part models with `uvlock` so the bricks on top line up.
- The fan blades are the standalone model `servus_manufacturing:block/sink_fan`, now designed at 2x2 block size (hub plus four blades, 32 pixels across). `SinkRenderer` is registered for the Sink block entity type but draws only for the controller of a formed Sink: it translates to the centre of the 2x2 front, rotates about the facing axis by `(gameTime + partialTick) * speed` while `ACTIVE` (speed `6 + 3 * height` degrees per tick, no faster above height 8), keeps the last angle when idle, and draws the model with `ModelBlockRenderer.renderModel` in the cutout render type, lit with the light of the block in front of the front's centre (the cube itself blocks light). The fan has four pitched main blades and four flat diagonal blades; its elements reach outside the block, so every face has explicit UVs. `getRenderBoundingBox` covers the whole cube so the fan does not vanish when the controller block leaves the view. The `SideMarkerRenderer` delegation is removed.
Alternative: one quarter fan model per front part (rejected: four pieces cannot spin as one fan around a shared centre without seams).

### D8. Smoke
`SinkBlock.animateTick` (client) acts only for a formed controller that is `ACTIVE`; it reads the synced column position and height and spawns `CAMPFIRE_COSY_SMOKE` above the top Chimney block of the tallest column, one or two particles per call with small random offsets. Without a chimney it spawns above a random top part of the cube.

### D9. GUI and manual voiding
`SinkMenu` has one `TrashSlot` backed by a one slot container whose `setItem` discards the stack (and calls `markVoided()` on the controller on the server), plus the player inventory; `quickMoveStack` from the inventory moves the whole stack into the trash slot. `SinkScreen` draws the slot and text lines for chimney height and the three rates (or Unlimited) from `ContainerData` (height plus three values, `-1` for Unlimited, items as per second); the side panel and side data are removed. `SinkBlock.useItemOn` on a formed block first tries `FluidUtil.interactWithFluidHandler` on the controller's unlimited manual handler; when that succeeds it returns success, the carrot skips to `onWrench`, a Chimney block item skips to its own placement (no sneaking needed to build the column), and anything else falls through to `useWithoutItem`, which opens the controller's menu. On an unformed block any held item skips to its own use (so the cube can be built by placing Sink blocks against it) and only an empty hand reaches `useWithoutItem`, which shows the action bar hint.

### D10. Data
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

### D11. Recipes
Shaped recipes by datagen: 8 Sink blocks from 8 `minecraft:bricks` around 1 `minecraft:iron_bars`; Chimney ×2 from 6 `minecraft:brick` in two columns (unchanged). Lang gains the action bar messages for an incomplete cube and the casing hint.

### Settled PLAN.md items
- None of section 5. Section 2.1 describes the Sink as a 2x2x2 multiblock formed with the carrot; M3 keeps the item "Sink and Chimney".

### Pure Java vs Minecraft
- Pure, JUnit: `SinkRates`, `VoidBudget`, `SinkLayout`.
- GameTest: carrot forms a cube from each front part; seven blocks do not form; forming ignores blocks of a formed neighbour; unformed blocks expose no capability; hopper into the side of an unlimited Sink empties; front face refuses; base rate refuses the excess of a 64 item insert; FE on one part and fluid on another share the Sink; chimney of five on a corner gives unlimited; the tallest of two columns counts; breaking a middle chimney block lowers the height; breaking a Sink block unforms the rest and drops a plain block; re-forming after repair; `ACTIVE` turns on when voiding and off after the active time; a bucket use on a formed part leaves an empty bucket; shift click into the trash slot.
- Manual: big fan look, spin and speed for all four facings, smoke position, casing and formed models, chimney cap, GUI.

## Risks / Trade-offs

- [An unlimited Sink on a Power Cable network drains every generator] → Intended; the rate tables let the player limit it, and the energy network already splits supply among receivers.
- [Shift click voids a stack by accident] → Accepted for a trashcan; the GUI says what it does. An undo slot can come later.
- [Mods that probe `getSlotLimit` or tank capacity may assume a huge buffer] → The handlers report honest refusals through `insertItem` and `fill`, which is what callers act on.
- [A formed Sink cut by a chunk border loads half] → All eight blocks lie in at most four chunks next to each other; a part whose controller is not loaded returns no capability until it is. Block states stay formed, so nothing is lost.
- [A world edit or `/setblock` replaces a part without `onRemove`] → The remaining parts stay formed but the controller check for capabilities verifies the controller is a formed Sink with the same facing; a broken cube is fixed by breaking and re-forming.
- [Recounting relies on Chimney `onPlace`/`onRemove`; pistons moving chimney blocks] → Pistons call `onRemove` and `onPlace` too, so the count stays right.

## Migration Plan

None, nothing is released. Sinks placed during the first play test load as unformed casings because the new properties default to unformed.
