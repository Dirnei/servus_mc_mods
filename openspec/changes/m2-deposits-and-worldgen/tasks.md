# Tasks

## 1. Blocks and plumbing

- [ ] 1.1 Add `ServusManufacturingBlocks`, `ServusManufacturingItems`, creative tab hook in `ServusManufacturingEvents`, register them in `ServusManufacturing`, paint four placeholder textures under `manufacturing/src/main/resources/assets/servus_manufacturing/textures/block/`, and add `ServusManufacturingDataGen` with blockstate and language providers. Verify `gradlew :manufacturing:runData` writes four blockstates, block and item models and `lang/en_us.json`, and `gradlew build` passes.

## 2. Deposit data

- [ ] 2.1 Add `DepositDefinition` with codec and `DepositManager` reload listener registered in `ServusManufacturingEvents`, plus the four JSON definitions under `data/servus_manufacturing/deposit/`. Verify a server run logs "Loaded 4 deposits" with no parse errors.

## 3. Worldgen

- [ ] 3.1 Add configured features, placed features and biome modifiers for the four deposits under `data/servus_manufacturing/`. Enable RCON in the run directories and add the RCON helper script. Verify a server run has no worldgen or biome modifier errors and `/place feature servus_manufacturing:iron_deposit` over RCON reports success for each of the four features.

## 4. Verification

- [ ] 4.1 Full check: `gradlew clean build` green, `gradlew runClientAll` reaches the title screen with no missing model or texture errors for the deposit blocks. Commit.
