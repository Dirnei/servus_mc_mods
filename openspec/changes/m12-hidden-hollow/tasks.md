# Tasks

## 1. Plan

- [x] 1.1 `PLAN.md`: add milestone M12 (Alchemy, Citrinitas and Rubedo) under "Open: Alchemy" pointing to `alchemy_plan.md` and listing its ten changes; remove Undines, Sylphs and the outdated Citrinitas and Rubedo notes in 2.1, the open points and the open decisions that `alchemy_plan.md` now settles. Verify: `openspec validate m12-hidden-hollow` passes, no em dash in the diff.

## 2. The hidden Hollow

- [x] 2.1 Pure `HollowStore` in `alchemy/.../hollow/` (count, capacity, `hasRoom`, `add`, `takeAll`, capacity change keeps the count). JUnit `HollowStoreTest`: `startsEmptyWithRoom`, `fillsToCapacity`, `takeAllEmpties`, `smallerCapacityKeepsCountButHasNoRoom`. Verify: `gradlew :alchemy:test` green.
- [x] 2.2 `GnomeHollowBlockEntity` as a plain `BlockEntity` with its own ticker, tanks, growth, store and team (design D1, D2); `GnomeHollowBlock` as a `BaseEntityBlock` without properties, `setPlacedBy` sets the team, sight gated `useItemOn` and `useWithoutItem` with action bar hints (D3), pick block (D6). Delete `GnomeHollowMenu`, `client/GnomeHollowScreen`, `ServusAlchemyMenus.GNOME_HOLLOW`, its screen registration and `ServusAlchemyEvents.registerGnomeHollow`. `machine/gnome_hollow.json` and `AlchemyMachineDefaults`: `item_capacity` 64. Lang in `ServusAlchemyLanguageProvider` (new hints, screen keys removed), blockstate without facing in `ServusAlchemyBlockStateProvider`; run `:alchemy:runData`. Adapt `HollowGameTests`: `seedingNeedsVillachAndSight` (by `helper.useBlock` with raw iron, without and with sight, before and after Villach), `ripeningTakesPrinciples`, `bucketNeedsSight` (Salt bucket without sight leaves the tank, with sight adds 1,000 mB and returns a bucket), `bindingNeedsElementalSight`, `boundHollowFillsItsStore` (store grows, chest beside it stays empty), `fullStoreStops`, `emptyHandTakesTheStore`, `bindingCountsForSchwaz`, `noCapabilities` (item, fluid, production capability null on every side). Verify: `gradlew build`, `:alchemy:test` and `:alchemy:runGameTestServer` green.
- [x] 2.3 Drops (D4): loot table in `ServusAlchemyBlockLoot` (Hollow with the sight, Stone with Silk Touch, else Cobblestone), store drop in `GnomeHollowBlock.playerDestroy` under the sight; run `:alchemy:runData`. GameTests `breakWithoutSightDropsCobblestone`, `silkTouchWithoutSightDropsStone`, `breakWithSightDropsAFreshHollow` (via `Block.getDrops` with a mock player and tool), `breakWithSightDropsTheStore` (via `playerDestroy`). Verify: `gradlew build`, `:alchemy:runGameTestServer` green.

## 3. Jade

- [x] 3.1 `integration/jade/ServusAlchemyJadePlugin.java` (D5): ray trace callback shows Stone without the sight; with the sight a `HollowProvider` server data (yield, ripeness, steps, bound, store, capacity, principles) and client component. Lang keys `jade.servus_alchemy.gnome_hollow.*` via datagen, `:alchemy:runData`. Verify: `gradlew build` green.

## 4. Texts and docs

- [x] 4.1 Journal: Workshop entry "Gnome Hollow" in `datagen/book/WorkshopCategory.java` (seed by hand under the sight, buckets by hand, no pipes, the store and taking it out, breaking with and without the sight) and the Schwaz text in `JournalEntries.java`; `:alchemy:runData`. Docs: `docs/src/content/docs/mods/alchemy.md` (Gnomes, principles by bucket, no producer on the data line) and `reference/datapack/machines.md` (the store capacity). Verify: `gradlew build` green, generated pages contain the new texts, no em dash in the diff.

## 5. Integration

- [x] 5.1 Full `gradlew build test :alchemy:runGameTestServer` green.
- [ ] 5.2 User play check in game:
  - Without Elemental Sight a placed Hollow looks like stone, Jade shows Stone with the stone icon, right click with raw iron, a Salt bucket, a Salt Crystal or an empty hand does nothing, pick block gives Stone.
  - A duct with item and fluid pipes beside it shows no connection.
  - Under the sight raw iron seeds it, Salt and Mercury buckets fill it and return empty buckets, Jade shows the vein and ripeness, a Salt Crystal binds the ripe vein.
  - The store fills to 64 and stops; an empty hand takes it into the inventory.
  - Breaking without the sight drops cobblestone (stone with Silk Touch); with the sight a fresh Gnome Hollow and the stored ore.
