# Design

## Context

- **Textures today.** `textures/block/duct_<line>.png` for one line, `duct_<lines>.png` and `duct_<lines>_v.png` for every set of two to four lines (bands for runs along X or Z, and along Y), `_duplex` and `_fast` copies of every texture with the data line, `duct_junction.png` for a multi line centre where several axes meet, `duct_ring.png` (tinted). Items: `textures/item/{item_pipe,fluid_pipe,power_cable,data_cable,duplex_data_cable,fast_data_cable}.png`. No generator is in the repository.
- **Part models.** `DuctPartModels` (datagen) writes a centre per line set and run axis, a junction cube per set of two or more lines and an arm per side and line set. A several line box shows bands on the faces whose u axis is the across axis, the outer line's texture on the two faces across the bands, and `bands_v` with flipped UVs elsewhere. Bands are side by side on the horizontal across axis (X for a run along Z or Y, Z for a run along X), first line on the negative side.
- **Runtime.** `UtilityDuctModel` picks the parts, `DuctPoseQuads` fits them onto the pose's boxes (`DuctShapes.center(radius, pose, faces, armRadius)` and `arm(...)`), and only plain one line faces get UVs from the position. The data tier reskin swaps sprites by name after that.
- **The preview** (session scratchpad `ducts_preview/`, `scripts/gen.py`) already draws option B textures as 16 x 16 strips: u runs along the arm, a face of thickness n shows rows `8 - n/2` to `8 + n/2`; and option C icons. Its strips are the source for the generator.
- **The resting pose** (archived `m9-duct-resting-pose`): centres grow on Y to bend in the middle of the block. Growing a band face stretches it, and taking its UVs from the position mirrors north and east faces; the archived fix keeps band UVs and stretches them.

## Goals / Non-Goals

**Goals:** one way to put UVs on every duct body face, so the pattern runs along the arm, tiles per block and never mirrors; the B and C artwork from a script in the repository.

**Non-Goals:** geometry, sizes, collision and pose stay as archived; no animation; no change to facades or panels.

## Decisions

### D1: UVs along the run, computed at runtime for every body face

A pure function `DuctUv` (in `logistics/duct`, no Minecraft imports) gives the texture position in pixels of a point on a body face:

- `u` = the point's coordinate on the part's **run axis**, in pixels 0 to 16. Every texture tiles horizontally, so a run continues without a seam across block borders and at arm to centre joints, and an arm of any length shows the pattern at its true texel size.
- `v` = 8 plus the point's offset from the part's **own middle** on the face's other axis, in pixels. So a face of thickness n always shows the rows `8 - n/2` to `8 + n/2`, wherever the pose put the part.
- The offset is taken in world direction, never per face, so the pattern is the same on all four sides and band order follows the world across axis (first line on the negative side) on the top, the bottom and both band faces.

`DuctPoseQuads` applies it to every body quad of a centre or arm part after fitting the quad onto its box (`MutableQuad.setUvFromSprite(vertex, u / 16, v / 16)`), replacing today's "plain faces only" rule. Ring quads keep their UVs. Ends of a part (faces normal to the run axis) are hidden by the neighbouring part or the ring and keep their datagen UVs.

Alternatives: UVs in datagen only. Rejected: the runtime stretches and moves the quads anyway, and datagen UVs cannot know the pose. `bakeUvsFromPosition`: rejected, its orientation depends on the face and mirrors north and east.

The run axis of an arm is its side's axis; of a straight centre the axis of its arms. Two faces of a several line part show all bands (their v axis is the across axis), the two faces across the bands show the outer line's strip (v axis Y for a horizontal run, Z for a vertical one), as today.

### D2: Fittings for every multi axis and bending centre

`UtilityDuctModel` uses the fitting part when the centre's arms lie on more than one axis or the centre grew (its box is taller than the baked centre), for any line count. A fitting has the same size as today's centre: the line radius, plus `JUNCTION_EXTRA` for two or more lines. Its faces use a fitting texture per size (`duct_fitting_2`, `_4`, `_5` for radius 2, 4 and 5) with UVs from the position, which is fine for a texture that is symmetric on both axes (black rubber, darker rim, steel cap in the middle of the larger sizes). `DuctPartModels` writes the fitting part for every line set, also single lines; `DuctParts.centerName(lines, null)` names it for every line set.

