## Context

See proposal.md for the why. The explore of 2026-10-20 (`temp/delicacies-brief.md`) decided the dishes (Sauerkraut from the Fermenting Crock over days, Red Cabbage, Bread Dumpling, Potato Dumpling, Cheese from milk and salt in the crock) and left the details here. `m14-tiled-stove` and `m14-bakery` set the patterns this change follows: vessels as `VesselBlockEntity` with data driven recipe types and `SlotMatcher`, the Cutting Board with `#c:tools/knife`, Salt in `c:dusts/salt`, food values in the synced data map `servus_delicacies:food` read by `DelicaciesFoodItem`, JEI categories, sprites in `tools/delicacies-textures/gen.py`. `m14-crops` gives Cabbage, Red Cabbage, Onion and Parsley with `c:crops/*` tags.

A parallel change, `m14-meat-dishes`, works in Delicacies too and uses `c:foods/cheese` for its Cordon Bleu. Shared files (registries, datagen providers, lang, `gen.py`) get appended, self contained blocks. Sources checked: `minecraft-patched-26.1.2.116-sources.jar`, `neoforge-26.1.2.116-sources.jar`, and the Delicacies and Foundation code named below.

## Goals / Non-Goals

**Goals:** the sides for the plated meals, Cheese for the Cordon Bleu, and a fermenting block that keeps game time without per tick work, all numbers in data.

**Non-Goals:** see proposal.md.

## Decisions

### D1 Fermentation timing, pure logic (package `crock`)
`Fermentation` (no Minecraft imports, JUnit tested) holds `startedAt` (game tick, or none) and `duration`:
- `start(now, duration)`, `stop()`, `active()`, `done(now)` (active and `now - startedAt >= duration`), `remaining(now)` (never below 0), `hoursLeft(now)` (rounded up, 1000 ticks per in-game hour), `nextCheck(now, interval)` (`max(1, min(remaining, interval))`), `backdate(ticks)` (moves the start back, as if that much game time had passed; GameTests use it), plus `restore(startedAt, duration)` and getters for saving. `days(ticks)` (24000 ticks per day) for JEI.

`CrockBatch.count(int[] have, int[] need, int fluidHave, int fluidNeed, int outputRoom, int resultCount)` (pure, JUnit tested): the number of full batches the ingredients, the fluid (ignored when `fluidNeed` is 0) and the output room allow.

The clock is `level.getGameTime()`. It advances for the whole level while the server runs, whether the crock's chunk is loaded or not, and is untouched by `/time set` and sleeping (those change the day time). Verified in 26.1 that scheduled ticks are saved with their remaining delay (`SavedTick#delay`, rescheduled as `currentTick + delay` on load), so a long scheduled tick alone would not count unloaded time. Hence the game time stamp is the truth and scheduled ticks only wake the crock: the crock schedules its next check at `nextCheck(now, CHECK_INTERVAL)` with `CHECK_INTERVAL = 100` ticks (a rule, not balance). Loaded, it finishes on the exact tick; after a reload it is done at most one interval late. No ticker and no scheduling in `onLoad` (verified: `LevelTicks#schedule` refuses positions whose chunk tick container is not registered yet).

### D2 Fermenting recipe (package `crock`)
`FermentingRecipe(List<SizedIngredient> ingredients, Optional<SizedFluidIngredient> fluid, ItemStackTemplate resultTemplate, int time) implements VesselRecipe`, the same codecs and stream codec as `PotRecipe`; `time` is game ticks. Type and serializer `servus_delicacies:fermenting` in `ServusDelicaciesRecipes`, synced with `ServusRecipes.syncToClients`. Matching, `accepts`, `acceptsFluid` and `slotsFor` come from `VesselRecipe`.

