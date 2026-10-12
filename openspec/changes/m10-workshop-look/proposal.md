# Proposal

## Why

The Shuttle Workshop still wears its first look: light grey metal with rivets, crosses on the sides and a gold window on top, over fixed oak boards. Next to the Shuttle Garage, the stations and the terminals, which all share a dark iron frame over boards in the wood of their recipe, it looks like a block of another mod. It also behaves differently: it does not remember its wood and the Configuration Carrot cannot dismantle it. The workshop is not part of the warehouse network, so it should belong to the family and still be told apart at a glance.

## What Changes

This change implements a follow-up to PLAN.md milestone **M10 Field work**, item **Shuttle Workshop** (section 2.3), added to the M10 list as item 12 `m10-workshop-look`. It follows the decisions taken in `/opsx:explore` on 2026-10-12 (memory note "workshop-look-decisions").

- **Family look, workbench style (design "A").** The workshop is drawn like the other wood blocks: the dark iron frame over wooden board panels in its wood. All four sides are the same: frame and boards without the iron crossbar of the stations, with a wrench and a yellow-handled screwdriver hanging on the boards. The top is a steel bench plate with an engraved shuttle outline and a vise at its edge. The bottom is the plain family top.
- **Unique because it is not on the network.** No connection lamp, no dark recessed port and no teal, which mark networked blocks; the yellow tool handles are its accent.
- **Wood from the recipe.** The workshop recipe accepts any planks and records their wood like the stations; the placed block keeps it, shows it on its boards and drops it.
- **Carrot dismantle.** Sneaking with the Configuration Carrot dismantles the workshop. The dropped item carries the wood, the shuttle and the supplies; placing it again restores them. A hand build in progress is cancelled. Breaking the block by hand still drops the shuttle and the supplies as items.
- **No facing.** The four sides are the same, so the block keeps no facing.
- `PLAN.md` M10 lists the change as item 12; the docs page names the wood and the carrot dismantle.

## Capabilities

### New Capabilities
- none

### Modified Capabilities
- `storage/shuttle-workshop`: the workshop block keeps its wood, is dismantled with the carrot keeping its contents, and has its own look without network marks.
- `foundation/wood-material`: the Shuttle Workshop joins the wood blocks (recipe records the wood) and the look requirement allows a non-networked block without teal.

## Non-goals

- A facing or a front for the workshop (decided: the sides are the same).
- Drawing the held shuttle on the block (a renderer); the GUI already shows it large.
- Changing the GUI, the build minigame or the recipe ingredients beyond accepting any planks.
- New looks for other blocks.

## Impact

- Code, Storage: `shuttle/module/ShuttleWorkshopBlock` (Wrenchable dismantle), `shuttle/module/ShuttleWorkshopBlockEntity` (WoodHolder, wood slot, model data, contents as item components), `ServusStorage` (`Wood.register`), datagen (`ServusStorageBlockStateProvider` layered cube model, `ServusStorageBlockLoot` WOODEN list, `ServusStorageRecipeProvider` wood recipe output), GameTests in `BlockStyleGameTests` and `ShuttleModuleGameTests`.
- Assets: `textures/block/shuttle_workshop_side.png`, `shuttle_workshop_top.png`, `shuttle_workshop_bottom.png` redrawn as overlays with transparent board panels.
- Docs: `docs/src/content/docs/mods/storage.md`; `PLAN.md` M10.
