# Tasks

## 1. Pure timing

- [x] 1.1 Pure `GiftBoost` in `alchemy/src/main/java/.../alchemy/hollow/` (design D1) and the `work` overload of `VeinGrowth.tick` (D2). JUnit `GiftBoostTest`: `baseRateWithoutGift`, `factorTwoDoublesWork`, `fractionalFactorCarries`, `runsOutAfterDuration`, `newGiftRefreshesInsteadOfStacking`, `loadKeepsFactorAndTicks`; `VeinGrowthTest`: `doubleWorkYieldsTwiceAsFast`, `workDoesNotSpeedRipening`. Verify: `gradlew :alchemy:test` green.

## 2. Gifts on the Hollow

- [x] 2.1 `GnomeGiftData` reload listener (D3), registered in `AlchemyEvents`; `data/servus_alchemy/servus_alchemy/gnome_gift/treats.json`. `GnomeHollowBlockEntity`: `GiftBoost`, `offerGift`, `boostTicksLeft`, `boostFactor`, tick with work, save and load, `Boosted` in the update tag (D4, D6). `GnomeHollowBlock.useItemOn` gift branch with hints (D5); lang keys `message.servus_alchemy.gnome_hollow.gift_taken`, `gift_disliked`, `gift_no_gnome` in `ServusAlchemyLanguageProvider`; `:alchemy:runData`. GameTests in `HollowGameTests`: `likedGiftSpeedsUpTheGnome` (boosted Hollow has twice the store of an unboosted one after 400 ticks), `dislikedItemIsRefused`, `noGiftWithoutSight`, `noGiftWithoutGnome`, `boostExpires` (short boost via `offerGift` path with data factor, then `ticksLeft` 0 and base rate), `boostIsSaved` (save and load through `saveWithFullMetadata` and `loadWithComponents` or the existing helper). Verify: `gradlew build`, `:alchemy:test`, `:alchemy:runGameTestServer` green.

## 3. Showing the boost

- [x] 3.1 Jade `HollowProvider` gift line (D7) with lang key `jade.servus_alchemy.gnome_hollow.gift`; `HollowRevealRenderer` faster gnome while boosted (D6); `:alchemy:runData`. Verify: `gradlew build` green.

## 4. Texts and docs

- [x] 4.1 Journal Workshop entry "Gnome Hollow" in `datagen/book/WorkshopCategory.java`: a short "Gifts" page; `:alchemy:runData`. Docs: `docs/src/content/docs/mods/alchemy.md` (gifts in the Gnomes section) and new `docs/src/content/docs/reference/datapack/gnome-gifts.md` (format and shipped gifts). Verify: `gradlew build` green, the generated page holds the gifts text, no em dash in the diff.

## 5. Integration

- [x] 5.1 Full `gradlew build test :alchemy:runGameTestServer` green.
- [x] 5.2 User play check in game:
  - Under Elemental Sight a cookie on a bound Hollow is used up, the action bar says the gnome is pleased, the red cap spins faster and Jade shows "Gift: x2" with the time counting down.
  - The store fills about twice as fast while boosted, at the base rate after the boost ends.
  - A stick on a bound Hollow stays in the hand with a hint; a cookie on a ripe but unbound Hollow stays with a hint.
  - Without the sight a cookie does nothing and says nothing; Jade still shows Stone.
  - The boost survives leaving and rejoining the world.
