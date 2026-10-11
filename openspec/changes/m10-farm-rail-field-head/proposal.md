# Proposal: m10-farm-rail-field-head

PLAN.md milestone M10 (Field work), item 2: `m10-farm-rail-field-head`.

## Why

Harvester shuttles (`m10-harvester`) need somewhere to work: a field they can drive along, kept wet without vanilla water channels, and a block that knows when the field is worth a harvest run. This change builds that field side, so the next change only has to add the Harvester module and its jobs.

## What Changes

- New **Farm Rail** in Storage: a Warehouse Rail variant. It stands, hangs, bridges, auto connects and joins the warehouse network exactly like a Warehouse Rail, so shuttles drive on it. It is not waterlogged. One connected run of farm rails (linked sideways to each other, ordinary rails do not count) is one **field**.
- New **Field Head** block: one per field, touching a farm rail of the field on a horizontal side. It takes water only, through the Foundation fluid pipe or any mod's fluid handler, into a buffer (`field.json` `capacity`, example 8000 mB). While it holds water it uses `water_per_rail` mB per farm rail every `water_interval` ticks and hydrates the field.
- **Hydration:** while the field is hydrated, every farm rail keeps the farmland in the 9 by 9 square around it, one block below the rail, wet through NeoForge `FarmlandWaterManager` tickets. Tickets are not saved: the head adds them on load and drops them when the water runs out, the field changes or the head goes away.
- **Ripeness:** the head knows which plots (the crop level of the hydrated squares, rail columns excluded) hold a crop and which hold a ripe one. It learns from `CropGrowEvent.Post`, `BonemealEvent`, block break and place events and farmland trampling, plus one pass over the field when it loads or changes. It never scans per tick. A **harvest due** signal comes on when the ripe share of the planted plots reaches `harvest_share` (example 0.5) and the field has a crop set or is in harvest only mode.
- **Field Head GUI:** water level, field size (rails and plots), planted and ripe counts, a ghost seed slot for the one crop of the field, and a harvest only toggle (exclusive with the crop).
- Diagnostic `/servus_warehouse field <pos>` and `/servus_warehouse field <pos> recheck`.
- New `data/servus_storage/storage/field.json` with the field balance numbers. Recipes, models, blockstates, loot, tags and lang through datagen; placeholder textures.
- `PLAN.md` M10 item 2 and the docs storage page describe the Farm Rail and the Field Head.

## Capabilities

### New Capabilities
- `storage/farm-rail`: the Farm Rail as a Warehouse Rail variant and fields as connected farm rail runs.
- `storage/field-head`: the Field Head, its water buffer and hydration, ripeness tracking with the harvest due signal, its GUI and the diagnostic command.

### Modified Capabilities
None. The Farm Rail is a rail in every requirement of `storage/warehouse-rail`, which already speaks of rails in general.

## Non-goals

- Harvester module, field jobs, tilling, planting and harvesting (`m10-harvester`).
- Tree fields and the Lumber module (`m10-lumber`).
- Crops that are not vanilla style age crops (stems, pitcher plants, sweet berries, nether wart).
- A field over more than one level: farm rails link sideways only, like all rails.

## Impact

- Storage only, new package `storage/field` (pure field logic plus the block, block entity, events and registry of loaded heads), `menu/FieldHeadMenu`, `client/FieldHeadScreen`.
- `ServusStorageBlocks`, `ServusStorageItems`, `ServusStorageBlockEntities` (Farm Rail reuses the rail block entity type; new Field Head type), `ServusStorageMenus`, `ServusStorageEvents` (fluid capability), `StorageStatsManager` (`field.json`), `ServusStorageCommands`, datagen providers.
- No new payloads: the GUI uses container data, slot clicks and menu buttons.
- Unreleased project: no migration.
