# Design

## Context

See proposal.md for the why. Today the Field Head owns the stage: `FieldHeadBlockEntity.stage` and `stageKnown`, set through `setStage`, which writes the stage onto every loaded rail of the field. `RailBlockEntity` already saves a `Corrosion` int per rail and draws it through `getModelData` (a texture swap from `block/farm_rail_head` to `block/farm_rail_head_<stage>`). Joins are handled twice: `FieldHeadBlockEntity.joinStage` on refresh and `FieldHeads.loosePlaced` from `FarmRailBlock.tick` for fields without a head. Hydration is one `FarmlandWaterManager` AABB ticket per `FieldStrip.boxes(rails, radius)` box. `FarmRailBlock.useItemOn` scrapes the whole field or refuses. The rail item drops through the shared `WOODEN` loot table that copies only `MaterialContent.MATERIAL`, and its item model is foundation's `MaterialItemModel`.

## Goals / Non-Goals

**Goals:**
- The rail is the single owner of its stage and scrape progress; the head only derives from rails.
- Water reach, aging steps and the scrape plan are pure Java and unit tested.
- No block scans: the wet set is recomputed only on refresh, aging steps and scrapes.

**Non-Goals:**
- Changing how a field is found (`FieldGraph`, `max_rails`) or how plots are tracked.
- Animating partial scrapes or showing progress on the rail model.

## Decisions

### Rail owns stage and progress
`RailBlockEntity` gets a `ScrapeProgress` int beside `Corrosion`, saved and sent in the update tag like `Corrosion`. A saved `-1` (the old loose stage) loads as copper; `FieldCorrosion.LOOSE`, `join`, `FieldHeadBlockEntity.joinStage`, `stage`, `stageKnown`, `setStage`, `FieldHeads.loosePlaced` and the loose tick in `FarmRailBlock` go away. The head stops reading its saved stage.
Alternative: keep the head stage as a cache. Rejected: two owners of one fact is how joins got complicated.

### Item component
A new data component `servus_shuttles:rail_corrosion`, a record of stage and progress with a `Codec` and `StreamCodec`, registered in `registry/ServusShuttlesComponents` like `RACK_WOOD`. `RailBlockEntity.collectImplicitComponents` writes it only when the stage is past copper or progress is above zero, so crafted and fresh rails carry nothing and stack; `applyImplicitComponents` reads it; `removeComponentsFromTag` drops the two keys. The farm rail moves out of the shared `WOODEN` loot table into its own table that copies `MATERIAL` and the new component (`CopyComponentsFunction.copyComponentsFromBlockEntity`, as the controller does). `RailItem.appendHoverText` (signature as in foundation's `FilterItem`) names the stage past copper. Vanilla stacking compares components, so different stages do not stack without extra code.

### Item look
Foundation's `MaterialSkin` (a final class) gets a constructor overload with a per stack swap function, empty for every existing skin; `MaterialItemModel` caches quads per material plus swaps and passes them to `MaterialTextures.apply`, which already takes a swap map. Shuttles adds a `FARM_RAIL_SKIN` in `ServusShuttlesClient` beside `RACK_SKIN`, with its own item model type registered in `onRegisterItemModels`, whose swap function returns `farm_rail_head -> farm_rail_head_<stage>` from the component; the farm rail item definition in `RailModels` names that type. The stage sprites are already on the block atlas because the block model swaps to them.
Alternative: a separate `FarmRailItemModel` in Shuttles. Rejected: it would copy `MaterialItemModel`, and `MaterialTextures.apply` is package private in foundation.

### Water reach
Pure `FieldCorrosion.wet(start, neighbours, corroded)`: breadth first from the head's start rail over rails that are not corroded; a corroded start gives an empty set. The head keeps `wet` beside `rails`, recomputed at the end of a refresh, after each aging step and after a scrape (the scrape calls the serving head's new `railsChanged()`, which recomputes without a full field walk). Hydration boxes become `FieldStrip.boxes(wet, radius)`; `useWater`, `waterPerMinute` and `secondsToAging` use `wet.size()`; a head with no wet rails uses no water and drops its tickets, as a corroded head does today. The rust rim (`FieldHeadBlock.CORRODED`) shows while any rail of the field is corroded.

### Aging
`FieldCorrosion.age` returns the counted water, the items used and the number of unheld steps instead of a stage. The head applies each step by moving every wet rail one stage older, recomputing `wet` between steps so a rail that just corroded blocks the next step. Rails outside `wet` are untouched.

### Scrape plan
Pure `FieldCorrosion.scrape(stages, progress, perRail, budget)` over rails in order returns the new stage and progress per rail, the points spent and the counts scraped and left. `FarmRailBlock.useItemOn` walks the field from the clicked rail with `FieldHeads.field` (its `LinkedHashSet` is in breadth first order from the start, which is "outward from the clicked rail"), with or without a head. The budget is the axe's remaining durability, or unlimited for an item that is not damageable. It applies the plan to the rails, calls `stack.hurtAndBreak(spent, player, slot)` (Unbreaking is applied inside `ItemStack.processDurabilityChange`, so progress counts the requested points), plays `SoundEvents.AXE_SCRAPE` and `LevelEvent.PARTICLES_SCRAPE` on scraped rails and tells the player the counts. `axeCanScrape` and the `axe_worn` message go away; `too_large` stays.

### Head counts
The head keeps an int per stage and a dry count, refreshed with `wet`. `FieldHeadMenu` syncs them in six `DataSlot`s instead of the single stage slot; `FieldHeadScreen` lists stages with rails and the blocked line. `/servus_warehouse field` prints the counts.

### Settled open decisions
`PLAN.md` has no open point for this; the decisions come from the explore session: stage per rail, water blocked by corroded rails, partial scrape with progress in durability points, 4 per rail as the default (stone axe for about 32 rails).

## Risks / Trade-offs

- [Rails that were unloaded when an old head changed its stage keep an older stage] → acceptable; they only look younger and age from there.
- [Recomputing `wet` after every scrape and step walks up to `max_rails` rails] → bounded by `max_rails` (128) and only on events, never per tick.
- [Foundation API change for item swaps] → default method, so other material blocks are unchanged; foundation and shuttles release together.

## Migration Plan

No data migration: rails keep their saved stage, `-1` reads as copper, the head's saved stage is ignored. `field.json` default changes to 4; packs that override it keep their value.
