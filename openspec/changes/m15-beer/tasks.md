# Tasks

## 1. Pure logic

- [x] 1.1 `beer/Fermentation` (D3) in `brewery/src/main/java/io/github/dirnei/mc/servus/brewery/`; JUnit `FermentationTest` (not done before its time, done at and after, remaining stops at 0, percent at start, half and end, zero time, game time before the start); verify `gradlew :brewery:test`

## 2. Textures

- [x] 2.1 `tools/brewery-textures/gen.py`: `helles`, `dunkles`, `weissbier`, `weissbier_glass`, `yeast` items; `fermenting_vat_side`, `vat_wort`, `vat_foam`, `vat_beer`, `cask_tap` blocks (D12); run it, check an 8x preview; verify `gradlew build`

## 3. Fluids, recipes, stats, items

- [x] 3.1 `registry/BreweryFluids`: `BrewFluid`, beers, `BEERS`, `ALL`; client fluid models and datagen loops on `ALL` (D9); verify `gradlew build`
- [x] 3.2 `beer/FermentingRecipe`, `FermentingInput`, `TappingRecipe`, `TappingInput`; types and serializers in `registry/ServusBreweryRecipes` with client sync and lookups; `beer/CellarStats`, `CellarStatsManager` with `data/servus_brewery/cellar/stats.json`, reload listener in `ServusBreweryEvents` (D2, D5, D10); verify `gradlew build`
- [x] 3.3 `beer/BeerItem`, `beer/BeerDrunkEvent`; items Yeast, Weissbier Glass, the three drinks, Beer Glass to 16 in `registry/ServusBreweryItems` (tab order) (D7, D8); verify `gradlew build`

## 4. Blocks

- [x] 4.1 `beer/VatContent`, `FermentingVatBlock`, `FermentingVatBlockEntity`, `VatFluids`; `beer/CaskBlock`, `CaskBlockEntity`; registration in `ServusBreweryBlocks`, `ServusBreweryItems`, `ServusBreweryBlockEntities`; capabilities in `ServusBreweryEvents` (D4, D6); verify `gradlew build`

## 5. Datagen

- [x] 5.1 Datagen: vat and Cask models and block states (D11), item models, lang, loot, axe tag, recipes (yeast, glasses, vat, Cask, fermenting, tapping), food data map entries (D7, D13); check the face tables of D11 against the generated models; run `gradlew :brewery:runData`, commit `src/generated/resources`; verify `gradlew build`

## 6. JEI

- [x] 6.1 `integration/jei/FermentingRecipeCategory`, registration and catalyst in `ServusBreweryJeiPlugin`, lang (D14); verify `gradlew build`

## 7. GameTests

- [x] 7.1 `gametest/BeerGameTests` (D15), listed in `BreweryGameTests`; verify `gradlew :brewery:runGameTestServer` and `gradlew build test`

## 8. Play check

- [ ] 8.1 Play check by the user:
  - Craft Yeast (Wheat, Sugar, Water Bottle), Beer Glasses, Weissbier Glasses, a Fermenting Vat and a Cask; empty glasses stack to 16.
  - Look at the vat from every side: open spruce vat with two iron bands, no flicker; empty, then wort, then foam with small bubbles while it ferments, then beer.
  - Pale Wort bucket into the vat, then Yeast: it ferments (empty hand shows the percent); wort cannot be taken back out; after about one and a half days the vat holds Helles and the Yeast is still there; sneak with an empty hand takes the Yeast back. Dark Wort gives Dunkles, Wheat Wort Weissbier.
  - Pipes: wort and yeast in, beer out only when done; a pipe from the vat to the Cask moves the beer.
  - Cask: barrel with a brass tap that faces you; no flicker on the tap from several angles; a wort bucket is refused; empty hand tells the beer and amount.
  - Tap with a Beer Glass: Glass of Helles or Dunkles; Weissbier needs the Weissbier Glass and refuses the Beer Glass; clicking the side of the Cask with a glass does nothing.
  - Drink each beer: drink animation, the empty glass comes back, a little hunger restored, drinkable when full; Dunkles shows the darker icon.
  - JEI shows the Fermenting category for Helles, Dunkles and Weissbier; beer buckets show their colours.
