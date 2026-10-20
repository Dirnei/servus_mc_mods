# Tasks

## 1. Pure logic

- [x] 1.1 `meat/LoafSlices` and `LoafSlicesTest` (left counts, last slice, next, box shrinks from the west end, out of range throws) (D6). Verify: `gradlew :delicacies:test` green.

## 2. Blocks and items

- [x] 2.1 `meat/LeberkaeseBlock` (facing, cut, shape, knife slicing, placement on a solid top); block registration; the meat items, `MEAT_FOODS`, `MEAT_ITEMS` and the tab order in `registry/ServusDelicaciesItems`; Gravy with `usingConvertsTo(Items.BOWL)`; cheese and mustard flour tags in `kitchen/KitchenTags` (D3, D5 to D7). Verify: `gradlew build` green.

## 3. Look

- [x] 3.1 `tools/delicacies-textures/gen.py`: port `blob`, the ramps and the mockup sprites (`weisswurst`, `senf`, `leberkas`, `leberkassemmel`, `schnitzel` without the lemon, `cordon_bleu`, `schweinebraten`); new sprites for the cuts, minced meat, breaded cutlets, Ham, Sausage Meat, Mustard Flour, slice, Gravy and the loaf faces (D10). Verify: the script runs clean, look at the 8x previews.
- [x] 3.2 Datagen: `datagen/MeatModels` (loaf models and block state), item models, lang, loot (loaf or slices by `cut`), block tags (mineable by hand: none), item tags, recipes (cutting, crafting, pot, pan, baking, milling), food data map (D1 to D9); overlap check from the D9 table against the final box. Verify: `:delicacies:runData`, generated files committed, `gradlew build` green.

## 4. JEI

- [x] 4.1 Loaf info page in `integration/jei/ServusDelicaciesJeiPlugin`, lang (D11). Verify: `gradlew build` green.

## 5. GameTests

- [x] 5.1 `gametest/MeatGameTests` (D12), registered in `DelicaciesGameTests`. Verify: `:delicacies:runGameTestServer` and `gradlew build test` green.

## 6. Play check

- [ ] 6.1 Play check by the user:
  - Cut a Porkchop into a Pork Cutlet and the cutlet into Minced Pork, Beef into Minced Beef on the Cutting Board.
  - Craft a Breaded Cutlet and fry it in the Pan into a Schnitzel; with a cheese in `c:foods/cheese` (from `m14-sides`) craft a Raw Cordon Bleu and fry it.
  - Simmer Ham from a Porkchop, Salt and water; craft Sausage Meat and simmer it into Weisswurst.
  - Mill Mustard Seeds into Mustard Flour and cook Sweet Mustard with Sugar and water.
  - Bake a Leberkaese loaf, place it, cut four slices with the Kitchen Knife (the loaf gets shorter, the cut face is pink, the last slice removes it), break a cut loaf and get the rest as slices; craft a Leberkaese Roll.
  - Roast Pork in the Baking Oven; make Gravy in the Pot and eat it, the bowl comes back.
  - Eat each dish; JEI shows the recipes and the loaf's info page.
  - Look at the loaf from several angles in every cut state: no flicker, crust and cut face read right.
