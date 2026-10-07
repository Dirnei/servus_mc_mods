# Design

## Context

- Logistics has no fluid code yet. Lib's `NetworkSet` and `ServusNetwork` give graph join and split with `onNodeAdded`, `onNodeRemoved`, `onAbsorb` and `onDisposed`; ducts and the warehouse use them, and the warehouse already pushes block state from the network (the connection lamp's `ONLINE`).
- `DuctStatsManager` shows the pattern for a small data JSON read by a reload listener.
- NeoForge 21.1.255 sources (checked in the Gradle cache jar): `Capabilities.FluidHandler.BLOCK` is a sided `BlockCapability<IFluidHandler, Direction>`; `IFluidHandler.fill(FluidStack, FluidAction)`, `drain(FluidStack, FluidAction)`, `drain(int, FluidAction)`; `FluidUtil.interactWithFluidHandler(Player, InteractionHand, Level, BlockPos, Direction)` handles buckets; `FluidStack.OPTIONAL_CODEC`, `OPTIONAL_STREAM_CODEC` and `isSameFluidSameComponents`; `SimpleFluidContent` is the data component form; client `IClientFluidTypeExtensions.of(fluid).getStillTexture(stack)` and `getTintColor(stack)`.

## Goals / Non-Goals

**Goals:**
- A rectangular multiblock tank that never loses fluid on forming, breaking, save, reload or chunk unload.
- No per tick work on idle tanks, and no walking of blocks beyond the graph the network already has.

**Non-Goals:**
- Rendering one big fluid box per tank; each block renders its own share.
- Special handling of tanks cut by unloaded chunks: the loaded part is not a full box, so its blocks work as singles until the rest loads.

## Decisions

### D1. Every block stores its share; the network only lays them out
`FluidTankBlockEntity` stores `share` (a `FluidStack`) in NBT and is a node of `TankNetwork` (`NetworkType` with `ConnectionRule.always()`). The network tracks its node positions and bounding box. Formed means the node count equals the box volume (pure `TankShape.isBox`). In a formed network the content is the sum of the shares, fill and drain act on that total, and after a change the network marks itself dirty and in `onTick` re-lays the total with pure `TankLayers` and writes each block's new share (setChanged and a client update only where the share changed, client updates throttled to every 10 ticks per block). In an unformed network every block's handler acts on its own share alone.
Because the shares are always current at the end of a tick, joins, splits, forming and unforming need no content transfer: the new network sums whatever the blocks hold. Forming re-lays the summed shares bottom up; unforming leaves every block with the share it had.
Alternatives: a master block entity holding everything (rejected: master election and content moves on master loss are the classic multiblock bug source); content in `SavedData` (rejected: drops and chunk unloads would need separate bookkeeping).

### D2. Pure logic
- `TankShape`: from node count and bounding box decide formed; JUnit for a box, an L, a box plus a touching block, a single block.
- `TankLayers`: for a box of `width * depth` blocks per layer, `height` layers, `capacity_per_block` and a total, the amount per block in each layer, lowest first, a partial layer split evenly with the remainder going to the first blocks in a fixed order so the sum is exact. JUnit: tower half full, 2 by 2 by 2 with six blocks' worth, empty, total clamped to capacity, remainder distribution sums exactly.

### D3. One fluid rule
`FluidTankItem extends BlockItem` overrides `place`: it reads the fluids of the item's share and of the tank networks of all touching tank blocks (formed content or single shares) and cancels with an action bar message when more than one distinct fluid is present. So every connected group holds at most one fluid and forming can never mix fluids. The handlers refuse fills of a different fluid.

### D4. Connected look by multipart model
`FluidTankBlock` (full cube, `noOcclusion`, glass sound) has six boolean properties (`north` to `down`) meaning "joined to a block of the same formed tank on that side". The network sets them on every member when it forms and clears them when it unforms or loses a member; unformed blocks keep all false. Datagen writes a multipart blockstate: six glass face parts, each shown when its side is not joined, and twelve frame edge bars, each shown when neither of its two sides is joined. That draws a frame only around the outside of the box with no extra textures. Fluid rendering: `FluidTankRenderer` (`BlockEntityRenderer`) draws a box from the block bottom to `share / capacity_per_block`, still sprite and tint from `IClientFluidTypeExtensions`, skipping faces toward joined sides.

### D5. Item, buckets, data
The item carries the share as `SimpleFluidContent` in a `servus_logistics:fluid` data component, read in `applyImplicitComponents`, written in `collectImplicitComponents`, copied by the loot table with `copy_components`; the tooltip shows fluid and amount. `useItemOn` calls `FluidUtil.interactWithFluidHandler`; an empty hand prints fluid, amount and capacity. Capability: `Capabilities.FluidHandler.BLOCK` on every face, returning the formed tank's handler or the block's own. `TankStatsManager` reads `data/servus_logistics/tank/fluid_tank.json` (`capacity_per_block`, default 16000).

### Settled PLAN.md items
- Section 1 "Fluids later: `IFluidHandler`" becomes current; 2.2 gains the Fluid Tank; M5 gains the item "Fluid Tank".

### Pure Java vs Minecraft
- Pure, JUnit: `TankShape`, `TankLayers`.
- GameTest: two blocks report summed capacity; L stays singles, fourth block forms and sums; breaking a corner unforms and keeps shares; touching single block unforms a box; bucket in and out; lava refused by a water tank; placement between water and lava refused; full block moved keeps its water.
- Manual: connected look and fluid rendering.

## Risks / Trade-offs

- [A large tank writes many block entities when its level changes] → Writes happen once per tick only when dirty, only for blocks whose share changed; a level change touches at most one partial layer plus the layer it completes.
- [Touching a tank with a stray tank block unforms it] → Intended and in the spec; the player sees the frame appear between the blocks.
- [Multipart with eighteen parts per block costs model bake time] → One block type, 64 states, acceptable.

## Migration Plan

None, nothing is released.
