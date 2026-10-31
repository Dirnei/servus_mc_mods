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

## 5. Play check

- [ ] 5.1 User play check: place stops at Warehouse Rail and Farm Rail ends, turn and dismantle them, look for flicker on every part from several angles and with a shuttle parked and a Harvester at the last rail, check the stub wood matches the rail.
