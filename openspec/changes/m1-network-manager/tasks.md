# Tasks

## 1. Pure core

- [ ] 1.1 Add `Mailbox` in `lib/src/main/java/io/github/dirnei/mc/servus/lib/network/core/` with JUnit `MailboxTest`: FIFO order, drain handles only events present at drain start, events posted during handling wait, concurrent posts from several threads all arrive. Verify `gradlew :lib:test` passes.
- [ ] 1.2 Add `ServusNetwork`, `NetworkFactory` and `NetworkSet` with JUnit `NetworkSetTest`: first node creates a network, neighbour joins, bridging merges with `onAbsorb` called on the survivor before dispose, cutting splits with new network for the smaller part, last node removed disposes, `nodeRefreshed` after a rule change, `disposeAll`. Verify `gradlew :lib:test` passes.
- [ ] 1.3 Add JUnit `NetworkSetTickTest`: events handled in order before `onTick`, self posted event waits a tick, networks tick in ascending id order. Verify `gradlew :lib:test` passes.

## 2. Minecraft glue

- [ ] 2.1 Add `NetworkType`, `NetworkTypes`, `NetworkNode`, `Conversions` in `lib/src/main/java/io/github/dirnei/mc/servus/lib/network/`. Verify `gradlew :lib:compileJava` passes.
- [ ] 2.2 Add `NetworkManager` (per level, lazy sets, `join`, `leave`, `refresh`, `tick`, `disposeAll`) and `NetworkEvents` (game bus subscriber for `LevelTickEvent.Post` and `LevelEvent.Unload`). Verify `gradlew :lib:compileJava` passes.
- [ ] 2.3 Add abstract `NetworkBlockEntity` with join on `onLoad`, leave on `setRemoved` and `onChunkUnloaded`, idempotent via a `joined` flag, plus `refreshNetworkConnections()`. Verify `gradlew build` passes.

## 3. Verification

- [ ] 3.1 Full check: `gradlew clean build` green with all lib tests, `gradlew runServerAll` reaches "Done" with the lib event subscriber registered (no errors in log), `gradlew runClientAll` reaches the title screen. Commit.
