# Tasks

## 1. Pure logic

- [x] 1.1 Add pure `alchemy/.../alchemy/PurificationText.java` (`line`, `kindLine`) with JUnit `PurificationTextTest`: Salt and residue, residue only (wash), Salt only (no residue), the kind line. Verify: `gradlew :alchemy:test` green.

## 2. Items

- [x] 2.1 Add `tools/purifier-textures/gen.py` and generate `kandis.png`, `brimstone.png`, `cinnabar.png` in `alchemy/src/main/resources/assets/servus_alchemy/textures/item/`; check the 8x preview next to Sulfur and Salt Crystal. Verify: the preview reads as rock sugar, yellow rock and red crystal.
- [x] 2.2 Register `KANDIS`, `BRIMSTONE`, `CINNABAR` as `QuoteItem`s in `ServusAlchemyItems` (creative tab after Quicksilver); item models in `ServusAlchemyItemModelProvider`; names and quotes in `ServusAlchemyLanguageProvider`. Verify: `gradlew build` green.

## 3. Recipes and book

- [x] 3.1 `AlchemyRecipeTable`: residue optional in `Purify`, the seed and Salt rows in `purification()`, `Wash` and `washes()` over `DyeColor`, `allPurification()`; `ServusAlchemyRecipeProvider` writes `allPurification()`. Verify: `gradlew :alchemy:runData` writes 8 plus 62 purification recipes, `gradlew build` green.
- [x] 3.2 `WorkshopCategory.purifier()`: "What It Washes" from `PurificationText`, the Leaching text names the other washes. Verify: `gradlew :alchemy:runData` and the generated Purifier entry lists the Sugar line and one line per wash kind.

## 4. Machine, looks and viewers

- [x] 4.1 `PurifierBlockEntity` sends the input and residue item ids in the update tag and resyncs on a kind change; `PurifierRenderer` draws block items with their block's particle sprite. List the overlapping boxes (unchanged) in the commit check. Verify: `gradlew build` green.
- [x] 4.2 Jade Purifier provider: item ids and names with the `jade.servus_alchemy.purifier.item` key; lang updated. Verify: `gradlew build` green.
- [x] 4.3 `PurificationRecipeCategory`: residue slot, Salt slot only above 0. Verify: `gradlew build` green.

## 5. Tests and integration

- [x] 5.1 GameTests in `alchemy/.../gametest/PurifierGameTests.java`: `purifierGrowsKandisFromSugar`, `purifierWashesConcreteWithoutSalt`, `purifierRefusesPlainWool`. Verify: `gradlew :alchemy:runGameTestServer` green.
- [x] 5.2 Full `gradlew build`, unit tests and the alchemy GameTests green; `gradlew :alchemy:runData` output committed.
- [x] 5.3 Play check in game by the user from main:
  - Kandis, Brimstone and Cinnabar look right in the hand and in JEI, next to Sulfur and Salt Crystal.
  - Sugar, Gunpowder and Redstone Dust in the Vat give their seed item in the Crock and a little Salt in the Basin.
  - Red Concrete Powder and Blue Wool wash without Salt; the Vat floor and the Crock heap show the block textures; Jade names the items.
  - JEI's Purification category shows the residue, and no Salt slot for a wash.
  - The Purifier entry lists the new recipes on readable pages.

  Verify: the user confirms in game.

## Workflow follow-up

- At archive: add under `## Unreleased` in `alchemy/CHANGELOG.md`: Added: the Purifier washes Sugar, Gunpowder and Redstone into Kandis, Brimstone and Cinnabar, Salt from Dried Kelp, Rotten Flesh, Soul Sand and Magma Cream, and colour out of Concrete Powder, Wool, Carpet and Terracotta.
- At archive: PLAN.md has no open point for this change; nothing to remove.
