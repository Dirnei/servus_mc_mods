# Design

## Context

See proposal.md (Why). Current state on `main` (Minecraft 26.1.2, NeoForge 26.1.2.116):

- `shuttle/module/ShuttleWorkshopBlock` extends `BaseEntityBlock`, has no block state properties and opens `ShuttleWorkshopMenu` on right click. It does not implement Foundation's `item/Wrenchable`, so the carrot does nothing.
- `shuttle/module/ShuttleWorkshopBlockEntity` holds `shuttle` (1 slot) and `supplies` (`SUPPLY_SLOTS` = 6) as `ItemSlots`, plus `handBuild` and `buildStep` for the build minigame; `preRemoveSideEffects` calls `dropContents()`.
- Datagen: `ServusStorageBlockStateProvider` builds `models().cubeBottomTop("shuttle_workshop", side, bottom, top)` and `simpleBlock`; `ServusStorageBlockLoot` lists the workshop in `SELF_DROPPING`; `ServusStorageRecipeProvider` saves the recipe (`III`, `PCP`, `PPP` with iron, crafting table, `ItemTags.PLANKS`) to the plain output.
- The wood blocks: `Wood.register(id)` in `ServusStorage` marks the block for the wood model loader; the block entity implements `WoodHolder` with a `WoodSlot` (save, load, `applyImplicitComponents`, `collectImplicitComponents`, `removeComponentsFromTag`, `getModelData`), the loot copies `WoodContent.WOOD` (`ServusStorageBlockLoot.WOODEN`), recipes save through `WoodRecipes.output(output, registries)`, models are built with Foundation's `datagen/LayeredModels` (board quads with `Wood.BOARD` under overlay quads). `ProductionTerminalBlockEntity` and `PatternStorageBlock` are the closest references (the latter dismantles into an item carrying its contents through `collectComponents()`).
- The family textures (for example `crafting_station_side.png`) are overlays: a dark frame (palette around `#2e2d31`, `#3e3d41`, light corner pixels `#5c5b5f`) with transparent panels where the board shows; sides carry a horizontal iron crossbar at rows 7 and 8.

## Goals / Non-Goals

**Goals:**
- The workshop is a wood block like the stations: recipe wood, wood on the boards, wood kept on break and dismantle.
- Carrot dismantle keeps the shuttle and the supplies in the item.
- A look of its own inside the family, without network marks.

**Non-Goals:**
- Facing, a renderer for the held shuttle, GUI changes (see proposal).

## Decisions

### D1. Layered cube without facing

`LayeredModels.cube(this, "shuttle_workshop", faces)` with `NORTH`, `SOUTH`, `EAST`, `WEST` = `block/shuttle_workshop_side`, `UP` = `block/shuttle_workshop_top`, `DOWN` = `block/shuttle_workshop_bottom`; `simpleBlock` and `simpleBlockItem` with that model. No `FACING` property: the four sides are the same, so a facing would add block states with nothing to show. `Wood.register(ServusStorageBlocks.SHUTTLE_WORKSHOP.getId())` joins the list in `ServusStorage`, so block and item models show the wood.

Alternative: a facing with tools only on the front (rejected by the user in explore).

### D2. Textures as overlays, drawn by script

The three PNGs are 16x16 overlays like the family: frame pixels opaque, board panels transparent.
- Side: the frame of `crafting_station_top.png` (no crossbar), a wrench hanging diagonally from a hook at the upper left (steel `#96989e` with a darker jaw), a screwdriver hanging on the right with a steel shaft and a yellow handle (`#d6a828`), hooks as single frame coloured pixels.
- Top: the frame with a steel bench plate filling the panel (`#6e7076`, a lighter top edge), an engraved shuttle outline in the frame's dark colour, and a vise at the lower edge.
- Bottom: the plain frame of `crafting_station_top.png`.
The script lives in the job dir (like the earlier `gen_*.py` texture generators) and is not part of the build. The concept sketches from explore are the starting point; the final pixels are cleaned up (consistent outlines, one highlight per steel part).

### D3. Wood on the block entity

`ShuttleWorkshopBlockEntity implements WoodHolder` with a `WoodSlot`, wired exactly like `ProductionTerminalBlockEntity`: save and load, implicit components, `removeComponentsFromTag`, `getModelData`. Loot moves the workshop from `SELF_DROPPING` to `WOODEN`. The recipe saves through `WoodRecipes.output(output, registries)`, which records the most frequent planks (five planks in the grid).

### D4. Dismantle keeps the contents in the item

`ShuttleWorkshopBlock implements Wrenchable`; `onWrench` passes without sneaking. While sneaking, on the server: build `new ItemStack(this)`, apply `workshop.collectComponents()` (wood plus contents), clear both handlers, then `level.removeBlock(pos, false)`, so `preRemoveSideEffects` finds nothing left to drop.

Contents travel as vanilla `DataComponents.CONTAINER` (`ItemContainerContents.fromItems(list)`, 7 entries: slot 0 the shuttle, slots 1 to 6 the supplies; restored with `copyInto(NonNullList)`), set in `collectImplicitComponents` only when a slot is not empty and read in `applyImplicitComponents`. `ItemContainerContents` implements `TooltipProvider` in 26.1.2 (checked with `javap` on `build/moddev/artifacts/minecraft-patched-26.1.2.116-merged.jar`), so the item tooltip lists what it holds like a shulker box. `removeComponentsFromTag` removes the container data from the saved tag so it is not stored twice. Breaking by hand keeps the current behaviour: the loot table copies only the wood, and `preRemoveSideEffects` drops the contents as items.

`handBuild` and `buildStep` are not carried: a dismantled workshop comes back with no build running, the shuttle at its current tier.

Alternative: a custom data component for the workshop contents (no gain over the vanilla container, which also brings the tooltip).

### D5. Tests

No pure logic is added, so no JUnit tests. GameTests:
- `BlockStyleGameTests.workshopRecipeRecordsAcacia` (five acacia planks give an acacia workshop).
- `ShuttleModuleGameTests.workshopDismantleKeepsContents` (spruce workshop with a Mk2 shuttle with Speed 1 and 8 iron ingots; sneak carrot; exactly one item drops; placed again via `BlockItem.updateCustomBlockEntityTag` pattern from earlier dismantle tests, the slots hold the shuttle and the ingots and the wood is spruce).
- `ShuttleModuleGameTests.workshopDismantleCancelsHandBuild`, `workshopPlainCarrotDoesNothing`.
- The existing "breaking drops the contents" test stays green.
The look is checked in game.

PLAN.md open decisions settled: none.

## Risks / Trade-offs

- [Item dupes] A dismantle that copied the contents into the item but still dropped them. → Clear both handlers before `removeBlock`; the GameTest counts dropped item entities.
- [Big item NBT] A workshop item carries a shuttle with its components. → Seven stacks at most, like a small shulker box.
- [Pixel art by script] Script drawn textures can look flat. → Clean up after the in game check; the play check task covers it.
