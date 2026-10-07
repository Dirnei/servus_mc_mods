# Design

## Context

Logistics has `UtilityDuctBlock` (a `ConnectableBlock` with radius 4 and six connection flags), `UtilityDuctBlockEntity` (an `EnumSet<Channel>` of carried channels, `SideMode` per channel per side, nine filter slots and a priority per side, a facade), `ItemChannelNetwork` and `EnergyChannelNetwork` on the Lib network manager, `UtilityDuctModel` (a dynamic baked model that swaps in the facade block's model), and a six side overview GUI (`UtilityDuctMenu`, `UtilityDuctScreen`, menu buttons by id, no custom payloads). Models come from `ConnectableModels` in Lib datagen: one centre and one arm model, multipart on the six flags. Manufacturing has its own `duct` and `cable` packages with four blocks, used by the reference block GameTest and by two assembling recipes gated by `splitter_filter_ducts`. Manufacturing's mods.toml template depends on `servus_lib` only; the root `build.gradle` expands `lib_version_range` into the templates and already runs all four mods together in `runClientAll` and the GameTest server. See proposal.md for motivation.

## Goals / Non-Goals

**Goals:**
- One duct family. Manufacturing compiles against Logistics and uses the Utility Duct.
- The look of a duct is computed from block entity data through model data, no block state explosion.
- Channels are a per line per side byte on the duct; networks group endpoints by channel.
- The GUI is opened for one side only, from the carrot click on that side's arm.

**Non-Goals:**
- A third line. Sizes and part models are generated for one and two lines; a third size is a datagen change later.
- Changing Lib's `ConnectableBlock`: rails and other connectables keep their fixed radius.

## Decisions

### D1: Manufacturing depends on Logistics
`manufacturing/build.gradle` adds `compileOnly project(':logistics')`, `testImplementation project(':logistics')` and puts the Logistics mod into `loadedMods` next to Lib. `manufacturing/src/main/templates/META-INF/neoforge.mods.toml` adds a required `servus_logistics` dependency with `ordering="AFTER"`, and the root `build.gradle` adds `logistics_version_range` next to `lib_version_range`. PLAN.md section 0, CLAUDE.md rules and `openspec/config.yaml` get the new rule: Storage and Logistics depend on Lib only, Manufacturing on Lib and Logistics. Alternative: move the Utility Duct into Lib. Rejected: Lib stays API and plumbing, the duct is content.

### D2: `Channel` becomes `Line`, channel means routing group
The enum is renamed to `Line` with `ITEM` and `ENERGY`, ids `item` and `energy` stay so the saved `Channels` list only changes its tag name to `Lines`. Items are registered as `item_pipe` and `power_cable` (`LineItem`), lang Item Pipe and Power Cable. A channel is an `int` 0 to 7 shown as 1 to 8, stored per line per side in `byte[Line][6]`, saved under `Channels` as a compound `line -> side -> int` like `Modes`, default 0 omitted. Channel colors are a fixed table in `DuctChannels` (pure, Lib free): white, orange, magenta, light blue, yellow, lime, pink, cyan as ARGB ints, plus the lang keys.

### D3: Networks route per channel
`ItemChannelNetwork.Endpoint` gains `channel`. Output groups become `Map<channel, List<List<Endpoint>>>` (per channel, priority groups highest first) with cursors keyed by channel and priority. `route(stack, origin, channel, simulate)` only looks at the group of that channel; `DuctItemHandler.insertItem` passes the inserting side's channel; `pull` passes the source side's channel. `EnergyChannelNetwork.onTick` runs its water filling once per channel over the endpoints of that channel. The pure part of the item routing (which endpoint gets how many, round robin and priorities within a channel) is lifted into `logistics/routing/RoundRobinPlan.java` with no Minecraft imports and a JUnit test; the network feeds it capacities from simulated inserts. Alternative: one network per channel through the Lib graph. Rejected: the graph is about which ducts touch, channels are about which endpoints talk, and a side can change channel without re-joining anything.

### D4: Line connections and endpoints live on the block entity
`UtilityDuctBlockEntity` computes `arms(Line)` as a six bit mask: line present, side mode not disabled, neighbour is a duct carrying the line or a block exposing `Capabilities.ItemHandler.BLOCK` or `Capabilities.EnergyStorage.BLOCK` on the facing side. `endpoints()` is the six bit mask of sides whose neighbour is not a duct and has at least one arm. The block state's six flags stay the union over lines, kept by the existing `refreshConnections`, so Lib's `ConnectableBlock` shape cache and the overlay keep working. Recomputed on neighbour change, line change and mode change, then `sendBlockUpdated` so the client gets new model data.

### D5: Dynamic model composed from part models
`DuctModelData` gains `LINES` (bitmask of present lines), `ARMS` (int array per line), `ENDPOINTS` and `CHANNELS` (per line per side). Datagen writes part models under `models/block/duct/`: `center_item`, `center_energy`, `center_both`, and per direction `arm_<dir>_item`, `arm_<dir>_energy`, `arm_<dir>_both`, each with the ring element as separate faces that carry `tintindex`. Thin parts use radius 2, both line parts radius 3; the both line skin texture carries two lengthwise bands (item color, energy color) and the ring element uses one white ring texture. Parts are registered with `ModelEvent.RegisterAdditional#register(ModelResourceLocation.standalone(id))` (verified in `net/neoforged/neoforge/client/event/ModelEvent.java`) and fetched with `Minecraft.getInstance().getModelManager().getModel(ModelResourceLocation)` (verified in the 1.21.1 sources). `UtilityDuctModel.getQuads` keeps the facade branch and otherwise concatenates the centre part for the line set and, per direction, the arm part for the lines connected there; ring quads are included only when the side is an endpoint. The blockstate file shrinks to one variant pointing at the thin centre so the wrapper always has a base model; `ConnectableModels` in Lib stays for the other blocks. Tint: ring faces get `tintindex = line * 6 + side`; a `BlockColor` registered with `RegisterColorHandlersEvent.Block#register` (verified) reads the duct from `BlockAndTintGetter.getBlockEntity(pos)` and returns the channel color of that line and side. Vanilla's `ModelBlockRenderer` asks `BlockColors` for every quad with a tint index, so no custom renderer is needed. Alternative: block state booleans per line per side. Rejected: fourteen booleans give sixteen thousand states.

### D6: Shape follows the lines
`UtilityDuctBlock.getShape` overrides the Lib shape: centre radius 2 for one line, 3 for two; per direction the arm of the thickest connected line set; cached in a map keyed by (lines mask, arm masks); facade keeps `Shapes.block()`. The collision shape is the same shape.

### D7: Carrot opens the endpoint GUI
`UtilityDuctBlock.onWrench` without sneaking: take `context.getClickLocation()` minus the block position, test it against the six arm boxes of the current shape in order, and if the hit arm's side is an endpoint, `player.openMenu(duct, buf -> buf.writeBlockPos(pos).writeEnum(side))` through `IMenuProviderExtension` style extra data (`openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)` on `ServerPlayer`, as the Storage menus already do). A hit on the centre or on a duct to duct arm returns `InteractionResult.PASS`. Sneaking keeps today's facade and dismantle path. `useWithoutItem` returns `PASS`. Alternative: raytrace sub shapes server side with `Shapes` per part. The box test on the click location is enough because the click location is on the shape that was hit.

### D8: Endpoint menu and screen
`UtilityDuctMenu` takes the fixed side; its `fromNetwork` reads position and side. Button ids: mode per line (`10 + line`), channel per line (`40 + line * 8 + channel`), remove per line (`20 + line`), priority down and up (30, 31). Side selection buttons go away. `UtilityDuctScreen` draws the title "Utility Duct, West: Chest", one row per present line with an icon of the line item, the mode button, eight small square channel buttons filled with the channel colors and a frame on the current one, and the remove button; the item row adds the nine ghost filter slots and the priority buttons; the energy row is one line high. GUI texture reworked for the two row layout.

### D9: Manufacturing GameTests on the Utility Duct
A test helper `duct(helper, pos, Line...)` places `ServusLogisticsBlocks.UTILITY_DUCT`, then adds lines on the block entity and calls `Block.updateFromNeighbourShapes` as the GameTest note in the project memory prescribes. The reference block test uses item ducts on channel 1 with default modes, which routes exactly like the removed Item Duct. Assemblers still touch their generators, no cable needed.

### D10: Techs
`splitter_filter_ducts.json` is deleted, `logistics.json` lists `crusher` as its only parent, and the research spec scenario names the remaining techs. The assembling recipes for splitter and filter ducts go with their items.

## NeoForge APIs

- `ModelEvent.RegisterAdditional`, `ModelEvent.ModifyBakingResult`, `IDynamicBakedModel`, `ModelData`, `ModelProperty` (`net.neoforged.neoforge.client.model`).
- `RegisterColorHandlersEvent.Block` and vanilla `BlockColor`.
- `ModelResourceLocation.standalone`, `ModelManager.getModel`.
- `Capabilities.ItemHandler.BLOCK`, `Capabilities.EnergyStorage.BLOCK`, `BlockCapabilityCache`.
- `ServerPlayer.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)` and `IMenuTypeExtension.create` for the menu with extra data.
- Mod metadata dependency entries in `neoforge.mods.toml`.

## Pure versus Minecraft

- Pure Java, JUnit: `RoundRobinPlan` (per channel priority groups and cursors), `DuctChannels` (color table and bounds), `ArmHit` (which arm box a local click point falls in, given radius and arm masks).
- Minecraft, GameTest in Logistics: item routing on channels, pull in with filter, line add and drop, model data masks after neighbour changes (server side `arms` and `endpoints`).
- Minecraft, GameTest in Manufacturing: the reference block on Utility Ducts, and energy per channel, because only Manufacturing has blocks with an energy storage (Logistics depends on Lib alone).
- Manual: thickness and colors, channel rings, GUI layout, facade on a two line duct, carrot on centre does nothing.

## PLAN.md open decisions

None of section 5. The change settles the dependency rule of section 0 and rewrites section 2.2 bullets 2, 6 and 7 (modes set in the endpoint GUI, dynamic model from part models, no per part selection shapes) and removes the Item Duct and Power Cable rows and bullet from section 2.1.

## Risks / Trade-offs

- [Model data refresh lag: the client shows old arms after a neighbour change] → every recompute calls `sendBlockUpdated` and the client side `loadAdditional` already requests a model data update and re-renders.
- [Tint index collision with the facade model's own tinted quads] → the facade branch returns the facade's quads untouched and the `BlockColor` delegates to the facade block's color for tint indices below 100 when a facade is present; ring indices are offset by 100.
- [Click location is on a face shared by centre and arm] → test arms first, centre last, so an endpoint arm wins.
- [Manufacturing GameTests now need Logistics on the classpath] → `runGameTestServer` already loads all four mods from the root build.
- [Renaming the items breaks existing dev worlds] → accepted, nothing is released.
