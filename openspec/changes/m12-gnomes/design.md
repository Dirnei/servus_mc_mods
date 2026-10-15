## Context

The elementals sheet (scratchpad `elementals.html`, published at https://claude.ai/artifact/YAv7LfXYaH3qHUzWnR9iG6) builds each gnome from boxes in sixteenths of a block: legs and boots, a 6x5x4 coat with belt, a 6x5x5 head with nose and beard, a hat brim and six stepped hat segments that lean back, arms on pivots, and per kind a prop. The salamander already shows the pattern for an entity drawn only under the sight (`Salamander.shownTo`, `SalamanderRenderer.shouldRender`).

## Decisions

- **One entity type** `servus_alchemy:gnome` (a `PathfinderMob`) with a synced `GnomeKind` (miner, lantern, house). One model class with the shared body and a prop part per kind; one renderer that picks the texture and prop by kind. The box sizes are copied from the sheet, scaled 0.8 as there.
- **Seen only under the sight:** the renderer's `shouldRender` returns false without Elemental Sight, as for a wild salamander. Server side the gnome is `invulnerable`, `isPushable` false, `canBeLeashed` false, `requiresCustomPersistence` true; no collision box for players (`canBeCollidedWith` false).
- **Old Miner goals:** stay within 4 blocks of the home Hollow on its Y (restricted home, custom wander goal); a tend goal walks around the Hollow and taps its sides while the vein ripens; a work goal walks to the block in front of the cave entry, faces the Hollow and plays the swing animation while the ripe vein yields; every 30 to 90 s he walks out for a while. The Hollow stores its gnome's UUID; binding spawns him at the cave entry, breaking the Hollow discards him; a chunk load finds him through the UUID or spawns a new one.
- **Gifts and seeds:** using a gift or a Seed on the Old Miner under the sight calls the Hollow's handling for it (seeds from `m12-vein-seeds`).
- **Lantern Bearer:** spawned by the code that wakes the knocking, owned by the player's team, walks to the Ancient Hollow with a move goal and waits there; discarded when the old gnome is found or after 20 minutes. His lantern gives no block light (no dynamic light, as for the salamander).
- **House Gnome:** spawned in front of the Ancient Hollow's open face when the Ancient Hollow is placed, discarded when found; replaces `OldGnomeRevealRenderer`. The journal on him calls the Ancient Hollow's find.
- **Animation:** walk swing of legs and arms, pickaxe swing while working, lantern sway, head turn for the House Gnome; blinking as on the sheet.
- **Textures:** drawn by a generator script after the sheet's palettes (miner: white beard, blue coat; lantern: brown beard, green coat; house: grey beard, teal coat, yellow patch).

## Risks / Trade-offs

- Entities under ground cost a little per tick; there is one per bound Hollow. Their goals tick only with players near (vanilla entity ticking).

## Assumptions to confirm with the user

- The Lantern Bearer's role: he leads to the Ancient Hollow when the knocking wakes (the plan left open whether the apprentice leads the player to the old gnome).
- The Old Miner wanders up to 4 blocks from his Hollow and walks out every 30 to 90 s.
- Gnomes cannot be hurt and have no collision with players.
- A gift can be given to the Old Miner as well as to the Hollow.
