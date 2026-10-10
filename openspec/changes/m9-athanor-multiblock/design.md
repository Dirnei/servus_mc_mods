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

### Look and no GUI (play check 2026-10-10)
The user wanted a real model, pipes to the tanks and no GUI. Formed models `athanor_formed_<tier>` and `_lit` (in `ServusManufacturingBlockStateProvider`, from vanilla stone bricks, iron and copper, stone bricks for contrast with the brick hearth): a plinth, a tower up to 20 px, a shoulder, an iron plate at 23 px, an iron framed fire door at the front whose face is the middle of the existing `athanor_front_<tier>[_lit]` texture (uv 4,5 to 12,15), and copper sockets high on the left, back and right walls. Turned by `MachineBlock.FACING`; unformed states keep the orientable block. The block gets `noOcclusion`, since faces of the tall model have no cull face and would take the light of an occluding block, which is black.

A block model reaches at most one block out, and the tanks stand two out, so `AthanorRenderer` draws the pipes: for each tank of `tankLayout()` a copper box from the socket out over the ring cell at 1.1 to 1.35 blocks, then down into the tank's top; when the block above the tank is a Fluid Tank too, the pipe ends in that block's side instead. It reads only block states on the client; its render box grows by two blocks around and one up. The item rests on the plate at 1.46 blocks when formed.

No GUI: `AthanorBlock.useWithoutItem` never opens a menu; an empty hand takes the output slot, sneaking takes the input back. `AthanorMenu`, `AthanorScreen`, the menu type and the tier container data go; `createMenu` returns null. The renderer shows the input, or the output when no input waits; the update packet is sent when either changes. Jade gets an Athanor provider (tier name, progress, item, result and what the tanks hold per principle). The item capability by side stays as it is.

### Diamond hearth and grown Salt Crystal Block (play check 2026-10-10)
The user saw the tanks float beside a 3 by 3 hearth, the Albedo ring hide the formed tower, and the Athanor not needed for Albedo at all. `AthanorLayout.hearth()` becomes the 13 cells with `|dx| + |dz| <= 2` at `dy = -1`; the tanks of `tanks(Facing)` stand on three of its four tips, the front tip stays free. The diamond is the same in every facing, so the hearth needs no rotation. Forming and the slow check read 13 bricks instead of 9.

`athanor_tier/albedo.json` lists the twelve cells of the 5 by 5 square at `dy = -1` outside the diamond (`|dx| + |dz| > 2`), also the same in every facing. Nothing of the ring stands at the Athanor's level any more, so the tower and its pipes are free; the pipes keep their height.

The Salt Crystal Block's shaped recipe and the shapeless split back into nine go away; a split would make one grown block nine crystals. A new Athanor craft in `AlchemyRecipeTable.athanor()`, `salt_crystal_block`: one Salt Crystal and 1,000 mB Salt at tier 0 give one Salt Crystal Block. Twelve blocks then cost twelve crystals and 12,000 mB Salt, the work that makes Albedo. The journal's Salt Crystal Block page shows the Athanor craft instead of the crafting recipe.

### Copper Ducts instead of drawn pipes (play check 2026-10-10)
The user wanted the pipes to be solid and to jump on the Athanor. `SULFUR_DUCT` becomes `COPPER_DUCT` (`copper_duct`, "Copper Duct", models `copper_duct*`), and `CalcinerDuctBlock` becomes `CopperDuctBlock` in `alchemy/structure`, shared by both builds. Nothing is released, so the old id is not kept.

`AthanorLayout.ducts(Facing)` (pure) gives two cells per tank at `dy = 1`: the near one at the unit step `u` toward the tank, the far one at `2u`, over the tank. `PIECE` grows from 0..2 to 0..4: 3 is the Athanor's near duct (straight, low in the block), 4 its far duct (straight, then down into the tank). `FACING` is set so the Athanor lies to the model's -x, as the Hood does for the Calciner pieces. The far cell accepts a `FluidTankBlock` instead (a stacked tank); then the near duct ends in the tank's side. The pipe lies at the bottom of the duct block, 0 to 4 px, so its top is 1.25 blocks above the ground around the build, just within a jump; the tower's sockets move to 16 to 20 px.

Forming checks the ducts after the tanks and names the side of a missing one (`message.servus_manufacturing.athanor.missing_duct`). Only an unformed duct, or one already formed as an Athanor piece, counts; a Calciner's duct does not. Forming sets the ducts' `FORMED`, `PIECE` and `FACING`; unforming and breaking the Athanor set them back. A duct with piece 3 or 4 calls the Athanor's check on removal and neighbour change (the Athanor lies one block down and one or two back along its -x), so a broken duct unforms it at once; the slow check reads the ducts too. `CalcinerStructure` and `CalcinerPorts` treat only pieces 0..2 as Calciner ducts.

`AthanorRenderer` draws only the item; its render box shrinks back to the block and the space above. Collision: an Athanor duct's shape is its pipe, 4 px wide and 4 px high, along the run (other duct states keep the full block). The formed Athanor's shape follows the tower (plinth, body, shoulder, plate) up to 23 px. A player jumps onto a pipe (1.25 blocks) and steps up onto the plate (1.44 blocks, within the step height).

### Pure vs Minecraft
Pure: `AthanorLayout` with JUnit `AthanorLayoutTest` (13 hearth cells in a diamond, Nigredo pattern, Albedo pattern with the ring in the Athanor layer, mapping). GameTests: forming, missing brick named, not formed does not craft, hearth brick broken unforms at the next check, schematics accept the builds. Existing Athanor GameTests build the hearth and form first.

### PLAN.md
M9 item 6d text.

## Risks / Trade-offs

- [Up to 5 seconds before a broken corner brick unforms] → The brick under the Athanor unforms at once; the corners only through the slow check, which also runs before every craft starts.
