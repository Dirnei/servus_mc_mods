# Proposal

## Why

PLAN.md M10, a play check follow up to `m10-shuttle-looks` and `m10-spreader`. The tool models flicker: the Harvester's arms share face planes with its reel and with the shuttle's chassis and hold, and the Spreader's arms and hopper rim do the same with the body and the Mk3 gold corners. A spreader is also usually mounted at the back of a vehicle, not the front.

## What Changes

- The Spreader's hopper box, funnel and spreader bar move to the back of the shuttle, behind the Handling mast and below the spoiler. The Harvester's reel stays at the front. A shuttle still carries one tool.
- The arms of both tools are inset so no face shares a plane with the reel, the chassis, the hold or the Mk1 crate wall, and the Spreader's rim no longer reaches into the Mk3 gold corners.
- The docs line on shuttle looks says the Spreader sits at the back.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/shuttle-looks`: "A shuttle is drawn from its parts" draws the Spreader at the back and says a shuttle carries one tool.

## Impact

- Datagen only: `harvester` and `spreader` in `storage/.../datagen/ShuttleModels.java` and the generated `models/shuttle/tool_*.json`.
- `docs/src/content/docs/mods/storage.md`.
- No change to the tool modules, field runs, the renderer or sync.

## Non-goals

- More than one tool per shuttle, or a front and a back tool slot.
- Other tool looks or animations.
