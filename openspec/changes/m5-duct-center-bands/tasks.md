# Tasks

## 1. Centre per axis

- [x] 1.1 Add the axis aware `centerName` to `DuctParts.java`, generate `center_both_x` in `DuctPartModels.java`, register it in `ServusLogisticsClient.java` and choose it in `UtilityDuctModel.java` from the arm masks. Run `gradlew :logistics:runData` and verify `gradlew :logistics:build` and the Logistics GameTests are green.

## 2. Verification

- [ ] 2.1 Run `openspec validate m5-duct-center-bands --strict`, `gradlew runClientAll` launches. Commit.
- [ ] 2.2 Play check: an east west two line run shows unbroken bands.
