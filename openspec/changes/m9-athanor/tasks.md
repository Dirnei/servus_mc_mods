# Tasks

## 1. Tiers

- [x] 1.1 Pure `AthanorTiers` with `TierDef` and `BlockAt` in `manufacturing/.../alchemy/athanor`, JUnit `AthanorTiersTest` (no tiers gives 0, full ring gives 1, missing lower tier block blocks higher tiers, gaps in numbers). Verify: `gradlew test`.
- [x] 1.2 `AthanorTierData` reload listener, shipped `albedo.json`, Salt Crystal Block (block, item, model, loot, tag, lang, 9 to 1 and 1 to 9 recipes). Verify: build, data run.

## 2. Athanor

- [x] 2.1 `AthanorRecipe` with serializer, type and shipped crafts; `AthanorBlock` (facing, lit, `TIER` state, use: bucket, place item, sneak take, GUI), `AthanorBlockEntity` (team owner, input and output, `PrincipleTanks`, tier checks, crafts, production, journal event), `AthanorMenu`, `AthanorScreen` with gauges and tier name, `AthanorRenderer`, registration, capabilities, stats JSON, placeholder textures and models, lang, loot, tag, recipe. GameTests: Blaze Powder at tier 0; ring raises tier and allows the Albedo craft, removal lowers it; bucket fills the Sulfur tank; production lists only allowed crafts. Verify: build, data run, GameTests green.

## 3. Journal, JEI and docs

- [ ] 3.1 `Journal.recordAthanor`, Schwaz's task in `schwaz.json`, the book's Schwaz task page and an Athanor entry text; GameTest that an Albedo craft finishes Schwaz's task for the team. Verify: GameTests green.
- [ ] 3.2 JEI Athanor category. Verify: build.
- [ ] 3.3 Docs: Manufacturing page (Athanor, tiers, Salt Crystal Block) and a datapack page for Athanor crafts and tiers; regenerated `gamedata.json`. Verify: docs tests and build pass.
- [ ] 3.4 Full `gradlew build`, `gradlew test`, GameTests of all three mods green, and the client launches with all three mods: feed an Athanor by bucket, craft Blaze Powder, build the ring and see the flame turn white.
