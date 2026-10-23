# Proposal

## Why

With `m9-athanor-compact-hearth` the Citrinitas stage is three Principle Vats with Amethyst Blocks on them, which is a placeholder. The user wants the stage to be cast: the player places the vats and offers amethyst to the Athanor, which grows yellow citrine crystals out of the vats in an animated ritual (heated amethyst really turns into citrine, the yellow quartz Citrinitas is named after). The animation was agreed in the mock `temp/athanor-stages.html` ("Citrinitas ritual"). PLAN.md M9, the Athanor stage gate; second of three changes, after `m9-athanor-compact-hearth`, before `m9-athanor-rubedo-ritual`.

## What Changes

- **Athanor rituals as data:** a ritual names the offering item, the stage the Athanor must have, and the blocks it turns into other blocks around the Athanor. The Athanor runs a ritual when a player uses the offering on it and every block to turn is in place.
- **Citrinitas ritual:** using an Amethyst Block on a formed Albedo Athanor with an empty Principle Vat beside each tank starts it. The amethyst rises from the plate, spins and hums, the flame turns violet, the amethyst bursts into three streams that turn gold on their way into the vats, the vats fill with glowing gold and a citrine cluster grows out of each, as tall as the tanks; a flash, and the flame turns yellow.
- **Grown vats:** a Principle Vat with a citrine cluster is a state of the vat block with its own model, a soft glint of particles and light. It no longer counts as a vat in a Coagulator. Breaking it drops the Principle Vat and a few Amethyst Shards; the Athanor falls back to Albedo at its next check and the ritual must be cast again.
- **Citrinitas tier:** the three vats with grown citrine replace the vats with Amethyst Blocks.
- The journal's Citrinitas page shows the vats and how to cast the ritual.

## Non-goals

- The Rubedo ritual and the obelisk (`m9-athanor-rubedo-ritual`), and the citrine turning orange there.
- Rituals at stages other than the Athanor.
- A screen or recipe viewer page for rituals.

## Capabilities

### New Capabilities

- `alchemy/athanor-rituals`: rituals as data, casting one on the Athanor, the animation, what the grown blocks do and what breaking them does; the Citrinitas ritual.

### Modified Capabilities

- `alchemy/athanor`: the Citrinitas tier needs the vats with grown citrine.
- `alchemy/journal`: the Citrinitas schematic and page.

## Impact

- New `alchemy/.../athanor/ritual/` package: ritual data (pure Java plus a reload listener), the ritual state on `AthanorBlockEntity`, a sync payload or block entity update for the start tick.
- `alchemy/.../coagulator/VatBlock.java` (crystal state), `CoagulatorStructure.java` (grown vats do not count), vat loot table, models and textures for the grown vat.
- `client/AthanorRenderer.java` (offering, streams), a vat renderer for the growth, particles and sounds.
- `athanor_tier/citrinitas.json`, new `athanor_ritual/citrinitas.json`.
- Journal datagen, lang, GameTests, `alchemy/CHANGELOG.md` at archive.
