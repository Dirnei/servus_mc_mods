## Context

See proposal.md for the why. `m15-brewery-project` and `m15-crops` (applied, not archived) left `:brewery` with Barley, Hops, the Hop Pole and `tools/brewery-textures/gen.py`. `m14-mill` (applied, not archived) added the Foundation Mill with the data driven recipe type `servus_foundation:milling` (`MillingRecipe(SizedIngredient input, ItemStackTemplate result, int work)`, registered in `MachineContent.MILLING`). Brewery depends on Foundation (compile only, loaded in every run), so it can write milling recipes and run the Mill in its GameTests. Sources checked: `minecraft-patched-26.1.2.116-sources.jar` and `neoforge-26.1.2.116-sources.jar`.

## Goals / Non-Goals

**Goals:** soaked grain, a Malting Floor that germinates it in data driven time without ticking every block, a small turning bonus, item capability for pipes, the malt and grist items the Brewhouse needs, milling recipes, textures, JUnit tests for the timing and GameTests for floor and Mill.

**Non-Goals:** see proposal.md, Non-goals.

## Decisions

### D1 Soaking is a crafting recipe
Shaped `GGG` / `GWG` / `GGG` with `G` Barley (or Wheat) and `W` a Water Bucket makes eight Soaked Barley (or Soaked Wheat). `Items.WATER_BUCKET` has `craftRemainder(BUCKET)` in 26.1.2 (`Items.java`), so the empty bucket stays in the grid. No water bottle recipe: a water bottle is a potion with a contents component and no plain ingredient. No soaking on the floor itself: one way to make soaked grain is enough.

### D2 Germinating recipe type
`malting/GerminatingRecipe(Ingredient input, ItemStackTemplate resultTemplate, int time, int turnBonus, int turnCooldown) implements Recipe<SingleRecipeInput>`, JSON:

```json
{"type": "servus_brewery:germinating", "input": "servus_brewery:soaked_barley",
 "result": {"id": "servus_brewery:green_malt"}, "time": 24000, "turn_bonus": 2400, "turn_cooldown": 1200}
```

`turn_bonus` and `turn_cooldown` are optional (default 0: turning does nothing). `matches` tests the ingredient, `isSpecial` true, `PlacementInfo.NOT_PLACEABLE`, never in the recipe book, like `MillingRecipe`. Registered in `registry/ServusBreweryRecipes` (`RECIPE_TYPES`, `RECIPE_SERIALIZERS`, `GERMINATING`) and synced to clients with Foundation's `ServusRecipes.syncToClients`, so the client predicts a right click with soaked grain the same way the server decides it. Shipped: `recipe/germinating/green_malt.json` (soaked barley) and `recipe/germinating/green_wheat_malt.json` (soaked wheat), both with the example numbers above. The count is kept: n soaked grain become n green malt.

### D3 Pure timing: `Germination`
`malting/Germination(long startedAt, long bonus, long lastTurn)`, no Minecraft imports, works on game time ticks:
- `start(now)`: started now, no bonus, never turned (`lastTurn` = `NEVER` = -1).
- `elapsed(now)` = `now - startedAt + bonus`; `isDone(now, time)`; `remaining(now, time)` (never below 0).
- `canTurn(now, cooldown)`: never turned, or at least `cooldown` ticks since the last turn.
- `turn(now, cooldown, bonus)`: `Optional` of the turned state (bonus added, `lastTurn` = now) or empty during the cooldown.

Game time keeps running for unloaded chunks too, so a floor in an unloaded chunk catches up on its next tick.

### D4 Ticks: scheduled, never per tick
The floor has no ticker. When soaked grain arrives it schedules a block tick (`Level.scheduleTick`) in `min(remaining, CHECK_TICKS)` with `CHECK_TICKS = 100`. The block's `tick` asks the block entity to check: done turns the stack into the result, not done schedules the next check. Rechecking at most every 100 ticks is needed because `LevelChunkTicks.schedule` ignores a second tick for the same position and block while one is pending (`ticksPerPosition` is a set keyed by position and type), so a turn that shortens the time cannot move an already scheduled tick earlier. Scheduled ticks are saved with the chunk, so the check survives a reload. Turning also checks at once, so a turn that finishes the batch shows green malt right away. `CHECK_TICKS` is a code constant (how often to look), not a balance number.

### D5 Malting Floor block and block entity
`malting/MaltingFloorBlock extends BaseEntityBlock` with `CONTENT`, an `EnumProperty<FloorContent>` (`EMPTY`, `SOAKED`, `GREEN`) that picks the model, so the client needs no block entity data. Shape and collision 0,0,0 to 16,3,16 px. Properties: spruce map colour, strength 1.0, wood sound, `noOcclusion`, axe tool (`minecraft:mineable/axe`).

