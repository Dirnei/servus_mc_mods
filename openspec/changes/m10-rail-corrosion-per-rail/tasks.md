# Tasks

## 1. Pure logic

- [ ] 1.1 `shuttles/.../field/FieldCorrosion`: remove `LOOSE`, `join` and `axeCanScrape`; `age` returns counted water, items used and unheld steps instead of a stage; add `wet(start, neighbours, corroded)` (breadth first, corroded rails dry and blocking, corroded start gives none) and `scrape(stages, progress, perRail, budget)` returning new stages and progress per rail, points spent, rails scraped and rails left. JUnit in `FieldCorrosionTest`: aging steps with and without items, steps counted past corroded; wet on a straight field with a corroded middle, corroded start, loop around a corroded rail, corroded far end; scrape with full budget, worn budget leaving progress, progress finishing a rail, copper rails skipped and free, unlimited budget, at most one stage per rail. Drop the join and axe check tests. Verify `gradlew :shuttles:test`

## 2. Rail owns stage and progress

- [ ] 2.1 `shuttles/.../registry/ServusShuttlesComponents`: `RAIL_CORROSION` component, a record of stage and progress with `Codec` and `StreamCodec`. `shuttles/.../rail/RailBlockEntity`: `ScrapeProgress` saved, loaded and in the update tag; saved `-1` loads as copper; collect the component only past copper or with progress, apply it, remove its keys in `removeComponentsFromTag`; Javadoc no longer says the stage is never on the item. Verify `gradlew build`
- [ ] 2.2 `shuttles/.../datagen/ServusShuttlesBlockLoot`: farm rail out of `WOODEN` into its own table copying `MATERIAL` and `RAIL_CORROSION`. `shuttles/.../rail/RailItem`: `appendHoverText` names the stage past copper. `ServusShuttlesLanguageProvider`: tooltip keys. Run `gradlew :shuttles:runData`; verify `gradlew build`
- [ ] 2.3 Item look: `foundation/.../material/client/MaterialSkin` constructor overload with a per stack swap function (empty for existing skins), `MaterialItemModel` caches quads per material plus swaps and applies them; `shuttles/.../client/ServusShuttlesClient` adds `FARM_RAIL_SKIN` with its own item model type registered in `onRegisterItemModels`, swapping `block/farm_rail_head` to `block/farm_rail_head_<stage>` from the component; datagen names that type for `farm_rail` through the `ModelProvider` item type override, like the rack. Run `gradlew :shuttles:runData`; verify `gradlew build`

## 3. Field Head derives from rails

- [ ] 3.1 `shuttles/.../field/FieldHeadBlockEntity`: remove `stage`, `stageKnown`, `joinStage`, `setStage` and reading the saved stage; keep `wet`, counts per stage and dry count, recomputed at the end of a refresh, after each aging step and from a new `railsChanged()`; hydration boxes from `wet`; `useWater`, `waterPerMinute`, `secondsToAging` use `wet.size()`; no wet rails drops tickets and uses no water; `age` applies each unheld step to the wet rails and recomputes `wet` between steps; `applyCorrodedLook` shows the rim while any rail is corroded. `shuttles/.../field/FieldHeads`: remove `loosePlaced`. `shuttles/.../field/FarmRailBlock`: remove the loose tick and `besideStagedRail`. Verify `gradlew build`
- [ ] 3.2 `shuttles/.../field/FarmRailBlock.useItemOn`: walk the field from the clicked rail with `FieldHeads.field` (with or without a head), budget from remaining durability or unlimited, apply `FieldCorrosion.scrape` to the rails, `hurtAndBreak(spent, ...)`, sound and scrape particles on scraped rails, tell the player rails scraped and left, call the serving head's `railsChanged()`. Language: replace `axe_worn` with the scraped messages. `data/servus_shuttles/shuttles/field.json`: `axe_damage_per_rail` 4, and the default in `ShuttlesStatsManager` if it has one. Verify `gradlew build`
- [ ] 3.3 `shuttles/.../menu/FieldHeadMenu`: six data slots (one per stage plus dry) instead of the stage slot; `shuttles/.../client/FieldHeadScreen`: Water tab lists stages with rails and the blocked line with dry rails, progress tooltip keeps the aging estimate, red marker while dry or blocked; `shuttles/.../command/ServusShuttlesCommands`: field print shows counts and dry rails, age command reports counts. Language keys. Run `gradlew :shuttles:runData`; verify `gradlew build`

## 4. GameTests

- [ ] 4.1 `shuttles/.../gametest/RailCorrosionGameTests`: rewrite for per rail stages: placed rail stays fresh in a weathered field, dropped rail keeps its stage, aging moves wet rails and leaves rails behind a corroded one, corroded middle rail dries the rails behind it, corroded first rail stops all water, iron axe scrapes five rails for twenty, worn axe scrapes two and leaves progress on the third, progress finishes a rail, scraping a corroded first rail waters the field again, fresh copper not scraped; drop `placedRailAdoptsStage`, `droppedRailIsFresh`, `headlessJoinTakesYoungest`, `wornAxeRefused`. `LumberGameTests`: check rail stages instead of `head().corrosion()`. Verify `gradlew :shuttles:runGameTestServer`

## 5. Wrap up

- [ ] 5.1 Full gradle build, tests green, game launches with all mods; play check by the user: place a fresh rail into an old field, break and replace an oxidized rail, scrape with a new stone axe and a nearly broken one, corrode the first rail and watch the field dry, read the Water tab counts, check the rail item textures and tooltips in hand and inventory

## Workflow follow-up

- At archive: update the Purpose line of `openspec/specs/shuttles/rail-corrosion/spec.md` (it still says per field and one durability per rail), add a Changed line to `shuttles/CHANGELOG.md` under Unreleased.
