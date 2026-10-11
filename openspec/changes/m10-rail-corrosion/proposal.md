## Why

The Farm Rail got copper heads in `m10-rail-look`, and copper ages. Decided with the user in the explore session of 2026-10-11: a field's copper corrodes with the water its Field Head uses, honeycomb (bees) holds the stage, an axe scrapes it back, and a fully corroded field stops being watered. This gives fields a light upkeep loop and a reason to keep bees, without ever breaking a farm.

## What Changes

- Five corrosion stages for farm rails: copper, exposed, weathered, oxidized, corroded. One stage per field, kept on every farm rail's block entity and drawn as a per block swap of the rail head texture. The Warehouse Rail never corrodes.
- Crafting gives fresh copper and the stage is never on the item. A placed rail is fresh until it joins a field, then takes that field's stage. Joined fields take the stage of the field whose head serves them, or the younger stage when no head serves. A split keeps each part's stage; a part without a head stops aging.
- The Field Head counts the water it uses for hydration. Every `corrosion_water` mB (new key in `storage/field.json`, example 64000) one aging step is due: the head uses one anti-corrosion item from its new upkeep slot and the stage stays, or the field goes one stage further.
- New item tag `servus_storage:anti_corrosion` (honeycomb by default). The Field Head GUI gets an upkeep slot for it; hoppers, ducts and other item handlers can insert into it, and the head is an upkeep holder (`servus_foundation:upkeep`) so a data line with a supplier keeps it stocked at `anti_corrosion_level` (new key, example 8).
- A corroded field is no longer watered: the head stops hydration, its GUI says "Rails corroded, field not watered", and its top plate gets a rust red rim (block state `CORRODED`). Harvest runs continue, nothing breaks, and since no water is used the field stops aging.
- Using an axe on any farm rail of a field scrapes the whole field back one stage, for one axe durability per rail. An axe with less durability left than the field has rails does nothing and the player is told what it needs. The vanilla scrape sound and particles play.
- New rail head textures for exposed, weathered, oxidized and corroded beside `farm_rail_head`.
- Foundation's `WoodModel` learns further sprite swaps from model data (a texture to texture map), so a block can swap materials besides its wood; the farm rail uses it for its head.
- `/servus_warehouse field <pos>` prints the stage and the aging progress; `/servus_warehouse field <pos> age <mB>` counts water as used, for testing.

## Capabilities

### New Capabilities

- `storage/rail-corrosion`: corrosion stages of a field, how rails take a stage, aging by water, anti-corrosion upkeep, the corroded field, scraping with an axe, and the stage looks.

### Modified Capabilities

- `storage/field-head`: the GUI gets the anti-corrosion slot and the corrosion line; the field command prints the stage and can count water for aging.
- `foundation/wood-material`: wood blocks can swap further textures from model data.

## Impact

- Storage: new pure `field/FieldCorrosion` with JUnit tests; `field/FieldHeadBlockEntity` (stage, aging counter, upkeep slot, upkeep holder, corroded stops hydration), `field/FieldHeadBlock` (`CORRODED`), `field/FieldHeads` (headless joins), `field/FarmRailBlock` (axe scrape), `rail/RailBlockEntity` (stage, sync, model data), `menu/FieldHeadMenu`, `client/FieldHeadScreen`, `command/ServusStorageCommands`, `ServusStorageEvents` (item handler and upkeep capability), `crate/StorageStatsManager` and `storage/field.json`.
- Datagen: new `ServusStorageItemTagsProvider`, field head top models with the corroded rim, language.
- Foundation: `wood/Wood` (swap property), `wood/client/WoodModel`.
- Textures: `farm_rail_head_exposed`, `_weathered`, `_oxidized`, `_corroded`, `field_head_top_corroded`.
- GameTests: new `RailCorrosionGameTests`; field, harvester and rail tests stay green.
- Docs: Farm Rail and Field Head entries in `docs/src/content/docs/mods/storage.md`; `PLAN.md` M10 lists the change.
