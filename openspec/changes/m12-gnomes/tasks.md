## 1. Entity

- [x] 1.1 `GnomeKind`, `Gnome` entity type registration, synced kind, invulnerable, no push, no leash, persistent; spawn egg per kind; lang; run `runData` and `:alchemy:build`
- [x] 1.2 Model after the sheet (shared body, prop per kind), textures from a generator script, renderer drawn only under the sight, walk, swing, sway and blink animation; list overlapping boxes for z-fighting; run `:alchemy:build`

## 2. Old Miner

- [x] 2.1 Hollow keeps its gnome's UUID; binding spawns the Old Miner at the cave entry, breaking discards him, a missing gnome is respawned on load; GameTests for bind and break; run `:alchemy:runGameTestServer`
- [ ] 2.2 Goals: wander near home, work at the cave entry while the vein works, walk out now and then; gifts on the gnome go to his Hollow; GameTest for the gift; run `:alchemy:runGameTestServer`

## 3. Old gnome and apprentice

- [ ] 3.1 House Gnome in front of the Ancient Hollow, journal on him finds the old gnome, `OldGnomeRevealRenderer` removed; GameTest for the find; run `:alchemy:runGameTestServer`
- [ ] 3.2 Lantern Bearer spawned when the knocking wakes, walks to the Ancient Hollow, leaves when found or after 20 minutes; GameTest that he spawns and goes; run `:alchemy:runGameTestServer`
- [ ] 3.3 Launch the client from main: all three gnomes under the sight, none without it

## 4. Play check

- [ ] 4.1 User play check: the Old Miner walks and works at his Hollow, the Lantern Bearer leads to the old gnome, the House Gnome waits there, all only under the sight
