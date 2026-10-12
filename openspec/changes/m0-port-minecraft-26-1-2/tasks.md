# Tasks

Work happens on branch `feature/port-26.1.2`. Foundation may not compile between the commits of section 2; every task ends with the build step that applies once section 2 compiles.

## 1. Tooling and plan

- [x] 1.1 `gradle.properties`: Minecraft 26.1.2, NeoForge 26.1.2.116, JEI 29.43.0.107, Parchment lines removed, Gradle heap 4 GB. `build.gradle`: Java toolchain 25, no `parchment` block, `data` run as `clientData()`, `-Xmaxerrs 10000`, combined runs and dev only mods only for included projects. `settings.gradle`: only `foundation` included, Manufacturing and Storage commented out with a pointer to sections 4 and 5 (design D1). Verify: `gradlew :foundation:compileJava` sets up 26.1.2 and the Java 25 toolchain (compile errors expected).
- [x] 1.2 `CLAUDE.md` Stack and `PLAN.md` section 1 (and the intro line) name Minecraft 26.1.2, NeoForge 26.1.2.x, Java 25, Mojang names, the transfer capabilities; `openspec/config.yaml` context line likewise. Verify: `openspec validate m0-port-minecraft-26-1-2`.

## 2. Foundation

- [x] 2.1 Mechanical renames across `foundation/src` (design D2): `Identifier`, `EnumProperty<Direction>`, `InteractionResult`, `ChunkPos` factories, NBT `Optional` getters, `isClientSide()`. Verify: error count of `gradlew :foundation:compileJava` drops, no new kinds of errors.
- [x] 2.2 Registration (design D2): ids in block and item properties for every `DeferredRegister` in `registry/`, `logistics/registry/`, `machine/`, `wood/`; `new BlockEntityType<>(...)`; block items with `useBlockDescriptionPrefix()`; menus and creative tabs. Verify: compile errors in the registry classes gone.
- [x] 2.3 Blocks and block entities (design D3): `ValueInput`/`ValueOutput` persistence in every block entity, `preRemoveSideEffects`/`affectNeighborsAfterRemoval` instead of `onRemove`, new `updateShape`/`neighborChanged` signatures, `useItemOn`/`useWithoutItem` results, update packets. Verify: compile errors in `block/`, `machine/`, `logistics/duct`, `logistics/tank`, `adapter/` gone apart from transfer and client code.
- [x] 2.4 Transfer API (design D4): Foundation inventories, tanks and energy buffers on `ItemStacksResourceHandler`, `FluidStacksResourceHandler`, `SimpleEnergyHandler`; side wrappers; capability registration with `Capabilities.Item/Fluid/Energy.BLOCK`; network code (`logistics/network`, `fluid/`, `energy/`, `data/`, `adapter/`, `machine/sink`, `machine/pump`) with transactions; menus with `ResourceHandlerSlot`. Verify: compile errors in those packages gone.
- [x] 2.5 Data reload listeners, payloads, commands, config, chunk tickets (`config/`, `data/`, `command/`, `machine/loader`, `upgrade/`, `team/`). Verify: compile errors there gone.
- [x] 2.6 Recipes and JEI (design D8): `FacadeRecipe`, `HollowFacadeRecipe`, other `CustomRecipe`s with serializers, server recipe lookups through `recipeAccess()`, client recipe pickers, `integration/jei`. Verify: compile errors there gone.
- [x] 2.7 Screens and menus (design D6): every `Screen`/`AbstractContainerScreen` and widget in `client/`, `logistics/client/`, `machine/menu`, `adapter/` on `GuiGraphicsExtractor`. Verify: compile errors there gone.
- [x] 2.8 Block entity renderers and world overlays (design D6): `FluidTankRenderer`, pump renderers, `SideMarkerRenderer`, `DuctOverlayRenderer`, `PlacementAssistRenderer`, goggles overlay. Verify: compile errors there gone.
- [x] 2.9 Block models (design D6): `UtilityDuctModel`, `PanelQuads`, `WoodModel`, block colours, model registration. Verify: `gradlew :foundation:compileJava` green.
- [x] 2.10 Datagen (design D7): block state, block model and item model providers, recipes, loot, tags, lang under `datagen/` and `logistics/datagen/`; `gradlew :foundation:runData` regenerates `foundation/src/generated/resources`. Verify: `runData` green and the generated diff reviewed (format changes only).
- [x] 2.11 GameTests (design D5): `@ServusGameTest` and the `ServusGameTests` registrar, all Foundation test classes switched, `GameTestHelper` call changes. Verify: `gradlew :foundation:runGameTestServer` green with all tests registered (97 or more).
- [x] 2.12 Unit tests in `foundation/src/test`: compile and pass. Verify: `gradlew :foundation:test` green.
- [x] 2.13 Foundation full build. Verify: `gradlew :foundation:build` green.

