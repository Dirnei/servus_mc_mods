# Design

## Context

See proposal.md for the why. Facts the approach rests on (all checked in the repo or the 26.1 sources):

- The Fluid Tank lives in Foundation (`foundation/.../logistics/tank`). `FluidTankBlockEntity` is public and offers `network()`, `tankContent()`, `tankCapacity()`, `savedBox()` (a `TankShape` with min/max corners, `width()`, `height()`, `depth()`, `volume()`), and `TankNetwork` offers `boxAt(NodePos)`, `content(NodePos)`, `fill(NodePos, FluidStack, boolean)` and `drain(NodePos, int, boolean)`. `Conversions.toNodePos(BlockPos)` is public. Delicacies already depends on Foundation, so it reads and drains a tank without any Foundation change.
- The tank's floor plate shows the block the tank was crafted with (`floor()`, default copper), not the block below it (`foundation/fluid-tank` "Connected look"). So the gravel bed is drawn by the aquarium renderer, not taken from the Floor block's top.
- The tank model has a 2 px copper (`minecraft:block/copper_block`) frame on its edges, a glass top face with `cullface: up` and a floor plate whose down face has `cullface: down` (generated `fluid_tank.json`).
- Multiblocks in Delicacies form with the Foundation Configuration Carrot through `Wrenchable.onWrench` (`StoveTileBlock`), with one master holding the state (`MasterFirebox`). The aquarium follows the same pattern.
- Delicacies takes FE today through `MachineEnergyStorage` (Foundation, a `SimpleEnergyHandler`) in `FireboxHeating`, exposed as `net.neoforged.neoforge.transfer.energy.EnergyHandler`.
- Custom fluids follow `BreweryFluids` (FluidType, `BaseFlowingFluid` source and flowing, `LiquidBlock`, `BucketItem`) with the water textures tinted on the client (`BreweryClient`, NeoForge `RegisterClientExtensionsEvent` and `IClientFluidTypeExtensions`).
- Delicacies has no GUI yet. Foundation has the GUI widgets (`foundation/gui-widgets`: one line number stepper) and menu patterns (`ServusLogisticsMenus` with `IMenuTypeExtension.create`, payloads in `ServusFoundationPayloads` through `RegisterPayloadHandlersEvent`).
- Block entity renderers in 26.1 use the render state split: `BlockEntityRenderer<T, S extends BlockEntityRenderState>` with `createRenderState`, `extractRenderState` and `submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)` (`FluidTankRenderer`). Entity models are drawn with `OrderedSubmitNodeCollector.submitModel(Model, state, poseStack, RenderType, light, overlay, outlineColor, crumbling)`. `BlockEntityRendererProvider.Context.bakeLayer(ModelLayers.COD / SALMON)` gives the model parts. `CodModel` takes a `LivingEntityRenderState`, `SalmonModel` a `SalmonRenderState`; both animate the tail from `ageInTicks` and `isInWater`. Renderers widen culling by overriding `getRenderBoundingBox(T)` (NeoForge extension, used by `AthanorRenderer`).
- The Carbon Filter and its filter API come from `m14-carbon-filter` (Foundation). The aquarium uses `Filters.isFilter(stack)` for the slot check and `Filters.wear(stack, points)`, which returns `WearResult(spent, stack)`; the master stores `result.stack()` back and moves it to a catch slot when it is no longer a filter (the spent item).

## Goals / Non-Goals

**Goals:**
- All game state lives on one master block entity, ticking only on the master.
- No block scanning: validation uses the tank network's box lookup and neighbour updates.
- The cycle, power and drawn count are pure Java with JUnit tests.

**Non-Goals:**
- Any change to Foundation's tank, network or renderer.
- Client prediction of the stock: the client only shows what the server syncs.

## Decisions

### Master and parts
The master is the Floor block at the tank footprint's north west corner (min x, min z). It holds species, stock (a double, so fractions carry over), dirt, unfed cycle count, auto catch, keep level, the slots (feed 1, filter 1, catch 3), the FE buffer, the cycle timer, the tank box and the power tick counters. Every other part stores the master position. Forming writes all of this; unforming clears it. Alternative: one SavedData per aquarium keyed by tank: rejected, a block entity already gives save, sync and drops.

### Forming check
Starting from the clicked part: a Floor finds the tank block above it, a Lid the tank block below it. From that tank block's `TankNetwork.boxAt` the box is known. The check walks only the box's footprint (W x D floor positions below `minY`, W x D lid positions above `maxY`) and the tank content (empty or water, `FluidTags.WATER`). No other blocks are read. A part may belong to one aquarium only.

### Staying valid
- Breaking a part: the part's `preRemoveSideEffects` tells the master, which unforms at once.
- Breaking a tank block: caught by the box check below within a second (the corner block gone or the box changed); no `neighborChanged` hook, since a tank block in a middle layer touches no part anyway.
- The tank's box changing (a layer added, a block added beside it): the master compares `boxAt(tank)` with its stored box once a second (20 ticks), one map lookup. A different box unforms.
- Unforming drops the slots and `floor(stock)` raw fish at the master.

