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
| Modonomicon | 1.120.7 | 2.17.0 (`modonomicon-26.1.2-neoforge`) | newest for 26.1.2 on the klikli-dev cloudsmith maven |
| Jade (dev runs) | 15.10.6 | 26.1.11+neoforge | newest Modrinth version for 26.1.2 NeoForge |
| FTB Library, Ultimine (dev runs) | 2101.1.37, 2101.1.15 | 26.1.2.9, 26.1.2.5 | newest on maven.ftb.dev |

Gradle itself keeps running on the installed JDK 21; the Java 25 toolchain is provisioned by Gradle into its cache through the foojay resolver plugin that `settings.gradle` already applies. No JDK is installed system wide.

The `data` run uses the `clientData()` run type (NeoForge 21.4 split datagen into client and server; one client data run generates both, as NeoForge recommends) and keeps its name so `gradlew runData` stays the command.

During the port `settings.gradle` included only the ported projects; all three are included again. The root combined runs load the included projects and carry the dev only mods when Manufacturing is present.

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

`saveAdditional(CompoundTag, Provider)` and `loadAdditional(CompoundTag, Provider)` become `saveAdditional(ValueOutput)` and `loadAdditional(ValueInput)` (1.21.6). The saved keys keep their names. Foundation adds `save/ValueIo` for what the value input and output lack: byte arrays (stored as int arrays), UUIDs, whole compound tags (nested structures like the duct's per line modes are still built as `CompoundTag` and stored with `CompoundTag.CODEC`) and `write(...)` for hand written update tags read back by `loadAdditional`. `block/BlockEntityItems.saveToItem` replaces the removed `BlockEntity#saveToItem`. `SavedData` uses `SavedDataType` with a codec (Chunk Loader team data, packet stash). Removal hooks: drops move to `BlockEntity.preRemoveSideEffects`, neighbour work to `Block.affectNeighborsAfterRemoval` (1.21.5); both only run when the block really changes, as the old `!state.is(newState.getBlock())` checks did.

### D4. Transfer API

The deprecated `IItemHandler`, `IFluidHandler`, `IEnergyStorage` still exist in NeoForge 26.1.2 (deprecated for removal since 21.9), but the capabilities are now `Capabilities.Item.BLOCK` (`ResourceHandler<ItemResource>`), `Capabilities.Fluid.BLOCK` (`ResourceHandler<FluidResource>`) and `Capabilities.Energy.BLOCK` (`EnergyHandler`). Foundation moves to the new API and keeps none of the deprecated types; Storage and Manufacturing use the same Foundation classes in package `io.github.dirnei.mc.servus.foundation.transfer`:

- `ItemSlots` (replaces `ItemStackHandler`) and `FluidSlot` (replaces `FluidTank`): real transactional handlers (`ItemStacksResourceHandler`, `FluidStacksResourceHandler`) with the old slot style methods (`getStackInSlot`, `insertItem(slot, stack, simulate)`, `fill(stack, simulate)`, `drain(...)`), `save`/`load` under the old keys, and a load that never resizes. `MachineEnergyStorage` extends `SimpleEnergyHandler`.
- `LegacyItemHandler`, `LegacyFluidHandler`, `LegacyEnergyHandler`: bases for the many filtered views and network entry points that were written as old handlers (machine side views, duct entry points, the Sink, the tank network, the Machine Adapter). They implement the transfer API on top of the old style methods. Inside a transaction they only simulate (asking for the pending plus the new amount, so several inserts in one transaction add up correctly) and record the operation in a `SnapshotJournal` (`DeferredOps`); the root commit runs the recorded operations for real, an abort drops them. This keeps behaviour identical for effects that cannot be rolled back (routing into a network, voiding in the Sink) while other mods get proper transactional semantics.
- `ItemView`, `FluidView`, `EnergyView` and `Transfer`: old style one shot calls on handlers of other blocks read from capabilities. Every call opens a transaction nested in the one open on the thread (`Transaction.open(getCurrentOpenedTransaction())`), so simulations that legacy handlers run inside a caller's transaction stay legal.
- Capability caches hold the real handler types; reads wrap them in views. Menus use `ResourceHandlerSlot` with `ItemSlots::set` as index modifier. `FluidUtil` moved to `transfer.fluid`; container items are detected with `ItemAccess.forStack(stack).getCapability(Capabilities.Fluid.ITEM)`, and `ItemAccess.forPlayerInteraction` gives creative players the vanilla bucket behaviour, so the old creative fill workaround in the Fluid Tank is gone.
- `EnergyView.canExtract`/`canReceive` simulate one unit, since the transfer API has no fixed flags; an empty buffer on a two way duct side now counts as a sink instead of a source, which only matters for the order within a tick.

### D5. GameTests

Vanilla replaced annotated GameTests with registered test functions, test instances and environments (1.21.5); NeoForge 26.1 drops `@GameTestHolder` (which also registered annotated classes on its own) and offers `RegisterGameTestsEvent.registerTest` and `registerEnvironment`. Foundation's `gametest/ServusGameTests` registrar keeps the test bodies untouched: `@GameTest(template = EMPTY, timeoutTicks = N)` became `@ServusGameTest(timeoutTicks = N)`; the registrar registers each method in `Registries.TEST_FUNCTION` through a `DeferredRegister` and, in `RegisterGameTestsEvent`, a `FunctionGameTestInstance` with `TestData` on the `empty_12x6x12` structure and an empty `<mod>:default` environment. Names are `<mod>:<class>.<method>` in lower case. The mod constructor lists the test classes. `GameTestHelper.getBlockEntity` needs the class now.

### D6. Rendering and models

- GUI: `GuiGraphics` is `GuiGraphicsExtractor`; screens override `extractRenderState`, `extractBackground` (call super first, it draws the dim background) and `extractLabels`; the tooltip is drawn by the container screen itself. `blit` and `blitSprite` take `RenderPipelines.GUI_TEXTURED`; text colours need the alpha byte; the GUI pose is a 2D `Matrix3x2fStack`; input handlers take `MouseButtonEvent`, `KeyEvent`, `CharacterEvent`; the image size goes into the `AbstractContainerScreen` constructor.
- Fluids: sprites and tints come from the baked fluid model (`client/FluidSprites`).
- Block entity renderers implement `BlockEntityRenderer<T, S>` with `createRenderState`, `extractRenderState` and `submit` to a `SubmitNodeCollector` (1.21.9). Custom geometry uses `submitCustomGeometry`, whole block models `submitMultiLayerBlockModel` (`client/BlockSubmit`), standalone models (Sink fan, Burner Pump beam and rods) are `StandaloneModelKey`s registered in `ModelEvent.RegisterStandalone` (`client/ModelKeys`).
- Level overlays subscribe to `RenderLevelStageEvent.AfterTranslucentBlocks`; the camera comes from the level render state. The see through overlay type is a `RenderType` over an own `RenderPipeline` (no depth test) registered in `RegisterRenderPipelinesEvent`.
- Block models: `BakedModel` is gone. `UtilityDuctModel` and `WoodModel` are NeoForge `DynamicBlockStateModel`s reading `ModelData` from `level.getModelData(pos)` in `collectParts`; quads are rebuilt with NeoForge's `MutableQuad` (panel faces, data tier reskin, wood boards). The duct's part models are standalone `BlockStateModelPart`s. Render layers are now chosen per quad from the texture's transparency (26.1), so `render_type` in model JSON and the old render type sets are gone; see through panels are quads in the translucent layer with half alpha vertex colour.
- Block colours are `BlockTintSource` lists indexed by tint index; the duct registers one source per index up to its panel range.
- Item models: wood block items use an own item model type `servus_foundation:wood` (`WoodItemModel`, registered in `RegisterItemModelsEvent`) that retextures the boards per stack. `WoodSkin` names the board texture, model data property and item reader, so the Storage rack reuses `WoodModel` and `WoodItemModel` with its own skin (`servus_storage:rack_wood`). Shuttle items use the item model type `servus_storage:shuttle`, which bakes the part models under `models/shuttle/` and composes body, drive, Handling and tool per stack.
- Storage renderers extend `LiveBlockEntityRenderer`: the render state keeps the block entity and the old drawing code runs at submit time. Items go through `RenderSubmit.item` (item model resolver and `ItemStackRenderState`), text through `RenderSubmit.text` (`submitText`), and vertex code draws into a `RecordedGeometry` that is submitted as one custom geometry. This keeps the 1.21.1 drawing logic unchanged; a later cleanup can move reads into `extractRenderState`.
- Blocks lost their tooltip hook; blocks that add item tooltip lines implement `TooltipBlock`, which `WoodBlockItem` calls.

### D7. Datagen

NeoForge removed its `BlockStateProvider`/`ItemModelProvider` builders (1.21.4); vanilla's `ModelProvider` has no free form element builder. Foundation therefore ships its own small JSON writer in `datagen/model` (`BlockStateProvider`, `ItemModelProvider`, `ModelBuilder` with element, face, rotation and display builders, `ConfiguredModel`, variant and multipart builders with nested condition groups) with the same method names the model code used, so the model datagen of all three mods only changes its imports. It writes plain vanilla block and item models and, for every item model, the 1.21.4 item model definition under `assets/<mod>/items/` (wood block items get the `servus_foundation:wood` type). `GatherDataEvent.Client` registers every provider; recipes use `RecipeProvider.Runner` with `ShapedRecipeBuilder.shaped(items, ...)`; tag providers lost their file helper; the loot copy function is `copyComponentsFromBlockEntity(LootContextParams.BLOCK_ENTITY)`. Block items registered with `registerItem` add `useBlockDescriptionPrefix()` to keep the block's name.

### D8. Recipes and JEI

Recipes are no longer synced to clients (1.21.2). The recipe types the mods read on the client are sent with NeoForge's `OnDatapackSyncEvent.sendRecipes` and kept from `RecipesReceivedEvent` (`ServusRecipes.syncToClients`, `client/ClientRecipes`): crafting for the Storage pattern editor, the four alchemy types for the Coagulator screen, JEI and Jade. Lookups go through `ServusRecipes` (server recipe manager on a server level, the synced map on a client level). Recipe serializers are `RecipeSerializer` records of a map codec and a stream codec; the facade and tank recipes have no settings and use unit codecs over one instance. `WoodRecipe` has its own codec with the shaped recipe fields. Server lookups use `ServerLevel.recipeAccess()` through `production/ServusRecipes` (by id, crafting match, display result, grid ingredients), which reads the synced map on a client level; `SingleInputRecipes` reads ingredients from the placement info and gets results by assembling the recipe for each input. The JEI plugin builds its example recipes with `Recipe.CommonInfo`, `CraftingBookInfo`, `ItemStackTemplate` results and NeoForge's `DataComponentIngredient` for facade ingredients (JEI 29.43.0.107).

## Risks / Trade-offs

- Size: about 25 000 lines in Foundation alone touch nearly every changed API. The port is done area by area with a commit per area; Foundation may stay non compiling between commits on this branch.
- Behaviour drift in the transfer rewrite: handled by keeping the GameTests (item, fluid and energy networks are covered) and running them after the transfer task.
- Models are the least testable part: they need a client check by the user, which this run does not do.
- The other agent changes Foundation on main (M11 chunk loader team cap) in parallel. The user merges; conflicts will be in the files that both touch.

## Notes for the next run

- Manufacturing is ported (section 5): compile, 81 unit tests, 73 GameTests, datagen (the docs `gamedata.json` export comes out unchanged). Decisions made while porting:
  - Alchemy recipes hold their results as `ItemStackTemplate` (`resultTemplate`, `residueTemplate`, vein `yieldTemplate`) with `result()`, `residue()`, `yield()` making stacks: since 1.21.5 an `ItemStack` needs bound item components, which datagen and data reloads do not have. JSON field names stay. Sized ingredients use NeoForge's nested form `{"ingredient": "#c:bones", "count": 1}` (the flat codec is gone); the docs reader accepts both.
  - Modonomicon 2 removed advancement conditions. The journal declares a research node `servus_manufacturing:journal/villach` (`JournalResearch`) granted by the Villach chapter advancement; Schwaz and the Albedo entries require that node. The text offsets moved into the book theme layout.
  - Principle fluids get `FluidModel`s in `RegisterFluidModelsEvent` (water textures, principle tint); buckets use NeoForge's `neoforge:fluid_container` item model, written as raw item definitions by the model shim (`itemModelDefinition`).
  - Block removal hooks moved to `affectNeighborsAfterRemoval`; the Purifier drops its stock in `preRemoveSideEffects`. The Athanor checks its hearth on every neighbour update (the update no longer names its source).
  - Renderers that still draw into buffers extend `BufferedBlockEntityRenderer` (`SubmitBuffers`); the Elemental Sight reveal draws on `SubmitCustomGeometryEvent`.
  - The dev only icon exporter (`IconExporter`, `iconExport` run) is not ported: its off screen GL drawing has no 26.1 equivalent without a rewrite against the GPU device API; it logs and writes nothing.
- Known test issue: in the combined `runGameTestServerAll` (all three mods plus Modonomicon) five Storage tests that create mock server players fail, because Modonomicon 2.17 sends its book sync on player join to the mock connection, which has no negotiated channels. The per mod GameTest runs are green. `SiteWakeGameTests.recordKeepsNameSpanAndStock` failed once in about ten Storage runs (span size check); it looks like test placement across a chunk border, not a port change.
- First starts (2026-10-12): `runClientAll` reaches the title screen and `runServerAll` creates a fresh world and reaches "Done" with all three mods, with no errors from Servus code or data (all datapack JSON of the three mods loads, 1606 recipes in total). Two startup crashes were fixed on the way:
  - `HollowRevealRenderer` built a static `ItemStack` during client setup, before item components are bound ("Components not bound yet"). The stack is now built on first draw. No other static `ItemStack`, `Ingredient` or registry lookup in static initializers or setup events was found outside GameTest classes (those only run when the test server runs).
  - Jade 26.1 refuses data providers that also implement `IComponentProvider` (since Minecraft 1.21.6). The Manufacturing Jade plugin keeps its three `IServerDataProvider`s and registers a separate `IBlockComponentProvider` per data provider with the same uid that calls the tooltip code. Foundation and Storage have no Jade plugins.
- Warnings left at startup, none from Servus: vanilla command ambiguity warnings, Modonomicon's missing refmap (dev only), JarJar's `mezz_config` source jar notice, NeoForge's `item_*_unlit` shader sampler notice, the offline mode notice on the dedicated server and "Missing data pack tests" on the client's title screen datapack load.
- The worktree's `runs/server/server.properties` uses port 25567, since 25565 can be held by a server started from the main checkout.

- First play test fixes (2026-10-12):
  - GUI text: `GuiGraphicsExtractor.text` skips any colour whose alpha byte is zero, so every Storage screen that kept 1.21.1 RGB constants (`0x404040`) drew no text at all (Shuttle Workshop, Production Terminal, Warehouse Controller, Storage Terminal, stations, Elevator, Garage, Zone Label, Field Head). All text colour constants and inline literals in Storage screens now carry `0xFF`. Foundation and Manufacturing screens already did; world text goes through `RenderSubmit.text`, which adds the alpha itself. Fill colours were ARGB on 1.21.1 already and are unchanged.
  - Large items in GUIs: `graphics.item` under a scaled pose only stretches the 16 pixel slot icon from the GUI item atlas. The Shuttle Workshop draws its shuttle with an own picture in picture state (`GuiLargeItemRenderState`, `GuiLargeItemRenderer`, registered in `RegisterPictureInPictureRenderersEvent`, submitted with NeoForge's `GuiGraphicsExtractor.submitPictureInPictureRenderState`), rendered like vanilla's `OversizedItemRenderer` into a texture the size of the board.
  - Slot icons are rendered into a 16 pixel atlas cell with a scissor, so model parts outside the slot are cut (the Harvester's cutter bar and the Spreader reach past it). Shuttle item definitions set `"oversized_in_gui": true` (`ModelProvider.oversizedInGui`), which sends vanilla through its oversized item renderer for those stacks.
  - Field Head problems: a new block state property `problem` draws the bar full and red (`field_head_front_problem`). `FieldHeadLook.problem` is the pure rule, the same one the GUI tab markers use (`FieldWork.problem`, `FertilizeStatus.problem`, not serving, dry, corroded). The head sets it whenever it updates its bar and, since warehouse changes (controller, shuttles, the way) reach it through no event, once every 20 ticks per head (staggered by position); that check reads cached warehouse state and one rail path search per head, no blocks.

- Storage is ported (section 4): compile, 457 unit tests, 363 GameTests, datagen. The pattern editor preview reads crafting recipes on the client from the synced crafting recipes (see D8). The terminal's dynamic height no longer changes `imageHeight` (final in 26.1); `init` centres the panel from the layout instead.
- Saved data that still saves to a compound tag uses `ValueIo.tagSavedData` (a `SavedDataType` over `CompoundTag.CODEC`); the file names stay the same.

- Foundation is fully ported (section 2 of tasks.md). Rendering, GUIs and models compile and data generates, but nothing has been seen in game: section 3 is the user's play check.
- The porting scripts used for Foundation (renames, registration, value IO, transfer bridges, GUI, GameTests, datagen imports) are kept in `C:/Users/Dirnei/AppData/Local/Temp/servus-helpers/port/`; run them on a module's sources first, then fix the rest by compile errors.
