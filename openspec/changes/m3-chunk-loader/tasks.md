# Tasks

## 1. Chunk loader

- [x] 1.1 Add `ServusManufacturingConfig`, `ChunkLoaderData`, `ServusManufacturingChunks`, `ChunkLoaderBlock`, `ChunkLoaderBlockEntity`, `ChunkLoaderMenu`, `ChunkLoaderScreen`, registry entries, capabilities, screen registration, stats JSON, textures and GUI, blockstate, loot, tag, lang and the assembling recipe. Verify `gradlew build` and `gradlew :manufacturing:runData` pass.

## 2. Verification

- [x] 2.1 Server check over RCON in a far chunk: generator, chunk loader, deposit and extractor; after removing the vanilla force load and waiting, the extractor's output grew. A control extractor without a loader did not. `gradlew :manufacturing:runGameTestServer` passes, `gradlew clean build` green, `gradlew runClientAll` reaches the title screen. Commit.
