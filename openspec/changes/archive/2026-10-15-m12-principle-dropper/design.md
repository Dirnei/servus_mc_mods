# Design

## Context

See proposal.md for the why. After `m12-hidden-hollow` the `GnomeHollowBlockEntity` is a plain `BlockEntity` with `PrincipleTanks` in accept mode, no registered capabilities, and the bucket path `FluidUtil.interactWithFluidHandler(player, hand, pos, hollow.tanks(), null)` under the sight. Ripening is `VeinGrowth.tick`, which takes `stepCost` from the tanks per step. Machine numbers come from `MachineStatsManager` (`data/<ns>/machine/<name>.json`) with code defaults in `AlchemyMachineDefaults`. Athanor crafts are rows of `AlchemyRecipeTable.athanor()` with a `tier`; `m12-athanor-citrinitas-rubedo` ships the Citrinitas tier (2) and the research node `JournalResearch.SCHWAZ`, which opens Wandering years and The Yellow Ring.

## Goals / Non-Goals

**Goals:**
- A pipe-fed block that ripens the Hollow below without giving the Hollow any capability.
- Rate and tank size as datapack stats; a pure drip rule with JUnit tests.
- A small model in the family's copper with three glass tubes that show each principle's level, drip particles, an Athanor craft at Citrinitas, a Workshop entry and docs.

