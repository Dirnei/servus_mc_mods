# Tasks

## 1. Pure rules and teams

- [ ] 1.1 Add `ResearchState` and `TechNode` under `manufacturing/.../research/` with JUnit `ResearchStateTest`: root available, locked child, start refused when unavailable, consume until complete, switching keeps progress, completed tech unavailable. Add `TeamProvider` and `Teams` in Lib. Verify `gradlew test` passes.

## 2. Data and sync

- [ ] 2.1 Add `TechDefinition`, `Techs`, the datapack registry registration, `ResearchSavedData`, `ResearchManager`, `ClientResearch`, `ResearchSyncPayload`, `SelectResearchPayload`, `ServusManufacturingNetwork`, login sync, and the five tech JSON files plus lang entries. Verify `gradlew build` passes.

## 3. Lab and screens

- [ ] 3.1 Add `OwnedMachine`, `MachineBlock.setPlacedBy`, `ResearchLabBlock`, `ResearchLabBlockEntity`, `ResearchLabMenu`, registry entries, capabilities, stats JSON, textures, blockstate, loot, tag, recipe and lang. Verify `gradlew build` and `gradlew :manufacturing:runData` pass.
- [ ] 3.2 Add `ResearchLabScreen`, `TechTreeScreen`, the LAB layout and GUI texture, assembler gating in `AssemblerMenu` and `AssemblerScreen`. Verify `gradlew build` passes.

## 4. Verification

- [ ] 4.1 Server run: the log shows the five techs loaded (registry count) and no payload or registry errors; a lab placed by command with an owner merged in idles without errors. `gradlew clean build` green and `gradlew runClientAll` reaches the title screen with no errors. Commit.
