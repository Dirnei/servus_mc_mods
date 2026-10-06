# Proposal

## Why

A player cannot see whether a crate or rack is actually part of a working warehouse, or whether a rail beside it has picked it up. The user asked on 2026-10-06 for a visible connection state on the storage blocks and a visible link between a rail and the block it serves. This is polish on the PLAN.md milestone M6 item "Rails as network connections".

## What Changes

- Crates, shelving racks, the warehouse controller and the counter terminal get a small lamp on their front face. The lamp is lit when the block belongs to an online warehouse (exactly one controller) and dark when the block stands alone or its network has no controller or more than one.
- The lamp state is carried by the block state, so every client sees it without a renderer, and it changes only when the network membership or controller count changes, never by a per tick scan.
- A Warehouse Rail renders a dock piece towards every crate, rack, controller or terminal beside it on its level: a short rail ending in a bumper plate in its own colour, visibly different from the plain arm towards another rail. The user asked for the distinct look on 2026-10-06 after seeing the plain arm in game.

## Capabilities

### New Capabilities
- `storage/connection-indicator`: the lamp on storage blocks and when it is lit.

### Modified Capabilities
- `storage/warehouse-rail`: the requirement "Rails auto connect" also connects a rail visually to a storage block beside it.

## Non-goals

- A lamp on rails or elevator segments. They are connectors, their arms show the link.
- A reveal overlay through the Configuration Carrot or goggles.
- Different lamp colours for "no controller" versus "too many controllers". The terminal already says which.

## Impact

- `servus_storage`: a shared `ONLINE` block state property on the four storage blocks, `WarehouseNetwork` pushes the state on membership and controller changes, `ServusStorageBlockStateProvider` writes two models per block, four new lit front textures and the four existing front textures gain a dark lamp, `RailBlock` gains four `dock_*` state flags and `connectsTo` accepts stock holding storage nodes, a dock model and texture, the GameTests assert the lamp state.
- Generated resources: blockstates of the four blocks gain the `online` property.