### Cycle (pure: `aquarium/AquariumCycle`)
Input record: stock, water mB, dirt, feed available, unfed cycles, filter durability left, powered share (0..1), keep level, auto catch, catch room, Murky Water room. Output record: new stock, dirt, feed used, unfed cycles, filter points used, fish caught, fish died, quality, Murky Water made. Order per cycle:
1. feed: need = ceil(whole fish x `feed_per_fish`) items; fed when available >= need (or stock 0). Unfed fish eat nothing that cycle.
3. dirt += stock x `dirt_per_fish` + eaten x `dirt_per_feed`.
4. filter (only for the powered share): limit = `filter_rate` x max(1, buckets / `filter_buckets`); cleaned = min(dirt, limit x share, durability, Murky room / `murky_per_dirt`, water / `murky_per_dirt`). Murky made = cleaned x `murky_per_dirt`; that much water leaves the tank.
5. capacity = remaining water / `water_per_fish`; quality = 1 - dirt / (buckets x `dirt_per_bucket`), clamped 0..1.
6. deaths: no water, all; quality < `dying_below`, `dirty_loss`; unfed cycles > `starve_after`, `starve_loss`; then clamp to capacity.
7. growth when fed, stock >= 2, quality >= `dying_below`: stock += `breed_rate` x quality x share x stock x (1 - stock / capacity).
8. catch when auto catch: floor(stock) - keep, limited by catch room.
The master drains the Murky made from the tank through `TankNetwork.drain` and adds what came out to its Murky Water amount (`AquariumMasterState.murky`, saved, cleared on unform: Murky Water is lost with the aquarium). Dirt leaves only through the filter. Draining through the glass needs no code: dirt stays, the water shrinks, quality falls. The Murky Water tank has no block or model of its own.

### Power
`MachineEnergyStorage` buffer on the master (size from data, example 4 x the cost per tick), offered as `Capabilities.Energy.BLOCK` on every Floor part of a formed aquarium. Cost per tick = tank blocks x `fe_per_tank_block` + lid slabs x `fe_per_lid` (pure `AquariumPower.cost`). Each tick: draw the cost if stored, count powered ticks; the cycle gets powered ticks / cycle ticks as its share, then the counters reset. The lit state of the lid follows "powered last tick", synced as a block state property `LIT` on Lid slabs (light emission 12 when lit), changed only when it flips.

### Catch and the spent filter
Raw fish go into the catch slots first; a worn out filter then moves there too, and stays in the filter slot (cleaning nothing) until a catch slot has room. Pouring a fish of the other species into an aquarium with no whole fish starts the new species at one fish.

### Item and fluid handlers
One `ResourceHandler<ItemResource>` per aquarium on the master, offered by every part: slot 0 feed (insert, `c:fish_feed` only), slot 1 filter (insert, filter API items only), slots 2 to 4 catch (extract only). One `ResourceHandler<FluidResource>`: insert water only (fills the tank through `TankNetwork.fill`), extract only Murky Water from the master's Murky Water amount (one tank, capacity `murky_capacity`); the tank's water never leaves through a part. Capabilities registered in `RegisterCapabilitiesEvent` for both part block entities; unformed parts return null.

### Buckets
`useItemOn` of both parts: fish bucket (`Items.COD_BUCKET`, `Items.SALMON_BUCKET`), water bucket, else the GUI. A fish bucket on the tank glass is caught by a `PlayerInteractEvent.RightClickBlock` listener (`AquariumTankClicks`), since the tank block is Foundation's: it walks down the tank column to the Floor below and pours into that aquarium, cancelling the tank's own bucket use. The species is an enum `FishSpecies { COD, SALMON }` with bucket, raw fish item and model layer; new species are a later change.

### Whole lid layer
The rules repeat `FluidTankItem`'s layer placement in Delicacies, since this change makes no Foundation change.
`AquariumLidItem.useOn`: on the top face of a tank block, not sneaking, uses `boxAt` for the footprint above `maxY`; same rules as `FluidTankItem` (enough items or creative, all spots free), else normal slab placement.

