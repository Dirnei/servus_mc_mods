# Proposal

## Why

The citrine grown by the Citrinitas ritual is a cluster of six sided prisms with a stretched texture, tilted at free angles, in a muddy dark yellow. It does not look like Minecraft next to the blocky Athanor. The user settled a new look in a mock (`temp/citrine-tower.html`): a stepped tower of upright boxes with small box buds, in clear yellows.

Milestone M9 (Alchemy), Athanor rituals. PLAN.md has no item for it: it is a look refinement of the archived Citrinitas ritual.

## What Changes

- A grown vat shows a stepped citrine tower instead of the prism cluster: three upright blocks that narrow upward, with a stepped tip, about as high as the tanks.
- Small buds grow on the tower: upright boxes with stepped pixel tips, whose side shards tilt only in 22.5 degree steps. Buds on the tower's sides lean upward only, buds on its top stand straight. Every bud has its own lean, turn and size, the same every time for the same vat, and neighbouring vats differ.
- Buds never grow into a block next to the vat (the Athanor's tanks): a side of the tower with a block beside it at that height grows no buds.
- The ritual grows the tower block by block, then the buds sprout through four sizes, like amethyst buds.
- Crystal colours become a ramp of clear yellows from shadow to highlight instead of one colour darkened toward brown. Rubedo gets an orange ramp. The Rubedo ritual turns the ramp from yellow to orange.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `alchemy/athanor-rituals`: the look of grown citrine and of the Citrinitas growth changes from a prism cluster to a citrine tower with buds, which keep clear of neighbouring blocks.

## Non-goals

- The Rubedo Obelisk look stays as it is.
- No new blocks, items or ritual rules; breaking, falling back and drops stay as they are.
- No vanilla style crossed sprite buds and no free angle prisms; both were tried in the mock and rejected.

## Impact

- `alchemy` client: `CrystalDrawing`, `GrownVatRenderer`, `RitualLooks`, `VatTints`.
- `alchemy` pure logic: `athanor/ritual/CrystalCluster` is replaced by a tower layout; `VatCrystal` carries a colour ramp instead of a body colour.
- Texture `ritual_crystal.png` becomes a plain white texel source, since the colours come from the ramp.
- Tests: `RitualTimelineTest` cluster test replaced by tower layout tests.
