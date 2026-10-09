# Tasks

## 1. Store through the Input row and an Access Point

- [x] 1.1 `storage/.../terminal/StorageTerminalBlockEntity.java`: `queueInput(stack)`, the store pass that sends input slots as packets to the nearest Access Point on the data line (`DataPackets.send`, stack back into its slot when the send fails at once), the input refusal; `storage/.../menu/StorageTerminalMenu.java`: `pushCarried`, `clearGrid` and the close path use `queueInput`, the message line shows the input refusal; lang key `gui.servus_storage.terminal.message.input_no_access_point` in `ServusStorageLanguageProvider` and `:storage:runData`. Verify with `gradlew build`.
- [x] 1.2 `storage/.../client/StorageTerminalScreen.java`: Input arrow up, Output arrow down. Verify with `gradlew build`.

## 2. Tests and docs

- [x] 2.1 GameTests in `storage/.../gametest/StorageTerminalRowsGameTests.java`: `inputReachesRack` (rack-only warehouse, Access Point, garage), `inputWaitsWithoutAccessPoint`, `gridPushGoesThroughInput`, `clearGoesThroughInput`; update existing terminal tests that expected an instant push to wait for the put-away. Verify with `gradlew test` and `:storage:runGameTestServer`, all green.
- [ ] 2.2 `docs/src/content/docs/mods/storage.md`: input goes through an Access Point, arrows. Verify with `npx vitest run` in `docs/`.

## 3. Integration

- [ ] 3.1 Full `gradlew build` and `gradlew test` green, Storage GameTests green.
