# Tasks

## 1. Framework

- [x] 1.1 Add `MachineStats`, `MachineStatsManager` (registered in `ServusManufacturingEvents`), `MachineBlock`, `MachineBlockEntity`, `BurnerMachineBlockEntity` under `manufacturing/.../machine/`, and the data files `data/servus_manufacturing/machine/burner_extractor.json` and `burner_smelter.json`. Verify `gradlew :manufacturing:compileJava` passes.

## 2. Machines

- [x] 2.1 Add `BurnerExtractorBlock`, `BurnerExtractorBlockEntity`, `BurnerSmelterBlock`, `BurnerSmelterBlockEntity`, registries `ServusManufacturingBlockEntities` and additions to blocks, items and creative tab, item handler capability registration. Verify `gradlew build` passes.
- [x] 2.2 Add `MachineMenu`, `MachineSlot`, `OutputSlot`, `BurnerExtractorMenu`, `BurnerSmelterMenu`, `ServusManufacturingMenus`, `BurnerMachineScreen`, `ServusManufacturingClient`, GUI texture. Verify `gradlew build` passes.

## 3. Assets and data

- [x] 3.1 Paint placeholder textures (shared side and top, front and lit front per machine), extend the blockstate provider with lit orientable models, add loot, block tag and recipe providers and lang entries. Verify `gradlew :manufacturing:runData` writes blockstates, four block models, loot tables, the pickaxe tag and two recipes, and `gradlew build` passes.

## 4. Verification

- [x] 4.1 Server check over RCON: place an iron deposit, an extractor on it, a smelter beside it and a chest beside the smelter, fill fuel with `/item replace block`, wait 10 seconds, verify with `/data get block` that the smelter's output or the chest holds iron ingots. Verify the extractor without a deposit keeps its fuel.
- [x] 4.2 Full check: `gradlew clean build` green, `gradlew runClientAll` reaches the title screen with no missing model, texture or screen errors. Commit.
