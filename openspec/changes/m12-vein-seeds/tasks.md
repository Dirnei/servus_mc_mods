## 1. Seed

- [ ] 1.1 Data component `servus_alchemy:vein`, item `servus_alchemy:seed` with its name from the vein, lang ("Seed of %s", vein names, "Unknown Seed"); run `runData` and `:alchemy:build`
- [ ] 1.2 `VeinSyncPayload` and `ClientVeins`, sent on datapack sync; tint source `servus_alchemy:vein_color` and item model with greyscale seed and husk layers (texture generator script); run `runData` and `:alchemy:build`

## 2. Planting and recipes

- [ ] 2.1 `VeinData` without `samples` and `chapter`, vein JSON updated; `GnomeHollowBlock` plants only seeds, action bar text for other items; JUnit for the vein parsing; GameTests for planting a seed and refusing raw ore; run `:alchemy:test` and `:alchemy:runGameTestServer`
- [ ] 2.2 Athanor seed recipes per tier (datagen), journal Workshop text and Athanor recipe list; GameTest that the Seed of Gold needs a Citrinitas Athanor; run `runData` and `:alchemy:runGameTestServer`
- [ ] 2.3 Launch the client from main: seeds show their names and colours, JEI shows the Athanor recipes

## 3. Play check

- [ ] 3.1 User play check: seeds made at the right Athanor tiers, planting by seed, raw ore refused
