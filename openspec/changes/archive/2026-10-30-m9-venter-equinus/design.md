# Design

## Context

- The Purifier is the closest model. `PurifierBlock` is the controller (`BaseEntityBlock` with `FORMED` and `HORIZONTAL_FACING`, a server ticker, Journal forming in `useItemOn`), `PurifierSideBlock` carries the side pieces, `PurifierStructure` forms and unforms, and the pure `PurifierLayout` lists the cells. Ports are fixed by position: insert-only and extract-only `SlotPort` views on the block entity or registered by block in `ServusAlchemyEvents.registerPurifier`. There is no GUI. Jade gets a provider in `ServusAlchemyJadePlugin`, JEI gets a category in `ServusAlchemyJeiPlugin`, the journal gets a `WorkshopCategory.purifier()` entry with a multiblock from `ServusAlchemyMultiblockProvider`, and `PurifierGameTests` cover it.
- Every alchemy build is drawn as formed pieces per block that turn with `ModelRotations.rotationY`, built from boxes with vanilla textures in `AlchemyModels`. Levels that change (brine, ash heaps) are drawn by a `BufferedBlockEntityRenderer` on the controller, with a `getRenderBoundingBox` that covers the build.
- `structure/Facing.offset(right, up)` covers only the vertical front plane, and every build so far is a wall one block deep. The Athanor turns ground offsets with `AthanorTiers.turn`. The Venter Equinus is the first build that lies flat.
- Alchemy recipes implement `AlchemyRecipe.CountedInput` (a `SizedIngredient` input, the `time()` of a cycle) and are registered in `ServusAlchemyRecipes`. They reach clients through `ServusRecipes.syncToClients` in `ServusAlchemy` and are read with `ServusRecipes.all` and `ServusRecipes.first`.
- `AlchemyRecipeTable` is the single source for the shipped recipes and the book lines, written by `ServusAlchemyRecipeProvider`. The Athanor's "Its Works" list is generated from `AlchemyRecipeTable.athanor()` with `PrincipleText.line`, so a new row shows up in the book by itself.
- Athanor tiers in code: 0 Nigredo, 1 Albedo, 2 Citrinitas, 3 Rubedo (`AlchemyRecipeTable.stageName`). The request's "tier 1 (Nigredo)" means the first stage, so the Grass Seed craft is tier 0.
- Machine stats come from `data/servus_alchemy/machine/<name>.json` (hand-written, for example `calciner.json` with `item_capacity`). Defaults are registered in `AlchemyMachineDefaults`.
- Data maps are used already: `DelicaciesDataMaps` builds a `DataMapType.builder(Identifier, Registries.ITEM, codec).synced(codec, false).build()`, registers it on `RegisterDataMapTypesEvent` and reads it with `Holder.getData`. `ServusDelicaciesDataMapProvider extends DataMapProvider` writes it in datagen (`builder(type).add(...)`). That is the verified 26.1.2 API this change copies.
- Calcination today (`AlchemyRecipeTable.calcination()`, mB of Salt, Sulfur, Mercury): Dirt (`#minecraft:dirt`) 50/20/20, Sand 80/0/10, Crops and Seeds 10/30/30, Leaves 0/40/20, Bones 100/0/0. Coagulation: Clay Ball 100/0/50, Bone Meal 60/20/0. Rotten Flesh, Spider Eye and mushrooms have no calcination.
- `m14-mixer` (proposed in parallel) adds `MixingRecipe` in `foundation/.../machine/mixer/` with recipe type `servus_foundation:mixing`: one to four counted item ingredients, at most one fluid with mB, one item result with count, and work. Its datagen helper is `MixingRecipes`. It ships Dirt and Water into Mud and Dirt and Gravel into Coarse Dirt.

## Goals / Non-Goals

**Goals:**
- A flat 3 by 3 build that forms, unforms, draws and reports like the other alchemy builds. Any side may be the front, chosen by where the Bed Hatch stands.
- Every balance number in datapack data: the recipe (count, result, warm and cold time), the bedding data map (warmth per item), the machine stats (rot capacity).
- The warmth rule and the batch accounting are pure Java and tested.
- No recipe loop gives free principles (see "Balance").

**Non-Goals:**
- A shared base class for all formed builds. The Venter follows the Purifier's shape and keeps its own small structure class.
- The production capability for the Crafting CPU.

