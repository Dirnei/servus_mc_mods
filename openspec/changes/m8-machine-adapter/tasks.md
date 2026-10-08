# Tasks

## 1. Pure profile logic

- [x] 1.1 Add `foundation/.../adapter/MachineProfile.java` (record, `Role`, Gson parsing) and `ProfileSides.java` (first side of a role, relative to absolute). Add `foundation/src/test/.../adapter/MachineProfileTest.java` (`parsesFurnaceProfile`, `missingSidesAreOff`, `upkeepIsOptional`) and `ProfileSidesTest.java` (`roleToAbsoluteForEachFacing`, `firstSideWinsInFixedOrder`, `missingRoleIsEmpty`). Verify with `gradlew :foundation:test`.

## 2. Supplier capability

- [ ] 2.1 Add `production/ItemSupplier.java` and `SUPPLIER` in `ServusFoundationCapabilities`; register it in Storage (`ServusStorageEvents`) on the Warehouse Controller and the Access Point via a small `access/WarehouseSupplier.java`. Verify with `gradlew build`.

## 3. Adapter block

- [ ] 3.1 Add `adapter/MachineProfiles.java` (reload listener, block and tag lookup), `MachineAdapterBlock`, `MachineAdapterBlockEntity` (recipes cache, active recipe, upkeep tick), `AdapterItemHandler`, `AdapterContent` registries and `AdapterEvents` (capabilities: production, item handler; reload listener; creative tab); default profiles `data/servus_foundation/machine_profiles/{furnace,smoker,blast_furnace}.json`. Verify with `gradlew build`.
- [ ] 3.2 Add `AdapterMenu`, client `AdapterScreen` (ghost slot, level buttons, machine name, count) and its registration. Verify with `gradlew build`.
- [ ] 3.3 Datagen: blockstate and model, loot, pickaxe tag, recipe, lang; textures from `servus-helpers/gen_machine_adapter.py`. Run `:foundation:runData`, check the generated files, `gradlew build`.
- [ ] 3.4 Foundation GameTests `adapter/AdapterGameTests.java` registered in `AdapterEvents`: `adapterReportsSmelting`, `adapterForwardsIngredients`, `adapterRoutesExpectedUpkeep`, `adapterOffersOutput`, `adapterOnChestOffersNothing`. Verify with `:foundation:runGameTestServer`.

## 4. Storage integration

- [ ] 4.1 Machines tab: `production/ProductionView.java`, `ProductionRows.java` (upkeep fields and codec), `client/ProductionTerminalScreen.java`; lang. Verify with `gradlew build` and `:storage:runGameTestServer`.
- [ ] 4.2 Storage GameTest `AdapterOrderGameTests.java`: upkeep refilled from the warehouse, an order for stone finishes through an adapter on a furnace, the order has no coal step. Verify with `:storage:runGameTestServer`, `:foundation:` and `:manufacturing:runGameTestServer`.

## 5. Docs and integration

- [ ] 5.1 Docs Foundation page section on the Machine Adapter, PLAN.md section 3 lists the supplier capability, `:manufacturing:runData` regenerates game data, `npx vitest run` in docs green. Full `gradlew build test`, all GameTest suites green, `runClientAll` reaches the title screen.
