# Tasks

## 1. Pure distribution

- [ ] 1.1 Add `EnergySource`, `EnergySink`, `EnergyDistributor` in `lib/src/main/java/io/github/dirnei/mc/servus/lib/energy/` with JUnit `EnergyDistributorTest`: rate cap, sink limited, fair split, unequal needs, sources drained in order, nothing moves with no sinks. Verify `gradlew :lib:test` passes.

## 2. Energy framework in Manufacturing

- [ ] 2.1 Extend `MachineStats` with `fuel_divisor`, add `DATA_ENERGY` and `DATA_ENERGY_CAPACITY` to `MachineBlockEntity`, add `MachineEnergyStorage`, `PoweredMachineBlockEntity`, `EnergyNeighbors`, and the fuel divisor in `BurnerMachineBlockEntity`. Verify `gradlew :manufacturing:compileJava` passes.

## 3. Generator and cable

- [ ] 3.1 Add `CoalGeneratorBlock`, `CoalGeneratorBlockEntity`, `CoalGeneratorMenu`, registry entries, energy and item capabilities, stats JSON `coal_generator.json`. Verify `gradlew build` passes.
- [ ] 3.2 Add `PowerCableBlock`, `PowerCableBlockEntity`, `PowerCableNetwork` with the network type registered in `ServusManufacturing`, registry entries and stats JSON `power_cable.json`. Verify `gradlew build` passes.
- [ ] 3.3 Generalize the screen to `MachineScreen` with a layout, register the generator screen, paint generator and cable textures, add blockstate (orientable generator, multipart cable), loot, tags, recipes and lang. Verify `gradlew :manufacturing:runData` writes the cable multipart blockstate, three cable models, the generator models, two recipes and loot tables, and `gradlew build` passes.

## 4. Verification

- [ ] 4.1 Server check over RCON: a generator with coal fills its buffer to capacity and stops burning new fuel; a chain of three cables between a generator and a second generator forms one network (cable count logged or `data get` on the cable positions); breaking the middle cable splits it. Verify from `data get block` and the log.
- [ ] 4.2 Full check: `gradlew clean build` green, `gradlew runClientAll` reaches the title screen with no model, texture or screen errors. Commit.
