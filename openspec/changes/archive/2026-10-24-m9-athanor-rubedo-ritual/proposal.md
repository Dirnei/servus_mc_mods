# Proposal

## Why

With `m9-athanor-compact-hearth` the Rubedo stage is four Crying Obsidian pillars, a placeholder. The user wants Rubedo cast like Citrinitas, without Crying Obsidian at all: the player places plain Obsidian and offers a Ghast Tear, and dark red crystal obelisks grow out of the obsidian while the citrine in the vats turns orange. The look was agreed in the mock `temp/athanor-stages.html` ("Rubedo ritual"). PLAN.md M9, the Athanor stage gate; third of three changes, after `m9-athanor-citrinitas-ritual`, whose ritual mechanism it reuses.

## What Changes

- **Rubedo Obelisk:** a new block, two blocks tall: a dark red faceted obelisk with a glowing core, a small shard floating and turning above its tip, bands of light running up it, embers rising around it. It has no item and is grown only by the ritual.
- **Rubedo ritual:** using a Ghast Tear on a formed Citrinitas Athanor with Obsidian on the four pillar cells (and room above each) starts it. The tear rises, the flame turns deep red and the citrine clusters pulse; the tear splits into four glowing drops that arc onto the obsidian; glowing veins crack through it while the citrine in the vats turns orange; an obelisk rises out of each block and a shard pops free on top; a red flash.
- **Rubedo tier:** the four obelisks and the three vats with orange crystal. Citrinitas counts citrine or orange vats, so reaching Rubedo keeps Citrinitas.
- **Breaking:** an obelisk drops its Obsidian and the Athanor falls back to Citrinitas at its next check; the ritual must be cast again. A broken orange vat drops the vat and shards like a citrine one.
- The journal's Rubedo page shows the obsidian cells and names the Ghast Tear.

## Non-goals

- Crying Obsidian recipes; the Sealed Flask keeps Crying Obsidian as its ingredient.
- Rituals beyond Rubedo.

## Capabilities

### New Capabilities

None (the rituals capability comes with `m9-athanor-citrinitas-ritual`).

### Modified Capabilities

- `alchemy/athanor-rituals`: the Rubedo ritual, the obelisk, orange vats, tiers accepting several property values.
- `alchemy/athanor`: the Rubedo tier and Citrinitas accepting orange vats.
- `alchemy/journal`: the Rubedo schematic and page.

## Impact

- New `alchemy/.../athanor/ritual/RubedoObeliskBlock.java` with a block entity and renderer, models, textures, loot, lang.
- `VatBlock.CRYSTAL` gains `rubedo`; `GrownVatRenderer` draws the orange cluster and the colour shift during the ritual.
- `AthanorRenderer` draws the Rubedo ritual pieces (tear, drops, veins, rising obelisk).
- `athanor_ritual/rubedo.json`, `athanor_tier/citrinitas.json`, `athanor_tier/rubedo.json`.
- Journal datagen, GameTests, `alchemy/CHANGELOG.md` at archive.
