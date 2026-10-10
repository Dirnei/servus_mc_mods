# Proposal

## Why

In Paracelsus' alchemy Salt is what stays behind in the fire: the ash. The Calciner made it a liquid straight away, which the user found wrong for the lore. Alchemists won the salt from the ash by leaching it with water, so the Calciner now leaves Ash and a new Purifier washes it with water into liquid Salt. Found in the play check of `m9-calciner-multiblock`.

PLAN.md milestone: M9 (Manufacturing becomes alchemy), new item 6b2 `m9-salt-leaching`, after `m9-calciner-multiblock`.

## What Changes

- New item **Ash**. The Calciner's Ash Pan gives Ash instead of liquid Salt: one Ash for each portion of Salt the Purifier makes from one Ash (example 100 mB). The calcination recipes keep their Salt numbers.
- The Ash Pan becomes an item port: hoppers, Item Pipes and an empty hand take the Ash out. It has no fluid port any more, and buckets do nothing there. The Ash Pan shows an ash heap.
- New build **Purifier**: a Leaching Vat on a log, with a Catch Basin and a **Residue Crock** beside the log (front view `_ V _` over `B L R`, mirrored builds form too), formed with the Journal. It washes Ash with water into liquid Salt and leaves **Caput Mortuum**, a residue item for a later stage (carbon, diamond), from a datapack recipe of type `servus_manufacturing:purification` (example 4 Ash and 400 mB water into 400 mB Salt and one Caput Mortuum in 8 seconds). No fuel, no power, no GUI. Ash and water go into the Vat by hand, hopper or pipe; Salt comes out of the Basin only, to pipes, touching tanks and buckets; Caput Mortuum comes out of the Residue Crock only, to hoppers, Item Pipes and an empty hand, so each output has its own port. The Crock is crafted from vanilla materials (example bricks around a barrel). Jade shows its contents.
- The formed Purifier looks like one build: the Vat gets spouts running out of its sides down into the Basin and the Crock, which get formed models facing the Vat. Unformed blocks keep their plain models.
- Forming any alchemy build with the Journal (Calciner, Purifier, Coagulator, Athanor) plays a sound and sends particles up from every block of the build.
- The Purifier reports a `purify` event to the journal; Villach asks to purify once.
- The journal gets a Purifier entry in the Workshop, and the Calciner and Principles pages explain the Ash.
- JEI shows the purification recipe.

## Capabilities

### New Capabilities
- `manufacturing/purifier`: the Ash and Caput Mortuum items and the Purifier build: leaching Ash with water into liquid Salt.

### Modified Capabilities
- `manufacturing/calciner-structure`: the Ash Pan gives Ash items instead of liquid Salt (ports, buckets, look). This capability is created by `m9-calciner-multiblock`, which is archived first.
- `manufacturing/journal`: the `purify` event, the Purifier entry in the Workshop, and the forming effect of the Journal.

## Impact

- Manufacturing: new `alchemy/purifier` package (block, block entity, recipe), `alchemy/calciner/CalcinerPorts`, `CalcinerBlockEntity`, `CalcinerPartBlock`, `client/CalcinerRenderer`, a Purifier renderer, registries, capability registration, Jade plugin, JEI plugin, datagen (models, recipes, purification recipe, loot, tags, lang, journal), the Villach chapter JSON, an Ash texture, GameTests.
- PLAN.md M9: the Tria Prima paragraph and the item list.
- The docs Balance lab still lists the Salt of calcination; purification is added to the exported game data only if the exporter needs it for its tests.

## Non-goals

- Any use of Caput Mortuum (planned for Rubedo).
- Making Sulfur or Mercury items.
- A balance pass of the Salt numbers.
