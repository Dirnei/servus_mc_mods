# Tasks

## 1. Lib model and widget

- [ ] 1.1 In `lib/src/main/java/io/github/dirnei/mc/servus/lib/side/`: extend `SideMode.java` (Off, Pull, Push, flags), rework `SideConfig.java` (supported list, fallback, factories machine, inbound, outbound), add `SideConfigData.java` and `SideConfigMenus.java`; update `SideConfigTest.java` and add tests for cycling within a set and fallback on unsupported modes. Add `lib/src/main/java/io/github/dirnei/mc/servus/lib/client/SideConfigPanel.java` and `lib/.../side/SideConfigWrench.java`, lang keys in `ServusLibLanguageProvider.java`. Add the two cases to `UtilityDuctBlockEntity.cycle` and rename Disabled references in Logistics. Run `gradlew :lib:runData` and verify `gradlew :lib:test` and `gradlew build` are green.

## 2. Manufacturing machines

- [ ] 2.1 `MachineBlockEntity.java`: `SideConfig.machine()`, push only on pushing sides, `pullInputs` on pulling sides, `cycleFace`. `MachineBlock.onWrench` through `SideConfigWrench`. `MachineScreen.java` draws the Lib panel and drops its own panel code; `MachineMenu` uses `SideConfigMenus.BUTTON_FIRST`. Remove the side lang keys from `ServusManufacturingLanguageProvider.java`. Add GameTests `machinePullsFromNeighbour` and `outputSideDoesNotPush` to `ServusManufacturingGameTests.java`. Run `gradlew :manufacturing:runData` and verify `gradlew :manufacturing:build` and the manufacturing GameTests are green.

## 3. Storage stations and Crafting Station

- [ ] 3.1 Replace `SideModes` with Lib `SideConfig` in `InboundStationBlockEntity.java` (Input, Pull, Off, pull ticker) and `OutboundStationBlockEntity.java` (Push, Output, Off); add `SideConfig.machine()` to `CraftingStationBlockEntity.java` with side aware handler, push and pull. Blocks use `SideConfigWrench`. Menus add `SideConfigData` and handle the buttons; `StationStatePayload` loses `sides`; delete `SideModes.java`, `CycleSidePayload.java`, `SideButtons.java`; screens draw the Lib panel and return to compact layouts (regenerate `inbound_station.png` and `outbound_station.png`). Rewrite the side GameTests for relative sides and add `inboundPullsFromChest` and `craftingStationSideOff`. Run `gradlew :storage:runData` and verify `gradlew :storage:build` and `gradlew :storage:runGameTestServer` are green.

## 4. Side markers

- [ ] 4.1 Add `lib/.../side/SidedBlockEntity.java` and `lib/.../client/SideMarkerRenderer.java`; implement the interface on `MachineBlockEntity`, `InboundStationBlockEntity`, `OutboundStationBlockEntity` and `CraftingStationBlockEntity`; register the renderer for those block entity types in `ServusManufacturingClient` and `ServusStorageClient`. Verify `gradlew build` is green and launch `gradlew runClientAll`: a Pull side shows its frame from afar, the carrot shows frames and glyphs on every face.

## 5. Verification

- [ ] 5.1 Reword the side configuration bullet in `PLAN.md` 2.1. Run `openspec validate m7-side-config-everywhere --strict`, full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. Commit.
- [ ] 5.2 Play check: side markers show on configured faces and with the carrot on every face; the panel looks the same on an Assembler, an Inbound Station and an Outbound Station; carrot on a face cycles with the action bar message; Inbound Station pulls from a chest on a Pull side; Assembler pulls ingredients on a Pull side and pushes only on Push sides.
