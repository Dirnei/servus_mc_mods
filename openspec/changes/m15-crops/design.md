## Context

See proposal.md for the why. `m15-brewery-project` (applied, not archived) left `:brewery` with a mod class, the creative tab with the Beer Glass, datagen for lang and item models, `tools/brewery-textures/gen.py` and smoke GameTests. This change adds the first blocks of the mod. Sources checked: `minecraft-patched-26.1.2.116-sources.jar` and `neoforge-26.1.2.116-sources.jar`.

## Goals / Non-Goals

**Goals:** barley that behaves exactly like a vanilla age crop (so Shuttles fields take it), the Hop Pole and a hop vine with simple local growth rules, data driven drops and first sources, tags and composting, textures and models in the family style, JUnit tests for the vine rules and GameTests for both crops.

**Non-Goals:** see proposal.md, Non-goals.

## Decisions

### Barley is a vanilla `CropBlock`
`BarleyBlock extends CropBlock` and only overrides `getBaseSeedId()` (pick block gives Barley Seeds). Properties copy vanilla wheat (`noCollision`, `randomTicks`, `instabreak`, `SoundType.CROP`, `PushReaction.DESTROY`, map colour yellow from age 6). Barley Seeds is `new BlockItem(BARLEY, properties.useItemDescriptionPrefix())`, the vanilla `createBlockItemWithCustomItemName` pattern, so the item is named "Barley Seeds" and not after the block. Growth, light, farmland, neighbour slowdown, bone meal (2 to 5 stages) and Ravager trampling are vanilla code.

Shuttles check: `HarvesterWork.classify` treats any `CropBlock` by `isMaxAge`, `FieldHeadBlockEntity.cropItems()` lists every `BlockItem` whose block is a `CropBlock` from the item registry, and `HarvesterWork.plant` places `crop.defaultBlockState()` after `canSurvive`. Barley therefore works on a field with no Shuttles change; the play check confirms it, since Shuttles must not depend on Brewery for a GameTest.

### The hop vine lives in the pole's block state
One block, `hop_pole`, with two properties:
- `hop`: `IntegerProperty` 0 to 3, read as the pure enum `HopStage` by ordinal: `BARE` (pole only), `YOUNG` (small shoots), `GROWN` (leaves wrap the pole), `CONES` (leaves with Hop cones).
- `rooted`: `BooleanProperty`, true only on the foot part that holds the rhizome; the loot table drops the rhizome from it.

No block entity and no second block: a vine is always on a pole, the state is small (8 states), and the loot table can read both properties.

**Column**: the poles directly on top of each other from the lowest one up. The block reads its column by walking down and up at most `HopVine.MAX_HEIGHT - 1` blocks; it never looks sideways.

**Height rule**: a column is at most three poles. `getStateForPlacement` returns null when three poles are already below, so a fourth is refused. A column of one pole leafs out but never bears cones; two or three poles bear cones on every part above the foot.

**Support**: `canSurvive` needs a Hop Pole below, farmland below, or `Block.canSupportCenter(level, below, UP)`; `updateShape` turns an unsupported pole into air, and vanilla `updateOrDestroy` drops it, so a column falls from the broken part up. `forceSolidOff()` keeps farmland under the pole (`FarmlandBlock.canSurvive` turns farmland to dirt under a solid block) and the pole is in `minecraft:maintains_farmland` so dry farmland does not revert under it.

### Pure growth rules: `HopVine`
`io.github.dirnei.mc.servus.brewery.crop.HopVine` (no Minecraft imports) works on the column as `List<HopStage>` from the foot up:
- `growthAt(column, part)`: the step a growth tick on that part makes, or empty. `YOUNG` becomes `GROWN`. A `GROWN` or `CONES` part with a `BARE` part above it climbs: the part above becomes `YOUNG`. A `GROWN` part that is not the foot, when the top part of the column is `GROWN` or `CONES`, becomes `CONES`. `BARE` never grows.
- `firstGrowth(column)`: the first step from the foot up, for bone meal.
- `canPlaceOn(polesBelow)`: false at three.
- `HopStep(part, stage)` is the record of one change.

A climb is tried before cones, so a column first covers its whole pole and then flowers.

