# Tasks

## 1. Colour ramp

- [x] 1.1 Add pure `athanor/ritual/CrystalRamp` (five RGB stops, `at(brightness)` over 0.45 to 1.1 with clamping, `mix(a, b, t)` stop by stop) with JUnit `CrystalRampTest`: darkest and brightest stop at the ends, clamping below and above, middle stop at the midpoint, mix at 0, 0.5 and 1. Verify with `gradlew :alchemy:test`.
- [x] 1.2 Replace `body()` in `coagulator/VatCrystal` with the ramp (citrine `#d29e00 #f0c000 #ffd92e #ffe96a #fff6b8`, Rubedo `#c4500a #ea6e10 #ff8a24 #ffb055 #ffdcaa`) and set the liquid colour to the third stop; `client/VatTints.Tint` carries a ramp instead of a body colour; `client/RitualLooks.vat` mixes ramps by recolour share and toward white by the pulse. Verify with `gradlew :alchemy:build`.

## 2. Tower layout

- [x] 2.1 Add pure `athanor/ritual/CitrineTower` per design: segment boxes of mock A with their growth over the first 55 percent, the stepped tip once the segments stand, the eleven bud placements with stage 1 to 4 over the last 45 percent, lean (top buds 0, side buds 0.5 to 1 of 38 degrees upward), spin within plus or minus 38 degrees, scale 0.6 to 1.4, all from bud index plus seed, and buds left out on closed sides per tower level. Remove `CrystalCluster` and its test in `RitualTimelineTest`. Add JUnit `CitrineTowerTest`: nothing at growth 0; segments rise in order and buds only after the tower stands; full growth gives three segments, three tip steps and eleven buds at stage 4; every side lean is upward and within range; every scale within range; a closed east side on level 0 drops exactly the buds on that face and level; same seed gives the same layout and two seeds differ. Verify with `gradlew :alchemy:test`.

## 3. Drawing

- [x] 3.1 Rewrite `client/CrystalDrawing.cluster` into a tower drawing: boxes as one quad per texel with the mock's tones (facet bands on tower sides, dark edge and bright middle on bud shards, bright tips), segments and tip steps without bottom faces, colour from the ramp, up normals, `GLOW` light, white texel of `ritual_crystal`; bud poses from the layout with world down as the axis the lean tips away from on every side; open sides read from the level beside the vat at each tower level (air only). Make `assets/servus_alchemy/textures/block/ritual_crystal.png` plain white 16x16. Verify with `gradlew :alchemy:build`.
- [x] 3.2 Update `client/GrownVatRenderer` (pass ramp, open sides; bounds three blocks up and one around) and `client/RitualLooks.vat` (growing tower with target ramp). Verify with `gradlew :alchemy:build` and `gradlew :alchemy:test`.

## 4. Spec text in the book and verification

- [x] 4.1 Check the journal entries (`datagen/book/JournalEntries`, `WorkshopCategory`) and lang for words like "cluster" or "prisms" about grown citrine and reword them to the tower; run `gradlew :alchemy:runData` when datagen changed. Verify with `gradlew :alchemy:build`.
- [x] 4.2 Play check in the client from main when the user starts it: cast Citrinitas on a test Athanor, watch the tower grow block by block and the buds in four sizes, no buds in the tanks, all side buds lean up, yellow not muddy, no flicker from several angles, Rubedo turns the towers orange; F3 frame time with three vats.
- [x] 4.3 Full `gradlew build`, tests green, game launches with all mods.

## Workflow follow-up

- Archive with `/opsx:archive`, add an Alchemy CHANGELOG line under Unreleased (Changed: grown citrine is now a stepped tower with buds in clear yellows).
