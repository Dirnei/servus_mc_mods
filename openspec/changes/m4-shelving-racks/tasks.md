# Tasks

## 1. Rack

- [x] 1.1 Add `RackCompartments` (pure, tested), `RackBlock`, `RackBlockEntity`, `RackRenderer`, registry entries, capability, renderer registration, textures, blockstate, loot, tag, recipe and lang. Verify `gradlew build` and `gradlew :storage:runData` pass.

## 2. Verification

- [x] 2.1 Server check over RCON: a hopper offering 100 cobblestone fills a rack to 64 and 36; a hopper below pulls from a rack with mixed slots. `gradlew :manufacturing:runGameTestServer` passes, `gradlew clean build` green, `gradlew runClientAll` reaches the title screen. Commit.
