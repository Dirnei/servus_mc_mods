# Proposal: m9-stone-sink-chimney

## Why

The Sink and the Chimney are always red bricks, so they clash with bases built in stone bricks, deepslate or sandstone, and a Chimney without a Sink is dead decoration. Players want to build the Sink in their own stone and to use chimneys on houses with smoke they control. This belongs to the Foundation utilities (PLAN.md section 4, "what lived on: pumps, Sink, ..."; M9 Foundation utilities). PLAN.md lists no open point for it; the request comes from play.

**Depends on `m0-material-kinds`**: it uses the stone kind, the `#servus_foundation:stones` tag and the material mechanism of `foundation/materials`. Apply and archive `m0-material-kinds` first.

## What Changes

- Sink blocks and Chimney blocks are stone kind material blocks. Their recipes take any stone of `#servus_foundation:stones` instead of bricks: Sink `BBB/BRB/BBB` with stone and iron bars gives 8, Chimney `B B/B B/B B` with stone gives 6. The result carries the stone that appears most often.
- Each block keeps its stone on the item and in its block entity; the Chimney becomes a block entity block. Only the brick faces switch to the stone; the iron frame, the fan, the grate and the cap's flue stay. Blocks without a stone show Bricks.
- A Sink forms only when all eight blocks share one stone. Otherwise the carrot refuses and the action bar names the stones found. Unformed blocks show their own stone. Rates, buffer and smoke of a formed Sink do not depend on the stone.
- Chimneys on a Sink may mix stones and count exactly as today.
- A Chimney column that does not stand on a formed Sink is configurable:
  - smoke: off, thin, cosy or signal, stepped for the whole column by a plain carrot click;
  - redstone mode: always, while powered, while unpowered, stepped by a sneak carrot click; a signal on any block of the column counts;
  - only the top block smokes; the action bar shows each new setting;
  - a Chimney placed on top of another copies its settings; new columns start at off and always.
- On a formed Sink the carrot leaves the chimney settings alone and the Sink keeps controlling the smoke; the settings come back when the column leaves the Sink. The Chimney has no carrot dismantle.

## Capabilities

### New Capabilities

- `foundation/chimney`: the Chimney block on its own: its stone, the smoke setting, the redstone mode, how the carrot sets a column and how a new block copies the one below.

### Modified Capabilities

- `foundation/sink`: crafting from any stone; forming needs one shared stone; Sink blocks keep and drop their stone; the chimney look in each block's stone; "Chimney without a Sink" becomes decorative with configurable smoke (see `foundation/chimney`).
- `foundation/materials` (created by `m0-material-kinds`): adds the Sink and the Chimney as the stone kind blocks.

## Impact

- `foundation/.../machine/sink/`: `SinkBlock` (one stone forming check, stone on drops), `SinkBlockEntity` (material slot, model data), `ChimneyBlock` (block entity, new block states, carrot, redstone, smoke), new `ChimneyBlockEntity`, new pure `ChimneySettings` and `SinkStones`, `SinkGameTests` and new `ChimneyGameTests`.
- `foundation/.../machine/MachineContent` (Chimney block entity type, material block items, `Material.register` with the stone kind).
- Datagen: `SinkModels` (casing split into a stone cube under an iron frame overlay, chimney cap rim), `MachineDataParts` (recipes through `MaterialRecipes` with the stone kind, lang), `MachineBlockLoot` (copy the material component).
- Textures: new `sink_casing_frame.png` and a split `chimney_top.png`, from `tools/sink-textures/gen.py`.
- Docs: the Sink and Chimney entry in `docs/src/content/docs/mods/foundation.md`, and `docs/src/data/gamedata.json`.

## Non-goals

- Other blocks in the stone kind (the Coal Generator chimney, Alchemy multiblocks, the Tiled Stove).
- Stone changing rates, buffer or anything else of the Sink.
- A GUI for the chimney, or smoke colours.
- Chimney columns that bend, or smoke from blocks other than the top.
- Changing the shape of any model.
