## 1. Seed

- [x] 1.1 Data component `servus_alchemy:vein`, item `servus_alchemy:seed` with its name from the vein, lang ("Seed of %s", vein names, "Unknown Seed"); run `runData` and `:alchemy:build`
- [x] 1.2 `VeinSyncPayload` and `ClientVeins`, sent on datapack sync; tint source `servus_alchemy:vein_color` and item model with greyscale seed and husk layers (texture generator script); run `runData` and `:alchemy:build`

## 2. Planting and recipes

- [x] 2.1 `VeinData` without `samples` and `chapter`, vein JSON updated; JUnit for the vein parsing; run `:alchemy:test`
- [x] 2.2 New order in `GnomeHollowBlock` / `GnomeHollowBlockEntity`: Salt Crystal binds an empty Hollow, Seed plants on a bound Hollow, another Seed swaps the vein, ripening only while bound, action bar texts; GameTests for bind first, plant, refuse raw ore, refuse without gnome, swap; run `:alchemy:runGameTestServer`
- [x] 2.3 Journal: `bind` without item, `ripen` event with the yield, chapter JSON and task names updated; GameTest that a ripe gold vein finishes the task; run `:alchemy:runGameTestServer`
- [x] 2.4 Athanor seed recipes per tier (datagen), journal Workshop text and Athanor recipe list; GameTest that the Seed of Gold needs a Citrinitas Athanor; run `runData` and `:alchemy:runGameTestServer`
- [x] 2.6 Play check fix: one Seed per loaded vein in the Servus creative tab (from `ClientVeins.ids()` in `ServusAlchemyEvents`) and each Seed its own JEI entry (`registerItemSubtypes` with the `vein` component in `ServusAlchemyJeiPlugin`); run `:alchemy:build :alchemy:test`
- [ ] 2.5 Launch the client from main: seeds show their names and colours, JEI shows the Athanor recipes

## 3. Play check

- [ ] 3.1 User play check: Salt Crystal calls the gnome first, seeds made at the right Athanor tiers and planted by him, another seed swaps the vein, raw ore refused
