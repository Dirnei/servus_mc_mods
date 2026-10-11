# Proposal: m10-harvester

PLAN.md milestone M10 (Field work), item 3: `m10-harvester`.

## Why

The two earlier M10 changes built shuttles with modules (`m10-shuttle-workshop`) and fields that stay wet and know when they are ripe (`m10-farm-rail-field-head`). Nothing works the field yet: a player still harvests and replants by hand. This change adds the Harvester module and field jobs, so a warehouse tends its fields with its own shuttles and the harvest comes home through put-away.

## What Changes

- New **Harvester** module (tool slot, field only), crafted and fitted in the Shuttle Workshop like the Cargo Hold. A shuttle with a Harvester never takes warehouse jobs.
- **Field jobs:** the warehouse whose rails hold a field checks the field's Field Head every `field_check_interval` ticks (new key in `field.json`). When a harvest is due, or the field has a crop set, is hydrated and has dirt to till or empty farmland to plant (with seeds in the warehouse), it queues one field job for that field. Only a shuttle with a Harvester takes it; one harvester works a field at a time.
- **Field run:** the harvester drives from its garage over the rails onto the farm rails and stops rail by rail. At each stop it works the plots that are nearest to that rail (at most 4 blocks away, the strip of the field), one plot per `work_ticks` ticks (new key in `field.json`): it harvests ripe crops into its cargo, tills dirt and grass blocks under empty plots into farmland, and plants the field's crop on empty farmland. In harvest only mode it only harvests. When its cargo is full or the work is done, it puts the harvest away into warehouse storage by the normal put-away routing (zones apply) and comes back while work remains.
- **Cargo:** a field run carries `max(stacks of the shuttle, harvester_stacks)` stacks (new key in `field.json`, example 2, so a crop and its seeds fit a shuttle without a Cargo Hold). A plot is harvested only when its drops fit.
- **Seeds:** replanting takes the crop item from the harvest first; while plots remain to plant, the shuttle keeps up to one stack of it through a put-away. Only when the field has nothing ripe to harvest and the shuttle carries too few seeds does it pick them from the warehouse before it drives out. Without seeds in the warehouse, empty farmland stays empty.
- **Edge cases:** a run ends early when the head is gone or no longer serves the field; water loss ends planting work at the next turn but harvesting goes on; rails removed under the shuttle, a removed garage and a recall drop the cargo at the garage like other lost loads; a full warehouse makes the shuttle wait at its garage with the cargo until there is room.
- The **Field Head** also tracks which plots can be tilled and which can be planted, from the same events plus changes at the farmland layer, so the warehouse knows of planting work without scanning.
- Recipe, item model and lang for the Harvester through datagen; placeholder texture.
- `PLAN.md` M10 item 3 and the docs storage page describe the Harvester.

## Capabilities

### New Capabilities
- `storage/harvester`: the Harvester module, field jobs, the field run (route, reach, harvest, till, plant, cargo, seeds, put-away) and its edge cases.

### Modified Capabilities
- `storage/field-head` (from `m10-farm-rail-field-head`): adds tracking of plots to till and plots to plant.
- `storage/shuttle-workshop` (from `m10-shuttle-workshop`): the Harvester is the first tool module and is field only.
- `storage/shuttles` (from `m10-shuttle-workshop`): a field job that no shuttle fits is dropped instead of served without a shuttle.

## Non-goals

- Trees and the Lumber module (`m10-lumber`).
- Several harvesters on one field at the same time.
- Crops that are not vanilla style age crops (stems, sweet berries, nether wart, cocoa).
- Bone meal, fertiliser or fortune on the harvester.
- Saving a run across a world reload: like every shuttle, a harvester is back in its garage after a reload and the field asks again.

## Impact

- Storage only. New pure classes in `storage/field` (plot states, reach, route, plot work, cargo, seed plan, run steps, field due) with JUnit tests; `FieldJob` and a field run in `storage/shuttle`; world side harvest, till and plant in `storage/field/HarvesterWork`.
- `ShuttleFleet` (field jobs and runs), `ShuttleJob` (permits `FieldJob`), `JobNeeds` (harvester needs), `WarehouseNetwork` (field checks, put-away target and stock for the fleet), `FieldHeadBlockEntity` and `FieldRipeness` (open plots), `FieldEvents` (farmland layer events), `StorageStatsManager` and `field.json` (`work_ticks`, `harvester_stacks`, `field_check_interval`).
- Registries: one item. No new payloads, blocks or menus.
- Unreleased project: no migration.
