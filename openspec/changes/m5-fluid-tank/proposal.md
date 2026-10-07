# Proposal

## Why

The mods cannot store fluids yet. PLAN.md lists `IFluidHandler` as the fluid surface for later, and the user asked for a tank next to the new Fluid Pipe and pumps. The tank comes first because the pipe and the pumps need somewhere to put fluid, also in their GameTests.

Milestone: adds the item "Fluid Tank" to M5 (Logistics first release). PLAN.md is updated in the same change. First of three fluid changes: `m5-fluid-tank`, then `m5-fluid-pipe`, then `m3-pumps`.

## What Changes

- New Logistics block **Fluid Tank**: a glass tank block with one fluid type.
- Touching tank blocks that fill a complete rectangular box (cuboid) form one tank with shared content and a capacity of blocks times `capacity_per_block`. Joined blocks connect their textures. A connected group that is not a full box works as separate single block tanks until the box is complete.
- The tank fills from the bottom layer up and renders its fluid level, takes and gives buckets, tells its content on an empty hand click and exposes `IFluidHandler` on every face.
- A broken tank block keeps the fluid of its own share in its item; the rest stays in the remaining blocks.
- `capacity_per_block` in datapack JSON (example 16,000 mB).

## Capabilities

### New Capabilities
- `logistics/fluid-tank`: the Fluid Tank block, cuboid forming, connected look, one fluid rule, filling order, buckets, capability and drops.

### Modified Capabilities
None.

## Non-goals

- Pipes and pumps (`m5-fluid-pipe`, `m3-pumps`).
- Non rectangular multiblock shapes, a size cap, a tank GUI, comparator output, gaseous fluids filling top down.

## Impact

- `logistics`: new `tank` package (block, block entity, item, tank network, pure shape and layer logic), `fluid` data component, renderer, `data/servus_logistics/tank/fluid_tank.json`, datagen, GameTests.
- `PLAN.md`: section 1 (fluids no longer "later"), 2.2 and M5.