`malting/MaltingFloorBlockEntity` holds a one slot `FloorSlot extends ItemSlots` (one stack, the item's own limit) and a nullable `Germination`:
- `isItemValid`: a germinating recipe takes the stack. The NeoForge `StacksResourceHandler.insert` already refuses a different item in an occupied slot, so green malt or other grain never mixes in.
- `extract` is overridden: it gives nothing while the stack is still an input of a germinating recipe, so pipes take only finished malt.
- `onContentsChanged` (after a transaction commits, or at once for `set`, per `StacksResourceHandler`) calls `contentsChanged()`: empty clears the germination and sets `EMPTY`; soaked grain restarts the germination at the current game time, schedules a check and sets `SOAKED`; anything else clears the germination and sets `GREEN`. So every addition of soaked grain starts the batch over.
- `check()`: called from the block tick; converts or reschedules (D4).
- `turn(now)`: applies `Germination.turn` with the recipe's numbers; true when it turned.
- Saves the slot (`Grain`) and the germination (`Started`, `Bonus`, `LastTurn`).
- `preRemoveSideEffects` drops the slot, like `MachineBlockEntity`.

### D6 Hand use
- `useItemOn` with an item a germinating recipe takes: on the server inserts the held stack into the slot and consumes what went in (`ItemStack.consume`, which skips players with infinite materials); returns `SUCCESS` when something went in, `FAIL` when nothing fit. Any other item returns `TRY_WITH_EMPTY_HAND`.
- `useWithoutItem`, only with an empty main hand: sneaking takes the whole slot (`Inventory.placeItemBackInInventory`); `GREEN` takes the whole slot; `SOAKED` turns the grain (composter fill sound and a few `ParticleTypes.COMPOSTER` particles when it turned, nothing during the cooldown); `EMPTY` passes.

### D7 Item capability
`Capabilities.Item.BLOCK` registered for the floor's block entity type in `ServusBreweryEvents.onRegisterCapabilities` (mod bus `RegisterCapabilitiesEvent`), the same slot on every side and for `null`.

### D8 Items
Ids and names: `soaked_barley` Soaked Barley, `soaked_wheat` Soaked Wheat, `green_malt` Green Malt, `green_wheat_malt` Green Wheat Malt, `pale_malt` Pale Malt, `dark_malt` Dark Malt, `wheat_malt` Wheat Malt, `pale_malt_grist` Pale Malt Grist, `dark_malt_grist` Dark Malt Grist, `wheat_malt_grist` Wheat Malt Grist, `malting_floor` Malting Floor. One grist item per malt: a recipe ingredient then says which grist it wants, with no component matching. Wheat Malt is added now, so Weissbier in `m15-beer` can ask for wheat grist; it uses the same floor. Pale, Dark and Wheat Malt come only from the Malt Kiln in `m15-brewhouse`; until then they exist only in the creative tab, and this change ships no temporary recipe for them.

### D9 Milling recipes
Written by `ServusBreweryRecipeProvider` through `RecipeOutput.accept` with Foundation's `MillingRecipe`, as `MachineDataParts` does for sand: `recipe/milling/pale_malt_grist.json`, `dark_malt_grist.json`, `wheat_malt_grist.json`; one malt to one grist, 100 work each (example: half of gravel, four crank turns).

### D10 Look and model (pixels)
Model `malting_floor_empty`, `_soaked`, `_green`, all of the same frame:
- `BASE` 0,0,0 to 16,2,16, `minecraft:block/spruce_planks` on every face (sides use the bottom 2 px of the texture).
- Rim boards, `spruce_planks`, y 2 to 3: `RIM_N` 0,2,0 to 16,3,1 and `RIM_S` 0,2,15 to 16,3,16 with every face but the bottom; `RIM_W` 0,2,1 to 1,3,15 and `RIM_E` 15,2,1 to 16,3,15 with up, the outer and the inner face only (their north and south ends touch `RIM_N` and `RIM_S` and are not drawn).
- `GRAIN` (soaked and green only): only an up face at y 2.8 over 1,1 to 15,15, texture `block/soaked_grain_floor` or `block/malt_floor`.

Overlapping faces:

| Pair | Shared plane? | How |
|---|---|---|
| base top (y 2) / rim bottoms | no | rim bottoms are not drawn |
| base top (y 2) / grain (y 2.8) | no | 0.8 px apart |
| grain (y 2.8) / rim tops (y 3) | no | 0.2 px apart, and the grain spans only the inside (1 to 15) |
| rim W, E ends / rim N, S inner faces (z 1, 15) | no | the W and E end faces are not drawn |
| rim N inner face (z 1) / rim W, E | no | rim N inner face spans x 0 to 16 at z 1, rim W and E start at z 1 and draw no face there |
| rim inner faces / grain | no | the grain has no side faces |

The floor is wood, so the steel rule does not apply; it has no moving parts. Every texture is fully opaque. The item model is the empty floor model.

### D11 Textures
`tools/brewery-textures/gen.py` gains:
- Items: `pale_malt` (port of the approved `malz` heap with its paired grain specks), `dark_malt` (same heap in a dark roast ramp), `wheat_malt` (same heap in a pale golden ramp), `green_malt` and `green_wheat_malt` (pale grain heaps with white rootlets), `soaked_barley` and `soaked_wheat` (grain heaps with blue water glints), three grist sprites (a low powder heap in the malt's ramp, per pixel noise instead of grains), `malting_floor` is not an item sprite (block item model).
- Blocks: `malt_floor` (port of the approved `malt_floor`: `tex_noise` over the malt ramp plus 40 paired grain specks) for the green malt layer, `soaked_grain_floor` (the same noise in the barley ramp with wet dark specks).
The port adds `tex_noise` from `sprites.py`.

### D12 Datagen
`ServusBreweryBlockStateProvider` writes the floor variants and the block item model (`simpleBlockItem`); the item model provider skips block items whose model the block state provider writes. Block loot: `dropSelf` for the floor (contents drop from the block entity). Block tag `minecraft:mineable/axe` gets the floor. Compostables: soaked grains and green malts 0.65 (as wheat). Recipes: soaking (D1), floor (`S S` / `SSS` spruce slabs, one floor), germinating (D2), milling (D9). Lang for every new item and block.

### D13 GameTests
`gametest/MaltingGameTests` (listed in `BreweryGameTests`):
- `soakingRecipeKeepsTheBucket`: the crafting recipe result and its remainder.
- `spreadingSoakedBarley`: right click with sixteen Soaked Barley puts them on the floor, the state is `SOAKED`.
- `otherGrainStaysOff`: Soaked Wheat does not go onto a floor with Soaked Barley; Barley does not go onto an empty floor.
- `soakedBarleyBecomesGreenMalt`: soaked barley inserted through the capability; its saved germination is loaded back with `Started` one recipe time in the past (`loadWithComponents`, as `AlchemyGameTests` does), and within the next check the floor holds the same count of Green Malt with state `GREEN`. A real day is too long for a GameTest.
- `notDoneBeforeItsTime`: right after spreading, a check leaves Soaked Barley.
- `turningGivesABonusOncePerCooldown`.
- `collectingGreenMaltAndTakingBackSoakedGrain`: empty hand collects green malt, sneaking takes soaked grain back.
- `pipesTakeOnlyGreenMalt`: extracting from a soaked floor gives nothing, from a green floor the malt.
- `breakingDropsTheGrain`.
- `millGrindsPaleMaltToGrist`, `millGrindsDarkMaltToGrist` and `millGrindsWheatMaltToGrist`: the shipped recipe exists and a Mill driven by an Electric Motor (as `MillGameTests`) grinds the malt to its grist.

### Settles from PLAN.md
No open decision of PLAN.md section 5 is touched.

## Assumptions

- Soaking: eight grain around a Water Bucket give eight soaked grain; no water bottle and no soaking on the floor.
- One floor holds one stack (the item's stack size); each block is its own batch.
- Adding soaked grain to a batch starts the whole batch over.
- Turning: empty hand right click, example bonus 2400 ticks once per 1200 ticks, so diligent turning makes a batch about three times faster.
- Green malt on the floor uses the approved brown `malt_floor` look; soaked grain a pale wet look.
- Wheat Malt is included for Weissbier: Soaked Wheat, Green Wheat Malt, Wheat Malt and Wheat Malt Grist.
- One grist item per malt, named "Pale Malt Grist", "Dark Malt Grist" and "Wheat Malt Grist".
- Milling malt takes 100 work (gravel takes 200).
- Pale, Dark and Wheat Malt come from the Malt Kiln in `m15-brewhouse`; until then only the creative tab has them.
- No `c:` tags: there is no common convention for malt or grist tags.
- The floor recipe is `S S` / `SSS` of spruce slabs and gives one floor.
- No JEI category for germinating yet.

## Risks / Trade-offs

- [The Mill's recipes come from Foundation's unarchived `m14-mill`] -> Brewery only writes data for its recipe type; if the JSON shape changes there, datagen here fails to compile and shows it.
- [Rechecking every 100 ticks per loaded floor] -> a scheduled tick per floor every five seconds only while grain soaks; empty and green floors schedule nothing.

## Migration Plan

None. Nothing is released.
