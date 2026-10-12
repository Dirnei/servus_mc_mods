# Tasks

## 1. Exporter on the picture in picture path

- [x] 1.1 `manufacturing/src/main/java/io/github/dirnei/mc/servus/manufacturing/client/IconExportRenderState.java` (record with the export list and a 1x1 rectangle) and `IconExportRenderer.java` (own 64x64 color and depth textures, item drawing as `GuiItemAtlas.drawToSlot`, copy to a mapped buffer, fence wait, PNG through `NativeImage`, restores overrides and projection) per design.md D1, D2, D3 and D5. Run `gradlew :manufacturing:compileJava`.
- [x] 1.2 `IconExporter.java` (registration when both properties are set, title screen hook submits the state once, quits once the renderer is done, unknown ids skipped, item components bound from the built in registries per design.md D4) and `ServusManufacturingClient.java` (registers the renderer in `RegisterPictureInPictureRenderersEvent` when the run asks for icons). Run `gradlew build` and `gradlew test`.

## 2. Icons

- [x] 2.1 Run `gradlew iconExport` from the worktree; check that every `servus_*` item of `gamedata.json` has a non empty 64x64 PNG, that icons of items unchanged since 1.21.1 match the committed ones in size and roughly in pixels, and that new items (Chunk Loader Upgrade, Wireless Packing Station, Field Head, Harvester) have icons. Commit `docs/src/icons/servus_*`; `git status` lists no vanilla icon.
- [x] 2.2 Per mod `runGameTestServer` for Foundation, Manufacturing and Storage stay green; docs tests if anything under `docs/` besides icons changed.
  Result: Foundation 98, Manufacturing 174, Storage 463 required GameTests passed; only icons changed under `docs/`, so no docs tests.

## 3. Check

- [x] 3.1 User looks at the icons in the docs Balance lab and recipe graph.