**Non-Goals:**
- Feeding gifts (the Receptaculum's job), taking the store out, or any automation of binding.
- A screen for the Dropper, a redstone switch, or dripping onto other blocks.

## Decisions

### D1 Directly on top of the Hollow
The Dropper works only when the block directly below it is a Gnome Hollow (`level.getBlockEntity(pos.below()) instanceof GnomeHollowBlockEntity`). That is the simplest rule to explain and to see, and the spout model ends at the bottom of its block, touching the Hollow's top.
- Alternative: one air block between them, drops falling through the gap. Rejected: more rules (what if the gap is filled), and the plan says "below it" without a gap.
- The lookup happens once per drip interval, a single block entity lookup, not a scan.

### D2 Classes live in the `hollow` package
`PrincipleDropperBlock` and `PrincipleDropperBlockEntity` sit next to the Hollow so `GnomeHollowBlockEntity.receiveDrip(Principle, int)` stays package-private. `receiveDrip` mirrors the bucket path: it fills the Hollow's tank of that principle through `PrincipleTanks.tank(principle).fill`, and returns what was taken. It accepts only while the vein is seeded, not ripe and its `stepCost` takes that principle; otherwise it returns 0.

### D3 Pure drip rule
`PrincipleDrip.plan(Principles held, Principles stepCost, Principles room, int perDrip)` returns per principle `min(perDrip, held, room)` where `stepCost > 0`, else 0. No Minecraft imports; `PrincipleDripTest` covers it. The block entity computes `room` from the Hollow's tank capacity minus contents and asks `receiveDrip` per principle (the Hollow re-checks), then takes what was accepted from its own tanks.

### D4 Tanks and capability
The Dropper holds `PrincipleTanks(capacity, false, setChanged)`: accept mode, so the capability fills and never drains, and each tank takes only its own principle. `ServusAlchemyEvents` registers `Capabilities.Fluid.BLOCK` for the Dropper block entity on every side (and null). A principle bucket used on the Dropper pours in via `FluidUtil.interactWithFluidHandler`, without a sight check; the Dropper is not hidden. Jade's built-in fluid storage view shows the tanks; the plugin adds nothing for the Dropper, so nothing about the block below appears.

### D5 Stats
`AlchemyMachineDefaults` adds `principle_dropper` (kind `crafter`, no recipe types) with `work_ticks` 20 (drip interval), `fluid_per_cycle` 25 (mB per principle per drip) and `fluid_capacity` 3000 (per tank, three buckets; the user asked for at least two or three); the shipped `data/servus_alchemy/machine/principle_dropper.json` repeats them. An iron vein takes 100 mB Salt and 20 mB Mercury per 100 tick step, so 125 mB per 100 ticks keeps it ripening without pause.

### D6 Particles by block event
When a drip moved anything the server calls `level.blockEvent(pos, block, DRIP_EVENT, mask)` with one bit per principle dripped. `BaseEntityBlock.triggerEvent` forwards it to `BlockEntity.triggerEvent` (verified in the 26.1.2 sources), which on the client spawns a few `DustParticleOptions(principle.color() & 0xFFFFFF, 0.6F)` (constructor `(int color, float scale)` verified) just below the spout tip with a small downward speed, and returns true. Block events reach players tracking the chunk; nothing is synced otherwise.

### D7 Model
Design A of https://claude.ai/artifact/MT83CP6P4v6afqX2DHcChH (chosen by the user 2026-10-15): three glass tubes, one per principle, around a copper column, so each liquid shows its own level. One static model, no block state properties, built in `AlchemyModels` with vanilla `copper_block`, `cut_copper` and `glass` textures; tube positions live in `PrincipleDropperTubes`, shared by model, shape and renderer. Boxes (pixels):
- four legs, cut copper, `(3,0,3)-(4.5,4,4.5)` and mirrors at x and z `11.5..13`;
- base plate, copper, `(1.75,4,1.75)-(14.25,5,14.25)`;
- per tube, centres (x, z) Salt `(8,12.2)`, Sulfur `(4.36,5.9)`, Mercury `(11.64,5.9)`: a cut copper collar `centre ± 2`, y `5..5.75`; glass `centre ± 1.75`, y `5.75..13`; a cut copper cap `centre ± 2`, y `13..13.75`;
- column, copper, `(6.5,5,6.5)-(9.5,14.5,9.5)`; inlet neck, cut copper, `(6,14.5,6)-(10,16,10)`;
- spout, cut copper, `(7,1,7)-(9,4,9)`, and nozzle `(7.5,0.25,7.5)-(8.5,1,8.5)`.
Overlap check: collars, glass and caps of one tube meet only at y 5.75 and 13 with opposite faces; tube footprints (x 2.36..6.36, 9.64..13.64, 6..10 at z 10.2..14.2) stay clear of each other and of the column (x and z 6.5..9.5); collars stand on the plate top at y 5, the column too, the neck on the column top at y 14.5; legs and spout meet the plate bottom at y 4. Side faces on one plane never overlap: glass at ± 1.75, collars and caps at ± 2, column and neck on other planes.
`PrincipleDropperRenderer` draws each tube's liquid with the fluid's still sprite and tint, inset 0.3 px from the glass and from y 5.8 up to at most 12.9, as high as its tank is full. The block entity sends its tanks and tank size to clients when a tube's level changes by one of 28 steps.

### D8 Athanor craft
`AlchemyRecipeTable.athanor()` gets `principle_dropper`: input one Principle Vat, 250 mB each of Salt, Sulfur and Mercury, tier 2, 200 ticks. The Principle Vat is already the copper vessel of the Coagulator, so the Dropper reads as a vat taught to drip. JEI shows it through the existing Athanor category.

### D9 Workshop entry
`WorkshopCategory` adds `principle_dropper` "Principle Dropper" (icon the Dropper) beside The Yellow Ring, parent The Yellow Ring, condition the Schwaz research node: spotlight, making it (yellow Athanor text from the table), placing and feeding it.

## Risks / Trade-offs

- [Drips and a falling Dropper tank tell a stranger that the "stone" below is a Hollow] → Only a Citrinitas player who built the Dropper sees it; Jade shows no Hollow data. Recorded as an assumption.
- [A Dropper on a Hollow without the sight player nearby still works] → Intended: automation needs no sight.
- [Fluid left in a broken Dropper is lost] → Same as other small vessels; it holds at most 3,000 mB.

## Assumptions to confirm with the user

- The Dropper sits directly on top of the Hollow, no gap.
- It drips only while the vein is seeded and not ripe, and only the principles the vein's steps take; a ripe or empty Hollow gets nothing, so nothing is wasted.
- Rate: 25 mB of each needed principle every 20 ticks; tanks 3,000 mB per principle (`principle_dropper.json`).
- Recipe: a Principle Vat with 250 mB each of Salt, Sulfur and Mercury on a Citrinitas Athanor, 10 seconds.
- Buckets pour into the Dropper by hand without Elemental Sight; it is never drained, not even by a bucket.
- Drip particles show whenever it drips, also to players without the sight.
- Breaking it drops the Dropper; its principles are lost.
- The Workshop entry opens with Wandering years (Schwaz finished), like The Yellow Ring.
