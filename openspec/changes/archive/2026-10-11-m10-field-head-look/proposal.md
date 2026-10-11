## Why

The Field Head still wears placeholder grass, dirt and blue textures, while every other block a player sets up for the warehouse looks like a station: board panels in the wood of the recipe under an iron frame. The head also hides what it knows (water, crop, ripeness) behind its GUI. Decided in the explore session of 2026-10-11: give it the station look and show water, crop and ripe share on the block itself.

## What Changes

- The Field Head becomes a wood block in the station family: board panels in the wood picked by its recipe under the station iron frame, the wood carried on the item and kept on drop, like the Inbound Station. The recipe takes planks.
- The head gets a horizontal facing; the front faces the player who places it.
- Front, lower panel: a ripe share bar. A small recessed dark window fills from the left with wheat gold in steps as the ripe share approaches the head's harvest threshold, and turns green and full once the threshold is reached and a harvest is due. Driven by a block state that the head updates only when its ripe count, crop, mode or threshold changes.
- Upper panel on all four sides: a dark glass window into the water buffer. The water is drawn as real fluid (still texture, biome tint) up to the fill share; the client learns the level only in eighths of the window height.
- Top: the field's crop item drawn flat on a dark inset plate; nothing on the plate when no crop is set or in harvest only mode.
- The placeholder textures `field_head_top`, `field_head_side` and `field_head_bottom` are replaced.
- Pumpkins and melons stay out (their stems are not age crops).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `storage/field-head`: adds the look requirements (wood and facing, ripe share bar, water window, crop icon) and the pure mapping from ripe share and threshold to bar steps and from water to window levels.

## Impact

- Storage: `field/FieldHeadBlock`, `field/FieldHeadBlockEntity`, a new pure `field/FieldHeadLook`, a new client `client/FieldHeadRenderer`, registration in `ServusStorage`, `ServusStorageItems`, `client/ServusStorageClient`.
- Datagen: blockstates and models (`ServusStorageBlockStateProvider`), loot (`ServusStorageBlockLoot`), recipe (`ServusStorageRecipeProvider`).
- Textures: new `field_head_side`, `field_head_top`, `field_head_bottom` and `field_head_front_<step>` overlays in `assets/servus_storage/textures/block`.
- Docs: the Field Head entry in `docs/src/content/docs/mods/storage.md`; `PLAN.md` M10 lists the change.
- Saves: unreleased, existing heads load with oak boards and face north.
