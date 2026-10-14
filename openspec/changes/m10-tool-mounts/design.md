# Design

## Context

Tools are part models (`models/shuttle/tool_<id>`) composed with the body, drive and Handling part by `ShuttleItemModel`. Model +z is the front, the chassis spans x 2..14, y 2..4, z 2..14, and the hold sits on it from y 4. At the back:
- the Handling mast stands at z 1..2 (posts x 3..4 and 12..13, crossbar x 4..12 y 3..4, forks x 5..6 and 10..11 at y 0.5..1.5 from z 0)
- the spoiler's end plates reach z -1.5 from y 12.5 up, and its struts lean back to z 0.75 above y 14

## Goals / Non-Goals

**Goals:** no shared face planes between a tool and any body, drive, Handling or spoiler part; the Spreader at the back.

**Non-Goals:** changes to `ShuttleLook`, the item model or the renderer.

## Decisions

**Harvester (front).** The arms become x 2.05..3 and 13..13.95, y 3.05..5.95, z 13.05..16.95:
- inside the reel at the front, top and bottom
- off the chassis and hold sides (x 2 and 14)
- off the Mk1 crate wall's back face (z 13)

The reel, its bars and the cutter bar stay.

**Spreader (back), model pixels:**
- arms x 2.05..3 and 13..13.95, y 3..6, z -1..2.5, reaching into the chassis
- hopper x 3..13, y 6..11, z -4..0
- rim x 2.5..13.5, y 11..12, z -4.5..0
- funnel x 5..11, y 3..6, z -3..-1, and the spinner x 7..9, y 1.5..3, z -2.5..-1.5
- bar x -2..18, y 0.5..1.5, z -3.5..-1.5, with pegs x -2..-1 and 17..18, y 1.5..3, z -3..-2

At 0.8 scale the back reaches about 2 px into the rail block behind, along the rail, as the Harvester does at the front.

Alternative: the hopper on top of the hold. Rejected because it clashes with the crane and the spoiler and is not where a spreader sits.

**Face check:**
- Arms and mast posts meet at x 3 and x 13 with opposite faces (edge contact).
- The arms' outer faces are 0.05 off the chassis sides, and their inner ends sit inside the chassis and hold.
- Hopper and funnel, funnel and spinner, and spinner and bar meet with opposite faces.
- The rim's back at z 0 lies 1 px behind the mast (z 1).
- The rim's top at y 12 lies 0.5 px under the spoiler plates (y 12.5).
- The bar and pegs at x -2 share that plane but not an area.
- Nothing reaches z 0..2 below y 1.5, where the forks are.

**PLAN.md.** Settles no open decision; the M10 list notes the follow up.

**Pure logic vs Minecraft.** No pure logic; datagen, the build and the user's play check.

## Risks / Trade-offs

- [The back hopper hides part of the Handling mast from behind] → Accepted; the mast still shows from the sides and above.
