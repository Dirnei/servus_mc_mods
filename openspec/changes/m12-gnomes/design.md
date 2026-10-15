## Context

The elementals sheet (scratchpad `elementals.html`, published at https://claude.ai/artifact/YAv7LfXYaH3qHUzWnR9iG6) builds each gnome from boxes in sixteenths of a block: legs and boots, a 6x5x4 coat with belt, a 6x5x5 head with nose and beard, a hat brim and six stepped hat segments that lean back, arms on pivots, and per kind a prop. The salamander already shows the pattern for an entity drawn only under the sight (`Salamander.shownTo`, `SalamanderRenderer.shouldRender`).

## Decisions

- **One entity type** `servus_alchemy:gnome` (a `PathfinderMob`) with a synced `GnomeKind` (miner, lantern, house). One model class with the shared body and a prop part per kind; one renderer that picks the texture and prop by kind. The box sizes are copied from the sheet, scaled 0.8 as there.
- **Seen only under the sight:** the renderer's `shouldRender` returns false without Elemental Sight, as for a wild salamander. Server side the gnome is `invulnerable`, `isPushable` false, `canBeLeashed` false, `requiresCustomPersistence` true; no collision box for players (`canBeCollidedWith` false). On the client `isPickable` is false without the sight (the client sets a sight check through `Gnome.watchSightWith`), so an unseen gnome never catches a click or a hit meant for the stone behind him and never betrays the Hollow; projectiles pass through him.
- **Old Miner goals:** stay within 4 blocks of the home Hollow on its Y (restricted home, custom wander goal); one `TendHollowGoal` asks the Hollow what to do (`GnomeHollowBlockEntity.work()`: tend while the vein is planted and unripe, dig while it is ripe and the store has room, else rest): tending walks to a random free side of the Hollow and taps for 5 to 10 s, digging stands at the cave entry, faces the Hollow and swings for 30 to 90 s, then he rests and wanders for 10 to 20 s ("walks out now and then"). He works standing beside the Hollow on its level, or one block lower when that block has no floor; the path ends on the spot itself so the last step never cuts a corner of the Hollow. The synced `GnomeAction` (idle, tap, dig) drives the arm animation.
- **Keeping the gnome:** a `GnomeKeeper` in the Hollow stores its gnome's UUID; binding (a Salt Crystal offered to the Hollow, see `m12-vein-seeds`) spawns him at the cave entry, or at another side with room when the entry is walled up; breaking the Hollow discards him. Entities load a little after their chunk, so the keeper looks for him once a second and counts him lost only after three misses; then it takes back a gnome of this Hollow standing near, else it spawns a new one. Every gnome asks his hollow once a second whether it still keeps his UUID and leaves when not, so a stray copy never stays.
- **Gifts and seeds:** using anything on the Old Miner under the sight is handled exactly as on his Hollow (`GnomeHollowBlock.tend` and `takeOut`): a gift or a Seed (seeds from `m12-vein-seeds`), but also a bucket or an empty hand. He stands in front of the cave entry, so he must never swallow a click meant for the Hollow (a bucket would otherwise pour into the world).
- **Lantern Bearer:** spawned by the code that wakes the knocking (`OldGnome.tryWake`) where the waking block was broken, so near the player who broke it; he belongs to the Ancient Hollow and through it to its team (no team of his own). A move goal walks him to the spot in front of the open face and he waits within 2 blocks of it. He leaves when the old gnome is found, when the Ancient Hollow is gone, or after 20 minutes counted while he is loaded. His lantern gives no block light (no dynamic light, as for the salamander).
- **House Gnome:** spawned in front of the Ancient Hollow's open face when the old gnome wakes, kept by a `GnomeKeeper` in the Ancient Hollow like the Old Miner (so an Ancient Hollow placed by other means gets him after a few seconds), discarded when found or when the Hollow breaks; he stays on his spot (home radius 1) and looks around. Replaces `OldGnomeRevealRenderer`, whose client tracker and the Hollow's client sync of `found` go with it. The journal on him calls the Ancient Hollow's find.
- **Animation:** walk swing of legs and arms, pickaxe swing while working, lantern sway, head turn for the House Gnome; blinking as on the sheet.
- **Textures:** drawn by a generator script after the sheet's palettes (miner: white beard, blue coat; lantern: brown beard, green coat; house: grey beard, teal coat, yellow patch).

## Risks / Trade-offs

- Entities under ground cost a little per tick; there is one per bound Hollow. Their goals tick only with players near (vanilla entity ticking).

## Assumptions to confirm with the user

- The Lantern Bearer's role: he leads to the Ancient Hollow when the knocking wakes (the plan left open whether the apprentice leads the player to the old gnome).
- The Old Miner wanders up to 4 blocks from his Hollow and walks out every 30 to 90 s.
- Gnomes cannot be hurt and have no collision with players.
- A gift can be given to the Old Miner as well as to the Hollow.