## Decisions

### Blocks and roles
| Cell | Block | Role |
|---|---|---|
| middle | `cucurbit` (`CucurbitBlock`, block entity `VenterEquinusBlockEntity`) | controller; rot in |
| middle of the front side | `bed_hatch` (`BedHatchBlock`) | Humus out |
| the other seven | `dung_bed` (`DungBedBlock`) | straw in |

- The controller is the Cucurbit because the work happens in the vessel and the rot goes in there. The build is "the Venter Equinus" in messages, Jade and the book. The block item is "Cucurbit". This mirrors the Purifier, whose controller block is the Leaching Vat.
- The hatch is its own block, not a Dung Bed in a special cell. The front then follows from the hatch, with no facing for the player to get right, and the hatch's port is a plain block registration like the Residue Crock.
- `VenterFrameBlock` (abstract, like `PurifierSideBlock`) is the base of `DungBedBlock` and `BedHatchBlock`. It has the properties `FORMED`, `FACING` (horizontal, the outward side of its piece) and `PIECE` (`corner`, `edge`; the hatch is always `edge`). It also has the removal hook into `VenterStructure.broken`, `PushReaction.BLOCK` while formed, and `cucurbit(pos, state)`:
  - edge: `pos.relative(facing.getOpposite())`;
  - corner: also one step toward `facing.getCounterClockWise().getOpposite()`.
- `CucurbitBlock` has `FORMED`, `FACING` (the build's front, set on forming, initially the player's facing on placement, used as the first facing to check) and `WORKING` (a batch runs; drives the steam). Its model does not turn. Its shape is `Block.box(1, 0, 1, 15, 16, 15)`. The model reaches higher (the neck), but a shape stays within one block.
- No block implements `Wrenchable`, so the Configuration Carrot passes.

### Layout on the ground
`alchemy/venter/VenterLayout` (pure) lists nine cells as `Cell(index, role, right, forward, piece, turns)`, with right to the player's right and forward toward the front. `turns` is the clockwise quarter turns of the piece's `FACING` from the build's front:

| Cell | right, forward | role | piece | turns |
|---|---|---|---|---|
| 0 | 0, 0 | CUCURBIT | none | |
| 1 | 0, 1 | HATCH | edge | 0 |
| 2 | -1, 1 | BED front left | corner | 1 |
| 3 | 1, 1 | BED front right | corner | 0 |
| 4 | -1, 0 | BED left | edge | 1 |
| 5 | 1, 0 | BED right | edge | 3 |
| 6 | -1, -1 | BED back left | corner | 2 |
| 7 | 0, -1 | BED back | edge | 2 |
| 8 | 1, -1 | BED back right | corner | 3 |

The corner model draws its post in the corner between its `FACING` side and the side counter-clockwise from it, so turn 0 of a front-facing-north build is the north-west post. `placement(cell)` gives the message words ("in the back left corner", "on the left side", "in the middle of the front"). `patternRows()` gives the top-down schematic rows `DDD`, `DCD`, `DHD` (back row first).

`Facing` gets `ground(int right, int forward)`, the `Offset` on the build's level, next to `offset(right, up)`. `Structures` gets `groundPos(origin, right, forward, facing)`. `VenterLayoutTest` checks the table, the pattern, the placements, the `ground` offsets in all four facings, and that each frame piece's `FACING` points away from the Cucurbit.

### Forming
`alchemy/venter/VenterStructure`, the same shape as `PurifierStructure`:
- `tryForm` uses `Structures.check(own facing, cells, matches)` over the four facings. A cell matches when the right block stands there unformed.
- On success, `form` sets `FORMED` and `FACING` on the Cucurbit and `FORMED`, `FACING` (build front turned by `turns`) and `PIECE` on every frame block, invalidates capabilities, plays `FormingEffect.play`, sets the owner and sends `message.servus_alchemy.venter_equinus.formed`.
- On failure, it names the first wrong cell with `message.servus_alchemy.venter_equinus.missing`.
- `unform` clears every formed block whose `cucurbit(...)` is this Cucurbit; `broken` and the `changing` guard work like the Purifier's.
- All nine cells are our own blocks, so the removal hooks are enough. No vanilla block needs a `neighborChanged` check.

