# Tasks

## 1. Push rule

- [x] 1.1 In `storage/.../crafting/CraftingStationBlockEntity.java` skip pushing outputs while a job is set (pulling stays); adjust `outboundServesCraftingStation` in `ServusStorageGameTests.java` to release a 2 craft job before expecting the sticks in the crates. Verify with `gradlew build` and `:storage:runGameTestServer`.
- [ ] 1.2 Add GameTest `stationBesideAccessPointFinishes` in `StationHandsGameTests.java`: a station directly beside the Access Point, an order for sticks finishes and the sticks are counted. Verify with `:storage:runGameTestServer` all green, plus `:foundation:` and `:manufacturing:runGameTestServer`.
