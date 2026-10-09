# Proposal: m9-athanor

PLAN.md milestone M9 (Manufacturing becomes alchemy), item 4: `m9-athanor`.

## Why

PLAN.md 2.1 makes the Athanor the heart of the lab, like the Blood Altar: the important crafts happen on it, it is fed principles by bucket or pipe, and the structure a team builds around it is the stage gate of the Great Work. The Elixir of Sight and the vein seeds of the next changes are Athanor crafts, so the Athanor comes first.

## What Changes

- **Athanor**: a block with an input slot whose item is shown resting on top, an output slot and one tank per principle. It needs no FE: it burns the principles. It accepts principles by pipe on every side and by bucket on right click.
- **Athanor crafts are data**: recipe type `servus_manufacturing:athanor` with an item input, the principles it takes, the minimum tier, a result and a time. The item in the input slot picks the recipe.
- **Tiers from the structure**: tier definitions are data (`data/<ns>/servus_manufacturing/athanor_tier/<tier>.json`): a tier number, a name and the blocks it needs at offsets from the Athanor. A tier counts when its blocks and those of every lower tier are in place. Nigredo (0) is the Athanor alone; Albedo (1) is a ring of eight Salt Crystal Blocks around it. The flame shows the tier's colour.
- **Salt Crystal Block**: crafted from nine Salt Crystals and back.
- **First Athanor crafts** (data examples): Blaze Powder from gunpowder and Sulfur (Nigredo), a Bottle o' Enchanting from a glass bottle and Mercury (Albedo).
- **Journal**: the Athanor reports an `athanor` event per finished craft; Schwaz gets its first task, finishing a craft on an Albedo Athanor.
- **Data cable**: the Athanor exposes the production capability for the crafts its current tier allows.
- **JEI**: an Athanor category with the tier.

## Capabilities

### New Capabilities
- `manufacturing/athanor`: the Athanor block, its crafts, tiers and structure.

### Modified Capabilities
- `manufacturing/journal`: the `athanor` event and Schwaz's first task.
- `manufacturing/recipe-viewer`: the Athanor category.

## Non-goals

- Citrinitas and Rubedo tiers and their blocks (later stages).
- The Elixir of Sight and vein seeds (their own changes add Athanor recipes).
- A drawing of the structure in the world (a ghost preview); the book describes it.

## Impact

- Code: new `alchemy/athanor` classes, a block entity renderer for the item on top, menu and screen, JEI category, journal event.
- Data: athanor recipes and tiers, the Salt Crystal Block, lang, models, loot, tags, recipes.
- Docs: Manufacturing page and a datapack page for Athanor recipes and tiers.
