# Tasks

## 1. Loaded-only warehouse network

- [ ] 1.1 In `storage/src/main/java/.../storage/network/WarehouseNetwork.java`: read `pullsAround` from the graph member, add a `loaded(BlockPos)` helper and use it for every block entity read, guard `dockRails` for unloaded positions, and drop non-member storage nodes in `poll`. Verify with `gradlew build`.
- [ ] 1.2 Add a Storage GameTest that a member leaving a network does not load another chunk of that network (or, where a GameTest cannot unload chunks, that `pullsAround` and candidate rebuilds use no world reads for members), and run the Storage GameTests (servus-helpers/gt_storage.sh). Verify all green.

## 2. Integration

- [ ] 2.1 Re-run the reproduction (user world copy on the dedicated server, forceload the warehouse, `stop`): the server exits and logs "All dimensions are saved". Full `gradlew build` and `gradlew test` green, Storage GameTests green.
