# Tasks

## 1. The machine

- [x] 1.1 Defaults: `AlchemyMachineDefaults` adds `salamander_forge` (D3), data `machine/salamander_forge.json`; JUnit `AlchemyMachineDefaultsTest.forgeSmeltsAtFurnacePace` (kind crafter, recipe type `minecraft:smelting`, `work_ticks` 200). Verify: `gradlew :alchemy:test` green.
- [x] 1.2 `forge/SalamanderForgeBlock.java`, `forge/SalamanderForgeBlockEntity.java`, `forge/SalamanderForgeMenu.java` (D1, D2, D4, D6); registrations in `ServusAlchemyBlocks`, `ServusAlchemyItems` (creative tab), `ServusAlchemyBlockEntities`, `ServusAlchemyMenus`; item and production capabilities in `ServusAlchemyEvents`; screen layout in `client/MachineScreens` and registration in `ServusAlchemyClient` (D7); GUI texture `textures/gui/salamander_forge.png` from a script in the job directory. Verify: `gradlew build` green.

## 2. Look

- [ ] 2.1 Foundation `ModelBuilder.ElementBuilder.lightEmission` (D8); emblem texture `textures/block/salamander_forge_emblem.png` from a script in the job directory; model and block state in `AlchemyModels` (D8, overlap check in design), loot `dropSelf`, pickaxe tag, lang name; `:alchemy:runData`, commit generated files. Verify: `gradlew build` green.

## 3. Behaviour tests

- [ ] 3.1 GameTests in new `ForgeGameTests` (registered in `ServusAlchemy`): `smeltsIronWithoutFuel`, `refusesWhatDoesNotSmelt`, `stopsWhenOutputIsFull`, `alwaysLitAndGlows`, `hopperInChestOut` (hopper above feeds, chest below gets the ingots through the push), `pipeFeedsTheForge` (Item Pipe pulls Raw Iron from a chest into the Forge), `offSideRefuses`, `frontIsClosed`, `exposesSmeltingProduction` (Raw Iron to Iron Ingot at the work time, active recipe), `producerOnTheDataLine` (producer, no upkeep), `breakingDropsContents`. Verify: `:alchemy:runGameTestServer` green.

## 4. Recipe, journal and docs

- [ ] 4.1 Shaped recipe in `ServusAlchemyRecipeProvider` (D9); GameTest `forgeRecipeShipped` in `ForgeGameTests` (the recipe resolves to the Forge); JEI crafting station in `ServusAlchemyJeiPlugin`; `:alchemy:runData`. Verify: `gradlew build`, `:alchemy:runGameTestServer` green.
- [ ] 4.2 Workshop entry `salamander_forge` in `WorkshopCategory` (D10); `:alchemy:runData`. Docs: Salamander Forge section in `docs/src/content/docs/mods/alchemy.md`, row and text in `reference/datapack/machines.md`. Verify: `gradlew build` green, no em dash in the diff.

## 5. Verification

- [ ] 5.1 Full `gradlew build test :alchemy:runGameTestServer` green; client smoke start from main reaches the title screen without errors mentioning servus.
- [ ] 5.2 User play check in game:
  - The Forge looks like a compact deepslate brick furnace with an open mouth, glowing embers and a full bright fire, a steel plate with a salamander above the mouth; no flicker from any angle, also while the fire animates.
  - It lights the room at all times; flames and smoke rise from the mouth.
  - The screen shows input, a full flame, the arrow and the output, plus the side panel; Raw Iron smelts at a furnace's pace without any fuel.
  - A hopper above and a chest below work; an Item Pipe feeds it; the carrot configures and dismantles it.
  - On a data line a Crafting CPU orders Iron Ingots from it.
  - The crafting recipe works; the Workshop entry "The Salamander Forge" reads well; JEI shows the Forge on smelting recipes.
