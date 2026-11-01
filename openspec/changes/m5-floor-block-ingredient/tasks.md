# Tasks

## 1. Floor ingredient (Foundation)

- [x] 1.1 Add `FloorBlockIngredient` (`ICustomIngredient`: `test`, `items`, `isSimple` true, static `accepts(Block)` holding the rule from `FacadeRecipe.isFacadeable`) and register its `IngredientType` as `servus_foundation:floor_block` in `NeoForgeRegistries.INGREDIENT_TYPES` (new `logistics/recipe` package or `logistics/facade`, `ServusLogisticsRecipes`, mod constructor wiring). Point facade placement and tank code at `accepts`. Add a GameTest that the ingredient accepts stone bricks and cobblestone and refuses glass, a slab and a chest. Verify with `gradlew :foundation:build` and the Foundation GameTests.

## 2. Real recipes (Foundation)

- [x] 2.1 Rewrite `FluidTankRecipe` as a `ShapedRecipe` subclass with its own shaped codec and serializer under `fluid_tank`. `assemble` sets the floor from the bottom middle cell. Update datagen in `LogisticsDataParts` (shaped builder, `CGC` / `G G` / `CFC`, floor ingredient) and regenerate. Check that `fluid_tank.json` holds the full pattern. Keep the `FluidTankGameTests` craft and glass-refused checks green, and add a GameTest that crafting with a cobblestone floor gives a cobblestone tank. Verify with `gradlew :foundation:runData`, build and GameTests.
- [x] 2.2 Rewrite `FacadeRecipe` as a `ShapelessRecipe` subclass (floor ingredient plus `#c:nuggets`, six facades, `assemble` copies the block) and `HollowFacadeRecipe` as a `ShapedRecipe` subclass (ring of Facades, `matches` also requires one block, `assemble` copies it). Update datagen and regenerate. Keep the `ServusLogisticsGameTests` facade checks (stone plus iron or gold nugget, slab and ingot refused) green, and add GameTests for the stone ring and the mixed ring refused (the existing `hollowRingRecipe` GameTest already covers both). Verify with runData, build and GameTests.

## 3. JEI display (Foundation)

- [x] 3.1 In `ServusFoundationJeiPlugin`, remove `fluid_tank_example`, keep the `facade_<block>` and `hollow_facade_<block>` examples, and hide the generic `servus_foundation:facade` and `hollow_facade` recipes with `IRecipeManager.hideRecipes` in `onRuntimeAvailable`. Verify with `gradlew :foundation:build`. JEI looks are checked in 5.2.

## 4. Transfer from shown stacks (Shuttles)

- [x] 4.1 Read JEI 26.1.2's vanilla crafting category and grid helper source to confirm that the input slot views come as nine cells in grid order. If they do not, map slots by position. Note the result in design.md if it differs from decision 4.
- [x] 4.2 Add the nine shown cells (at most 64 stacks each) to `OrderGridRecipePayload` and `OpenRecipeEditorPayload`. Fill them in `StorageTerminalRecipeTransferHandler` and `ProductionTerminalRecipeTransferHandler` from `IRecipeSlotsView`. Verify with `gradlew :shuttles:build`.
- [x] 4.3 Add `CraftingGrid.templateOfShown(cells, stock)`, using the component rule from design decision 4 and `MostStocked.pick`. Make `StorageTerminalMenu.fillFromRecipe` and `ProductionTerminalMenu.openEditorWith` use `templateOf` for a known id and `templateOfShown` otherwise. Add GameTests in `OrderGridGameTests` and `PatternGameTests`, where the existing transfer GameTests live: the known tank recipe with cobblestone stocked lays out a cobblestone floor; an unknown id with brick Facade ring cells, while stone and oak Facades are stocked, lays out eight brick Facades; an unknown id with nothing stocked takes the first shown stack. Verify with `gradlew :shuttles:build` and the Shuttles GameTests.

## 5. Changelog and play check

- [x] 5.1 Add `Fixed` lines under `## Unreleased` in `foundation/CHANGELOG.md` (the Fluid Tank, Facade and Hollow Facade recipes are real recipes that JEI and terminals can read; the tank shows every allowed floor in JEI) and in `shuttles/CHANGELOG.md` (JEI's transfer button fills the order grid and the pattern editor for the Fluid Tank and facades). Verify that both files list them.
- [ ] 5.2 Full `gradlew build` and `gradlew test` green, and the game launches with all mods. Play check in the client:
  - JEI shows one tank recipe whose floor cycles.
  - JEI shows per-block facade and hollow examples and no nameless facade.
  - "+" at the Storage Terminal fills the order grid for the tank (stocked floor) and for "Facade (Oak Planks)", and the order crafts.
  - "+" at the Production Terminal saves a tank pattern, and the craftable list shows one Fluid Tank entry.
  - Saving and previewing a tank pattern has no noticeable lag.

## Workflow follow-up

- Archive the change after the user's play check sign-off, in one commit with the CHANGELOG lines.
