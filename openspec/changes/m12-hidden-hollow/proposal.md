# Proposal

## Why

`alchemy_plan.md`, section "Gnome Hollow" and "Changes to what is built", decided with the user on 2026-10-15. Today the Gnome Hollow is a machine like any other: it has a screen, pipes fill it, it pushes raw ore into chests and the Crafting CPU sees it as a producer. That makes the gnomes a plain ore generator. The plan turns the Hollow into a hidden thing: a stone block that only an alchemist under Elemental Sight can tend, so the elixir, the gnomes and later the homunculus (the only automated extraction, in `m12-receptaculum`) matter. This is the first change of milestone M12 (Citrinitas and Rubedo), so it also adds M12 to `PLAN.md`.

## What Changes

- The Gnome Hollow is disguised stone. Without Elemental Sight it looks, answers and breaks like stone: right click does nothing, Jade shows plain Stone (name and icon), pick block gives Stone, breaking drops Cobblestone (Stone with Silk Touch) and the vein is lost.
- No screen, no sample or output slot, no item, fluid or production capability, no output push. Pipes and ducts do not connect to it, and the data line no longer sees it as a producer (the Receptaculum takes that role later).
- Under Elemental Sight only, by hand:
  - a sample of ore seeds an empty Hollow (same chapter gating as today),
  - a principle bucket pours into its internal tanks and the empty bucket comes back,
  - an empty hand takes the internal store into the inventory (what does not fit drops),
  - a Salt Crystal binds a gnome to a ripe vein (as today).
  A failed action under the sight shows a short hint on the action bar.
- A bound gnome fills a small internal store (example 64 items, datapack `item_capacity`) and stops when it is full.
- Breaking under Elemental Sight drops a fresh, empty Gnome Hollow plus the stored ore; vein, tanks and gnome are lost.
- Jade under Elemental Sight shows the real Hollow: vein, ripeness or a working gnome, the store and the principles.
- Journal Workshop and Schwaz texts and the docs describe the new handling.
- `PLAN.md` gets milestone M12 pointing to `alchemy_plan.md`; Undines, Sylphs and the outdated Citrinitas and Rubedo notes go.

No earlier M12 change exists when this one starts; it is written against the main specs only.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `alchemy/gnome-hollow`: seeding by hand under the sight instead of a sample slot, principles by bucket only, an internal store instead of an output slot and push, no production capability, no screen or capabilities, behaviour and drops without the sight, Jade disguise, binding without a message when the sight is missing.

## Impact

- `alchemy/.../hollow/GnomeHollowBlock.java`, `GnomeHollowBlockEntity.java` (no longer a Foundation machine), new pure `HollowStore`; `GnomeHollowMenu` and `client/GnomeHollowScreen` deleted with their registrations in `ServusAlchemyMenus`, `ServusAlchemyClient` and `ServusAlchemyEvents`.
- `alchemy/src/main/resources/data/servus_alchemy/machine/gnome_hollow.json` and `AlchemyMachineDefaults` (store capacity).
- Datagen: loot table (stone, cobblestone or Hollow), blockstate without facing, lang, journal pages.
- `integration/jade/ServusAlchemyJadePlugin.java`.
- `gametest/HollowGameTests.java` adapted, `HollowStoreTest` added.
- `docs/src/content/docs/mods/alchemy.md`, `reference/datapack/machines.md`, `PLAN.md`.
- Existing Hollows in dev worlds lose facing and their sample and output slots; Servus is unreleased, so no migration.
