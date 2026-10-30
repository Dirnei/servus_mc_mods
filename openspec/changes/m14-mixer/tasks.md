# Tasks

## 1. Pure slot logic

- [x] 1.1 `foundation/.../production/SlotMatcher.java` copied unchanged from `delicacies/cooking/SlotMatcher`, and `foundation/src/test/.../production/SlotMatcherTest.java` with the Delicacies cases (one requirement finds its slot, two requirements need distinct slots, backtracking finds the other assignment, a leftover item fails, empty slots are ignored, no requirements match only empty slots, too few items fail) (D3). Verify: `gradlew :foundation:test` green.
- [x] 1.2 `foundation/.../production/KindSlots.java` (the one kind per slot rule, generic, no Minecraft imports) and JUnit `KindSlotsTest`: fits the slot holding the same kind, fits an empty slot when no other holds the kind, refused into an empty slot when another slot holds the kind, refused into a slot of another kind (D3). Verify: `gradlew :foundation:test` green.

## 2. Recipe type

- [x] 2.1 `machine/mixer/MixingInput`, `machine/mixer/MixingRecipe` (codec with `listOf(1, 4)` ingredients and optional `SizedFluidIngredient`, stream codec, `matches`, `slotsFor`, `accepts`, `acceptsFluid`); `MIXING_ID`, `MIXING`, `MIXING_SERIALIZER` in `MachineContent`; `ServusRecipes.syncToClients(MachineContent.MIXING)` in `ServusFoundation` (D1). Verify: `gradlew :foundation:build` green.

## 3. Mixer block

- [x] 3.1 `MachineStats.fluidHolder(int)`; `machine/mixer/MixerParts` (boxes from D6, `shapeParts()`); `MixerBlockEntity` (slots, tank with recipe validator, `Drivable`, `KindSlots` in `acceptsInput`, Items and Fluids layers with back, left and right as fluid sides, menu data, save and load, contents sync of D7); `MixerBlock` (shape, bucket use through `FluidUtil.interactWithFluidHandler`, client ticker for the paddle `Flywheel`); `MixerMenu`; registrations in `MachineContent` (block, item, block entity, menu, creative tab order after the Mill); item, fluid and drive capabilities in `MachineEvents`; `data/servus_foundation/machine/mixer.json` with `fluid_capacity` 4000 (D4, D5). Verify: `gradlew :foundation:build` green.
- [x] 3.2 `client/MixerScreen` (Layout with arrow and side panel, own `FluidGauge` at 148,17 with tooltip) registered in `MachineClient` (D5). Verify: `gradlew :foundation:build` green.

## 4. Look

- [x] 4.1 `tools/mill-textures/gen.py` writes `block/mixer_mix.png`, `block/mixer_port.png` and `gui/mixer.png` (D8); run it, commit the PNGs. Verify: `python tools/mill-textures/gen.py` runs clean, `gradlew :foundation:build` green.
- [x] 4.2 Datagen `datagen/MixerModels` (block model with the static parts, standalone `block/mixer_paddle`, block state, item model with the paddle at rest) wired into `MachineDataParts.blockStates`; loot `dropSelf` and pickaxe tag through `MachineDataParts.selfDropping`; lang name. Go through the overlap table of D6 against the final boxes. Verify: `gradlew :foundation:runData`, generated files committed, `gradlew :foundation:build` green.
- [x] 4.3 `client/MixerRenderer` (paddle turned by the `Flywheel`, contents layer from the synced fluid or `mixer_mix`), paddle model key registered in `MachineClient.onRegisterStandaloneModels`, renderer in `onRegisterRenderers` (D6, D7). Verify: `gradlew :foundation:build` green.

## 5. Recipes and JEI

- [x] 5.1 `datagen/MixingRecipes` with the table of D2 (Mud, Coarse Dirt, 16 Concrete Powders), called from `MachineDataParts.recipes`; the Mixer crafting recipe there (D8); `gradlew :foundation:runData` and check `foundation/src/generated/resources/data/servus_foundation/recipe/mixing/` holds 18 files. Verify: `gradlew :foundation:build` green.
- [x] 5.2 `integration/jei/MixingRecipeCategory` (items, fluid slot, arrow, output, work text), category, recipes and the Mixer as catalyst in `ServusFoundationJeiPlugin`, lang `gui.servus_foundation.jei.mixing` and `.work` in `MachineDataParts` (D8); `gradlew :foundation:runData`. Verify: `gradlew :foundation:build` green.

## 6. Behaviour tests

- [x] 6.1 GameTests in new `machine/mixer/MixerGameTests` (registered beside `MillGameTests`): `motorMixesMud`, `mixerWithoutDriveDoesNothing`, `noWaterNoMud`, `coarseDirtLeavesTheWater`, `concretePowderPerColour`, `leftoverItemBlocksTheMix`, `outputFullWantsNoWork`, `oneKindPerSlot`, `refusesUnusedItems`, `fluidOnlyThroughTheBack`, `automationNeverDrains`, `bucketPoursInAndOut`, `tankRefusesLava`, `pullBackFillsFromTank`, `driveOnlyOnBackAndSides`, `crankTurnsAnEmptyMixer`, `hopperAboveChestInFront`, `chuteRefusesInput`, `mixingRecipesShipped`, `shippedMixingRecipesDoNotCompete` (D9). Verify: `gradlew :foundation:runGameTestServer` green.

## 7. Verification

- [ ] 7.1 Full `gradlew build test` and `:foundation:runGameTestServer` green; the game launches with all mods (Foundation, Alchemy, Shuttles, Delicacies, Brewery).
- [ ] 7.2 User play check in game:
  - The Mixer looks like the mockup: heavy iron base, open steel tub with a brass rim, steel bridge with a brass hub, two crossed heavy iron blades, steel chute, brass port on the back; no flicker from any angle, also while the paddle turns. The item shows the whole Mixer.
  - A Hand Crank goes onto the back or sides only; holding it turns the paddle and Dirt with a Water Bucket poured in becomes Mud in about three seconds; let go, the paddle coasts to a stop and the arrow stops. An Electric Motor does it faster.
  - Water shows as a surface in the tub with the blades through it; Dirt and Gravel without water show the mix surface; an empty tub shows the floor.
  - A hopper of Sand above fills one slot only; Sand, Gravel and a dye give 8 Concrete Powder of that colour; a Dirt and a Sand together mix nothing.
  - A Fluid Tank behind the Mixer with the back on Pull fills it; ducts on the sides or top find no fluid; an empty bucket takes water back out; Lava is refused.
  - A chest in front of the chute collects the result; the carrot cycles item sides, the Fluids tab offers back, left and right, a crank on the back with water piped in from the left works, and the carrot dismantles the Mixer; JEI shows the Mixing category with the fluid.

## Workflow follow-up

- On archive: groom `PLAN.md` (remove `m14-mixer` from "Open: Foundation"; add the open point "switch Delicacies `VesselRecipe` and Brewery `BrewingRecipe` to Foundation's `SlotMatcher` and delete the Delicacies copy" once their changes are archived). Add an "Added" line for the Mixer to `foundation/CHANGELOG.md` under Unreleased.