### GUI
`AquariumMenu` (first Delicacies menu, registered with `IMenuTypeExtension.create`, opened with the master position) with the 5 slots plus player inventory, status synced through `ContainerData` (stock, capacity, quality, powered, keep level, auto catch, species, Murky Water and its capacity). `AquariumScreen` draws the status lines, a Murky Water `FluidGauge` between the filter and catch slots (tooltip with amount; the JEI plugin's `IGuiContainerHandler` makes it a clickable `NeoForgeTypes.FLUID_STACK` ingredient), the auto catch toggle and the keep level with the Foundation one line number stepper. Changes go to the server as `AquariumSettingsPayload(pos, autoCatch, keep)`.

### Rendering
- `AquariumRenderer` on the master's block entity. Its render state carries tank box, water height (tank content over capacity times height), species, drawn count, quality, deaths since last frame, game time.
- Fish: the drawn count from pure `AquariumLook.drawnFish(stock, capacity, cap)`. Each fish index has a seed from the master position; its position is a sum of two sines per axis over game time scaled by `0.25 + 0.75 x quality`, inside the water box minus a margin, below the surface (pure `FishPath`, unit tested for staying inside the box). Heading and pitch come from the path's derivative; the model's `ageInTicks` advances slower in dirty water. Models baked from `ModelLayers.COD` / `ModelLayers.SALMON`, drawn with `submitModel` and the model's render type for the vanilla texture.
- Floaters: when the synced stock drops, up to three fish are drawn belly up at the surface for 8 seconds (client only).
- Flopping: when the water height is under 6 px the fish lie on the bed and hop, with the out of water tail speed (`isInWater = false`).
- Gravel bed and seagrass: custom geometry with `minecraft:block/gravel` on the block atlas, a box from y 1.05 to 1.95 px over the inner footprint inset 0.05 px from the glass walls; seagrass as crossed quads of `minecraft:block/seagrass` swaying by a small shear over time.
- `getRenderBoundingBox` covers the whole tank box plus the lid.
- Murky Water client extension: water still and flowing textures tinted brown green (example 0x5A6B3A).

Z-fighting check (CLAUDE.md models and textures):
- Floor top (y 16 of the floor block) and the tank's floor plate down face (y 0 of the tank block): the plate's down face has `cullface: down` and the Floor is a full solid block, so the plate face is culled; only the Floor top remains on that plane.
- Lid underside (y 0 of the lid block) and the tank's glass top (y 16 of the tank block): the glass top has `cullface: up`; the Lid slab's occlusion shape covers its full bottom, so the glass top is culled. The Lid must not use `noOcclusion`. Checked again in the play check.
- Gravel bed bottom at 1.05 px against the plate top at 1 px: 0.05 px apart. Bed sides inset 0.05 px from the glass walls. Bed top at 1.95 px against the frame bars' top at 2 px: different planes; inside the bars the bed is hidden.
- Seagrass quads cross at their centre line only; quads of different plants get a 0.005 px offset per plant index.
- Lid model: one box 0..16 x 0..4 x 0..16 drawn as two elements of the same box with disjoint faces: the body has the top and the four sides, the panel only the down face (light emission 15 in the lit model), so the panel glows without the rest and no face is drawn twice. The top face carries no cullface, since it lies at 4 px, not on the block edge. Floor model: one cube. Fish models are vanilla.

### Data
`data/servus_delicacies/aquarium/aquarium.json`, loaded by `AquariumStatsManager` (a `SimpleJsonResourceReloadListener` like `KitchenStatsManager`; its 20 optional fields are read from the JSON object by hand, one more than a record codec group takes). The drawn cap is synced to clients in the master's update tag:
```
{ "cycle_ticks": 1200, "water_per_fish": 4000, "breed_rate": 0.12,
  "feed_per_fish": 0.1, "starve_after": 3, "starve_loss": 0.05,
  "dirt_per_fish": 0.5, "dirt_per_feed": 1, "dirt_per_bucket": 10,
  "dying_below": 0.2, "dirty_loss": 0.1, "filter_rate": 40,
  "filter_buckets": 16, "murky_per_dirt": 25, "murky_capacity": 8000,
  "fe_per_tank_block": 2, "fe_per_lid": 1, "energy_buffer_ticks": 4,
  "keep_default": 8, "drawn_cap": 16 }
```
Tag `c:fish_feed` (new `KitchenTags.FISH_FEED`) holds `#c:seeds` from datagen.

### Recipes (datagen, examples)
- Aquarium Floor x2: copper ingots around gravel and kelp (`C K C / C G C / C C C`).
- Aquarium Lid x6: `C C C / D D D` with copper ingots and glowstone dust.

### Settled decisions from the explore
Both layers required; light only looks; power all or nothing per tick with the powered share scaling breeding and cleaning; one species; buckets on parts; capacity from water; dirt only leaves through the filter, as Murky Water in a hidden tank; Carbon Filter in the aquarium; Murky Water in Delicacies; feed through `c:fish_feed`.

### Pure versus Minecraft
Pure Java with JUnit: `AquariumCycle`, `AquariumPower`, `AquariumLook`, `FishPath`, `AquariumStats` defaults. GameTests: forming and refusals, unforming drops, buckets, capability slots and fluid exchange, power cost, a forced cycle with growth and catch, lid layer placement. Manual play check: rendering, light, GUI.

## Risks / Trade-offs

- [Unforming spills a large stock as items] Players see the fish drop rather than vanish; no duplication since the stock is gone.
- [Box check once a second leaves a short window after a tank change] Harmless: the next cycle runs at most every 1,200 ticks and checks first.
- [Many drawn fish in many aquariums] Capped per aquarium (example 16) and culled by the bounding box.
- [Translucent water over solid fish] Fish are drawn before the tank's translucent water, so they look under water; checked in the play check.

## Migration Plan

New content only; nothing to migrate.