### Random ticks and bone meal
`isRandomlyTicking` is true for every part with a vine (`CONES` included, so a vine climbs onto a pole added later). `randomTick` needs raw brightness 9 at the part (as crops), rolls `random.nextInt(HopPoleBlock.GROWTH_CHANCE) == 0` with `GROWTH_CHANCE = 4`, wraps the roll in `CommonHooks.canCropGrow` and calls `CommonHooks.fireCropGrowPost` after the change, like `SweetBerryBushBlock`. The block implements `BonemealableBlock`: `isValidBonemealTarget` is `firstGrowth(column).isPresent()`, `performBonemeal` applies that step. One bone meal is one step.

### Planting, picking and loot
- `useItemOn` with a Hop Rhizome: finds the foot of the clicked column; if the foot is `BARE` and stands on farmland or a block in `minecraft:dirt`, sets it to `YOUNG` with `rooted=true`, shrinks the stack (not in creative), plays `CROP_PLANTED` and returns `SUCCESS`. With bone meal on a column that can still grow it returns `PASS` so `BoneMealItem` runs. Anything else returns `TRY_WITH_EMPTY_HAND`, so `useWithoutItem` picks.
- `useWithoutItem` on a `CONES` part calls `Block.dropFromBlockInteractLootTable` with `servus_brewery:harvest/hop_pole` (param set `BLOCK_INTERACT`, verified in `Block` and `VanillaBlockInteractLoot`), plays `SWEET_BERRY_BUSH_PICK_BERRIES`, sets the part to `GROWN` and sends a `BLOCK_CHANGE` game event. Example data: 1 to 3 Hops per part.
- Block loot of `hop_pole`: one Hop Pole always; one Hop Rhizome when `rooted=true`; 1 to 2 Hops when `hop=3`.
- Block loot of `barley`: `createCropDrops(barley, BARLEY, BARLEY_SEEDS, age 7)`, the wheat table.

### First seeds through Global Loot Modifiers
`neoforge:add_table` modifiers (`AddTableLootModifier`, every JSON in `data/<mod>/loot_modifiers/` is loaded by `LootModifierManager`, no global list in 26.1) written by a `GlobalLootModifierProvider`:
- `short_grass`: target `minecraft:blocks/short_grass` (`LootTableIdCondition`), adds `servus_brewery:gameplay/short_grass`. Its pools only roll without shears: Barley Seeds at 0.0625 and a Hop Rhizome at 0.015 (example numbers).
- `village_plains_house` and `village_taiga_house`: add `servus_brewery:chests/village_house`: Barley Seeds 2 to 5 at 0.4 and a Hop Rhizome at 0.3 (example numbers).
The tables are written by a `LootTableSubProvider` per param set (`BLOCK` for grass, `CHEST` for chests, `BLOCK_INTERACT` for picking).

### Tags and composting
Item tags: `c:crops` (barley, hops), `c:crops/barley`, `c:crops/hops`, `c:seeds` and `c:seeds/barley` (barley seeds), `minecraft:villager_plantable_seeds` and `minecraft:chicken_food` (barley seeds). `c:crops/*` and `c:seeds/*` follow the existing `Tags.Items` names (`crops/wheat`, `seeds/wheat`). Block tags: `minecraft:crops` (barley, which also gives `bee_growables`), `minecraft:maintains_farmland` (barley, hop pole), `minecraft:mineable/axe` (hop pole). Compostables through `DataMapProvider` and `NeoForgeDataMaps.COMPOSTABLES`: seeds and rhizome 0.3, barley and hops 0.65, as vanilla wheat seeds and wheat.

### Recipe
Three sticks in a column make two Hop Poles (shaped, no vanilla recipe uses that pattern). No recipes for seeds, rhizomes, barley or hops.

### Models
Barley: eight models with parent `minecraft:block/crop` and textures `block/barley_stage0` to `barley_stage7`, one per age.

Hop Pole (all in pixels, 16 per block):
- Pole: box `7..9` on x and z, `0..16` on y, texture `block/hop_pole`, all six faces.
- Young: four leaf planes `0..8` high with `block/hop_vine_young`: x planes at `6.5` and `9.5` spanning z `5..11`, z planes at `6.5` and `9.5` spanning x `5..11`; each plane has its two outward faces.
- Grown: the same four planes `0..16` high with `block/hop_vine`.
- Cones: the grown planes plus four outer planes with `block/hop_cones`: x at `4.5` and `11.5` spanning z `3..13`, z at `4.5` and `11.5` spanning x `3..13`.