### D3 Crock block entity on the vessel base
`VesselBlockEntity` already owns four input stacks, one output, the ingredient rules (one item kind per stack, only what some recipe takes), hand insertion, `takeOut` and the automation view (`KitchenSlots`). The crock reuses that:
- New `cooking/TankVesselBlockEntity<R>` (abstract): the Pot's `FluidSlot` tank, its validator (fluids some recipe of the type uses), `fluid()`, `drainFluid`, `fluidCapability()`, `useFluidContainer` (`FluidUtil.interactWithFluidHandler` when `Transfer.isFluidContainer`), `capacity()`, save and load. `PotBlockEntity` becomes a thin subclass with the pot type and `pot_capacity`. Hooks: `tankTakes(FluidStack)` and `tankGives()` (default true) so the crock can shut its tank.
- `VesselBlockEntity#canHoldInput` becomes `protected` so the crock can refuse inputs while sealed. Its firebox and progress code only run from `VesselBlock`'s ticker, which the crock does not use.
- `FermentingCrockBlockEntity extends TankVesselBlockEntity<FermentingRecipe>`: capacity `CROCK_CAPACITY = 4000` mB, a `Fermentation`, and `check()`:
  - sealed and done: look the recipe up again from the contents, compute `CrockBatch.count` from the slots `slotsFor` assigns, take `count` times each ingredient count and the fluid, insert `count` results, stop, unseal; then fall through to the open case.
  - sealed and not done: schedule the next check.
  - open: when a recipe matches and at least one batch fits the output, start, seal, schedule.
  - a sealed crock whose recipe is gone (datapack change) unseals without a result.
  `contentsChanged()` schedules a check one tick later (`level.scheduleTick(pos, block, 1)`, `ScheduledTickAccess`, verified); `FermentingCrockBlock#tick` (`BlockBehaviour#tick(BlockState, ServerLevel, BlockPos, RandomSource)`, verified) calls `check()`. Saved: `StartedAt`, `Duration` next to the slots and tank.
- While sealed: `canHoldInput` false (hand and automation), the tank takes and gives nothing, the automation view still extracts only the output. `SEALED` on the block state follows `active()`.

`Transfer`, `FluidSlot`, `KitchenSlots` and `ServusRecipes` are Foundation and Delicacies code used as they are.

### D4 Crock block and hand use
`FermentingCrockBlock extends BaseEntityBlock` with `SEALED` (boolean), no facing (the crock is the same all round), shape from `CrockParts`. Hand use (`useItemOn`): sealed passes every item but tells the status; open: a fluid container goes through the tank, an accepted item into the inputs, anything else passes. Empty hand (`useWithoutItem`): sealed shows `message.servus_delicacies.crock.fermenting` ("Fermenting into %s, about %s in-game hours left") as an overlay; open takes the output or the last input, and an empty crock shows `.empty`. `animateTick` on a sealed crock: now and then a `ParticleTypes.BUBBLE_POP` (verified in 26.1) over the water seal. Stoneware: `SoundType.STONE`, mined with a pickaxe, no tool needed for the drop.

### D5 Milk as a fluid
`NeoForgeMod.enableMilkFluid()` in the `ServusDelicacies` constructor (verified: registers `minecraft:milk` and `flowing_milk` with the Milk Bucket as their bucket; `BucketResourceHandler` then treats the Milk Bucket as 1000 mB of milk; datagen registers it always). The fluid tag `c:milk` (`Tags.Fluids.MILK`, NeoForge fills it with the milk fluids) is what recipes name: `new SizedFluidIngredient(FluidIngredient.of(registries.lookupOrThrow(Registries.FLUID).getOrThrow(Tags.Fluids.MILK)), amount)` (verified: `FluidIngredient.of(HolderSet<Fluid>)`). Other mods that enable milk too are fine; the call only sets a flag.

### D6 Dishes and recipes (datagen)
- Cutting: `cutting/shredded_red_cabbage` (`#c:crops/red_cabbage` to two Shredded Red Cabbage), `cutting/grated_potato` (`#c:crops/potato` to one Grated Potato). The board's knife "cuts" the potato into gratings; no grater tool, day one.
- Fermenting: `fermenting/sauerkraut` (four Shredded Cabbage, one `#c:dusts/salt`, 72000 ticks, four Sauerkraut), `fermenting/cheese` (one `#c:dusts/salt`, 1000 mB `#c:milk`, 48000 ticks, four Cheese).
- Pot: `pot/braised_red_cabbage` (two Shredded Red Cabbage, one Apple, one Sugar, 250 mB water, 300 ticks, two Braised Red Cabbage), `pot/bread_dumplings` (two Bread Crumbs, one `#c:eggs`, one Chopped Onion, one `#c:crops/parsley`, 250 mB `#c:milk`, 300 ticks, two Bread Dumplings), `pot/potato_dumplings` (two Grated Potato, one Boiled Potatoes, one `#c:dusts/salt`, 250 mB water, 300 ticks, two Potato Dumplings). Each fills at most the Pot's four input stacks.
- Crafting (shapeless): Bowl of Sauerkraut (Sauerkraut, Bowl), Bowl of Red Cabbage (Braised Red Cabbage, Bowl), Potato Salad (two Boiled Potatoes, Chopped Onion, Bowl). Fermenting Crock: shaped `B B`/`B B`/`BBB` with Bricks (`minecraft:brick`), fired clay, cheap for day one.
- Items: Sauerkraut, Braised Red Cabbage, Shredded Red Cabbage and Grated Potato are plain items (ingredients). Bowl of Sauerkraut, Bowl of Red Cabbage and Potato Salad are `DelicaciesFoodItem`s with `stacksTo(16)` and `usingConvertsTo(Items.BOWL)` (verified in 26.1: `Item.Properties#usingConvertsTo` sets `USE_REMAINDER`, applied by `ItemStack#finishUsingItem`, as Mushroom Stew uses it). Cheese, Bread Dumpling and Potato Dumpling are plain `DelicaciesFoodItem`s.
- Food values (data map): Bowl of Sauerkraut 4 and 4.8, Bowl of Red Cabbage 5 and 6.0, Potato Salad 6 and 7.2, Bread Dumpling 6 and 7.2, Potato Dumpling 5 and 6.0, Cheese 3 and 3.6.
- Tags: every edible side in `c:foods`; the three bowl foods in `c:foods/soup` (NeoForge `Tags.Items.FOODS_SOUP`, "bowl foods"); Cheese in `c:foods/cheese` (`KitchenTags.CHEESE`, declared by `m14-meat-dishes` for the Cordon Bleu). Crock mineable with a pickaxe; loot `dropSelf`.

