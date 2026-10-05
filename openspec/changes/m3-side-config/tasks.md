# Tasks

## 1. Lib types

- [ ] 1.1 Add `SideMode`, `RelativeSide`, `SideConfig` under `lib/.../side/` with JUnit `SideConfigTest` and `RelativeSideTest` (cycle order, permissions, default Auto, facing north and east mappings, byte round trip). Verify `gradlew :lib:test` passes.

## 2. Machines

- [ ] 2.1 Wire `SideConfig` into `MachineBlockEntity` (per side handlers, push filtering, data indices, NBT), handle side buttons in `MachineMenu`, pass the side in the capability registrations, keep data on wrench dismantle in `MachineBlock`. Verify `gradlew build` passes.
- [ ] 2.2 Add the side panel to `MachineScreen` with tooltips and clicks, and the lang keys. Verify `gradlew build` and `gradlew :manufacturing:runData` pass.

## 3. Verification

- [ ] 3.1 Server check over RCON: smelter with a hopper above holding coal and a chest east; with top Disabled the coal stays in the hopper; with top Input it enters; with east Output and other sides Input the ingots go only east. Verify with `data get block`.
- [ ] 3.2 `gradlew clean build` green and `gradlew runClientAll` reaches the title screen without errors. Commit.
