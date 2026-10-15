## Why

`alchemy_plan.md` (Citrinitas) asks for a Principle Dropper: fed by pipes, it drips the principles onto the Gnome Hollow below it, so ripening a vein is automated without any pipe touching the Hollow. Since `m12-hidden-hollow` a Hollow takes principles only by bucket under Elemental Sight, which gets tedious once a player runs several noble veins.

## What Changes

- New block **Principle Dropper**: a small copper and glass drip vessel with a spout. It holds one tank per principle (example 3,000 mB, three buckets), each shown in its own glass tube, fills from pipes, ducts and buckets through the standard fluid capability (fill only, never drained) and takes only Salt, Sulfur and Mercury.
- Placed directly on top of a Gnome Hollow, it drips the principles the Hollow's vein still drinks into the Hollow's tanks at a datapack rate (example 25 mB per principle every 20 ticks) until the vein is ripe. A ripe vein, an empty Hollow (no vein) or any other block below gets nothing; the Dropper never pushes into other blocks.
- The Hollow keeps exposing no capabilities; it gets an internal method to receive drips, mirroring the bucket path. Jade shows the Dropper's own tanks and nothing about the block below.
- Coloured drip particles of each principle dripping, sent by a block event, so the Dropper shows it works.
- Crafted on a Citrinitas Athanor (tier 2) from a Principle Vat with Salt, Sulfur and Mercury.
- Stats in datapack JSON `data/servus_alchemy/machine/principle_dropper.json` (`work_ticks` drip interval, `fluid_per_cycle` per drip, `fluid_capacity` per tank).
- Journal Workshop entry "Principle Dropper" opened with Wandering years (the Schwaz research node, like The Yellow Ring); docs entry in the Alchemy page and the machine table.

This change builds on the applied but unarchived `m12-hidden-hollow`, `m12-gnome-gifts` and `m12-athanor-citrinitas-rubedo`; their deltas (the Hollow without capabilities, the Citrinitas tier and the Schwaz node) are the base it reads. It adds a new capability and modifies no requirement of theirs.

## Capabilities

### New Capabilities
- `alchemy/principle-dropper`: the Dropper's tanks and fluid capability, dripping onto a Hollow below, the rate as data, the disguise in Jade, the particles, the Athanor craft and the Workshop entry.

### Modified Capabilities
None.

## Impact

- `alchemy/.../hollow/`: new `PrincipleDropperBlock`, `PrincipleDropperBlockEntity`, pure `PrincipleDrip`; `GnomeHollowBlockEntity` gets a package-private `receiveDrip`.
- Registries: `ServusAlchemyBlocks`, `ServusAlchemyItems`, `ServusAlchemyBlockEntities`; capability in `ServusAlchemyEvents`; defaults in `AlchemyMachineDefaults`.
- Datagen: block state and model (`ServusAlchemyBlockStateProvider`), loot, lang, `AlchemyRecipeTable` Athanor craft, `WorkshopCategory`.
- Data: `data/servus_alchemy/machine/principle_dropper.json`.
- Docs: `docs/src/content/docs/mods/alchemy.md`, `reference/datapack/machines.md`.
- Tests: `PrincipleDripTest`, `AlchemyMachineDefaultsTest`, new `DropperGameTests`.
