# Proposal

## Why

PLAN.md M10, a follow up to item 2 `m10-shuttle-looks`. A shuttle with all its Speed upgrades shows its top drive step as tracks plus two thin copper exhaust stacks at the back corners. From a distance they read as "some copper on the corner", not as a fast shuttle. A rear wing makes the top Speed step read as a racer at a glance.

## What Changes

- The top Speed look step (6 or more Speed in the example steps) loses its two copper exhaust stacks and gets a rear spoiler instead: a full width steel wing set back over the back of the hold, black iron end plates on both wing tips, and two steel struts that lean back from the hold into the wing.
- The struts sit near the middle of the shuttle and the wing sits above the Handling mast and crane, so every Handling step and tool combines with the spoiler.
- The spoiler stays inside the rail's block like the rest of the shuttle body.
- The docs line on shuttle looks names the spoiler instead of the exhaust stacks.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/shuttle-looks`: "A shuttle is drawn from its parts" names what the last Speed step shows (the spoiler) and adds a scenario for it.

## Impact

- Datagen only: the `drive_3` part model in `storage/.../datagen/ShuttleModels.java`, using the family steel (`servus_foundation:block/electric_pump_steel`) and the dark iron of the Heavy Duty Rack (`servus_storage:block/heavy_rack_iron`).
- `docs/src/content/docs/mods/storage.md`, the shuttle looks line.
- No change to look steps (`ShuttleLook`), the composed item model, the renderer, the server or sync.

## Non-goals

- New look steps or other step boundaries.
- Spoilers for lower Speed steps or per tier spoilers.
- Moving parts on the spoiler.
- Final textures for the rest of the shuttle parts, which stay vanilla placeholders.