### Putrefaction recipe
Recipe type `servus_alchemy:putrefaction` over a data map or a machine-level rule, because the Purifier, the Prelum and the Calciner all speak in recipes. JEI then gets a category for free, datapacks can add other rots with other yields, and the book lines come from the same table.

`alchemy/venter/PutrefactionRecipe(SizedIngredient input, ItemStackTemplate resultTemplate, int time, int coldTime)` implements `AlchemyRecipe.CountedInput`. Codec: `input`, `result`, `time`, and `cold_time` (optional, default `4 * time`, must be at least `time`). It is registered in `ServusAlchemyRecipes` as `PUTREFACTION` and synced with `ServusRecipes.syncToClients`.

Shipped, written by datagen as `recipe/putrefaction/humus.json`:
```json
{ "type": "servus_alchemy:putrefaction",
  "input": { "items": "#servus_alchemy:putrefiable", "count": 8 },
  "result": { "id": "servus_alchemy:humus" },
  "time": 1200, "cold_time": 4800 }
```

The rule for choosing: recipes in id order, the first whose ingredient matches at least its count in the store. A running batch remembers its recipe id. The Cucurbit accepts any item that any putrefaction recipe's ingredient takes. With the shipped data that is the `servus_alchemy:putrefiable` tag: Rotten Flesh, Spider Eye, Poisonous Potato, Brown and Red Mushroom, the `c:crops/*` sub tags except wheat, `#minecraft:leaves`, written by `ServusAlchemyItemTagsProvider`.

### Bedding and warmth
- Bedding is a data map `servus_alchemy:bedding` on items, value `{"warmth": <work ticks>}` (codec `Bedding(int warmth)`, synced so Jade and JEI can name it), in a new `alchemy/AlchemyDataMaps` registered on `RegisterDataMapTypesEvent`. Shipped by a new `ServusAlchemyDataMapProvider`: `minecraft:hay_block` with 6,000.
  - Alternatives: an item tag plus a machine stat (one warmth for everything, and modded straw could not differ); a field on the recipe (warmth is not per recipe).
- The bed holds one bedding item per frame block. That is eight, `VenterLayout.FRAME_BLOCKS`, a structural number like the three tanks of the Athanor, not a balance number. A bale's warmth is its balance number.
- Pure `alchemy/venter/StrawBed`:
  - holds up to `capacity` bales, each with its full and its remaining warmth, oldest first;
  - `add(warmth)` returns false when full;
  - `burn()` takes one tick from the oldest bale and drops it at 0;
  - `levelMilli()` is the bales left, the oldest counting by its remaining share, divided by `capacity`, in thousandths;
  - `bales()` is the count for Jade;
  - saved as an int array of pairs.
  - JUnit `StrawBedTest`: full and refused, burning drops the oldest, level with a part-burned bale, empty level 0, mixed warmths.
- Pure `alchemy/venter/PutrefactionWork`: progress in points.
  - A batch needs `time * coldTime * 1000` points.
  - Each working tick adds `time * 1000 + (coldTime - time) * levelMilli` points.
  - A full bed then finishes in `time` ticks and an empty one in `coldTime` ticks. The speed is linear in the straw left, never zero.
  - `ticksAt(time, coldTime, levelMilli)` (ceiling) serves Jade, the book and the tests. All arithmetic is in `long`.
  - JUnit `PutrefactionWorkTest`: 1,200 / 4,800 at full, empty and half straw gives 1,200, 4,800 and 1,920 ticks; points are never zero; no overflow at `time = coldTime = 72,000`.
- Straw burns only on ticks that advance a batch, so an idle bed keeps its warmth. A bed waiting on a full hatch or on missing rot does not lose straw. Planners can leave it, and casual players are not punished for coming back late.

### Rot store and the hatch
- Pure `alchemy/venter/RotStore<K>`:
  - a capacity and counts per kind in insertion order;
  - `room()`, `add(kind, n)` (returns what was accepted), `count(Predicate<K>)`, `take(Predicate<K>, n)` (takes from the oldest matching kinds first, all or nothing) and `drain()` (everything, for the sneak and for drops).
  - JUnit `RotStoreTest`: capacity, mixed kinds, all-or-nothing take, oldest first, drain.