### D7 Crock model (pixels, the same from every side)
Textures: `crock_side` (brown salt glazed stoneware with a dark band), `crock_top` (the belly top: water seal ring and dark inside), `crock_lid`. Parts in `CrockParts`:
- `BELLY` 2,0,2 to 14,8,14, all faces; its top shows `crock_top`.
- Neck walls on the belly: `NECK_N` 4,8,4 to 12,12,5; `NECK_S` 4,8,11 to 12,12,12; `NECK_W` 4,8,5 to 5,12,11; `NECK_E` 11,8,5 to 12,12,11. No down faces.
- Water seal lip: `LIP_N` 2,8,2 to 14,10,3; `LIP_S` 2,8,13 to 14,10,14; `LIP_W` 2,8,3 to 3,10,13; `LIP_E` 13,8,3 to 14,10,13. No down faces. The channel between lip and neck (x or z 3 to 4) is the water seal; `crock_top` paints water there.
- Sealed only: `LID` 3,12,3 to 13,13,13; skirt dipping into the seal `SKIRT_N` 3.05,9,3.05 to 12.95,12,3.95, `SKIRT_S` 3.05,9,12.05 to 12.95,12,12.95, `SKIRT_W` 3.05,9,3.95 to 3.95,12,12.05, `SKIRT_E` 12.05,9,3.95 to 12.95,12,12.05; `KNOB` 7,13,7 to 9,14.5,9. The sealed model leaves out the neck's up faces (under the lid), the skirts' up faces (under the lid), the west and east skirts' north and south faces (inside the north and south skirts) and the knob's down face.
- Renderer (open only): fluid surface an up face at x and z 5 to 11, from y 8.5 to y 11.5 by fill; items lie 0.3 px above the fluid or above the belly top (y 8), scale 0.22, a 2x2 grid with 0.05 px steps, the result 0.5 px above them.

Overlapping box pairs and their planes (CLAUDE.md "Models and textures"):

| Pair | Shared plane? | How |
|---|---|---|
| belly / neck walls, lip | no | they stand on the belly top (y 8) and have no down faces |
| neck walls among each other | no | west and east end at the north and south walls' inner faces (z 5, z 11), back to back |
| lip parts among each other | no | west and east end at the north and south lips' inner faces (z 3, z 13) |
| lip / belly sides | no | the lip's outer faces continue the belly's side planes above y 8; they only meet along the edge |
| lid / neck tops | no | the sealed model has no neck up faces; only the lid's down face is at y 12 |
| skirt / lid | no | no skirt up faces |
| skirt / lip, neck | no | 0.05 px gaps: the skirt runs 3.05 to 3.95 between lip (3) and neck (4) |
| skirt west or east / north or south | no | their north and south faces are left out |
| knob / lid | no | no knob down face |
| fluid, items / belly top, neck | no | the fluid starts 0.5 px above the belly top, items 0.3 px above what they lie on, inside the neck |

Item model: the open crock.

### D8 Textures
`tools/delicacies-textures/gen.py` gets one appended block: the mockup's `bowl` helper with its `KRAUT`, `SKN`, `KKN` and `CER` ramps, `sauerkraut` (Bowl of Sauerkraut), `blaukraut` (Bowl of Red Cabbage), `semmelknoedel` (Bread Dumpling), `kartoffelknoedel` (Potato Dumpling), and new sprites in the same painted style: Sauerkraut and Braised Red Cabbage as a heap without a bowl, Shredded Red Cabbage (the Shredded Cabbage drawing in the red ramp), Grated Potato (a pale heap of shreds), Cheese (a yellow wedge with holes), Potato Salad (the bowl with potato slices, onion and parsley flecks), and the crock textures `crock_side`, `crock_top`, `crock_lid`.

