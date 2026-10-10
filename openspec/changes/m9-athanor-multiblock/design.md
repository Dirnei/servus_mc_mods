# Design

## Context

See proposal.md. The Athanor (`AthanorBlock`, `AthanorBlockEntity`) already computes its tier from data (`AthanorTierData`, `athanor_tier/*.json`, offsets from the Athanor) with pure `AthanorTiers.tierOf`, on placement, use, craft start and every `TIER_INTERVAL` ticks. The Calciner and Coagulator builds form with the Journal and generate their schematics from pure layouts; Modonomicon's Visualize anchors the `0` center one block above the clicked block.

## Goals / Non-Goals

**Goals:**
- Make the Athanor a build with the least change: a hearth, forming, and schematics per stage from the existing tier data.

**Non-Goals:**
- A new look or dropping the GUI (left for the structure discussion).

## Decisions

### Hearth, checked slowly
`AthanorLayout` (pure) lists the nine hearth offsets `(dx, -1, dz)`, `dx, dz` in -1..1. The hearth Bricks stay vanilla. Forming (`AthanorBlock.useItemOn` with the Journal) checks the nine positions and sets `FORMED`. `checkTier` (already run on use, craft start and every 5 seconds) also reads the nine bricks and unforms when one is missing; `neighborChanged` catches the brick right under the Athanor at once. Nine known block reads every 5 seconds fit the existing rule for the tier check. Alternative: a shell block for the hearth like the Calciner's; not worth it while the hearth is plain bricks.

`AthanorBlockEntity.canCraft` also needs `formed()`. Tier computing is unchanged, so the GUI still shows the stage of an unformed Athanor.

### Schematics per stage
`AthanorLayout.schematic(List<BlockAt> extra)` (pure) builds the dense pattern: layers top to bottom over the bounding box of the hearth, the Athanor and the given tier blocks; rows along z, characters along x; the hearth centre is `0` (the anchor, so the hearth stands on the clicked ground), other hearth cells `B`, the Athanor `A`, tier blocks one character per distinct block id from `C` on, everything else `_`. It returns the layers and the character to block id map.

`ServusManufacturingMultiblockProvider` reads the shipped tier JSONs from the classpath (the data generator runs with the main resources on it) and writes `athanor_nigredo` (hearth and Athanor) and one `athanor_<tier>` per tier with the blocks of that tier and all lower ones. The tier list it reads is `AthanorTierData.SHIPPED` (`albedo`); a datapack's own tiers still work in game, they just have no book page.

### Journal
The Athanor entry gets the Nigredo multiblock page and a line on the hearth; the Salt Crystal Block entry gets the Albedo page.

### Tanks (play check 2026-10-10)
The user wanted three tanks the principles are piped into. `AthanorLayout.tanks(Facing)` (pure) gives three offsets at the Athanor's level two blocks out: behind, left and right of its front (`MachineBlock` facing); the front stays open for the player. No two of them touch, so the Foundation Fluid Tanks never merge into one; a player may stack tank blocks on one to grow it, and its whole group counts. Forming needs a `FluidTankBlock` at each position; the slow check (every 5 s and before every craft) reads the three positions too and unforms when one is gone, like a corner brick.

The Athanor reaches the tanks through `Capabilities.FluidHandler.BLOCK` at those positions, on the server thread. `canCraft` sums each principle over the three handlers; a finished craft drains each principle from the handlers in turn, simulating first. Any tank may hold any principle. `PrincipleTanks` leaves the Athanor with its save data, the bucket use and the `fluidHandler` capability; the Athanor's side panel keeps only items. The menu's principle data shows the summed amount and the summed capacity of the tanks holding it, so the GUI keeps its bars. Nothing is released, so old Athanors simply lose their stored fluid.

Risk: a datapack tier whose blocks use the tank positions would clash; the shipped Albedo ring stays one block from the Athanor.

### Pure vs Minecraft
Pure: `AthanorLayout` with JUnit `AthanorLayoutTest` (nine hearth cells, Nigredo pattern, Albedo pattern with the ring in the Athanor layer, mapping). GameTests: forming, missing brick named, not formed does not craft, hearth brick broken unforms at the next check, schematics accept the builds. Existing Athanor GameTests build the hearth and form first.

### PLAN.md
M9 item 6d text.

## Risks / Trade-offs

- [Up to 5 seconds before a broken corner brick unforms] → The brick under the Athanor unforms at once; the corners only through the slow check, which also runs before every craft starts.
