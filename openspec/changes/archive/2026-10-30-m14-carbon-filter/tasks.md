# Tasks

## 1. Pure logic

- [x] 1.1 Add `foundation/.../filter/FilterWear` (record of points and used; `spend(requested)` returning spent, new used and worn out; `left()`; `barWidth(maxWidth)`). JUnit `FilterWearTest`: normal wear, wearing past the last point, wearing a worn out filter, zero and negative requests, bar width at fresh, half and one point left. Verify: `gradlew :foundation:test` green.

## 2. Filter data, items and API

- [x] 2.1 Add `filter/FilterSpec` (record points and spent item, codec) and `filter/FilterDataMaps` (`servus_foundation:filter` item data map, synced, registered on `RegisterDataMapTypesEvent`); add the `filter_used` component to `registry/ServusFoundationComponents`. Verify: `gradlew :foundation:build`.
- [x] 2.2 Add `filter/FilterItem` (bar and tooltip from the data map and `filter_used`) and register `charcoal_powder`, `activated_charcoal`, `carbon_filter` (`FilterItem`, stacks to 16) and `spent_filter` in `registry/ServusFoundationItems`; add them to `registry/ServusFoundationCreativeTabs`. Verify: `gradlew :foundation:build`.
- [x] 2.3 Add `filter/Filters` (`wear` returning `WearResult`, `pointsLeft`, `isFilter`) on top of `FilterWear`. Verify: `gradlew :foundation:build`, GameTests in `gametest/` (new `FilterGameTests`): fresh Carbon Filter worn by 40 shows 472 left, wearing past the last point gives one Spent Filter, Paper returns 0 unchanged, a stack of 3 returns 0 unchanged, the input stack is never mutated.

## 3. Datagen and assets

- [x] 3.1 Recipes: Charcoal to 2 Charcoal Powder (work 100) in `datagen/MillingRecipes`; blasting Charcoal Powder to Activated Charcoal and shapeless 2 Activated Charcoal + `#servus_foundation:filter_binders` + Paper to Carbon Filter in `datagen/ServusFoundationRecipeProvider`; tag `servus_foundation:filter_binders` (`c:crops/wheat`, optional `c:flours`) in `datagen/ServusFoundationItemTagsProvider`. Verify: `runData`, generated JSON reviewed and committed, `gradlew :foundation:build`.
- [x] 3.2 New `datagen/ServusFoundationDataMapProvider` (filter entry for the Carbon Filter with 512 points and the Spent Filter, compostable Spent Filter 0.65), registered in `datagen/ServusFoundationDataGen`. Verify: `runData`, `data/servus_foundation/data_maps/item/filter.json` and `data/neoforge/data_maps/item/compostables.json` generated, GameTest that the Carbon Filter's spec loads with 512 points.
- [x] 3.3 Item models in `datagen/ServusFoundationItemModelProvider`, English names and the tooltip line in `datagen/ServusFoundationLanguageProvider`, four 16x16 textures in `assets/servus_foundation/textures/item/` (`charcoal_powder`, `activated_charcoal`, `carbon_filter`, `spent_filter`). Verify: `runData`, `gradlew :foundation:build`.

## 4. Finish

- [x] 4.1 Full gradle build, tests green, game launches with all mods; in game: mill charcoal, blast the powder, craft a filter with wheat (and with Delicacies flour), check the creative tab, JEI recipes, a used filter's bar and tooltip (via a GameTest or `/give` with the component), and composting Spent Filters. Verify: `gradlew build` and `gradlew test` green, client starts from main.

## Workflow follow-up

- At archive: add the change to the PLAN.md M14 list (Foundation part before `m14-aquarium`) and then groom it away with the archive, and add an Added line to Foundation's `CHANGELOG.md` under `## Unreleased`.