### D9 JEI
`VesselRecipeCategory` becomes non-final with two hooks, `timeText(recipe)` and `note()`. `FermentingRecipeCategory` extends it with the fluid slot, the time in days (`Fermentation.days`, "%s days") and the note "Sealed in the Fermenting Crock". The plugin registers the category, its recipes and the crock as catalyst. The pot and cutting recipes appear in their categories with no code.

### D10 GameTests (`gametest/SidesGameTests`, registered in `DelicaciesGameTests`)
- `sauerkrautAfterTime`: four Shredded Cabbage and one Salt seal the crock; half the time backdated it is still sealed with no result; backdated past the time it opens with four Sauerkraut.
- `cheeseAfterTime`: a Milk Bucket by hand fills 1000 mB and leaves a Bucket; Salt in; backdated past the time it holds four Cheese and no milk.
- `crockBatchesAndLeftovers`: eight Shredded Cabbage and two Salt ferment into eight Sauerkraut; six Shredded Cabbage and one Salt into four Sauerkraut with two Shredded Cabbage left and the crock open.
- `sealedCrockRefusesInput`: a hopper with Salt above a sealed crock keeps its Salt; a hopper below pulls only results.
- `breadDumplingsInThePot`, `potatoDumplingsInThePot`, `redCabbageInThePot`: on a fuelled stove the Pot makes them (milk for the bread dumplings).
- `sidesRecipesShipped`: Red Cabbage and Potato cut on the board, the three crafting recipes, and a Bowl of Sauerkraut eaten raises hunger by its value and leaves a Bowl.

JUnit: `FermentationTest` (inactive is never done, done exactly at the duration, remaining never below zero, hours left rounded up, next check capped by the interval and at least 1, backdate, restore, unloaded time counts because only `now` matters), `CrockBatchTest` (limited by an ingredient, by the fluid, by the output room, no fluid needed, zero when nothing fits).

### Settles from PLAN.md
PLAN.md lists `m14-sides` with "Sauerkraut, Red Cabbage, Bread Dumpling, Potato Dumpling, potato salad" and `m14-meat-dishes` with "Cheese from the Fermenting Crock". The Fermenting Crock and Cheese are built here, so `m14-meat-dishes` only uses `c:foods/cheese`. PLAN.md is groomed when the changes are archived (the brief asks agents not to edit it now).

## Assumptions

- One cheese: Cheese wedges come straight from the crock (four per 1000 mB of milk and one Salt, two in-game days); no wheel to cut.
- Sauerkraut: four Shredded Cabbage and one Salt into four Sauerkraut, three in-game days. Served as Bowl of Sauerkraut (one Sauerkraut and a Bowl); plain Sauerkraut and Braised Red Cabbage are ingredients for the plated meals of `m14-teller` and are not edible.
- Red Cabbage: no vinegar; Apple and Sugar give the sweet sour taste.
- Bread Dumpling: Bread Crumbs (from `m14-bakery`) instead of new bread cubes, simmered in milk; the Pot's one tank means milk instead of water for this recipe.
- Potato Dumpling: grated on the Cutting Board with the Kitchen Knife (no Mill, works on day one), with Boiled Potatoes and Salt in water.
- Potato Salad is crafted, not cooked: two Boiled Potatoes, a Chopped Onion and a Bowl.
- The crock seals as soon as its contents match a recipe; put the Salt in last to ferment a big batch. While sealed nothing goes in or out but the result. Breaking the crock is the only way to stop early.
- Crock tank 4000 mB, check interval 100 ticks; both are rules of the block, not balance numbers.
- Crock recipe: seven Bricks (`B B`/`B B`/`BBB`).
- The bowl foods stack to 16.

## Risks / Trade-offs

- [Milk fluid turned on for the whole game] Other mods then see the Milk Bucket as a fluid container; NeoForge made it opt in for that reason, and many kitchen and tech mods turn it on too -> expected in a kitchen mod.
- [Up to one check interval late after a reload] Harmless for days long recipes.
- [GameTests backdate the start instead of waiting days] `backdate` is a small public hook on the block entity; the timing itself is covered by JUnit.
- [Bulk sides are not edible] Players need a Bowl; JEI shows the crafting.

## Migration Plan

None. Nothing is released.
