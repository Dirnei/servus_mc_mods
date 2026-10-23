# Design

## Context

`m9-athanor-citrinitas-ritual` brings ritual data (`AthanorRitual`, `RitualPlan`, `AthanorRitualData`), the ritual state on `AthanorBlockEntity` with phase sounds, `BlockAt` properties, `VatBlock.CRYSTAL` with `GrownVatBlockEntity` and `GrownVatRenderer`, and the ritual drawing in `AthanorRenderer`. The look to build is the "Rubedo ritual" tab of `temp/athanor-stages.html`: obelisks dark red, vat crystals turning from yellow to orange.

## Goals / Non-Goals

**Goals:**
- The Rubedo ritual on the existing mechanism, with only data, one new block and its looks.

**Non-Goals:**
- Changes to the ritual mechanism beyond accepted value lists.

## Decisions

- **Accepted value lists.** A `BlockAt` property holds a list of accepted values (`{"crystal": ["citrine", "rubedo"]}`, a single value still reads as a one element list); the first value is the one a ritual sets. Citrinitas accepts both crystals, so Rubedo keeps Citrinitas, and the Rubedo ritual can be cast again on vats already orange.
- **Air above as a turn.** A turn from `minecraft:air` to the obelisk's upper half places it. A missing air cell gets its own message naming the block below and its place ("the space above the Obsidian behind the tank left of it must be free"). Missing blocks are named by their place next to the nearest tank (`AthanorLayout.place`: on its outer side, left or right of it, behind or in front of it), so the pillar cells read right as well as the vats.
- **Turned blocks count as in place.** `RitualPlan.check` accepts a block that already is the turn's `to`, so after one broken obelisk only its Obsidian goes back and the ritual turns that one; the others stay as they are and get no stream and no sound. The same lets the Citrinitas ritual be cast again after one broken vat.
- **Streams and sounds are data.** A turn may say `"stream": false` (the air above and the vats in Rubedo); only streamed turns get a stream, a landing sound and a growing sound. The ritual JSON names its sounds (`offering`, `burst`, `land`, `grow`, `end`, each an id with volume and pitch); Citrinitas keeps its amethyst sounds there, Rubedo uses `entity.ghast.ambient` softened (offering), `block.lava.pop` (drops landing), `block.respawn_anchor.charge` (rising) and `block.beacon.power_select` (end).
- **Rubedo Obelisk as a double block.** `RubedoObeliskBlock` with `DoubleBlockHalf` (`LOWER`, `UPPER`), like vanilla doors: breaking either half removes both (a creative player's break of the upper half drops nothing); loot drops one Obsidian from the lower half only. Pickaxe with a diamond tool like Obsidian, light level 10, amethyst sounds, no item. The lower half's block model is a 3 px plinth of the Obsidian it grew out of; the upper half has no faces. After the turn the Athanor updates the shape of every turned block, so half an obelisk whose other half could not turn breaks and drops its Obsidian.
- **The obelisk is drawn as quads.** The Citrinitas change draws its crystal prisms as quads; the obelisk follows: `client/ObeliskDrawing` draws the tapered four sided shell (9 px at the base, 6 px at the shoulder, tip to 32 px, no bottom face) with the `rubedo_obelisk` texture, the light bands, the floating shard (an octahedron in `ritual_crystal` tinted hot red) and the glowing cracks (`rubedo_veins`, transparent). `RubedoObeliskRenderer` on the lower half draws it at rest with a slow core pulse; the Athanor's ritual look draws the same obelisk rising with an overshoot, so it looks the same before and after the turn. A baked model could neither taper nor rise. Embers are `DustParticleOptions` in `animateTick`. Textures come from `tools/athanor-textures/gen.py`.
- **Orange vats.** `VatBlock.CRYSTAL` gains `rubedo` (body `#F07A18`, glow `#FF6A00`, light 10). During the ritual the grown vat keeps drawing itself: the Athanor's vat look puts the blended colour (citrine to orange from the landing to before the flash, with a pulse of white while the tear rises) into `client/VatTints` each frame, and `GrownVatRenderer` draws in it while it is fresh.
- **Ritual drawing.** `AthanorRenderer` hands each look a `Moment` (timeline, ticks, index, game time). The Ghast Tear rises like any offering; the drops are the streams with their trailing motes, landing on the Obsidian's top (`landing` of the look). `RitualTimeline` gains `cracks` (the veins open as the drop lands), `rise` (done before the flash), `recolor` and `answer`. The cracks open from the top edge down over the free sides and spread over the top; the obelisk grows from the plinth height inside the Obsidian and pushes out through its top; the shard pops free past 60 % of the rise. The red flash is the server's flash in `result_color`. Colours: ritual `#FF2A2A`, result `#E04020`; obelisk shell `#160303`, glow `#9A1010`, hot `#E04020`.
- **Crying Obsidian leaves the tier.** `athanor_tier/rubedo.json` lists the orange vats and the obelisk halves; the Sealed Flask recipe keeps Crying Obsidian, which players still get from vanilla. The journal entry keeps its id `crying_obsidian` but shows the Ghast Tear, and its schematic shows Obsidian on the pillar cells (cells a ritual grows out of air are left out).
- **Name.** "Rubedo Obelisk"; it has no item, so the name shows only in Jade and the journal.

## Testing

- JUnit: `AthanorTiersTest.acceptedValueListsMatch`, `AthanorRitualTest.airAboveIsATurn`, `AthanorRitualTest.orangeVatsTurnAgain`, `AthanorRitualTest.turnedBlocksCountAsInPlace`, `AthanorLayoutTest.placeNamesTheSideOfTheTank`, `RitualTimelineTest.obelisksCrackThenRiseBeforeTheFlash`, `RitualTimelineTest.grownCrystalTurnsItsColourBeforeTheFlash`.
- GameTests: `rubedoRitualGrowsObelisks`, `rubedoRitualNeedsFreeSpaceAbove`, `brokenObeliskDropsObsidianAndTheStage`, `rubedoRitualCastsAgainAfterABrokenObelisk`, `brokenOrangeVatDropsToAlbedo`.
- Manual: the ritual from several angles; obelisk idle animation and embers; flicker check on the obelisk faces, the tip and the shard.

## Risks / Trade-offs

- [Two animated renderers per obelisk and vat] → A few dozen quads per obelisk; seven animated block entities per Athanor at most.
- [The obelisk is drawn by its block entity renderer] → Beyond the block entity view distance only the plinth shows, like the vat clusters.
- [Players expect Crying Obsidian from the Athanor] → The journal names the Ghast Tear and the Obsidian; Crying Obsidian keeps its vanilla sources.

## Overlap check

- Shell: the four tapered sides and the four tip faces share only the ring of corners at the shoulder, with no face there. The shell has no bottom face, so nothing lies on the plinth's top; its slanted sides meet the plinth in a line.
- Bands: each lies parallel to a side, a tenth of a pixel outside it (the half width plus 0.1 px), between 3.5 and 26.8 px, below the shoulder at 27 px; the four quads of a band meet at the corner edges; the three bands of an obelisk sit 7.5 px apart and are 0.8 px tall.
- Shard: eight triangles meeting at edges; its lowest point stays at 32.8 px while it bobs, above the apex at 32 px.
- Cracks: side quads 0.1 px outside the Obsidian's or the plinth's sides, the lid 0.1 px above the top (16.1 px during the ritual, 3.1 px at rest); side quads of neighbouring sides meet at the corner edges; sides against a neighbour that hides them are not drawn. The obelisk crosses the lid at a slant, never in its plane. Diagonal pillar cells touch only at a corner line, where their crack quads cross at a right angle.
- Plinth: one box from 0 to 3 px; its bottom and sides face away from the neighbours' faces in the same planes, like any slab.
- Hand-over: the Athanor stops drawing the rising obelisk and the cracks when its ritual ends, the tick the obelisk's renderer starts; the shard keeps its turn because both use the game time.
