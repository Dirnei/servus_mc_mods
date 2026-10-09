# Proposal

## Why

In Paracelsus' alchemy Salt is what stays behind in the fire: the ash. The Calciner made it a liquid straight away, which the user found wrong for the lore. Alchemists won the salt from the ash by leaching it with water, so the Calciner now leaves Ash and a new Purifier washes it with water into liquid Salt. Found in the play check of `m9-calciner-multiblock`.

PLAN.md milestone: M9 (Manufacturing becomes alchemy), new item 6b2 `m9-salt-leaching`, after `m9-calciner-multiblock`.

## What Changes

- New item **Ash**. The Calciner's Ash Pan gives Ash instead of liquid Salt: one Ash for each portion of Salt the Purifier makes from one Ash (example 100 mB). The calcination recipes keep their Salt numbers.
- The Ash Pan becomes an item port: hoppers, Item Pipes and an empty hand take the Ash out. It has no fluid port any more, and buckets do nothing there. The Ash Pan shows an ash heap.
- New block **Purifier**, a wooden leaching vat: takes Ash and water and makes liquid Salt after a time, from a datapack recipe of type `servus_manufacturing:purification` (example 1 Ash and 100 mB water into 100 mB Salt in 2 seconds). No fuel, no power, no GUI. Ash by hand, hopper or pipe; water by pipe or bucket; Salt out to pipes, touching tanks and buckets. Jade shows its contents.
- The Purifier reports a `purify` event to the journal; Villach asks to purify once.
- The journal gets a Purifier entry in the Workshop, and the Calciner and Principles pages explain the Ash.
- JEI shows the purification recipe.

## Capabilities

### New Capabilities
- `manufacturing/purifier`: the Ash item and the Purifier: leaching Ash with water into liquid Salt.

### Modified Capabilities
- `manufacturing/calciner-structure`: the Ash Pan gives Ash items instead of liquid Salt (ports, buckets, look). This capability is created by `m9-calciner-multiblock`, which is archived first.
- `manufacturing/journal`: the `purify` event, and the Purifier entry in the Workshop.

## Impact

- Manufacturing: new `alchemy/purifier` package (block, block entity, recipe), `alchemy/calciner/CalcinerPorts`, `CalcinerBlockEntity`, `CalcinerPartBlock`, `client/CalcinerRenderer`, a Purifier renderer, registries, capability registration, Jade plugin, JEI plugin, datagen (models, recipes, purification recipe, loot, tags, lang, journal), the Villach chapter JSON, an Ash texture, GameTests.
- PLAN.md M9: the Tria Prima paragraph and the item list.
- The docs Balance lab still lists the Salt of calcination; purification is added to the exported game data only if the exporter needs it for its tests.

## Non-goals

- The Purifier as a multiblock. It starts as one block; a build can follow with the Coagulator and Athanor multiblocks if wanted.
- Making Sulfur or Mercury items.
- A balance pass of the Salt numbers.