Face pairs that overlap:
- Pole side faces (x `7`, `9`; z `7`, `9`) against inner leaf planes (`6.5`, `9.5`): 0.5 px apart, no shared plane.
- Inner x planes against inner z planes: they cross along vertical lines (perpendicular), no shared plane.
- Inner planes against outer planes: outer x planes at `4.5`/`11.5` lie outside the inner z planes' span (`5..11`), and the other way round; no overlap at all.
- Outer x planes against outer z planes: perpendicular crossings only.
- The two faces of one leaf plane share a plane but face opposite ways and are both drawn only from their own side (as the vanilla `cross` model).
- Top face of a pole and bottom face of the pole above share the plane y `16`/`0` but face opposite ways and sit inside the pole column, hidden by both poles.
- Barley uses the vanilla crop model (planes at 4 and 12 px), no new geometry.

The pole is real 2 by 2 px geometry, not a flat face. It is wood, not metal, so the steel texture rule does not apply. Every texture uses only fully opaque or fully clear pixels so 26.1 picks the cutout layer from the sprite (`ChunkSectionLayer.byTransparency` in `BakedQuad.MaterialInfo.of`); no render type is set in the models.

Shapes: the collision shape is the pole box. The outline shape is the pole box when `BARE` and `5..11` on x and z when it carries a vine, so the leaves can be clicked.

### Textures
`tools/brewery-textures/gen.py` gains a block folder and these sprites:
- Items: `barley` (port of `gerste`), `hops` (port of `hopfen`, using a seeded `random.Random('hop')` like the mockup), `barley_seeds` (pale grains), `hop_rhizome` (brown knobby root with a green shoot), `hop_pole` (diagonal pole with a pointed tip).
- Blocks: `barley_stage0` to `barley_stage7` (stalks growing from green to golden ears with awns), `hop_pole` (vertical bark grain), `hop_vine_young`, `hop_vine`, `hop_cones`.
The port brings over `ellipse`, `poly` and `paint` from `sprites.py` as plain Python, keeping the PNG writer.

### Datagen
`ServusBreweryDataGen` adds a `ServusBreweryBlockStateProvider` (Foundation `datagen.model.BlockStateProvider`), a `LootTableProvider` with the block loot (`BlockLootSubProvider`), the grass, chest and harvest tables, the loot modifier provider, block and item tag providers, a recipe provider and the data map provider. The event is `GatherDataEvent.Client`, which in 26.1 generates assets and data in one run (as Delicacies and Shuttles).

### GameTests
`BreweryCropGameTests`: barley on moist farmland grows to max with bone meal and its drops at max contain Barley and Barley Seeds (`Block.getDrops`), young barley drops seeds only; short grass broken 300 times by hand drops Barley Seeds and with shears none (the loot modifier runs inside `Block.getDrops`); random ticks make the vine climb; a rhizome on a pole on stone does nothing; a fourth pole is refused; a rhizome plants at the foot; bone meal makes the vine climb a three pole column and grow cones on the upper parts; right click on a cones part drops Hops and leaves the part `GROWN`; breaking the foot drops the rhizome.

### Settles from PLAN.md
No open decision of PLAN.md section 5 is touched.

## Assumptions

- Hop columns are at most three poles; a single pole leafs out but bears no cones, so hops want two or three poles.
- Cones grow on every part above the foot, only after the vine has leafed out on the top pole.
- Picking works per part, not for the whole column at once.
- The vine stays as long as its poles stand; it does not check its soil again after planting.
- The growth chance per random tick (1 in 4) is a code constant like the vanilla crop growth rolls, not datapack data; drops and first source chances are data.
- First sources: short grass (Barley Seeds 6.25 %, Hop Rhizome 1.5 %) and plains and taiga village house chests. No villager trades.
- The Hop Rhizome is in no `c:seeds` tag, since it is a root cutting and not a seed.
- Three sticks in a column give two Hop Poles.
- Pole strength 1.0, axe tool, wood sound.

## Risks / Trade-offs

- [Other mods that add seeds to short grass] -> add_table adds to the roll, it does not replace vanilla or other drops.
- [A vine is stored on the poles, so moving poles with a piston would split it] -> the pole has `PushReaction.DESTROY`.

## Migration Plan

None. Nothing is released.
