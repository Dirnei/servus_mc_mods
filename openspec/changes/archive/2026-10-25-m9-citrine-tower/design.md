# Design

## Context

Grown citrine is drawn by `CrystalDrawing.cluster` from the pure layout `athanor/ritual/CrystalCluster` (six prisms with a stretched `ritual_crystal` texture, tinted with `VatCrystal.body()`). The same drawing serves the Athanor's ritual look (`RitualLooks.vat`, growth 0 to 1) and the grown vat afterwards (`GrownVatRenderer`, growth 1). The Rubedo ritual recolours grown vats through `VatTints`, mixing body and liquid colours per frame. The look to build is the mock `temp/citrine-tower.html` (tower A, box buds, 38 degree upward lean, 40 percent size variation, yellow ramp).

## Goals / Non-Goals

**Goals:**
- Draw the mock's tower and buds in game with the same proportions, pixel grid and colours.
- Keep growth driven by the existing `RitualTimeline` growth value; no timeline changes.

**Non-Goals:**
- No change to ritual rules, blocks, loot or the obelisk.

## Decisions

### Pure layout `CitrineTower` replaces `CrystalCluster`
A pure Java class in `athanor/ritual` (no Minecraft imports) gives, for a vat seed, a growth value and four "side open" flags per tower level:
- the tower segments as boxes in pixels (8x18, 6x16, 4x12, offsets as in mock A) plus a 2 pixel tip, 2 high; the play check found mock A's 12 pixel base wider than the vat's 10 pixel opening, with each segment's own growth: segments rise one after another over the first 55 percent of growth, widening from half width as they rise;
- the buds: the mock's eleven placements (segment, face, along, up, start), each with stage 1 to 4 over the last 45 percent of growth, a lean (0 for top buds, otherwise 0.5 to 1 times 38 degrees upward), a spin (-38 to 38 degrees) and a scale (0.6 to 1.4), all derived from the bud index plus the vat seed;
- buds on a side whose flag is closed are left out.

The seed comes from the vat position (`x * 7 + z * 13`, as today), so it stays the same per vat and differs between neighbours. Alternative: keep `CrystalCluster` and swap its spikes. Rejected, since its model (radius, tilt from upright) does not describe boxes on faces.

### Drawing: per pixel cells, coloured from a ramp
`CrystalDrawing` draws every box face as one quad per texel, like the mock, so the pixel shading (facet bands on the tower, dark edge columns and bright middle on bud shards, bright tips) needs no new texture. A grown vat is about 2,500 quads; three vats are fine for a block entity renderer. Each cell gets a brightness from its face (tone times a fixed per face light, since the ritual crystal glows and does not use world light) and turns it into a colour through the crystal's ramp. The quad samples a white texel (`ritual_crystal.png` becomes plain white), so the vertex colour is the final colour.

All crystal quads carry an up normal. The entity cutout shader of `Sheets.cutoutBlockSheet()` darkens sides by their normal, which would bring back the muddy look; with an up normal every face keeps its ramp colour and the per face light comes from our own brightness. Alternative: a full colour texture per crystal. Rejected, since the Rubedo recolour mixes colours per frame.

Bud geometry uses `PoseStack` rotations (lean about the face's horizontal axis toward up, spin about the bud's axis, 22.5 degree shard tilts) from the layout's numbers; the face basis uses world down as the axis the lean tips away from, on every side (the mock bug where two sides leaned down came from a basis that flipped).

### Colour ramp in `VatCrystal`
`VatCrystal` holds five ramp stops instead of `body()`: citrine `#d29e00 #f0c000 #ffd92e #ffe96a #fff6b8`, Rubedo `#c4500a #ea6e10 #ff8a24 #ffb055 #ffdcaa`. A pure `CrystalRamp` maps brightness 0.45 to 1.1 onto the stops and mixes two ramps stop by stop. The liquid colour becomes the third stop. `VatTints.Tint` carries the mixed ramp instead of a body colour; `RitualLooks.vat` mixes the grown and target ramps by the recolour share and toward white by the pulse, as it does now with single colours.

### Open sides from the client level
The renderer reads the block beside the vat on each side at the vat's level and the two levels above (one per tower segment) from the client level, as `ObeliskDrawing.openSides` already does for the obelisk cracks. A side is open where that block is air. Reading the client level in a renderer is client side drawing, not world logic, so the server thread rule does not apply.

### Render bounds
`GrownVatRenderer` bounds grow to three blocks above the vat and one around it, since the tower tip stands about 3.4 blocks above the vat floor and buds reach into the air beside it. The Athanor's ritual bounds already cover four blocks around and two above plus its own height.

### Pure and Minecraft parts
- Pure, JUnit: `CitrineTower` (segment boxes and their growth order, bud stages, leans only upward and within range, sizes within range, closed sides grow no buds, same seed same layout, different seeds differ) and `CrystalRamp` (ends and middle of the ramp, clamping, mixing).
- Minecraft, play check: `CrystalDrawing`, `GrownVatRenderer`, `RitualLooks`, `VatTints`.

## Risks / Trade-offs

- [Per texel quads cost more than the six prisms] → about 2,500 quads per vat at most three vats per Athanor; checked in the play check with F3 frame time. If it shows, merge runs of equal brightness along a row.
- [The tower stands about 6 pixels above the tanks] → the user approved these proportions in the mock; the play check confirms it next to the tanks.
- [Buds on an open side reach up to about 10 pixels into the air block beside the vat] → only where that block is air; a block placed there later hides the buds from then on.
- [Faces sharing a plane] → the lowest tower faces lie at 4 and 12 pixels, the vat's walls at 0, 3, 13 and 16, its rim at 12, segment tops at 20, 36 and 48, the tip top at 50; buds start 0.05 pixels off the face; bud shards (2 pixels wide in the middle, 1 at the sides, two thirds of the mock's size to match the narrower tower) stand 0.1 pixel apart from the middle shard and tilt, so none share a plane with it. Each segment and tip step stands on the top of the one below; those two faces would share the plane, so segments and tip steps draw no bottom face. Stacked segments whose sides line up (the first two east faces at 12) share a plane but not an area, so they do not flicker. The play check looks for flicker.

## Open Questions

None.
