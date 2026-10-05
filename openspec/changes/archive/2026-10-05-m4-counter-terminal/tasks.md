# Tasks

## 1. Terminal

- [x] 1.1 Add `StockEntry`, `StockSyncPayload`, `PullItemPayload`, `PushCarriedPayload`, `ServusStoragePayloads`, `ClientStock`, `CounterTerminalMenu`, `CounterTerminalScreen`, `CounterTerminalBlock`, `CounterTerminalBlockEntity`, registry entries, menu type, screen registration, GUI texture, block textures, blockstate, loot, tag, recipe and lang. Verify `gradlew build` and `gradlew :storage:runData` pass.

## 2. Verification

- [x] 2.1 Add `ServusStorageGameTests.terminalPullsAndPushes` with the copied `empty_12x6x12` template and its registration; `gradlew :storage:runGameTestServer` and `gradlew :manufacturing:runGameTestServer` pass, `gradlew clean build` green, `gradlew runClientAll` reaches the title screen. Commit.