Results of section 2 (2026-10-12): `:foundation:build` green, 263 unit tests green, `:foundation:runGameTestServer` 98 of 98 required tests passed (97 Foundation tests plus vanilla's `always_pass`), `:foundation:runData` green with format only diffs in the generated resources.

## 3. Foundation play check

- [ ] 3.1 The user launches the client from the port branch with Foundation alone: ducts with panels and facades, tank rendering, wood blocks, goggles and carrot overlays, the duct, machine, sink and adapter GUIs. Verify: the user confirms in game.

## 4. Storage

- [x] 4.1 `settings.gradle` includes `storage`; `storage/build.gradle` JEI 26.1.2 API. Verify: `gradlew :storage:compileJava` sets up.
- [x] 4.2 Port Storage by the same areas as section 2 (renames, registration, block entities, transfer, menus and screens, renderers and shuttle models, datagen, GameTests, unit tests). Verify: `gradlew :storage:build`, `:storage:test`, `:storage:runGameTestServer` green.
  Result: `:storage:build` green, 457 unit tests pass, `:storage:runGameTestServer` passes all 461 required tests (363 Storage, 97 Foundation, 1 vanilla), `:storage:runData` regenerates the resources.
- [ ] 4.3 The user checks Storage in game: crate, rack (wood on block and item), shuttle look on rails and in inventories, elevator, field head water and crop, terminal screens and their dynamic height, JEI transfer. Verify: the user confirms in game.

## 5. Manufacturing

- [x] 5.1 `settings.gradle` includes `manufacturing`; Modonomicon, Jade and FTB versions for 26.1.2 in `gradle.properties` (or dropped from the dev runs if none exist); root combined runs and their dev only mods back on. Verify: `gradlew :manufacturing:compileJava` sets up.
- [x] 5.2 Port Manufacturing by the same areas as section 2, plus the Modonomicon book and the multiblocks. Verify: `gradlew :manufacturing:build`, `:manufacturing:test`, `:manufacturing:runGameTestServer` green.
  Result: `gradlew build` green (263 Foundation, 457 Storage, 81 Manufacturing unit tests), `:manufacturing:runGameTestServer` passes all 171 required tests (73 Manufacturing, 97 Foundation, 1 vanilla), `:manufacturing:runData` regenerates the resources and the docs game data. Not ported: the dev only icon exporter (see design.md notes).
- [ ] 5.3 The user checks Manufacturing in game: the four builds forming with the journal, principle fluids and buckets, renderers, the Coagulator and Gnome Hollow screens, Elemental Sight, the journal book with the Villach research unlock, JEI categories and Jade. Verify: the user confirms in game.

## 6. Integration

- [ ] 6.1 Full gradle build, tests green, game launches with all three mods.
  Status: `gradlew build` green with all three mods; `runGameTestServerAll` passes 529 of 534 (five Storage mock player tests fail on Modonomicon's join sync, see design.md notes). The client launch is the user's check.
