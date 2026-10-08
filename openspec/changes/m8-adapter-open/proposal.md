# Proposal

## Why

During the M8 play test the user found that the Machine Adapter GUI opens only with an empty hand, which is annoying: a player usually holds something. Vanilla blocks with a GUI (chests, furnaces) open with any item in hand and let a sneaking player use the held item instead, for example to place a block against them. The Crafting CPU summary had the same empty hand rule. This is a follow up to the PLAN.md M8 items `m8-machine-adapter` and `m8-crafting-cpu`.

## What Changes

- The Machine Adapter GUI opens on a right click with any item in hand, including the Configuration Carrot; a sneaking player with an item in hand uses the item instead (places a block, dismantles with the carrot).
- The Crafting CPU slot summary shows on a right click with any item except the Configuration Carrot, which keeps forming the cube; sneaking with an item uses the item.
- Audit of every Servus block with a click action; the ones already following the vanilla rule and the deliberate exceptions are recorded in design.md.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `foundation/machine-adapter`: how the GUI opens.
- `storage/crafting-cpu`: how the slot summary is shown.

## Non-goals

- Removing the Access Point GUI (the user has not decided yet).
- Changing ducts, tanks, crates and racks, whose held item actions are part of their use (see design.md).

## Impact

- `foundation/.../adapter/MachineAdapterBlock.java`, `storage/.../cpu/CpuBlock.java`.
- GameTests for both blocks.
