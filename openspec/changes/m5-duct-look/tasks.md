# Tasks

## 1. Artwork

- [x] 1.1 Write `tools/duct-textures/gen.py` (Python 3, standard library only) from the preview's option B strips and option C icons (design D3 to D5): line strips in 4, 6 and 8 px cuts, band sets with `_duplex` and `_fast` copies, `duct_fitting_2/4/5`, `duct_ring`, the six item icons; `--check` verifies the band split (every band at least 2 px, bands cover the thickness). Run it into `foundation/src/main/resources/assets/servus_foundation/textures/`, delete every `duct_*_v*.png` and `duct_junction.png`. Verify: `python tools/duct-textures/gen.py --check` passes, a second run changes no file (`git status` clean after it), `gradlew build` green.

## 2. UVs along the run

- [x] 2.1 Add pure `DuctUv` in `foundation/.../logistics/duct/` (design D1): u from the run axis coordinate, v from the offset to the part's own middle in world direction. JUnit `DuctUvTest`: `uContinuesAcrossBlockBorder`, `uContinuesAtArmToCentreJoint`, `sameOnAllFourSides`, `stripRowsByThickness` (4, 6, 8 px at the middle and low poses), `bandOrderFollowsWorldAcross` (top and bottom), `endFacesUntouched` (replaced in 6.2). Verify: `gradlew :foundation:test` green.
- [x] 2.2 `foundation/.../logistics/client/DuctPoseQuads.java`: apply `DuctUv` to every body quad of centres and arms after fitting (also for a centre at the middle pose, which today returns the raw part quads), with `MutableQuad.setUvFromSprite`; rings keep their UVs. Verify: `gradlew build` green.

## 3. Parts and fittings

- [x] 3.1 `foundation/.../logistics/datagen/DuctPartModels.java` and `duct/DuctParts.java`: one `bands` texture per line set (no `bands_v`), the outer line's 6 or 8 px strip on the faces across the bands, a fitting part for every line set including single lines (`centerName(lines, null)`), fitting textures by size. `client/UtilityDuctModel.java`: the fitting part when the arms lie on more than one axis or the centre grew (design D2); the data reskin pattern without `_v`. Run `gradlew :foundation:runData`. Verify: the generated part models reference only existing textures (`gradlew build` and the client log show no missing texture), `gradlew build` and `gradlew :foundation:test` green.

## 4. Texts and docs

- [x] 4.1 `docs/src/content/docs/mods/foundation.md`: the new duct look (hoses and cables per line, data braids by tier, fittings at bends and branches, textures along the run, icons). Run `gradlew iconExport` for `docs/src/icons/servus_foundation/`. Verify: docs tests green, the six line icons in `docs/src/icons/servus_foundation/` are the new ones.

## 5. Integration

- [x] 5.1 Full `gradlew build test runGameTestServerAll` green; the game launches with all three mods.
- [ ] 5.2 Play check in game by the user from main:
  - Every line and data tier looks like its hose or cable; the icons in the hotbar match.
  - On straight runs along X, Z and Y the ribs and braids cross the duct on all four sides, at one spacing, without seams at block borders or joints, never mirrored.
  - Two, three and four line ducts show their bands in the same order from every side.
  - Bends, branches and crossings, including a one line bend and a centre that bends between two heights, are black rubber fittings.
  - Rings at chests and machines still show the channel and tier colours.
  - Nothing flickers, from several angles.

  Verify: the user confirms in game.

## 6. Play test rework

- [x] 6.1 Lower the contrast of the line strips in `tools/duct-textures/gen.py` (the light and dark steps of the ribs, corrugation, spiral and braids one ramp step closer, less noise; the edge shading stays) and regenerate the textures. Verify: a second run changes no file, `gradlew build` green.
- [x] 6.2 The end face of a two line duct showed its bands lying on top of each other. `DuctUv.side` maps end faces as a cross-section with the bands side by side on the across axis; `DuctPoseQuads` applies it to every face. JUnit `DuctUvTest.endFaceShowsBandsSideBySide` replaces `endFacesUntouched`. Verify: `gradlew build test` green, bands upright on duct ends in game.
