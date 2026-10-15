## Why

The gnome should come first: the user wants to call a gnome into the Hollow with a Salt offering, then give him the seed to plant and grow.


A vein is planted with a raw ore or an ore block, and gold, redstone and diamond only wait for the Schwaz chapter, so they arrive in Albedo instead of their stages. The user wants the stages to gate the veins through the Athanor: a seed of the metal is made at the Athanor of the right stage (the alchemists' "seed of metals" that grows in the earth).

## What Changes

- New item **Seed**: one item that names its vein (`servus_alchemy:vein` component), is named after it ("Seed of Gold") and tinted in the vein's colour.
- New order at the Hollow, all under Elemental Sight: a **Salt Crystal** on an empty Hollow calls a gnome in (no ripe vein needed any more), then a **Seed** given to the Hollow or to the gnome is planted by him, principles ripen it while he tends it, and once ripe he digs. A different seed makes him dig out the old vein and plant the new one, so a Hollow can change what it grows without being broken.
- A raw ore or ore block no longer plants a vein. Veins already planted keep growing.
- Journal: `bind` counts the gnome moving in (no vein); a new `ripen` event counts a ripe vein by its yield. Wandering years asks for a ripe gold vein, Basel and Salzburg for a ripe diamond vein.
- Seeds are Athanor recipes, so the Athanor tier is the gate:
  - Albedo: Seed of Iron (Raw Iron), Seed of Copper (Raw Copper).
  - Citrinitas: Seed of Gold (Raw Gold), Seed of Redstone (Redstone Dust).
  - Rubedo: Seed of Diamond (Diamond).
- `samples` and `chapter` leave the vein JSON. A vein is planted only by its seed.
- The client learns the veins (id, colour, yield) at login and reload, for the seed's name and tint.

## Capabilities

### New Capabilities

### Modified Capabilities
- `alchemy/gnome-hollow`: binding first with a Salt Crystal, planting by seed (to the Hollow or the gnome) and swapping veins; veins lose samples and chapter; the Seed item and its Athanor recipes per tier.
- `alchemy/journal`: `bind` without a vein, new `ripen` event. Written against the main spec plus the unarchived deltas of m12-hidden-hollow, m12-athanor-citrinitas-rubedo and m12-hollow-look (vein colours), which must be applied and archived first.

## Impact

- New: `VeinSeedItem`, data component `servus_alchemy:vein`, item tint source for the vein colour, vein sync payload, Athanor seed recipes (datagen), lang ("Seed of %s", vein names), journal Workshop entry text.
- `VeinData` / vein JSON: `samples` and `chapter` removed; `GnomeHollowBlock` seeding by seed only.
- Journal prose and the Albedo and Citrinitas Workshop entries name the seeds.
- Depends on `m12-hollow-look` for the vein colour.
