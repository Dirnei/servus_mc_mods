# Design

## Context

`CraftingStationBlockEntity.pushOutputs` runs every few ticks and, for every Auto or Push side with an item handler neighbour, inserts the outputs into it. Since `m8-station-hands` the CPU collects a step's outputs with `DataPackets.move` to an Access Point and credits the step when the packets arrive (see `OrderBook`). Outputs that leave the station any other way are never credited. See proposal.md.

## Goals / Non-Goals

**Goals:** a held job's outputs only leave through the CPU, whatever the neighbours are.

**Non-Goals:** crediting outputs that leave by hand or by a hopper pulling them.

## Decisions

- "Holds a job" means the station has a job object set, from `assign` until `release`, including the moment after the last craft before the CPU releases it. Using `working()` (crafts left) instead would push the outputs of the last craft away in the gap before the CPU's collection, which is exactly the bug.
- Pull sides keep pulling while a job is held; only the push half of the side loop is skipped. Inserting into the station through sides is unchanged.
- No new state, no saved data: the existing `job` field decides.
- The dock GameTest `outboundServesCraftingStation` hands a 2 craft job and releases it once both crafts are done, then expects the sticks in the crates as before.

## Risks / Trade-offs

- [A player who used Auto sides to feed a chest beside a station loses that push while a job is held] -> intended; without a CPU the station cannot have a job anyway, and leftovers still push once the job ends.