- The block entity keeps a `RotStore<Item>` (kinds by item, components dropped: rot is rot), saved as a list of `{id, count}`. The capacity is the machine stats `item_capacity` from the new `data/servus_alchemy/machine/venter_equinus.json` (`{"item_capacity": 256}`) and an `AlchemyMachineDefaults` entry `venter_equinus` (kind `crafter`, recipe type `servus_alchemy:putrefaction`).
- The Humus waits in a one slot `ItemSlots` (the hatch). A batch completes only when the slot can take the result. `canRun` is checked before a tick advances, so a full hatch holds the progress.

### Block entity
`VenterEquinusBlockEntity implements TeamOwned`, ticked on the server by the Cucurbit's ticker, works only when formed:
- Per tick: pick or keep the batch's recipe. If it can run, `burn()` the straw and add `PutrefactionWork` points. At the total, take the count from the store, add the result to the hatch, record `Journal.recordEvent(level, team, "putrefy", count)` and reset.
- `WORKING` on the Cucurbit is set only when it changes.
- The update tag carries the straw level in sixteenths. It is resent only when that changes (like the Purifier's brine), at most a few times per bale.
- `preRemoveSideEffects` drops `drain()` as stacks plus the hatch slot via `Containers.dropContents`. The straw is lost.
- Ports, all `LegacyItemHandler`:
  - `rotPort()`: one virtual slot that shows empty, inserts what the recipes accept up to `room()`, and never extracts;
  - `beddingPort()`: one virtual slot that takes a bedding item per free bale place and never extracts;
  - the hatch: `SlotPort.extractOnly(hatch, 0)`.
- Registration in `ServusAlchemyEvents.registerVenter`: `Capabilities.Item.BLOCK` by block entity on the Cucurbit while formed; by block on `DUNG_BED` (`VenterStructure.bedHandler`) and on `BED_HATCH` (`VenterStructure.hatchHandler`), both null unless formed and their Cucurbit is formed.
- Hand use:
  - every block of the bed, through `VenterStructure.use`, takes bedding (laid up to the room left) and accepted rot (into the Cucurbit) from the hand;
  - sneaking with an empty hand on the Cucurbit drains the store to the player (`PlayerItems.give`) and resets progress;
  - the Bed Hatch gives the hatch slot to an empty hand.
  - A Journal on the Cucurbit or on a frame block forms; a loose frame block forms the Cucurbit among its eight neighbours. Anything else passes.

### Grass Seed
- `alchemy/venter/GrassSeedItem extends QuoteItem` overrides `Item.useOn(UseOnContext)` (verified in the 26.1.2 source of `BoneMealItem.useOn`).
- On the server, when the clicked block `is(Blocks.DIRT)` and grass could live there, it sets `Blocks.GRASS_BLOCK.defaultBlockState()`, consumes one item unless the player is in creative, plays `SoundEvents.GRASS_PLACE` and sends `BoneMealItem.addGrowthParticles`. It returns `SUCCESS`; otherwise it returns `PASS` and keeps the seed.
- "Grass could live there" copies vanilla's private `SpreadingSnowyBlock.canStayAlive` (26.1.2 source):
  - the block above is a single snow layer, or
  - its fluid is not full and `LightEngine.getLightBlockInto(grass, above, Direction.UP, above.getLightDampening()) < 15`.

  The vanilla method is private, so a pure-ish helper `GrassSeedItem.grassCouldLive(level, pos)` keeps the copy in one place.
- Athanor row: `AthanorCraft("grass_seed", "Wheat Seeds", "Grass Seed", Input.of(Items.WHEAT_SEEDS), new Principles(0, 20, 20), 0, Grass Seed, 100)`.
- The item is in no tag. In particular it is not in `c:seeds`, so the Calciner's Seeds recipe does not take it.

### Dirt from the Mixer
- `AlchemyRecipeTable.dirtMixing()` holds two `Mix` rows (name, a list of `(Input, count, label)`, water mB, result, label, work):
  - `mixing/dirt_from_humus`: `#c:sands` x4, Clay Ball, Humus and 250 mB `minecraft:water` give 8 Dirt;
  - `mixing/dirt_from_bone_meal`: `#c:sands` x2, Clay Ball, Bone Meal and 250 mB Water give 2 Dirt.
- The work is the Mixer's default example from `m14-mixer`.
- `ServusAlchemyRecipeProvider` writes them through Foundation's `MixingRecipe` record (or its `MixingRecipes` builder) into `data/servus_alchemy/recipe/mixing/`. Field names and the work unit are those `m14-mixer` settles. The apply step reads its archived design first.
- Neither recipe matches a Foundation recipe's contents: the Mud recipe takes Dirt and Water, Coarse Dirt takes Dirt and Gravel, Concrete Powder takes Sand, Gravel and a dye.
- **Order:** this change is applied after `m14-mixer` is archived. Tasks 1 to 8 do not touch the Mixer. Task 9 writes the mixing recipes and fails to compile without `MixingRecipe`.

### Balance: no free principles
- Humus gets **no calcination recipe** and is in no tag the Calciner reads.
  - Its principle value reaches the Calciner only through Dirt.
  - A direct calcination would let rot that cannot be calcined (Rotten Flesh, Spider Eye, mushrooms) skip Sand, Clay and the Mixer, and would only duplicate the crops' own value.
- Bulk route: 8 Dirt calcine to 400/160/160.
  - Inputs: 4 Sand (320/0/40 if burned), a Clay Ball (100/0/50 at the Coagulator) and one Humus.
  - From crops, a Humus is 8 crops (80/240/240).
  - Inputs 500/240/330 against outputs 400/160/160: every principle loses.
  - From Rotten Flesh, the Humus brings principles from matter that has no calcination, like any other matter the Calciner burns. Nothing in the loop is made from principles, so no cycle gains.
- Quick route: 2 Dirt calcine to 100/40/40.
  - Inputs: 2 Sand 160/0/20, Clay Ball 100/0/50, Bone Meal 60/20/0, together 320/20/70.
  - Sulfur rises by 20 at the cost of 220 Salt and 30 Mercury, and the Sand is consumed. A lossy converter, not a source.
- Grass Seed: the Grass Block it makes is in `#minecraft:dirt` like the Dirt it replaces, so the value does not change. The seed costs a Wheat Seed (10/30/30) and 20 Sulfur and 20 Mercury, and gives nothing back.
- Humus and Grass Seed stay out of `servus_alchemy:putrefiable`, so nothing rots back into itself.
- Hay Bales and Wheat are not putrefiable: Wheat is the straw that insulates the bed, so the tag takes the `c:crops/*` sub tags except `c:crops/wheat`. 9 Wheat give a Hay Bale that warms 6,000 ticks, about five Humus at full speed.

### Look
Model space: the front faces north, and the player's right is west (-x). Textures:
- vanilla `spruce_log` (side), `spruce_log_top` (post tops and bottoms), `spruce_planks`, `oak_planks` (hatch board), `terracotta`, `cut_copper`, `hay_block_top`, `hay_block_side`, `mud`, `packed_mud`;
- the family steel `servus_foundation:block/electric_pump_steel` for the hatch handle (a metal part, so not `iron_block`).

All boxes are in pixels, block local. Formed frame pieces (piece facing north):

| Model | Box | x | y | z |
|---|---|---|---|---|
| `dung_bed_edge` | wall | 0 to 16 | 0 to 12 | 0.5 to 2.5 |
| `dung_bed_corner` | post | 0 to 3 | 0 to 14 | 0 to 3 |
| | north wall | 3 to 16 | 0 to 12 | 0.5 to 2.5 |
| | west wall | 0.5 to 2.5 | 0 to 12 | 3 to 16 |
| `bed_hatch` (formed and plain) | wall | 0 to 16 | 0 to 12 | 0.5 to 2.5 |
| | hatch board, no south face | 4 to 12 | 3 to 10 | 0 to 0.5 |
| | handle (steel), no south face | 6.5 to 9.5 | 6 to 7 | -1 to 0 |

Plain models:
- `dung_bed`, a small bed of its own:
  - posts in the four corners (0 to 3 and 13 to 16, y 0 to 14);
  - walls between them (north z 0.5 to 2.5 and south z 13.5 to 15.5, x 3 to 13; west x 0.5 to 2.5 and east x 13.5 to 15.5, z 3 to 13; y 0 to 12);
  - straw x and z 2.55 to 13.45, y 0.05 to 10;
  - a packed mud mat x 5 to 10, z 6 to 11, y 10.05 to 10.55.
- `cucurbit` (one model, formed or not):
  - foot x and z 4 to 12, y 0 to 2;
  - body 1 to 15, y 2 to 20 (14 by 18);
  - shoulder 3 to 13, y 20 to 23;
  - neck 6 to 10, y 23 to 28;
  - cut copper seal 5.5 to 10.5, y 28 to 30.

`VenterRenderer` (a `BufferedBlockEntityRenderer` on the Cucurbit, bounding box the 3 by 3 cells and one up) draws, in Cucurbit-local pixels where the frame spans -16 to 32:
- the bedding: x and z -13.45 to 29.45, y 0.05 to `h`, where `h = 4 + 6 * level`, from 4 (empty) to 10 (full) in sixteenths of the level. Top face `hay_block_top` and sides `hay_block_side` while straw is left; all `mud` when the bed is empty (the rotted litter);
- three `packed_mud` dung mats, 0.5 px thick from `h + 0.05` to `h + 0.55`:
  - x -11 to -5, z -10 to -4;
  - x 19 to 26, z 20 to 25;
  - x 20 to 27, z -9 to -4.

Steam: `CucurbitBlock.animateTick` (verified on `Block` in 26.1.2) spawns `ParticleTypes.WHITE_SMOKE` (verified) while `WORKING`. It picks a random point over the inside of the bed at the bedding height, with a slow upward speed, about one puff in three calls.

**Overlap check** (every pair of boxes that overlap or touch):
- Post and walls in a corner piece: the walls start at x 3 and z 3, exactly at the post's faces. Only back-to-back contact, the walls' end faces against the post's side faces. Wall tops (12) and post top (14) differ. The bottoms (0) touch only along an edge.
- North and west wall of a corner: their volumes do not overlap (x at least 3 and z 0.5 to 2.5, against x 0.5 to 2.5 and z at least 3).
- Wall pieces of neighbouring cells meet at the block border (x 16 / 0) end to end: back-to-back contact, same top plane but no overlapping area.
- Hatch board and wall: board z 0 to 0.5, wall from z 0.5. Contact only, and the board has no south face. Handle and board: contact at z 0, and the handle has no south face.
- Bedding and walls: the bedding ends 0.05 px inside the wall faces (-13.5 and 29.5). Its top (at most 10) is below the wall tops (12) and its bottom (0.05) above the wall bottoms (0).
- Bedding and posts: they intersect in the inner corners. The planes are -13.45 against -16 and -13 (and 29.45 against 29 and 32), y 0.05 and `h` against 0 and 14. None shared.
- Bedding and Cucurbit: they intersect. The bedding planes (x/z -13.45, 29.45, y 0.05, `h` in 4 to 10) never meet the Cucurbit's planes (x/z 1, 3, 4, 5.5, 6, 10, 10.5, 12, 13, 15; y 0, 2, 20, 23, 28, 30).
- Mats and bedding: mat bottoms are 0.05 px above the bedding top. The mats do not overlap each other, the walls (at least 1.5 px inside) or the Cucurbit (other cells).
- Cucurbit parts: each part sits on the next with back-to-back contact (foot top and body bottom at 2, body and shoulder at 20, shoulder and neck at 23, neck and seal at 28). Different widths, no same-facing shared planes.
- Plain `dung_bed`: the same pattern as the corner piece (walls end at post faces), straw 0.05 inside the walls, mat 0.05 above the straw.

### Jade, JEI, book
- **Jade:** `VenterProvider` (server data: rot total and capacity, bales and capacity, warmth percent, progress percent, the hatch's item and count), registered for `CucurbitBlock` and `VenterFrameBlock`. Keys:
  - `jade.servus_alchemy.venter.rot` "Rot: %s / %s";
  - `.straw` "Straw: %s / %s";
  - `.warmth` "Warmth: %s%%";
  - `.progress`;
  - the hatch through the shared item key "%2$s: %1$s".
- **JEI:** `PutrefactionRecipeCategory` extends `AlchemyRecipeCategory`:
  - the input with count, the result, and "60 s warm, 240 s cold" from `PutrefactionWork.ticksAt`;
  - a render-only row of the bedding items read from the data map;
  - catalysts Cucurbit, Dung Bed and Bed Hatch.
- **Book:** `WorkshopCategory.venterEquinus()` on the grid as `n` below the Purifier (`_______f___n___________`), parent Purifier, Nigredo (no condition). Pages:
  - spotlight of the Cucurbit;
  - multiblock `ServusAlchemyMultiblockProvider.VENTER_EQUINUS` (one layer from `VenterLayout.patternRows()`, the Cucurbit as anchor `0`, frame blocks shown formed=false);
  - crafting pages for the Cucurbit, the Dung Bed and the Bed Hatch;
  - text "The Horse's Belly", the flavor;
  - text "Rot and Straw", the how-to: rot into the gourd by hand or pipe, Hay Bales into the bed by hand or pipe, Humus from the hatch, sneak to take the rot back, an empty bed only slows it, straw rots only while it works;
  - list "What Rots" from `PutrefactionText.line`;
  - list "Dirt for the Mixer" from `PutrefactionText.mixLine`, with a closing line that the Athanor makes Grass Seed for the Dirt.
- The flavor, as a draft for the apply step: *"Every stable boy knows dung is warm in January. The old masters knew more: seal a gourd, bury it to the neck in the horse's belly, the venter equinus, and wait forty days. Or forty nights, I forget which, the wine forgets for me. What will not rot will not live again. I have buried other gourds there. Not tonight. Tonight, turnip peels."* No mention of the homunculus.
- **Pure `alchemy/venter/PutrefactionText`** (JUnit `PutrefactionTextTest`):
  - `line(count, inputLabel, resultCount, resultLabel, warmSeconds, coldSeconds)` gives "8 rot: 1 Humus, 60 s in fresh straw, 240 s cold";
  - `mixLine(List<String> parts, water, resultCount, resultLabel)` gives "4 Sand, 1 Clay Ball, 1 Humus, 250 mB water: 8 Dirt".

### Textures
`tools/venter-textures/gen.py` writes `humus.png` (a dark brown crumbly heap with a few lighter fibres, in the three tone style of `ash` and `caput_mortuum`) and `grass_seed.png` (three pale seeds with a green sprout) into `alchemy/src/main/resources/assets/servus_alchemy/textures/item/`. It has an 8x `--preview`. The blocks use vanilla textures only, plus the family steel.

### NeoForge and Minecraft APIs used
- `BaseEntityBlock`, `BlockEntityTicker`, `useItemOn` / `useWithoutItem`, `affectNeighborsAfterRemoval`, `BlockEntity.preRemoveSideEffects`, `getUpdateTag`: as in `PurifierBlock` and `PurifierBlockEntity`.
- `RegisterCapabilitiesEvent.registerBlockEntity` and `registerBlock` with `Capabilities.Item.BLOCK`: as in `ServusAlchemyEvents.registerPurifier`.
- `DataMapType`, `RegisterDataMapTypesEvent` and `DataMapProvider`: as in `DelicaciesDataMaps` and `ServusDelicaciesDataMapProvider`.
- `RecipeType.simple`, `RecipeSerializer(MapCodec, StreamCodec)`, `SizedIngredient.NESTED_CODEC`, `ItemStackTemplate.CODEC`: as in `AthanorRecipe`.
- `Item.useOn`, `BoneMealItem.addGrowthParticles`, `LightEngine.getLightBlockInto`, `BlockState.getLightDampening`, `ParticleTypes.WHITE_SMOKE`, `Block.animateTick`: checked in `minecraft-patched-26.1.2.116-sources.jar`.
- `ModifyRecipeJsonsEvent` for GameTest-only recipes: as in `gametest/GameTestRecipes`.
- Modonomicon `MultiblockProvider` and the Workshop page helpers: as for the Purifier.

### Pure vs Minecraft
- **Pure and JUnit tested:**
  - `VenterLayout` (`VenterLayoutTest`);
  - `StrawBed` (`StrawBedTest`);
  - `PutrefactionWork` (`PutrefactionWorkTest`);
  - `RotStore` (`RotStoreTest`);
  - `PutrefactionText` (`PutrefactionTextTest`);
  - `Facing.ground` (in `VenterLayoutTest`).
- **GameTests** in `VenterGameTests`, using a GameTest recipe `servus_alchemy:gametest/quick_rot` (`#servus_alchemy:putrefiable` x8 into 1 Humus, 40 ticks warm, 160 cold) added by `GameTestRecipes` only under `servus.gametest.recipes`. It sorts before `putrefaction/humus` and keeps tests short:
  - forming in all four facings by hatch position;
  - the missing-block message;
  - unform on breaking a Dung Bed, the hatch and the Cucurbit, keeping rot and straw;
  - Cucurbit drops;
  - a batch in a full bed takes 40 ticks and in an empty bed 160;
  - straw burns only while working;
  - a full hatch stops the work;
  - mixed rot;
  - the ports (rot by pipe, Hay Bales refused at the Cucurbit, bales by pipe into a Dung Bed up to the room, hatch extract only, nothing in at the hatch);
  - the hand uses;
  - the `putrefy` event;
  - Humus refused by the Calciner;
  - the schematic accepting the build.
- **GameTests in `AlchemyGameTests`:**
  - the Grass Seed on Dirt with air (becomes grass, one used);
  - under Stone (kept);
  - on Coarse Dirt (nothing);
  - the Grass Seed craft on a tier 0 Athanor;
  - Grass Seeds refused by the Calciner.
- **After `m14-mixer`:** a GameTest that both mixing recipes load and match their contents through Foundation's mixing matcher.
- **Manual:** models and the formed look from all sides, the straw height falling, litter when empty, steam, Jade, JEI, the book pages and the flavor text, and Dirt from a driven Mixer.

### Decisions made during apply
- `Facing` also got `turned(quarters)`, the clockwise turn the frame pieces use for their `FACING`; `VenterLayout.pieceFacing` reads it.
- The putrefaction recipe JSON uses `SizedIngredient.NESTED_CODEC` as every alchemy recipe does: `"input": {"ingredient": "#servus_alchemy:putrefiable", "count": 8}`. The design's example with `items` was only a sketch.
- The Bed Hatch places with its board toward the player and draws the same model formed and unformed, turned by `FACING` in both.
- The frame blocks' shape is a 12 px slab while formed, so players can stand on the bed; unformed, a Dung Bed is 14 px high and the hatch is its wall.
- `AlchemyMachineDefaults` got a `store(items)` stats helper for a machine without tanks that only holds its input.
- `VenterRenderer` draws the bedding one block cell at a time, so every face keeps its UVs on its sprite; the box planes are the design's. `CalcinerRenderer.drawFaces` draws a chosen set of faces, and `drawBox` calls it with all six.
- The steam uses `level.addParticle` in `animateTick`, and the Grass Seed's sparkles use level event 1505, which runs `BoneMealItem.addGrowthParticles` on the clients.
- `putrefyCountsInJournal` checks that a finished batch queues the owner's team journal for sync (`JournalSync.isQueued`). No chapter task counts `putrefy` yet, so the ledger keeps no count to read.
- The mixing recipes use `#c:sands`, Clay Ball, Humus or Bone Meal and 250 mB water with work 100, the Mixer's quick work (`MixingRecipes.QUICK`).

## Risks / Trade-offs

- [`m14-mixer` changes its recipe fields or work unit before archive] → The mixing task is last before integration and reads the archived Mixer design. The two rows in `AlchemyRecipeTable` are the only place to adapt.
- [A batch reads as stuck in an empty bed (four minutes for one Humus)] → Jade shows the warmth and progress, and the book says an empty bed only slows. Steam rises while it works.
- [A pipe floods a Dung Bed with Hay Bales from a farm] → The bed takes only as many as it has room for; the rest stays in the pipe.
- [Rot kinds lose their components in the store] → Rot is consumed anyway. A sneak take-back returns plain stacks, which only matters for modded rot with data.
- [A model taller than its block (the Cucurbit's neck to 30 px)] → The shape stays one block. The model is drawn by the block, so light and culling follow vanilla tall models like the bell. Faces above 16 px have no cullface.
- [Mats or straw drawn by the renderer flicker against the frame] → All renderer boxes keep at least 0.05 px from every model plane (see the overlap check). The play check looks from several angles while the straw level drops.
- [The Grass Seed is cheap] → Grass spreads on its own anyway. The seed only saves the walk to the nearest grass.

## Migration Plan

Nothing shipped is changed. New blocks, items, a recipe type, a data map and new recipes only. Rolling back removes them; worlds lose the placed blocks.
