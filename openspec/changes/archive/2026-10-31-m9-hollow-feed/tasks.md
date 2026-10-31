# Tasks

## 1. Feed logic (pure Java)

- [x] 1.1 Add `alchemy/hollow/VeinFeed` (record of principle to `{amount, bonus}`, `NONE`, `fed(held)`, `factor(held)`, `cost(held)`) and put it on `VeinGrowth.Vein`; update every `new Vein(...)` caller. Add `VeinFeedTest`: empty feed gives factor 1 and cost none; a principle below its amount is not fed; Salt only gives 1 + its bonus; all three sum; cost holds only fed principles; principles outside the feed never count. `gradlew :alchemy:test`
- [x] 1.2 Change `GiftBoost.work()` to `work(double feedFactor)` with one carry for gift times feed. Extend `GiftBoostTest`: x2.5 feed over 4 ticks gives 10 work, gift x2 with feed x3 gives 6 per tick, countdown unchanged. `gradlew :alchemy:test`
- [x] 1.3 Rename `PrincipleDrip.plan`'s `stepCost` to `wanted`, keep its rule. Add a `PrincipleDripTest` case with feed amounts as wanted (only feed principles drip). `gradlew :alchemy:test`

## 2. Data

- [x] 2.1 Parse the optional `feed` map in `VeinData`'s codec (`Codec.unboundedMap` over the principle id codec, amount at least 1, bonus at least 0). `gradlew :alchemy:build`
- [x] 2.2 Rewrite the eight vein JSONs in `alchemy/src/main/resources/data/servus_alchemy/servus_alchemy/vein/` with the new `yield_ticks` and the feed table from design.md. Extend `VeinFilesTest`: every shipped vein has a feed of all three principles with bonuses summing to 2, and `yield_ticks` are at least coal 600, iron and copper 1200, diamond and emerald 4800. `gradlew :alchemy:test`

## 3. Hollow and Dropper

- [x] 3.1 In `GnomeHollowBlockEntity.tick`, pass `VeinFeed.factor` to `boost.work` for a ripe vein with store room (else 1), and on `YIELD` take `VeinFeed.cost`. Make `dripNeed()`/`receiveDrip()` want the feed amounts when ripe and the step cost when unripe; pass that to `PrincipleDrip.plan` in `PrincipleDropperBlockEntity`. `gradlew :alchemy:build`
- [x] 3.2 Sync `feedPace` (rounded factor) in the Hollow's update tag only when it changes, and use `feedPace * gifted pace` in `client/sight/HollowRevealRenderer`. `gradlew :alchemy:build`
- [x] 3.3 Jade: in `ServusAlchemyJadePlugin.HollowProvider`, send the fed flags and the ticks per item for a ripe, bound vein. Show "fed" or "hungry" on feed principle lines and "One item per %s". Add the lang keys in `ServusAlchemyLanguageProvider`, run datagen. `gradlew :alchemy:runData :alchemy:build`
- [x] 3.4 Journal: in `datagen/book/WorkshopCategory`, say on the Gnome Hollow page that a working gnome eats Salt, Sulfur and Mercury and digs faster for it, slowly without, and that each vein has its own taste. Say on the Principle Dropper page that it feeds ripe veins too. Run datagen. `gradlew :alchemy:runData :alchemy:build`

## 4. GameTests

- [x] 4.1 `HollowGameTests`: unfed ripe iron digs at base rate and uses nothing; Salt fed digs at x2 and uses 10 mB per item; all three fed x3; full store uses nothing; replace hard-coded 200 tick yields with the loaded `yieldTicks`. `GnomeGameTests` or gift tests: gift times feed. `DropperGameTests`: replace "ripe vein gets nothing" with "ripe vein gets its feed principles", add a ripe vein without feed (datapack-free: a test vein or a feed-less copy) keeps the Dropper's Salt. `gradlew :alchemy:runGameTestServer` (known: FTB Chunks anchor failures unrelated)

## 5. Verification

- [x] 5.1 Full `gradlew build` and `gradlew test` green; launch the client with all mods. Under the sight, check a ripe iron Hollow: unfed slow flow, pour Salt and see the flow speed up, Jade fed/hungry and per item time, a Dropper on top keeps it fed.

## Workflow follow-up

- At archive: add under `## Unreleased` in `alchemy/CHANGELOG.md`, Changed: ripe Gnome Hollows dig much slower unfed and up to three times faster fed with Salt, Sulfur and Mercury; the Principle Dropper feeds ripe Hollows too.
- At archive: groom PLAN.md: "Gnome upkeep for speed" now covers feed and gifts; add open point "feed quality: a principle gives a chance of a second ore".
