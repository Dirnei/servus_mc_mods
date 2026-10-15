## Why

`alchemy_plan.md` (Citrinitas) says Ignis Perpetuus, the eternal flame made from the salamander's Sal Ammoniac, builds the **Salamander Forge**, an infinite furnace. `m12-salamander` shipped Ignis Perpetuus as a building part with nothing to build yet. The Forge is the first reward for befriending a salamander: a furnace that never needs fuel.

## What Changes

- New block **Salamander Forge**: a compact deepslate brick furnace with an open fire mouth that always burns (glowing embers, a flame that gives off light, a small steel plate with a salamander on the front). It is lit at all times and gives off light like a lit furnace.
- It smelts every smelting recipe like a vanilla furnace, one item per work cycle, and never takes fuel. It has an input slot and an output slot, no fuel slot. The work time per item is datapack data (example 100 ticks, twice the pace of a vanilla furnace).
- A normal Foundation machine: faces the player, opens a furnace like screen (input, eternal flame, progress arrow, output, the side panel), exposes its items to pipes and hoppers on every side but the front with Foundation's side configuration, pushes its output into neighbours, is configured and dismantled with the carrot and drops its contents when broken.
- A producer on the data line: it exposes the Foundation production capability with every single item smelting recipe, so the Crafting CPU can order smelting from it directly, without a Machine Adapter. It keeps no upkeep.
- Made at a crafting table from one Ignis Perpetuus, a Furnace and seven Deepslate Bricks. A Workshop entry "The Salamander Forge" opens with the Wandering years chapter, beside "The Salamander". JEI lists the Forge as a smelting station.
- Foundation's model datagen learns per element light emission, so the Forge's fire is drawn full bright.

This change builds on the applied but unarchived `m12-hidden-hollow`, `m12-gnome-gifts`, `m12-athanor-citrinitas-rubedo`, `m12-principle-dropper`, `m12-elixir-dose` and `m12-salamander` (Ignis Perpetuus and the Workshop entry it hangs under). It adds a new capability only and does not modify their specs.

## Capabilities

### New Capabilities
- `alchemy/salamander-forge`: the Salamander Forge block, its fuel free smelting, its always lit look, screen, automation sides, production capability, recipe and Workshop entry.

### Modified Capabilities
None.

## Impact

- New package `alchemy/.../forge/`: block, block entity, menu.
- Registries: block, block item, block entity, menu; screen layout in `client/MachineScreens`; capabilities in `ServusAlchemyEvents`; machine defaults in `AlchemyMachineDefaults` plus `data/servus_alchemy/machine/salamander_forge.json`.
- Foundation: `ModelBuilder.ElementBuilder` gains a light emission setter (datagen only).
- Assets: GUI texture, emblem texture, generated block state, model, item model, loot, pickaxe tag, recipe, lang.
- Journal: `WorkshopCategory` entry. JEI catalyst. Docs: Alchemy page and the datapack machines table.
- Tests: JUnit for the new defaults, GameTests in a new `ForgeGameTests`.
