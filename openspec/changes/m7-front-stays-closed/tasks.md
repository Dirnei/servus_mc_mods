# Tasks

## 1. Fixed sides

- [x] 1.1 Add `lock`, `isLocked` and the locked rules to `lib/.../side/SideConfig.java` with `SideConfigTest.lockedSideStays`; grey locked cells in `lib/.../client/SideConfigPanel.java` and add the lang key. Lock the front in `MachineBlockEntity.java` and `CraftingStationBlockEntity.java`; add GameTest `machineFrontIsClosed` to `ServusManufacturingGameTests.java`. Verify `gradlew :lib:test`, `gradlew build` and the Manufacturing GameTests are green.

## 2. Verification

- [ ] 2.1 Run `openspec validate m7-front-stays-closed --strict`, `gradlew build` green, `gradlew runClientAll` launches. Commit.
- [ ] 2.2 Play check: the front cell is greyed on an Assembler and the carrot on the front only reports Off.