Alternative: keep bands on a grown centre and stretch them. Rejected: the stretched bands look wrong and a bend reads naturally as a moulded fitting.

### D3: One band texture per line set, built from the line strips

The `_v` textures go: D1 orients every face, so `duct_<lines>.png` serves every run axis. A band texture stacks the line strips across: for n lines on a part of thickness t, line i fills rows `8 - t/2 + i*t/n` to `8 - t/2 + (i+1)*t/n` with that line's strip pattern at band width t/n (2 lines on 6 px: 3 px each; 3 lines on 8 px: 3, 2 and 3 px, rounding to whole pixels so the middle band is never empty; 4 lines on 8 px: 2 px each). Data line textures keep their `_duplex` and `_fast` copies, so the tier reskin works by name as today. The reskin pattern in `UtilityDuctModel` drops the `_v` alternative.

### D4: A committed generator

`tools/duct-textures/gen.py` (Python 3, standard library only, PNG writer included) holds the palettes and patterns from the preview's option B strips and option C icons and writes:

- block: one line strips (`duct_item`, `duct_energy`, `duct_fluid`, `duct_data`, `duct_data_duplex`, `duct_data_fast`), every band set with its tier copies, `duct_fitting_2/4/5`, `duct_ring`;
- item: the six line icons.

It writes into `foundation/src/main/resources/assets/servus_foundation/textures/` and deletes nothing itself; the task removes the `_v` files and `duct_junction.png` by hand once. Running it twice gives the same bytes. The docs icons come from the existing `gradlew iconExport` task.

### D5: Data line colours

The block and the ring already show the tier (white, gray, black; rings from `DataCableItem.ringColour`); the spec still said teal and is corrected. The strips follow the tier: white braid, white and gray braid, black braid with a cyan tracer every few texels.

### APIs

`MutableQuad.setUvFromSprite(int, float, float)`, `setFrom`, `positionComponent`, `sprite()` and `bakeUvsFromPosition()` in `neoforge-26.1.2.116-sources.jar`, `net/neoforged/neoforge/client/model/quad/MutableQuad.java` (lines 351 to 424). Datagen face `uvs` and `texture` as used today in `BlockModelBuilder`. No new event or capability.

### Pure logic and tests

- Pure Java with JUnit: `DuctUv` (`DuctUvTest`: u continues across a block border and an arm to centre joint; the same u and v on all four sides of an arm, no mirroring; rows 6 to 10, 5 to 11 and 4 to 12 for thicknesses 4, 6 and 8 at any pose; band order by world across axis on top and bottom). The band row split lives in the generator, which checks it itself (`gen.py --check`: every band at least 2 px, bands cover the thickness exactly).
- Minecraft side: datagen output by `runData` and the build; the look by the play check. No GameTest: nothing behaves differently.

### PLAN.md

Settles the Utility Duct part of the open decision "Art style and texture direction (shared look across the family)": hoses, rubber and braids, dark rubber fittings with steel. The decision stays open for the rest of the family. The `m5-duct-look` bullet goes when this change is archived.

## Risks / Trade-offs

- [The pattern on a fitting of a single Item Pipe changes from orange to black, so a one line bend no longer shows the line's colour on its centre] → The arms carry the colour right up to the fitting; the spec accepts this as the fitting look.
- [Three lines on 8 px do not split evenly] → 3, 2, 3 px with the middle band the narrow one; the play check looks at it.
- [Resource packs that replaced the old textures lose them] → Nothing is released yet.
- [The atlas sprite of a strip is used with v rows outside 4 to 12 on a grown face] → Grown centres are fittings (D2), so a strip face never grows.
- [A hand edit of a generated texture is overwritten by the next run] → The generator is the source; its header says so.
