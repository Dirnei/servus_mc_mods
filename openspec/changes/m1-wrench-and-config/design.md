# Design

## Context

Lib has the graph and network manager but no registries, no datagen and no config. See proposal.md. This change sets the pattern that Manufacturing, Logistics and Storage will copy.

APIs verified in the NeoForge 21.1.255 sources:
- `ModContainer.registerConfig(ModConfig.Type.COMMON, ModConfigSpec)`, `ModConfigSpec.Builder.define(String, boolean)` and `.comment(...)`.
- `DeferredRegister.Items.registerItem(String, Function<Item.Properties, I>, Item.Properties)`, `DeferredRegister.create(Registries.CREATIVE_MODE_TAB, modid)`, `CreativeModeTab.builder()`.
- `GatherDataEvent` with `getGenerator()`, `getExistingFileHelper()`, `getLookupProvider()`, `includeClient()`, `includeServer()`; `DataGenerator.addProvider(boolean, DataProvider)`.
- `ItemModelProvider(PackOutput, String, ExistingFileHelper)` with `basicItem(Item)`; `LanguageProvider(PackOutput, String, String)`; `RecipeProvider(PackOutput, CompletableFuture<HolderLookup.Provider>)` with `buildRecipes(RecipeOutput)`; `ShapedRecipeBuilder.shaped(RecipeCategory, ItemLike)`.
- `Item.useOn(UseOnContext)`, `BlockStateProperties.FACING` and `HORIZONTAL_FACING`, `BlockState.cycle(Property)`.

## Goals / Non-Goals

**Goals:**
- Working wrench and config with the least machinery.
- Datagen wired so every later block or item only adds a line to a provider.

**Non-Goals:**
- Final textures, goggles, dismantling.

## Decisions

### D1: Mod constructor
`ServusLib(IEventBus modEventBus, ModContainer container)`. Registers `ServusLibItems.ITEMS`, `ServusLibCreativeTabs.TABS` on the mod bus and the common config on the container. NeoForge injects both parameters by type.

### D2: Wrenchable before rotation
`WrenchItem.useOn`: if the block implements `Wrenchable`, return `wrenchable.onWrench(context, state)`. Otherwise, when the config allows and the player is not sneaking, pick `HORIZONTAL_FACING` if present, else `FACING`, else pass. On the server, set `state.cycle(property)` with `Block.UPDATE_ALL` and play `SoundEvents.ITEM_FRAME_ROTATE_ITEM`; return `InteractionResult.sidedSuccess(level.isClientSide)`.
Alternative: `BlockState.rotate(level, pos, Rotation.CLOCKWISE_90)`. Rejected because it cannot reach UP and DOWN for six way blocks; `cycle` visits every value.

### D3: Config
`ServusLibConfig` builds one `ModConfigSpec` with a `wrench` section holding `wrenchRotatesBlocks` (true). Accessor `wrenchRotatesBlocks()` reads the value at use time. Registered as `ModConfig.Type.COMMON`, so the file is `config/servus_lib-common.toml`.

### D4: Creative tab shared by the family
`ServusLibCreativeTabs.SERVUS` is registered by Lib with the wrench as icon. The key `ServusLibCreativeTabs.SERVUS_KEY` is public so content mods can add their items by listening to `BuildCreativeModeTabContentsEvent` and comparing `getTabKey()`. One tab for the family keeps the creative inventory tidy; content mods can still add their own tabs later.

### D5: Datagen layout
`ServusLibDataGen` is a mod bus subscriber for `GatherDataEvent` that adds `ServusLibLanguageProvider` (en_us), `ServusLibItemModelProvider` and `ServusLibRecipeProvider`. Output goes to `lib/src/generated/resources`, which is already a resource root, and the `--existing src/main/resources` argument lets the model provider validate the texture. Generated files are committed so builds do not need a datagen run.

### D6: Texture
A 16 by 16 placeholder painted by a script into `lib/src/main/resources/assets/servus_lib/textures/item/wrench.png`. Replaced when the art direction is decided (PLAN.md open decision).

## Pure Java vs Minecraft

Everything here touches Minecraft. Verified by running datagen and checking the generated JSON, by launching the client with the generated resources, and by the user's manual check in game (rotate a furnace, craft a wrench, open the Servus tab). No GameTests yet.

## PLAN.md decisions settled

None. Art direction stays open; the texture is a placeholder.

## Risks / Trade-offs

- [Cycling FACING on blocks whose shape depends on it, like pistons or hoppers, may create odd states] -> Vanilla accepts any value of the property, and the option can be switched off. Servus machines use `Wrenchable` anyway.
- [Datagen in the same source set as the mod] -> Standard NeoForge practice; the providers are tiny.
