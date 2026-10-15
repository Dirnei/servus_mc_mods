## Context

`HollowRevealRenderer` draws, only under Elemental Sight, the yield item spinning at eight spots and a red mushroom over a bound Hollow, through `SubmitCustomGeometryEvent` (`RevealRenderers`). The block model is stone, so a player without the sight sees stone; the server never changes a block. The chosen look is mockup D on https://claude.ai/artifact/TNuKxtSB5tkCS7K8dohEYu.

## Goals / Non-Goals

**Goals:** look D under the sight, driven by data the client already has (`shownYield`, `shownRipeness`, `shownBound`, `shownBoosted`).

**Non-Goals:** the gnome (an entity in `m12-gnomes`), a different look without the sight, Ancient Hollow (the old gnome's look moves to `m12-gnomes`).

## Decisions

- **Shell, not a model swap.** The renderer draws a cube 0.005 larger than the block on every face, so no face shares the block's plane (no z-fighting), with three layers in order, each 0.002 further out: the rock layer (darkened stone with dry cracks), the material layer (the flowing material, masked to the cracks) and the cave layer on the facing side. A model swap would show to everyone, since block models are not per player.
- **Filling by ripeness.** The crack mask texture stores in its red channel how far along its crack each pixel is (0 at the crack's start, 1 at its end); the material shows where that value is at most the ripeness. Done on the CPU per face per frame: the material layer is drawn as one small quad per crack pixel (at most about 60 per face) with the colour and alpha of the pixel, so no custom shader is needed.
- **Flow.** The material colour per pixel is the vein colour times `0.6 + 0.4 * sin(time * speed - position * 9)`, so a bright band moves along each crack; `speed` doubles while the gnome is gifted. Full bright light.
- **Cave entry only when bound.** A fixed 16 by 16 texture with a dark cut out and depth shading, on the facing side, over the rock layer, drawn only while a gnome is bound (`shownBound`). When the client sees a Hollow become bound it plays a short opening: over about one second the opening grows from the centre outwards pixel ring by pixel ring, with stone crumble particles. Crystals are single pixel quads around the mouth, shown from 30 % ripeness up on a bound Hollow. An unbound Hollow shows cracks on all four sides.
- **Facing.** `GnomeHollowBlock` gets `HorizontalDirectionalBlock.FACING`, set to the placer's opposite direction; the blockstate maps every facing to the same stone model. Existing Hollows load with the default facing (north).
- **Colours.** `VeinData` gets an optional `color` (`#rrggbb`), defaults to the yield's map colour; the shipped veins name iron `#d88c62`, copper `#e26c40`, gold `#fad63e`, redstone `#e8281e`, diamond `#50e8de`.
- **Textures** are drawn in the family's pixel style with the generator approach of `tools/` (a Python script writes the PNGs), so they can be redrawn.

## Risks / Trade-offs

- Up to a few hundred quads per visible Hollow under the sight; drawn only within 32 blocks and only under the sight, as today.

## Assumptions to confirm with the user

- The cave entry faces the player who placed the Hollow and opens only once a gnome is bound, with a one second crumbling animation.
- Nodules and crystals start at 85 % and 30 % ripeness.
