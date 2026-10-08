# Tasks

## 1. Blocks

- [x] 1.1 Remove the empty hand only `useItemOn` override from `foundation/.../adapter/MachineAdapterBlock.java`; in `storage/.../cpu/CpuBlock.java` skip only for the Configuration Carrot. Verify with `gradlew build`.

## 2. Tests

- [ ] 2.1 Add GameTests: `adapterOpensWithAnyItem` in `foundation/.../adapter/AdapterGameTests.java` (useItemOn with cobblestone and with the carrot returns PASS_TO_DEFAULT_BLOCK_INTERACTION) and `cpuSummaryWithAnyItem` in `storage/.../gametest/CraftingCpuGameTests.java` (cobblestone passes, carrot skips). Verify with `:foundation:runGameTestServer` and `:storage:runGameTestServer`, all green.

## 3. Integration

- [ ] 3.1 Full `gradlew build` and `gradlew test` green, Foundation, Storage and Manufacturing GameTests green.
