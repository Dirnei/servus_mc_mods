# Proposal

## Why

A producer that pushes its outputs on its own (a Burner Smelter or Crusher on Auto sides, or any machine of another mod) and stands directly beside an Access Point pushes the outputs of a CPU step into it. The items are put away, but the step only counts outputs that the CPU moved itself as packets, so the order stays running forever. `m8-station-job-collect` fixed this for the Crafting Station only, by keeping its job outputs. This change closes the gap for every producer and finishes PLAN.md M8 (follow up of item 5, `m8-crafting-cpu`).

## What Changes

- Items that a producer pushes into an Access Point through the face that touches it count for the producer's running step of that item, up to what the step still needs. Steps of the CPUs on the Access Point's data line are served oldest order first.
- Items arriving through any other face (packets from a data duct, a hopper, a chest, a player) count for no step, as today.
- Nothing else changes: the CPU still collects outputs as packets, and items beyond a step's need are put away as before.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `storage/crafting-cpu`: a new requirement, outputs pushed into an Access Point by the step's producer count for the step.

## Non-goals

- Counting outputs that reach an Access Point through something in between (a hopper or duct between a machine and the Access Point). Such lines run all the time and belong to stations and pipes, not orders.
- Stopping native machines from pushing while they hold a CPU step: the credit rule covers them and foreign machines alike.

## Impact

- `storage/.../access/AccessPointBlockEntity.java`: side aware intake handlers.
- `storage/.../cpu/CpuOrders.java`, `storage/.../order/OrderStep.java`: crediting pushed outputs.
- Storage GameTests.
