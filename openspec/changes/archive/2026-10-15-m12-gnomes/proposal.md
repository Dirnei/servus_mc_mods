## Why

The gnomes are only hinted at: a spinning red mushroom over a bound Hollow and a mushroom over white wool for Paracelsus' old gnome. The user wants real gnomes that walk, and all three gnomes of the elementals sheet: the Old Miner, the Lantern Bearer and the House Gnome.

## What Changes

- A gnome entity with the three looks of the sheet, built as a model from the sheet's boxes: red pointed hat, beard, coat, belt, boots, and each one's prop (pickaxe, lantern, Salt Crystal). Like the salamander, a gnome is drawn only for players with Elemental Sight.
- **Old Miner:** a Salt Crystal offered to an empty Hollow brings one out of the stone (`m12-vein-seeds`). He wanders a few blocks around his Hollow on its level, takes a Seed and plants it, taps the cracks while the vein ripens, then walks into the cave entry and hammers there, and comes out again now and then. Breaking the Hollow sends him back into the rock. A gift or Seed given to him works like one given to the Hollow.
- **House Gnome:** Paracelsus' old gnome at the Ancient Hollow is drawn as the House Gnome model instead of the mushroom and wool. Using the journal on him finds him, as before. What he does after that stays in `m12-house-gnome`.
- **Lantern Bearer:** the old gnome's young apprentice. When the knocking for the old gnome wakes, he appears near the player under the sight and walks ahead towards the Ancient Hollow with his lantern lit, then waits at it.
- Gnomes cannot be hurt, pushed or leashed, and do not despawn while their Hollow stands.

## Capabilities

### New Capabilities
- `alchemy/gnomes`: the gnome entity, its three kinds and how each behaves.

### Modified Capabilities
- `alchemy/hidden-hints`: the old gnome is shown as the House Gnome; the journal finds him on the gnome too. Written against the unarchived delta of m12-hidden-hints, which must be archived first.

The Old Miner's tie to the Hollow (bind, break, gifts) and the Lantern Bearer are requirements of `alchemy/gnomes`.

## Impact

- New: `Gnome` entity (`GnomeKind` miner, lantern, house), its goals, model, renderer and layer; spawn on bind and on waking the knocking; lang, entity type, a spawn egg per kind for tests.
- `GnomeHollowBlockEntity`: keeps its gnome's UUID, removes it when broken.
- `OldGnomeRevealRenderer` goes; the Ancient Hollow keeps a House Gnome in front of it.
- Depends on `m12-hollow-look` (cave entry, facing) and `m12-vein-seeds` (gnome first, seeds).
