# Design: Port the Servus mods to Minecraft 26.1.2

## Context

The mods were written for Minecraft 1.21.1 on NeoForge 21.1.255 with Parchment names, ModDevGradle 2.0.148 and Java 21. Between 1.21.1 and 26.1 Mojang shipped ten releases with deep internal rewrites (registration ids, recipes, item models, GameTests, rendering, serialization, block models) and NeoForge replaced its transfer API in 21.9. The port has to cross all of them at once.

Sources used and verified for every API named below:
- NeoForge release posts 21.2, 21.4, 21.5, 21.6, 21.9, the 21.9 transfer rework post, 21.11 and 26.1 (neoforged.net/news).
- The NeoForge porting primers 1.21.2 up to 26.1 (docs.neoforged.net/primer/docs/).
- The decompiled and patched Minecraft 26.1.2 sources MDG produces (`foundation/build/moddev/artifacts/minecraft-patched-26.1.2.116-sources.jar`) and the NeoForge 26.1.2.116 sources jar from the Gradle cache. Every class and method used in the port was looked up there first.

## Goals / Non-Goals

**Goals:**
- Foundation, then Storage and Manufacturing, compile and run on Minecraft 26.1.2 / NeoForge 26.1.2.x with the same behaviour as on 1.21.1.
- Unit tests, GameTests and datagen work again, so later milestones keep their verification loop.

**Non-Goals:**
- Gameplay changes, new 26.1 content, refactors that are not forced by the platform.
- A 1.21.1 maintenance branch.

## Decisions

### D1. Versions

| Piece | 1.21.1 | 26.1.2 | Why |
|---|---|---|---|
| Minecraft | 1.21.1 | 26.1.2 | ATM11 runs 26.1.2 |
| NeoForge | 21.1.255 | 26.1.2.116 | newest stable 26.1.2 build on maven.neoforged.net on 2026-10-12 (26.2 and 26.3 builds exist but ATM11 is on 26.1.2) |
| ModDevGradle | 2.0.148 | 2.0.148 | newest MDG; 26.1 needs 2.0.141 or newer |
| Gradle wrapper | 9.2.1 | 9.2.1 | 26.1 needs 9.1.0 or newer, already met |
| Java | 21 | 25 | Minecraft 26.1 runs on Java 25 |
| Mappings | Parchment 2024.11.17 | none | 26.1 ships unobfuscated with Mojang's parameter names |
| JEI | 19.57.0.451 | 29.43.0.107 (`jei-26.1.2-*`) | newest JEI for 26.1.2 on maven.blamejared.com |

Gradle itself keeps running on the installed JDK 21; the Java 25 toolchain is provisioned by Gradle into its cache through the foojay resolver plugin that `settings.gradle` already applies. No JDK is installed system wide.

The `data` run uses the `clientData()` run type (NeoForge 21.4 split datagen into client and server; one client data run generates both, as NeoForge recommends) and keeps its name so `gradlew runData` stays the command.

While Manufacturing and Storage are not ported, `settings.gradle` includes only `foundation`, the root combined runs load only the included projects, and the root dev only dependencies (Modonomicon, Jade, FTB) are skipped. Re-enabling them is part of tasks 4 and 5.

### D2. Mechanical renames

Applied with scripts over the sources, then fixed by hand where the compiler complains:
- `ResourceLocation` is `net.minecraft.resources.Identifier` (1.21.11).
- `DirectionProperty` is gone; `EnumProperty<Direction>` (1.21.2).
- `ItemInteractionResult` and `InteractionResultHolder` are folded into `InteractionResult` (1.21.2): `useItemOn` returns `InteractionResult` (`TRY_WITH_EMPTY_HAND` replaces `PASS_TO_DEFAULT_BLOCK_INTERACTION`), `Item.use` returns `InteractionResult`, `sidedSuccess(client)` becomes `SUCCESS` / `SUCCESS_SERVER` as vanilla does.
- `ChunkPos` is a record: `ChunkPos.containing(pos)`, `ChunkPos.pack`, `ChunkPos.unpack` (26.1).
- NBT getters return `Optional` or take a default (`getIntOr`, `getCompoundOrEmpty`, `getListOrEmpty`) (1.21.5).
- Registration: blocks and items need their id in the properties (`setId`), so every block and item goes through `DeferredRegister.Blocks.registerBlock` / `DeferredRegister.Items.registerItem` or sets the id itself; block items use `useBlockDescriptionPrefix()`. `BlockEntityType` is constructed directly (1.21.2).
- `Level#isClientSide` is a method `isClientSide()` (1.21.9).

### D3. Block entity persistence

`saveAdditional(CompoundTag, Provider)` and `loadAdditional(CompoundTag, Provider)` become `saveAdditional(ValueOutput)` and `loadAdditional(ValueInput)` (1.21.6). Nested data goes through `ValueOutput.child`/`ValueInput.childOrEmpty` and lists through `list`/`childrenList`; codecs through `store`/`read`. The saved keys keep their names so the structure of the saved data does not change. `getUpdateTag` uses `saveCustomOnly`/`saveWithoutMetadata` with the registries as vanilla 26.1 does. Removal hooks: `onRemove` is replaced by `BlockEntity.preRemoveSideEffects` and `Block.affectNeighborsAfterRemoval` (1.21.5).

### D4. Transfer API

