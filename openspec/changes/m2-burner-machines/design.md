# Design

## Context

Manufacturing has deposits and the registry and datagen plumbing. Lib provides `Wrenchable`. See proposal.md and the three delta specs.

APIs verified in the NeoForge 21.1.255 sources:
- `BaseEntityBlock` with `codec()`, `simpleCodec`, `createTickerHelper`; `BlockBehaviour.useWithoutItem` and `useItemOn` returning `ItemInteractionResult`.
- `BlockEntity.loadAdditional/saveAdditional(CompoundTag, HolderLookup.Provider)`.
- `Capabilities.ItemHandler.BLOCK`, `RegisterCapabilitiesEvent.registerBlockEntity`, `ILevelExtension.getCapability(BlockCapability, BlockPos, Direction)`, `ItemHandlerHelper.insertItem`, `ItemStackHandler.serializeNBT(HolderLookup.Provider)`, `SlotItemHandler`.
- `IItemStackExtension.getBurnTime(RecipeType)`, `RecipeManager.getRecipeFor(RecipeType, RecipeInput, Level)`, `SingleRecipeInput`.
- `IPlayerExtension.openMenu(MenuProvider, BlockPos)`, `IMenuTypeExtension.create(IContainerFactory)`, `RegisterMenuScreensEvent.register`, `AbstractContainerMenu.stillValid(ContainerLevelAccess, Player, Block)`, `addDataSlots`.
- `AbstractContainerScreen.renderBg`, `GuiGraphics.blit(ResourceLocation, x, y, u, v, w, h)`.
- `ModelProvider.orientable(name, side, front, top)`, `BlockStateProvider.getVariantBuilder`, `LootTableProvider(PackOutput, Set, List<SubProviderEntry>, CompletableFuture)`, `BlockLootSubProvider(Set<Item>, FeatureFlagSet, HolderLookup.Provider)`, `BlockTagsProvider`.

## Goals / Non-Goals

**Goals:**
- One abstract machine stack (block, block entity, menu, screen) that later machines extend with a few overrides.
- Server authoritative: all work happens in the server ticker; the client only renders menu data.

**Non-Goals:**
- Energy. Added in the next change as a sibling of the burner base.

## Decisions

### D1: Class layout
Package `io.github.dirnei.mc.servus.manufacturing.machine`:
- `MachineStats` record and `MachineStatsManager` reload listener (directory `machine`). Fields are optional in JSON and merged over per machine defaults given in code.
- `MachineBlock` (abstract, `BaseEntityBlock`, `Wrenchable`): `FACING` and `LIT` properties, ticker on the server only, GUI on right click, drop contents on removal, wrench rotate and sneak dismantle. `useItemOn` returns `SKIP_DEFAULT_BLOCK_INTERACTION` when the held item is a `WrenchItem`, so the wrench reaches `Item.useOn` instead of the GUI opening.
- `MachineBlockEntity` (abstract): `ItemStackHandler`, slot role predicates (`isInputSlot`, `isOutputSlot`, `isFuelSlot`), one `IItemHandler` view for all sides enforcing the roles, output push every 8 ticks, lit state update, `ContainerData` with progress, work ticks, burn time, burn duration.
- `BurnerMachineBlockEntity` (abstract): fuel slot, burn countdown, fuel consumed only when `canWork()`; progress advances while burning and work is available, resets when there is no work.
- `extractor/BurnerExtractorBlock`, `BurnerExtractorBlockEntity` (slots: fuel 0, output 1) and `smelter/BurnerSmelterBlock`, `BurnerSmelterBlockEntity` (fuel 0, input 1, output 2).
- `menu/MachineMenu` (abstract) with generic `quickMoveStack`, `MachineSlot`, `OutputSlot`; `BurnerExtractorMenu`, `BurnerSmelterMenu`.
- `client/BurnerMachineScreen` shared by both menus; `client/ServusManufacturingClient` registers screens on the client mod bus.

### D2: Fixed rates, not recipe times
The smelter ignores the vanilla recipe's cooking time and uses `work_ticks` from its stats (default 80). Plan rule: "fixed speeds and clean ratios".

### D3: Output push targets
Every side, every 8 ticks, into any `IItemHandler` neighbour. Other Servus machines accept into their input and fuel slots, so an extractor on a coal deposit next to a smelter feeds the smelter's fuel. Pushing into a neighbour's output is impossible because outputs reject insertion.

### D4: Inventory sync
Menus reference the block entity on both sides (client looks it up by the position written into the open packet). Slot contents sync through the container, numbers through `ContainerData`. No custom packets.

### D5: Models
One `orientable` model per machine and lit state, built in the blockstate provider through `getVariantBuilder(...).forAllStates`. Textures: shared `machine_side` and `machine_top`, per machine `<name>_front` and `<name>_front_lit`. GUI background `textures/gui/burner_machine.png` with flame at (176, 0) 14x14 and arrow at (176, 14) 24x17, generated placeholders.

### D6: Tools and drops
Machines require a pickaxe (`mineable/pickaxe` tag through `BlockTagsProvider`), drop themselves through a generated loot table, and drop contents from `onRemove`. Dismantling through the wrench bypasses the tool requirement by spawning the item directly.

## Pure Java vs Minecraft

All Minecraft. Verified by datagen output, build, server run with RCON (place deposit, extractor, fuel through `/item replace block`, wait, inspect output with `/data get block`), client launch for models and screens. The player facing GUI check is manual.

## PLAN.md decisions settled

None. Rates match the Tier 1 table (1 per 4 s).

## Risks / Trade-offs

- [Output push every 8 ticks across 6 sides is a capability lookup per side] -> Cheap; capability lookups are cached by NeoForge. Revisit when machine counts are large.
- [Smelter accepts any smeltable item, including food] -> Matches "vanilla smelting recipes"; fine for Tier 1.
- [Lit state flips call setBlock] -> Only on change, and `UPDATE_CLIENTS` without neighbour updates, so no cascades.
