# Tasks

## 1. Junction and axis centres

- [x] 1.1 Extend `DuctParts.centerName`, generate the Y and junction centres in `DuctPartModels.java`, add the `duct_junction` texture, register the parts in `ServusLogisticsClient.java` and choose them in `UtilityDuctModel.java`. Run `gradlew :logistics:runData` and verify `gradlew :logistics:build` and the Logistics GameTests are green.

## 2. Verification

- [ ] 2.1 Run `openspec validate m5-duct-junction-centre --strict`, `gradlew runClientAll` launches. Commit.
- [ ] 2.2 Play check: bends and crossings show a plain centre, straight runs keep their bands in every direction.