The deprecated `IItemHandler`, `IFluidHandler`, `IEnergyStorage` and their helpers still exist in NeoForge 26.1.2 (deprecated for removal since 21.9), but the capabilities are now `Capabilities.Item.BLOCK` (`ResourceHandler<ItemResource>`), `Capabilities.Fluid.BLOCK` (`ResourceHandler<FluidResource>`) and `Capabilities.Energy.BLOCK` (`EnergyHandler`). Decision: Foundation moves to the new API, not to the wrappers. Reasons: the old types are marked for removal, the transfer post says no general old to new wrapper is possible (transactions), and Storage and Manufacturing read Foundation handlers, so the shared types must be the new ones.

Mapping applied:
- `simulate` booleans become a transaction that is not committed; real moves open `Transaction.openRoot()` (or a nested one when a caller passes one) and commit.
- `ItemStackHandler` inventories become `ItemStacksResourceHandler` subclasses; `FluidTank` becomes `FluidStacksResourceHandler`; `EnergyStorage` becomes `SimpleEnergyHandler`. Their `onContentsChanged` hooks keep calling `setChanged()`.
- Directional wrappers (input only, output only, a single tank per side) become small `ResourceHandler` implementations or `RangedResourceHandler` over the backing handler, so sides expose the same subsets as before.
- Menus: `SlotItemHandler` becomes `ResourceHandlerSlot`.
- Foreign handlers are read with `level.getCapability(Capabilities.Item.BLOCK, pos, side)` and `BlockCapabilityCache` with the same capability objects.
- Moves between two handlers use `ResourceHandlerUtil.move` where the old code had an insert after a simulated extract.

### D5. GameTests

Vanilla replaced annotated GameTests with registered test functions, test instances and environments (1.21.5); NeoForge 26.1 drops `@GameTestHolder` and `@PrefixGameTestTemplate` and offers `RegisterGameTestsEvent.registerTest` and `registerEnvironment`. To keep the 106 test bodies untouched, Foundation gets a small `ServusGameTests` registrar: the old `@GameTest(template = EMPTY, timeoutTicks = N)` annotation is replaced by our own `@ServusGameTest(timeoutTicks = N)`; the registrar scans the listed classes, registers each method as a `Consumer<GameTestHelper>` in `Registries.TEST_FUNCTION` through a `DeferredRegister`, and in `RegisterGameTestsEvent` registers one `FunctionGameTestInstance` per method with `TestData` on the existing `servus_foundation:empty_12x6x12` structure and the `maxTicks` from the annotation. Test names stay the lower case method names under `servus_foundation:`.

### D6. Rendering and models

- GUI: `GuiGraphics` is `GuiGraphicsExtractor`; screens override `extractRenderState`, `extractBackground` and `extractLabels`; `blit` takes a `RenderPipeline` (`RenderPipelines.GUI_TEXTURED`); text colours need an alpha byte (0xFF000000 or) (1.21.6, 26.1).
- Block entity renderers implement `BlockEntityRenderer<T, S extends BlockEntityRenderState>` with `createRenderState`, `extractRenderState` and `submit` to a `SubmitNodeCollector` (1.21.9).
- Baked models: `BakedModel` is gone. The Utility Duct, panel and wood models become NeoForge `DynamicBlockStateModel`s whose `collectParts` reads the `ModelData` (`net.neoforged.neoforge.model.data`) from `level.getModelData(pos)` and returns `BlockStateModelPart`s; quads are `BakedQuad` records built with NeoForge's `MutableQuad`. They are installed in `ModelEvent.ModifyBakingResult` as before.
- Item models are item model definitions under `assets/<mod>/items/` produced by datagen (1.21.4).
- Level overlays (goggles, side markers, placement ghost) move from `RenderLevelStageEvent` stages to the 26.1 NeoForge level render events (see tasks 3.x for each).

### D7. Datagen

NeoForge removed its `BlockStateProvider`/`ItemModelProvider` builders (1.21.4) in favour of vanilla's `ModelProvider` with `BlockModelGenerators` and `ItemModelGenerators`. The custom duct, sink, tank and pump models (many element based JSON models) move to vanilla `ModelTemplate`/`TextureMapping` where they fit and otherwise to a small JSON writing provider that emits the same model files as before. `GatherDataEvent.Client` registers every provider. Recipe providers follow the 1.21.2 `RecipeProvider.Runner` shape; loot tables, tags and lang keep their providers with the new constructors.

### D8. Recipes and JEI

Recipes are no longer synced to clients (1.21.2). Server side code that looked recipes up through `level.getRecipeManager()` uses `ServerLevel.recipeAccess()`; client side code (JEI plugin, recipe pickers in menus) cannot see recipes any more and gets what it needs through recipe displays or our own payloads. JEI 26.1.2 gets recipes from the server through its own handling; our plugin registers the same categories with the new API.

## Risks / Trade-offs

- Size: about 25 000 lines in Foundation alone touch nearly every changed API. The port is done area by area with a commit per area; Foundation may stay non compiling between commits on this branch.
- Behaviour drift in the transfer rewrite: handled by keeping the GameTests (item, fluid and energy networks are covered) and running them after the transfer task.
- Models are the least testable part: they need a client check by the user, which this run does not do.
- The other agent changes Foundation on main (M11 chunk loader team cap) in parallel. The user merges; conflicts will be in the files that both touch.

## Notes for the next run

(Filled in at the end of each run: where the port stands, what is known broken.)
