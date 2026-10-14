# Design

## Context

Each Speed look step is one part model `models/shuttle/drive_<step>` from `ShuttleModels.drives`. `ShuttleItemModel` composes body, drive, Handling part and tool per look key, and `ShuttleGarageRenderer` draws the composed item at 0.8 scale around the block centre. `drive_3` today is the tracks of `drive_2` plus two copper stacks, x 1..2 and 14..15, z 2..3, y 3..13. Model +z is the front, the hold spans z 2..14, and the Handling mast and crane end at y 13 (crane beam x 7..9, z 1..10). The drive model is the same for every tier. By default only a Mk3 reaches the step, but packs can raise the max counts of any tier.

The look was picked in explore on 2026-10-14 from a three.js preview with the real textures, variant "S2b".

## Goals / Non-Goals

**Goals:**
- The spoiler is part of the `drive_3` model, so the item, garage, workshop and rail views all get it through the existing composition.
- Every face pair is free of z-fighting.

**Non-Goals:**
- Any change to `ShuttleLook`, `ShuttleItemModel` or the renderer.

## Decisions

**Geometry (model pixels).**
- Tracks as in `drive_2`, no copper stacks.
- Wing: steel, x 0.5..15.5, y 14..15, z -1..2.5. It hangs 3 px past the back of the hold and clears the mast and crane tops (y 13) by 1 px.
- End plates: dark iron, x 0..0.5 and 15.5..16, y 12.5..15.5, z -1.5..3. They meet the wing exactly at its tip faces.
- Struts: steel, 1 by 1, at x 5.5..6.5 and 9.5..10.5, clear of the crane beam by 0.5 px. Each runs from bottom centre (y 9, z 3) to top centre (y 14.4, z 0.75), so it leans back about 22.6 degrees. Each strut is one element built upright with length hypot(5.4, 2.25) and turned about the X axis around its bottom centre. y 9 is the top of the Mk1 crate wall, so the strut stands on every tier; on Mk2 and Mk3 its foot sinks into the hold and lid. The top sinks into the wing; at that angle its upper corners land between y 14.2 and 14.6.
- Alternatives seen in the preview: struts at the back corners (the old stack positions) looked like posts and were dropped. Further in (x 5..6) was fine, but x 5.5 was preferred. A wing 1 px further back (z -2) would put the end plates about 0.4 px outside the rail block at 0.8 scale, which breaks the shuttle size rule.

**Element rotation.** The project's `ModelBuilder.ElementBuilder.rotation()` writes vanilla `rotation { origin, axis, angle }`, and 26.1 takes any angle (already used by `ElectricPumpModels.turned`). The sign that turns the top toward -z is checked in the item preview before the play check. The `box` helper gets an overload or a sibling that adds a rotation, so UV sizing stays in one place.

**Textures.** Wing and struts use `servus_foundation:block/electric_pump_steel`, the family steel. End plates use `servus_storage:block/heavy_rack_iron`, the dark iron the user called black iron. Both are block textures on the block atlas, and Storage already depends on Foundation, so no texture is added. `box` keys textures by file name; the two names differ from the existing keys.

**PLAN.md.** This settles no open decision. The M10 list gets a line for this follow up next to `m10-shuttle-looks`.

**Pure logic vs Minecraft.** No pure logic changes; `ShuttleLookTest` stays as is. The look is datagen output, checked by `runData`, the build and the user's play check.

## Risks / Trade-offs

- [Rotated struts are not axis aligned, so their shading differs from neighbours] → Accepted; it reads as a separate metal part. Checked in the play check.
- [On the open Mk1 crate the strut foot shows a sliver inside the crate near the back wall] → Only reachable by raising the Mk1 max Speed in a pack; accepted.
- [Wing tip faces and end plate faces share the planes x 0.5 and 15.5] → They face opposite ways and the wing face is hidden behind the plate, the "end exactly at the neighbour's edge" case. Strut faces are tilted, so they share no plane with the hold, the lid or the wing.
