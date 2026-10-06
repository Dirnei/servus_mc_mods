# Design

## Context

See proposal.md for motivation. `HeavyRackRenderer` uses the crate pose chain: translate to the block centre, rotate by the facing, move to the front face, flip, then draw each sample item with `ItemDisplayContext.FIXED` scaled 0.3 and flattened to 0.02 in depth at two columns and two levels. The rack model has a floor board at y 0 to 1 and a middle shelf at y 8 to 9 in pixels, posts at the corners and an open front. `HeavyRackBlock` carries the `ONLINE` property and `HeavyRackModels` writes lamp parts for both lamp states, with `heavy_rack_lamp_off` and `heavy_rack_lamp_on` textures.

## Goals / Non-Goals

**Goals:**
- Items read as goods on a shelf from the aisle and from the side.
- No change to what is sampled or synced.

**Non-Goals:**
- Per item fitting or physics. Everything is one scale and one yaw pattern.

## Decisions

### Pose
The renderer keeps the first two steps of the chain, translate to the centre and rotate by `180 - facing.toYRot()`, then rotates 180 degrees so local plus z points to the aisle, exactly as before, but no longer moves to the front face. Each item is translated to its column, `-0.22` or `0.22` on x, its level, `1/16 + 0.165` for the floor board and `9/16 + 0.165` for the middle shelf on y, and `-0.05` on z so it sits slightly behind the centre between the posts. It is scaled 0.33 in all three axes, no flattening, and yawed by 20 degrees alternating sign per column so cubes show two faces. Display context stays `FIXED`, which gives blocks the item frame cube look and flat items an upright sprite. Light is sampled at the rack's own position, which is fine because the block does not occlude.

### Lamp removal
`HeavyRackBlock` drops `ONLINE` from its state definition and default state; the network's lamp push skips blocks without the property. `HeavyRackModels` no longer writes the lamp models or parts, the two lamp textures are deleted, and the GameTest assertion on the rack lamp goes.

## Pure versus Minecraft logic

No pure logic. Renderer, block state and models are Minecraft; the look is checked in the client, the sample by the existing GameTest.

## Risks / Trade-offs

- [Flat items at 0.33 are thin from the side] -> Intended, they read like boxes with labels from the aisle.
- [Two items per shelf touch at larger scales] -> 0.33 leaves a gap of about a tenth of a block.
