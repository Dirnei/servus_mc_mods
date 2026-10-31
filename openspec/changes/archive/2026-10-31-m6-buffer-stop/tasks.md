# Tasks

## 1. Pure logic

- [x] 1.1 `shuttles/.../rail/RailLook`: add `Part.FRAME`. New `shuttles/.../rail/BufferStopLook` (no Minecraft imports): `boxes()` returns the unrotated boxes of the design table (stub sleepers and heads, sole, uprights, two brace steps, beam, buffer heads) as `RailLook.Box`, front facing north; `boxes(rotation)` turns them with `RailLook.rotated`. JUnit `BufferStopLookTest`: every box inside 0..16; no two boxes share volume; no two same facing faces lie in one plane over a shared area (for every rotation); stub heads sit on `RailLook.HEADS` and end where the sole starts; buffer heads sit in front of the beam and in front of each rail head line. Verify `gradlew :shuttles:test`

## 2. Block

- [x] 2.1 New `shuttles/.../rail/BufferStopBlockEntity` holding a `MaterialSlot` (`MaterialHolder`): save, load, collect and apply the material component like `ElevatorBlockEntity`. New `shuttles/.../rail/BufferStopBlock`: `BaseEntityBlock` with horizontal `FACING`, codec, shape from the design, `getStateForPlacement` with `RailFacings.frontToRail(context, RailFacings.ANY_RAIL)`, `canSurvive` on `canSupportRigidBlock` below, `updateShape` drops it when the floor goes, `Wrenchable.onWrench` turns it clockwise or dismantles it on sneak keeping the wood. Registries: block `buffer_stop` (metal, `noOcclusion`), block entity, block item that shows the wood, creative tab right after `RAIL`; `ServusShuttles` registers it as a wood material block. Verify `gradlew build`
- [x] 2.2 `shuttles/.../rail/RailBlock.connectsTo`: true for a `BufferStopBlock` neighbour whose `FACING` points at the rail; dock flags unchanged. Javadoc of `RailModels`/`RailBlock` mention the stop where they list connected sides. Verify `gradlew build`

## 3. Datagen

- [x] 3.1 New `shuttles/.../datagen/BufferStopModels`: one block model from `BufferStopLook.boxes()` (textures: `heavy_rack_iron` for `FRAME`, `rail_dock` for `DOCK`, `rail_head` for `HEAD`, wood placeholder for `SLEEPER`, every face textured), a blockstate with a y rotation per `FACING`, and an item model with GUI, fixed and ground transforms. Call it from `ServusShuttlesBlockStateProvider`. `ServusShuttlesBlockLoot`: drop itself copying `MATERIAL`. `ServusShuttlesRecipeProvider`: shaped `III` over ` R ` (iron ingot, Warehouse Rail) to one Buffer Stop through `MaterialRecipes.output(..., MaterialKind.WOOD)`. `ServusShuttlesLanguageProvider`: "Buffer Stop". Pickaxe tag if the rail has one. Run `gradlew :shuttles:runData`; verify `gradlew build`

## 4. GameTests

- [x] 4.1 New `shuttles/.../gametest/BufferStopGameTests`: placed beside the last rail faces it; sneak placement faces the player; faces a Farm Rail; the rail end shows straight with the stop facing it and END again when the stop is broken or turned away with the carrot; carrot turns it clockwise; sneak carrot dismantles it with its wood; floor removed drops it with its wood; a hung rail beside only a stop is not held; controller, three rails and a stop: the network holds three rails and no node at the stop; a field ending at a stop has the same rail count as without it; the recipe result carries the rail's wood. Verify `gradlew build` and the full GameTest run

## 5. Buffer Stop remembers its rail

- [x] 5.1 (superseded by 5.4) second block `farm_buffer_stop` with copper heads and its recipe.
- [x] 5.2 (superseded by 5.5) GameTests for the second block.
- [x] 5.3 (moved to `m10-rail-corrosion-per-rail`, which owns the component) On main once `m10-rail-corrosion-per-rail` is there: `BufferStopGameTests` case where a Farm Rail with a `RAIL_CORROSION` stage past copper still crafts a Buffer Stop with `rail=farm` and no corrosion component. Verify `gradlew build` and the full GameTest run
- [x] 5.4 Remove `farm_buffer_stop` (block, item, creative tab entry, material registration, loot, tag, lang, models, recipe, generated files). `shuttles/.../rail/BufferStopBlock`: `rail` enum property (`warehouse`, `farm`), default warehouse. `BufferStopModels`: a block model per `rail` value (head `rail_head` or `farm_rail_head`, dock `rail_dock` for both), blockstate by facing and rail, item model selecting on the stack's `rail` block state over the material item model per kind. Loot copies `MATERIAL` and the `rail` state. Recipes: the Warehouse Rail gives the default, the Farm Rail gives a result with `block_state {rail: farm}`. Item tooltip names the rail; lang key. Run `gradlew :shuttles:runData`; verify `gradlew build`
- [x] 5.5 `BufferStopGameTests`: replace the farm block tests: a stop with `rail=farm` faces and connects a Farm Rail and a Warehouse Rail; the Farm Rail recipe gives `rail=farm` and the rail's wood, the Warehouse Rail recipe gives `rail=warehouse`; breaking a farm stop drops an item with `rail=farm` and its wood, and placing that item gives `rail=farm` again; aging a field ending at a farm stop leaves the stop and the field's rail count unchanged. Verify `gradlew build` and the full GameTest run

## 6. Play check

- [x] 6.1 User play check: place stops crafted from both rails at Warehouse Rail and Farm Rail ends, turn and dismantle them, look for flicker on every part from several angles and with a shuttle parked and a Harvester at the last rail, check the stub wood matches the rail and a stop from a Farm Rail shows copper heads, in hand and placed, under a yellow and black beam.
