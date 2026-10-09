# Design: m9-athanor

## Context

See proposal.md for why. The Calciner and Coagulator (`m9-tria-prima`) give the building blocks: `PrincipleTanks` (three restricted tanks), `AlchemyMachineBlockEntity` (team owner), `PrincipleGauges` in screens, the recipe pattern with a `MapCodec` and `StreamCodec`, JEI categories, and the journal (`m9-journal`) with `Journal.recordItem`.

## Goals / Non-Goals

**Goals:** the Athanor with data driven crafts and tiers, bucket and pipe feeding, item on top, production, JEI, a journal event and Schwaz's first task.

**Non-Goals:** later tiers' content, a structure preview, Elixir and seeds.

## Decisions

### D1: A machine without energy

`AthanorBlockEntity extends AlchemyMachineBlockEntity`? No: that base is powered. The Athanor extends `MachineBlockEntity` (Foundation) directly and implements `OwnedMachine` with the same team logic, factored into a small `TeamOwner` helper shared with `AlchemyMachineBlockEntity`. Its `tick()` advances progress only while a craft can run. Slots: input 0, output 1. Tanks: `PrincipleTanks` in accept mode, offered on every side but the top (side mode still applies).

### D2: Pure tier logic

`AthanorTiers` (pure Java): a tier is `TierDef(name, number, colour, List<BlockAt>)` with `BlockAt(dx, dy, dz, block id)`. `tierOf(tiers, Function<BlockAt, String> blockAt)` returns the highest number whose blocks and those of all lower numbers match; 0 when none. JUnit covers ordering, gaps in tier numbers, a missing block in a lower tier, and an empty definition list.

### D3: Tier data and the shipped tiers

`AthanorTierData` is a `SimpleJsonResourceReloadListener` on `servus_manufacturing/athanor_tier`:

```json
{"tier": 1, "color": "#F2F0EA", "blocks": [{"offset": [1, 0, 0], "block": "servus_manufacturing:salt_crystal_block"}]}
```

Shipped: `albedo.json` (tier 1, the eight horizontal neighbours on the Athanor's level). Tier 0 (Nigredo) is implicit. Names come from the file name.

### D4: When the tier is checked

On placement (`onPlace` schedules a check), on use, when a craft starts, and every 100 ticks while idle; never every tick. The result is stored in the block state property `TIER` (0 to 3), which drives the flame colour through the model, and in a synced data slot for the GUI. Reading eight blocks every five seconds per Athanor is cheap and needs no neighbour listeners for diagonal blocks, which Minecraft does not notify.

### D5: Crafts

`AthanorRecipe(SizedIngredient input, Principles principles, int tier, ItemStack result, int time)`, type `servus_manufacturing:athanor`, `SingleRecipeInput`. Default time 100, tier 0. Like the Coagulator, principles and the item are taken when the craft completes.

### D6: Item on top and buckets

`AthanorBlock.useItemOn`: a principle bucket fills the matching tank via `FluidUtil.interactWithFluidHandler`; another item goes into the input slot (one stack); sneaking with an empty hand takes the input back; otherwise the GUI opens. `AthanorRenderer` (`BlockEntityRenderer`) draws the input item lying flat on top, slowly turning while a craft runs.

### D7: Model and flame

A placeholder model: a brick furnace body with a front texture per tier (`athanor_front_<tier>`) generated as placeholder PNGs (black, white, yellow, red flame tints) and a lit variant.

### D8: Journal

`Journal.recordAthanor(level, team, result, tierName)`: filters `tier:<name>`, an item id or `#tag`. Schwaz gets `{"id": "albedo_craft", "event": "athanor", "filter": "tier:albedo", "count": 1}`; the book's Schwaz task page is updated.

### APIs

`MachineBlockEntity`, `BaseEntityBlock.useItemOn` (1.21.1 `ItemInteractionResult`), `FluidUtil.interactWithFluidHandler`, `BlockEntityRenderer` with `ItemRenderer.renderStatic`, `SimpleJsonResourceReloadListener`, JEI `IRecipeCategory`. All in use elsewhere in the repository.

### Pure and Minecraft logic

Pure, JUnit: `AthanorTiers`. GameTest: Blaze Powder from gunpowder and piped Sulfur at tier 0; the Albedo ring raises the tier and lets the Bottle o' Enchanting run, removing a block lowers it; a Sulfur bucket fills the tank; production lists only allowed crafts; the journal counts an Albedo craft for Schwaz. Manual: renderer, flame colours, GUI.

## PLAN.md decisions settled

PLAN.md 2.1 named "Salt Crystal blocks for Albedo"; settled as a full ring of eight on the Athanor's level. Section 5 is not affected.

## Assumptions to confirm with the user

1. **Albedo structure**: eight Salt Crystal Blocks in a ring on the Athanor's level.
2. **Tier checks** on use, placement, craft start and every 5 seconds.
3. **No FE** for the Athanor; principles are its fuel.
4. **Principles taken on completion**, like the Coagulator, not drained over the craft.
5. **First crafts**: Blaze Powder (gunpowder + 200 Sulfur, tier 0, 100 ticks) and Bottle o' Enchanting (glass bottle + 100 Mercury + 50 Salt, tier 1, 100 ticks).
6. **Recipe**: Athanor from bricks, a blast furnace and a cauldron.
7. **Placeholder art** for the Athanor and the Salt Crystal Block.
8. **Schwaz's first task**: one craft on an Albedo Athanor.

## Risks / Trade-offs

- [Players find the ring unclear] → The Schwaz entry describes it; a preview can follow.
- [Tier lags behind building by up to five seconds] → Using the Athanor checks at once.

## Migration Plan

None.
