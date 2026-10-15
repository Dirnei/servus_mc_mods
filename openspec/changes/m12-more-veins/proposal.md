# Proposal

## Why

The play check of the Hollows asked for more veins: a gnome grows only iron, copper, gold, redstone and diamond. Nether Quartz, Coal and Emerald are missing, and quartz in particular means a trip to the Nether for every stack. This change adds them to M12 (Alchemy, Citrinitas and Rubedo), next to the seeds of `m12-vein-seeds`.

## What Changes

- Three new veins as datapack JSON, each with its yield, colour, ripening steps, cost per step and yield time:
  - **Coal**: yields Coal, cheap and quick like iron.
  - **Quartz**: yields Nether Quartz, a middle vein like redstone.
  - **Emerald**: yields Emerald, a noble vein like diamond.
- Three new Athanor recipes for their Seeds, at the tier of their stage:
  - Seed of Coal from Coal on an Albedo Athanor.
  - Seed of Quartz from Nether Quartz on a Citrinitas Athanor.
  - Seed of Emerald from an Emerald on a Rubedo Athanor.
- Names "Coal", "Quartz" and "Emerald" for the veins, so the Seeds read "Seed of Coal", "Seed of Quartz", "Seed of Emerald".
- The Journal's seed pages list the new Seeds with their stages. They are generated from the recipe table, so no hand-written text changes.
- Docs: the datapack page `reference/datapack/veins.md` lists all eight veins and their Seeds. It and the Gnomes steps in `mods/alchemy.md` still describe samples and chapters from before `m12-vein-seeds`; both are brought up to date.

## Non-goals

- Other ores (lapis, Ancient Debris, modded metals). A datapack can add them the same way.
- Ores of other mods by tag in the Seed recipes beyond the common `c:` tags.
- New Hollow looks per vein beyond its colour.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `alchemy/gnome-hollow`: "Veins are data" ships eight veins instead of five; "Seeds come from the Athanor" adds the Seeds of Coal, Quartz and Emerald at their tiers.

## Impact

- Data: `alchemy/src/main/resources/data/servus_alchemy/servus_alchemy/vein/coal.json`, `quartz.json`, `emerald.json`.
- Datagen: `AlchemyRecipeTable.java` (three `seed(...)` rows), `ServusAlchemyLanguageProvider.java` (three vein names); `runData` writes the recipes, lang and docs game data.
- Tests: `VeinFilesTest` lists the eight shipped veins; GameTests for the tiers of the new Seeds next to `seedOfGoldNeedsCitrinitas`; `everyShippedSeedPlants` covers the new veins once their files exist.
- Docs: `docs/src/content/docs/reference/datapack/veins.md`, `docs/src/content/docs/mods/alchemy.md`.
- No code outside datagen and tests.
