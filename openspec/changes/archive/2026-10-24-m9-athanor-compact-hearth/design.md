# Design

## Context

`AthanorLayout` (pure Java) holds the 13 brick diamond, three tanks two blocks out and six ducts one block up; `AthanorBlockEntity` checks them on Journal use and on its slow check. Tiers are `AthanorTiers.TierDef` lists of `BlockAt(dx, dy, dz, block id)` read by `AthanorTierData` from `athanor_tier/*.json`; offsets are absolute today because the old rings were symmetric. `ServusAlchemyMultiblockProvider` builds the Modonomicon schematics from `AthanorLayout.schematic`. The formed look is a datagen model per tier (`athanor_formed_<tier>[_lit]`) plus `AthanorRenderer` for the item on top, and `CopperDuctBlock` has the `ATHANOR_NEAR` and `ATHANOR_OVER` shapes for the two old duct positions. The target build is the one the user built in "Port Test 26.1" (Nigredo at -130 65 -195, Albedo at -130 65 -203, Citrinitas at -124 65 -208, Rubedo at -131 64 -212) and the look mocked in `temp/athanor-stages.html`.

## Goals / Non-Goals

**Goals:**
- The four stage builds from the test world, checked and shown in the journal.
- Tier blocks that turn with the Athanor's front.
- The new formed model with stage coloured door window and vents.

**Non-Goals:**
- Rituals, grown crystals, the obelisk (follow-up changes).
- Moving existing Athanors to the new hearth; they unform and are rebuilt.

## Decisions

- **Hearth cells by role, matched by block id.** `AthanorLayout.hearth()` returns `BlockAt`s turned by facing: `minecraft:bricks` under the Athanor and in front, `minecraft:brick_slab` on the other seven cells, `minecraft:brick_stairs` as the step two in front. Matching stays by block id, as for tiers today: a slab of any half and a stair of any facing count, which keeps the check simple and forgiving; the schematic shows a bottom slab and a stair rising toward the Athanor. Alternative: block states in the check; rejected, a top slab or a turned stair does not change the build's meaning.
- **Tanks three high.** A tank side counts when Fluid Tanks stand at dy -1, 0 and +1. Foundation merges stacked tanks into one box tank whose fluid handler acts on the box (`FluidTankBlockEntity`), so `tankHandlers()` keeps reading one handler per side from the block on the Athanor's level. The missing-tank message names the side, as today.
- **One duct per tank on the Athanor level.** `AthanorLayout.Duct` loses `overTank`; there is one duct per side at (sign(dx), 0, sign(dz)). `CopperDuctBlock` replaces `ATHANOR_NEAR` and `ATHANOR_OVER` with one formed shape `ATHANOR`: a straight pipe from the flange to the tank side, collision box of the pipe.
- **Tier blocks turn with the front.** Tier JSON offsets are written for a north facing Athanor. `AthanorTiers.turn(BlockAt, Facing)` rotates `dx, dz` with the same `rightX/rightZ` math as `AthanorLayout.tanks`, and `tierOf` takes the facing. The Albedo ring is symmetric; the Citrinitas vats and Rubedo pillars are not, so this is needed now.
- **Interim Citrinitas and Rubedo as built.** Citrinitas: `principle_vat` at (0,-1,3), (-3,-1,0), (3,-1,0) and `amethyst_block` above each (north facing: back is +z). Rubedo: `crying_obsidian` at (-1,y,2), (1,y,2), (-2,y,1), (2,y,1) for y = -1 and 0. The follow-up rituals replace these entries; keeping them now keeps every stage reachable between changes.
- **Schematic anchor.** Modonomicon's Visualize puts `0` one block above the clicked block, so `0` must be the bottom middle cell. From Albedo on the bottom layer is the hollow salt ring; the centre cell is an "any block" matcher. Verified in Modonomicon 2.17.0 for 26.1.2: `DenseMultiblockBuilder.any(char)` writes `modonomicon:any`, whose `AnyMatcher` accepts every block, so no fallback is needed. For Nigredo the bottom layer is the hearth and `0` stays the centre Brick. A dense pattern is read as `pattern[layer][x].charAt(z)`, so `AthanorLayout.schematic` writes rows along x and characters along z. Slabs and the stair go in as `blockstateproperty` matchers (`type=bottom`, `facing=south`), which only check the listed properties.
- **Formed model.** One datagen model per tier and lit state as today, in `ServusAlchemyBlockStateProvider` where the tower already lived, elements as in the mock: brick plinth 16x3x16, brick body 1.5..14.5 up to 17, steel posts on the corners (up to 17.25, so their tops do not share the body's top plane), copper bands at y 3..4 and 15..16 (0.9..15.1, outside the posts so no shared planes), door frame of steel with a black iron plate split around a window 6..10 by 7..10.5, a fire plate at z 0.75..0.78 behind the window in the tier colour, flanges on the left, right and back sides at y 5.5..10.5, dome steps in brick and copper, vents on the brick dome step, steel plate at 22.5..23.5. Steel uses the family textures (`electric_pump_steel`, `coal_generator_side` for the black iron), never `iron_block`. Tier colours come from new small textures per tier for the fire plate and the vents (`athanor_fire_<tier>`), emissive in the lit model. The flame flicker is a texture animation (`.mcmeta`), so no renderer work beyond the existing item renderer. The z-fighting list for every overlapping pair goes into the model task.
- **Collision.** `AthanorBlock.TOWER` follows the new plinth, body and plate; the pipe of each formed duct is low enough to step onto from the hearth slab, and the plate within a jump of the pipe.
- **PLAN.md decisions settled:** the Athanor's stage structure (PLAN "one ring or part per stage") becomes: salt ring under the hearth, vats beside the tanks, pillars behind; the rituals come next.

## NeoForge and library APIs

- Block models via the existing `AlchemyModels` datagen (`BlockModelBuilder` elements), texture animation via a `.mcmeta` next to the PNG (vanilla resource format).
- Modonomicon `DenseMultiblockBuilder` (datagen, already used here) for the schematics.
- No new capabilities; `Capabilities.Fluid` on the Foundation tanks as today.

## Testing

- Pure Java (JUnit): `AthanorLayoutTest` (hearth roles per facing, three high tanks, one duct per side, schematic layers for Nigredo, Albedo and Rubedo), `AthanorTiersTest` (turning offsets, tiers with facing, interim Citrinitas and Rubedo).
- GameTests in `AlchemyGameTests`: forming on the new hearth, the missing step and short tank messages, tier per stage on a north and an east facing Athanor, unform on a broken duct, schematics match the builds (existing test updated).
- Manual: the formed model at every stage from several angles for flicker, stepping onto the pipes and the plate, the journal Visualize pages.

## Risks / Trade-offs

- [Existing worlds lose their formed Athanors] → They unform at the next check and name the missing block; the changelog says the build changed. No migration.
- [Modonomicon has no any-matcher for `0`] → Fallback anchor on a ring block, verified by the schematic GameTest.
- [A 3 high tank merges with a neighbouring tank of the player's own] → Tanks of the build are two apart and the vats are not tanks; a player's extra tank touching one merges into it, which is how Foundation tanks work everywhere.
