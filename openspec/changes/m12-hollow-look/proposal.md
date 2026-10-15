## Why

Under Elemental Sight a Gnome Hollow is still a stone block with spinning item icons of its yield and a red mushroom for the gnome. It does not look like a different thing and the icons read as a debug view. The user chose look D of the mockup page: dry cracks while empty, the vein's material slowly filling the cracks as it ripens, and a cave entry in front.

## What Changes

- Under Elemental Sight the Hollow is drawn as a darkened, cracked block instead of stone with icons. Without the sight it stays plain stone, as before.
- An empty Hollow shows only dry, dark cracks. Once a sample is planted, the vein's material creeps along the cracks with ripeness and flows slowly, glowing in the vein's colour; near ripe, a few nodules break out of the cracks.
- Binding a gnome opens a cave entry on one side: the rock crumbles away, dark inside with a faint glow, crystals of the vein's colour around its mouth near ripe. Before that every side is cracked rock. Binding comes first (see `m12-vein-seeds`), so the cave opens before the vein is planted. The side faces the player who placed the Hollow.
- A gifted gnome makes the material flow faster.
- The spinning yield icons and the red mushroom go. The gnome itself becomes an entity in `m12-gnomes`.

## Capabilities

### New Capabilities

### Modified Capabilities
- `alchemy/gnome-hollow`: what the sight shows of a Hollow, and the Hollow's facing. Written against the main spec plus the unarchived deltas of m12-hidden-hollow, m12-gnome-gifts and m12-athanor-citrinitas-rubedo.

## Impact

- `alchemy/.../hollow/GnomeHollowBlock.java`: a horizontal `FACING` set on placement; the block model stays stone for every facing.
- `alchemy/.../client/sight/HollowRevealRenderer.java`: draws the shell instead of item icons.
- New textures: rock, crack mask, material grain (greyscale, tinted per vein), cave entry, crystals; a colour per vein in the vein data.
- `alchemy/.../hollow/VeinData.java` and the vein JSON: a `color` per vein.
