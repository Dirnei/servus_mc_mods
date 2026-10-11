# Proposal: m10-field-range

PLAN.md milestone M10, item 4: `m10-field-range`.

## Why

A field reaches 4 blocks to each side of every farm rail from the start. That number is a constant in code, so early fields need few rails and there is nothing left to grow into. Harvesters are wide tool shuttles, and the plan wants them to stay on their own floor: they cross a shaft but never ride a lift car, which also makes the warehouse layout matter for fields.

## What Changes

- The field radius belongs to the Field Head. It starts at `radius` from `field.json` (2 by default) and grows by one per Range Upgrade in the head, up to `max_radius` (4 by default). Hydration, the plots the rails see, the ripe share and the plots the harvester works all use the head's radius.
- New Range Upgrade item, crafted, that goes into a new upgrade slot of the Field Head GUI. The slot takes as many as the radius can still grow. Putting one in or taking one out redraws the field at once (event driven, no scan per tick). Breaking the head drops the upgrades.
- The Field Head GUI shows the radius next to the field size.
- Tool modules can be wide. The Harvester is wide. A shuttle with a wide tool finds its ways without the up and down steps of elevator shafts: it crosses a shaft on its floor but never calls or rides a lift car.
- A wide shuttle puts its harvest away only into storage it can drive to; storage that is reached only by lift is skipped, also crates that would otherwise take their part at once.
- New Field Head work reason: no harvester of the warehouse can reach the field without a lift. The warehouse does not send a run then.
- The Harvester tooltip says it does not fit into lifts.
- `FieldStrip.RADIUS` goes away; the pure field classes take the radius as an argument.

## Non-goals

- Range Upgrade tiers or other head upgrades (one item, counted).
- Lumber and other tools: they choose their own wide trait when they come.
- A Field Head warning when only the storage is behind a lift: the harvest then waits at the garage like with a full warehouse.
- Moving a harvester between floors by hand or by any other block.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `storage/field-head`: hydration, plots in sight and the GUI follow the head's radius; a Range Upgrade slot; a new work reason for fields no harvester reaches without a lift.
- `storage/harvester`: the Harvester module is wide and its tooltip says so; harvesters never ride a lift car; the plots worked follow the head's radius; put-away only into reachable storage.

## Impact

- Storage code: `field/FieldStrip`, `FieldReach`, `PlotReach`, `FieldWork`, `FieldHeadBlockEntity`, `FieldHeadBlock`, `menu/FieldHeadMenu`, `client/FieldHeadScreen`, `shuttle/ShuttleFleet`, `shuttle/HarvestRun`, `shuttle/module/ModuleSpec`, `ShuttleProfile`, `network/WarehouseNetwork`, `crate/StorageStatsManager`, registry and datagen.
- Data: `storage/field.json` gains `radius` and `max_radius`.
- Pure tests: `FieldStripTest`, `FieldReachTest`, `PlotReachTest`, `FieldWorkTest`, `ShuttleProfileTest`, new `FieldRadiusTest` and `LiftlessStepsTest`.
- GameTests for the radius and for a harvester at a shaft.
- Archive order: after `m10-shuttle-workshop`, `m10-farm-rail-field-head`, `m10-harvester`, `m10-shuttle-tiers`, `m10-shuttle-looks` and `m10-shuttle-build-minigame`, because its deltas modify their requirements.
