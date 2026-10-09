# Tasks

## 1. Loaded-only warehouse network

- [x] 1.1 In `storage/src/main/java/.../storage/network/WarehouseNetwork.java`: read `pullsAround` from the graph member, add a `loaded(BlockPos)` helper and use it for every block entity read, guard `dockRails` for unloaded positions, and drop non-member storage nodes in `poll`. Verify with `gradlew build`.
- [x] 1.2 GameTests cannot unload chunks, so the unload loop is covered by the dedicated server stop check (servus-helpers/stop_check.sh: start runServerAll, optional RCON commands, `stop`, thread dump on a hang). Run the existing Storage GameTests (servus-helpers/gt_storage.sh) to show the loaded-only lookups change no behaviour. Verify all green.

## 2. Integration

- [x] 2.1 Re-run the reproduction (user world copy on the dedicated server, forceload the warehouse, `stop`): the server exits and logs "All dimensions are saved". Full `gradlew build` and `gradlew test` green, Storage GameTests green.
