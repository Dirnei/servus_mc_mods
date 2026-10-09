# Tasks

## 1. Vein logic and data

- [x] 1.1 Pure `VeinGrowth` in `manufacturing/.../alchemy/hollow` with JUnit `VeinGrowthTest` (waits without principles, step timing and cost, ripeness cap, no yield before binding, yield rate, no yield with a full output). Verify: `gradlew test`.
- [x] 1.2 `VeinData` reload listener with the vein codec, shipped `iron.json` and `copper.json`. Verify: build, the GameTest server logs two veins.

## 2. Gnome Hollow

- [x] 2.1 `GnomeHollowBlock`, `GnomeHollowBlockEntity`, `GnomeHollowMenu`, `GnomeHollowScreen`, registration, capabilities (item, fluid, production), stats JSON, model, lang, loot, tag, the Athanor craft, binding with the journal `bind` event. GameTests: seeding refused before Villach and accepted after; a ripening step takes the principles; binding needs Elemental Sight; a bound ripe Hollow yields raw iron into a chest; production reports the yield. Verify: build, data run, GameTests green.
- [ ] 2.2 Client `HollowTracker` and `HollowRevealRenderer` registered with `RevealRenderers`, update tag sync. Verify: build.

## 3. Journal and docs

- [ ] 3.1 Schwaz's `bind` task and the book's Schwaz text about the gnomes; GameTest that binding counts for Schwaz. Verify: GameTests green.
- [ ] 3.2 Docs: Manufacturing page (Gnome Hollow), a vein datapack page, journal and Athanor pages; regenerated `gamedata.json`. Verify: docs tests and build pass.
- [ ] 3.3 Full `gradlew build`, `gradlew test`, GameTests of all three mods green, and the client launches with all three mods: make a Hollow, seed it with raw iron, ripen it with Salt and Mercury, drink the elixir, bind a gnome with a Salt Crystal, see the buds and the gnome, collect raw iron.
