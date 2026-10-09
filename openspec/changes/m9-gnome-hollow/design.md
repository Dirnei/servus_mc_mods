# Design: m9-gnome-hollow

## Context

See proposal.md for why. Available: `PrincipleTanks`, `AlchemyMachineBlockEntity` (team owner, `resolveTeam`), the Athanor with data crafts and tiers, Elemental Sight with the client `RevealRenderers` hook, the journal with `Journal.isFinished` and events, and the machine base (output push by side modes).

## Goals / Non-Goals

**Goals:** veins as data, the Hollow's seed, ripen, bind and work cycle, its production, the reveal drawing, the journal event.

**Non-Goals:** upkeep, late veins, final art.

## Decisions

### D1: Pure vein state

`VeinGrowth` (pure Java): a vein's numbers (`steps`, `stepTicks`, `stepCost` as `Principles`, `yieldTicks`) and a state (`ripeness`, `bound`, `stepTimer`, `yieldTimer`). `tick(state, tanks contents, outputHasRoom)` returns what happened this tick: `NONE`, `RIPENED` (with the cost to take), `YIELD`. Ripe means `ripeness >= steps`. JUnit covers waiting without principles, step timing and cost, ripeness cap, no yield before binding, yield rate, and no yield with a full output.

### D2: Vein data

`VeinData` reload listener on `servus_manufacturing/vein`:

```json
{"samples": ["c:ores/iron", "c:raw_materials/iron"], "yield": {"id": "minecraft:raw_iron"}, "chapter": "servus_manufacturing:villach",
 "steps": 20, "step_ticks": 100, "step_cost": {"salt": 100, "mercury": 20}, "yield_ticks": 200}
```

The vein of a sample is the first vein (by id) whose sample tags contain the item.

### D3: The Hollow block entity

`GnomeHollowBlockEntity extends MachineBlockEntity implements OwnedMachine`, like the Athanor: no energy at all, slots: sample 0 (input), output 1. It stores the vein id, ripeness, bound flag and timers. `acceptsInput` for the sample slot asks `VeinData` and `Journal.isFinished` for the owner's team; on the first tick with a sample it seeds and clears the slot. Its `tick()` runs `VeinGrowth` and, on `RIPENED`, takes the principles; on `YIELD` inserts the yield. The shared machine tick keeps the output push and the lit state. A sample stack larger than one seeds with one item; the rest stays in the slot for the player to take back.

### D4: Binding

`GnomeHollowBlock.useItemOn`: principle buckets fill the tanks; a Salt Crystal on a ripe, unbound Hollow binds a gnome if `ElementalSight.active(player)`, using the crystal and reporting `Journal.recordItem(..., "bind", yield, 1)`; otherwise an action bar message explains (no sight, not ripe, no vein, already bound). Other items fall through to the GUI.

### D5: Client tracking and drawing

`HollowTracker` (common package, no client imports, like Foundation's `DuctTracker`) keeps the Hollow block entities loaded on the client (added in `onLoad` on the client, removed in `setRemoved` and `onChunkUnloaded`). The update tag carries vein yield item, ripeness, steps and bound. `HollowRevealRenderer` is registered with `RevealRenderers` from the client setup; for each Hollow within 32 blocks it draws the yield item small at up to eight bud positions around the block (count and size from ripeness) and, when bound, a red mushroom as the gnome's cap bobbing in front, drawn with `ItemRenderer.renderStatic` at full light.

### D6: Recipe and data

Athanor craft `athanor/gnome_hollow`: cracked stone bricks, 500 Salt, 250 Mercury, tier 1. Veins `iron.json` and `copper.json` after Villach. Model: a plain stone cube (`minecraft:block/stone` texture) so it reads as plain stone.

### D7: Journal

`bind` event via `Journal.recordItem` with the yield; Schwaz adds `{"id": "gnome", "event": "bind", "count": 1}`; the Schwaz book entry ends with the gnomes.

### APIs

`SimpleJsonResourceReloadListener`, `BlockEntity.onLoad` and `setRemoved`, `getUpdateTag`, `ItemRenderer.renderStatic`, `RenderLevelStageEvent` through `RevealRenderers`, `FluidUtil.interactWithFluidHandler`. All used elsewhere in the repository.

### Pure and Minecraft logic

Pure, JUnit: `VeinGrowth`. GameTest: seeding refused before Villach, accepted after; a ripening step takes the principles; binding needs sight (mock player with and without the effect); a bound ripe Hollow yields raw iron into a chest; production reports the yield. Manual: the reveal drawing.

## PLAN.md decisions settled

PLAN.md 2.1 left how a gnome is bound and how seeds gate; settled: the sample itself is the seed, gated by a journal chapter per vein, and a gnome is bound with a Salt Crystal offering under Elemental Sight. Gnome upkeep stays open in section 5.

## Assumptions to confirm with the user

1. **The sample is the seed**: any item of the vein's tags (ore block or raw ore) seeds it; there is no separate Vein Seed item.
2. **Gating per vein by journal chapter**: iron and copper after Villach.
3. **Ripening**: 20 steps of 100 ticks, each taking 100 mB Salt and 20 mB Mercury (about 100 seconds and 2,000 mB Salt per vein).
4. **Yield rate**: one raw ore per 200 ticks (10 seconds), forever, no upkeep.
5. **Binding**: a Salt Crystal used under Elemental Sight on a ripe Hollow.
6. **Hollow recipe**: Albedo Athanor, cracked stone bricks, 500 Salt, 250 Mercury.
7. **Look**: plain stone; buds are the yield item drawn small; the gnome is a red mushroom placeholder.
8. **Schwaz's third task**: bind a first gnome; finishing Schwaz then opens nothing yet.

## Risks / Trade-offs

- [Infinite raw ore may be too strong] → All numbers are vein JSON; the balance pass is M9 item 7.
- [Drawing many Hollows each frame] → Only within 32 blocks and only under the sight.

## Migration Plan

None.
