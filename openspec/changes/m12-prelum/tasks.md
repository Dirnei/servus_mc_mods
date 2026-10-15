# Tasks

## 1. Recipe and machine

- [ ] 1.1 Defaults: `AlchemyMachineDefaults` adds `prelum` (kind crafter, recipe type `servus_alchemy:pressing`, `work_ticks` 600, D5), data `machine/prelum.json`; JUnit `AlchemyMachineDefaultsTest.prelumPressesSlowly`. Verify: `gradlew :alchemy:test` green.
- [ ] 1.2 `prelum/PressingRecipe.java` (D1), registration and client sync in `ServusAlchemyRecipes` and `ServusAlchemy`; `prelum/PrelumBlock.java`, `prelum/PrelumBlockEntity.java`, `prelum/PrelumMenu.java` (D3, D4, D6, D10 event); registrations in `ServusAlchemyBlocks`, `ServusAlchemyItems` (block item, `prelum_screw`, creative tab), `ServusAlchemyBlockEntities`, `ServusAlchemyMenus`; capabilities in `ServusAlchemyEvents`; `client/MachineScreens.PRELUM` and registration in `ServusAlchemyClient` (D7); GUI texture `textures/gui/prelum.png` and item texture `textures/item/prelum_screw.png` from a script in the job directory. Verify: `gradlew build` green.

## 2. Look

- [ ] 2.1 Models `prelum` and `prelum_item` and the block state in `AlchemyModels` (D9, overlap check in design), item model for the screw, loot `dropSelf`, pickaxe tag, lang names; `client/PrelumRenderer.java` registered in `ServusAlchemyClient` (D9); `:alchemy:runData`, commit generated files. Verify: `gradlew build` green.

## 3. Recipes

- [ ] 3.1 `AlchemyRecipeTable.pressing()` and the Prelum Screw Athanor row (D2, D8); recipe provider writes the pressing recipe and the shaped Prelum recipe; `:alchemy:runData`. Verify: `gradlew build` green.

## 4. Behaviour tests

- [ ] 4.1 GameTests in new `PrelumGameTests` (registered in `ServusAlchemy`): `pressesCaputMortuumIntoScrap`, `waitsForAFullCharge`, `refusesOtherItems`, `stopsWhenOutputIsFull`, `outputThroughTheCapability`, `frontRefuses`, `hopperInChestOut`, `exposesPressingProduction`, `producerOnTheDataLine`, `pressingCountsForBasel`, `pressingRecipeShipped`, `screwNeedsDragonsBreathOnRubedo` (the Athanor recipe takes Dragon's Breath at tier 3 and the crafting recipe needs the screw), `breakingDropsContents`. Verify: `:alchemy:runGameTestServer` green.

## 5. Journal, JEI and docs

- [ ] 5.1 `basel_salzburg.json` gains the `press` task and the Basel and Salzburg task page lists it (D10); Workshop entry `prelum` in `WorkshopCategory` (D11); JEI `PressingRecipeCategory` and catalyst in `ServusAlchemyJeiPlugin`; lang for the category; `:alchemy:runData`. Verify: `gradlew build`, `:alchemy:runGameTestServer` green.
- [ ] 5.2 Docs: Prelum section in `docs/src/content/docs/mods/alchemy.md`, pressing recipe and machine row in the datapack reference. Verify: no em dash in the diff.

## 6. Verification

- [ ] 6.1 Full `gradlew build test :alchemy:runGameTestServer` green; client smoke start from main reaches the title screen without errors mentioning servus.
- [ ] 6.2 User play check in game:
  - The Prelum looks like a heavy screw press: deepslate base, steel bed, dark oak posts and beam, steel screw, bar and plate; no flicker from any angle, also while the plate moves.
  - While pressing, the bar turns and the plate goes down and up; idle, the plate rests at the top. The item shows the whole press.
  - The screen shows input, arrow and output plus the side panel; 16 Caput Mortuum become one Netherite Scrap after 30 s, without fuel; Cobblestone is refused.
  - A hopper above and a chest below work; an Item Pipe feeds it; the carrot configures and dismantles it.
  - On a data line a Crafting CPU orders Netherite Scrap from it.
  - A Rubedo Athanor makes the Prelum Screw from Dragon's Breath; the crafting recipe works; the Workshop entry "The Prelum" reads well; JEI shows the Pressing category.
  - The first Netherite Scrap counts for Basel and Salzburg.
