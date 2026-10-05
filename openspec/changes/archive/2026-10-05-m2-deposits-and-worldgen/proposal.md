# Proposal

## Why

Manufacturing starts at the deposits: visible, infinite patches of iron, copper, coal and stone that the player builds a factory around. Nothing else in M2 can be placed meaningfully without them. PLAN.md milestone M2, item "Deposit blocks and worldgen for iron, copper, coal, stone".

## What Changes

- Four deposit blocks in `servus_manufacturing`: iron, copper, coal and stone deposit. Unbreakable in survival, available in the Servus creative tab, with placeholder textures.
- Deposit definitions as datapack JSON under `data/servus_manufacturing/deposit/`: which block is a deposit and what one extraction yields. Loaded by a reload listener so modpacks can add or retune deposits.
- Worldgen as datapack JSON: a disk feature per deposit type placed on the overworld surface through NeoForge biome modifiers, so patches are visible from above.
- Manufacturing registry, datagen and creative tab plumbing, following the Lib pattern.

## Capabilities

### New Capabilities
- `manufacturing/deposits`: what deposits are, where they generate, how they are defined in data and what they yield.

### Modified Capabilities
None.

## Non-goals

Deferred to later changes:
- The Burner Extractor that actually mines deposits (next change). This change only defines what a deposit yields.
- Tier 2 deposits and any deposit richness or drift mechanics (PLAN.md decided against drift).
- Final textures and worldgen tuning. Rarity and patch size get a balance pass after the M2 playtest.

## Impact

- `ServusManufacturing` constructor registers blocks and items.
- New blocks appear in overworld generation of new chunks. Existing worlds get deposits only in newly generated chunks.
- New datapack content under `manufacturing/src/main/resources/data/servus_manufacturing/`.
