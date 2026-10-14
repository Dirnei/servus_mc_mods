# Proposal

## Why

Every Utility Duct floats in the middle of its block, so a run of pipes and cables across a floor or up a wall looks weightless. A few local rules make ducts rest on what holds them up: on the floor, and draped level over the edge of a step. Across the block they stay in the middle, so climbs run dead centre. Connections into machines stay centred like a plug in a socket. All four lines live in the one duct block, so this one change covers every cable of the mod family.

This is a look follow up to M9, listed under "Open: Foundation" in PLAN.md. It touches the Foundation Utility Duct only.

## What Changes

- Each duct gets a **resting pose**: an offset of its centre from the block centre, worked out from the blocks around it when it is placed or a neighbour changes, never per tick.
  - The pose only ever moves a duct down. Across the block (X and Z) every duct stays in the middle, so vertical runs are dead centre.
  - **Floor**: a duct above a block with a sturdy top face lies on it. A floor run turning up bends cleanly: the bend lies low and its arm up rises from there. A duct that feeds a machine beside it and has an arm up stays centred instead, so the run goes straight up from the connection.
  - **Drape**: a duct with nothing under it takes the height its connected neighbour ducts beside it ask for, as long as they agree. A run over the edge of a step or a short gap stays level.
  - Everything else stays centred: ducts in open air, under a ceiling, on top of another duct, above a bottom slab, and every duct with a facade panel.
  - The play test dropped an earlier wall stage (climbing ducts hugging a wall or a corner): it looked wrong wherever the run met a machine or another connection. A second play test dropped keeping every duct with an arm up centred: a floor run turning up stepped up before the bend.
- **Arms meet cleanly**: where two connected ducts still sit at different heights, the horizontal arm makes a small step up or down (step, then straight) so the run stays joined. An arm into an inventory or machine always ends centred on that face with its channel ring, stepping there from the resting duct.
- The **outline, collision and carrot hits** follow the drawn duct, and the goggles data load overlay is drawn where the duct actually is.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `foundation/duct-appearance`: the duct no longer always sits at the block centre. New requirements for the resting pose (floor, drape, centred cases), for stepped arms between ducts at different positions and toward endpoints, and for the outline, carrot hits and load overlay following the pose. The size requirement now speaks of a centre that may be offset.

## Impact

- Pure logic (new, no Minecraft imports, JUnit): a pose solver in `foundation/.../logistics/duct` that turns the surroundings of a duct into its height and the height of each horizontal arm at its face; `DuctShapes` and `ArmHit` gain offset aware boxes.
- `UtilityDuctBlockEntity`: keeps its base pose and final pose, recomputes them on placement and neighbour change, tells neighbour ducts when its base pose changes, saves and syncs the pose, adds it to the model data (`DuctModelData`).
- `UtilityDuctBlock`: shape cache keyed by pose and arm offsets, carrot arm hit with the pose.
- Client: `UtilityDuctModel` moves the cached part quads by the pose, keeps ring quads at the face, and builds step pieces in code; `DuctOverlayRenderer` draws the load box at the pose.
- No datapack, recipe or network behaviour change. No save migration (nothing is released).
- PLAN.md: the "Open: Foundation" list names the change while it is in flight.

## Non-goals

- Real cable physics: no sagging between supports, no catenary, no movement or swinging.
- Ducts sticking to ceilings, or any duct moving toward a wall or into a corner.
- Resting on partial blocks (bottom slabs, stairs, carpets at their real height); only sturdy full faces count.
- Ducts with facade panels keep the centred look; panels with off centre holes are a later change if wanted.
- More than one hop of draping: a long unsupported span stays centred between its two ends and steps at each end.
- Config option to turn the resting pose off; can follow if players ask.
