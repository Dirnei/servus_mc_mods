# Tasks

## 1. Reserve target in stats

- [x] 1.1 Add `fuelReserve` to `manufacturing/.../machine/MachineStats.java` (record field, `Partial` field with `fuel_reserve` codec entry, `over` merge clamped at 0, default 0 in every factory, a `burner(int workTicks, int fuelReserve)` overload). Add `"fuel_reserve": 4` to `manufacturing/src/main/resources/data/servus_manufacturing/machine/burner_extractor.json`. Verify `gradlew :manufacturing:build` is green.

## 2. Pure routing rule

- [x] 2.1 Add `manufacturing/.../machine/extractor/FuelReserve.java` with `wantsYield(int target, boolean yieldIsFuel, boolean slotEmptyOrSameItem, int slotCount)` and `manufacturing/src/test/java/.../machine/extractor/FuelReserveTest.java` with tests: target 4 and count 0 wants, count 3 wants, count 4 does not, count 64 does not, different item does not, non fuel yield does not, target 0 does not. Verify `gradlew :manufacturing:test` is green.

## 3. Extractor routing

- [x] 3.1 In `manufacturing/.../machine/extractor/BurnerExtractorBlockEntity.java` set the code default to reserve 4, add `reserveWants(ItemStack yield)` built on `FuelReserve`, make `canWork()` accept reserve or output and `finishWork()` insert into the fuel slot when the reserve wants the yield. Update the Burner Extractor line in the `PLAN.md` section 2 machine table. Verify `gradlew :manufacturing:build` is green.
- [x] 3.2 Add GameTests to `manufacturing/.../gametest/ServusManufacturingGameTests.java`: `coalExtractorKeepsFuelReserve` (coal deposit, one coal, after 500 ticks the fuel slot holds 4 coal and the output slot at least 1) and `ironExtractorNeverReservesOre` (iron deposit, one coal, after 200 ticks the fuel slot holds no raw iron). Verify `gradlew :manufacturing:runGameTestServer` passes all manufacturing tests.

## 4. Verification

- [x] 4.1 Run `openspec validate m3-extractor-fuel-reserve --strict`, full `gradlew clean build` green, `gradlew test` green, `gradlew runClientAll` launches with all four mods. Play check: an extractor on coal lit with one coal fills its fuel slot to four, then pushes coal into a duct. Commit.
