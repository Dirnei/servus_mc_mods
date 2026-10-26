# Tasks

Apply after `m5-duct-endpoint-gui`.

## 1. Pure logic

- [x] 1.1 Add pure `foundation/logistics/duct/FluidFilter` (nine nullable ids, `allows`, `set`, `clear`, `firstFree`, `entries`, `isEmpty`). JUnit `FluidFilterTest`: empty passes all, listed passes only listed, set and clear, first free after gaps, out of range ignored. `gradlew :foundation:test` green.
- [x] 1.2 `fluid/FluidRouting`: `Sink.priority()` default 0, `routeSupply` per priority group highest first with the remaining supply. Extend `FluidRoutingTest`: tiers 5, 5, 0 (fair inside, leftover down), a full high tier passes everything down, all zero equals the old expectations, mixed keys with tiers. `gradlew :foundation:test` green.

## 2. Block entity and network

- [x] 2.1 `logistics/duct/UtilityDuctBlockEntity`: `fluidFilters[side]` and `fluidPriorities[side]` with getters and setters, save under `"FluidFilters"` and `"FluidPriorities"`, load with unknown ids as empty, update tag, dismantle keeps them. `gradlew build` green.
- [x] 2.2 `logistics/network/FluidChannelNetwork`: filtered offers per design D5, sinks refuse unlisted keys, `accepts` honours the filter, `fill` checks the pushing side's filter and the sinks' filters, sink priority from `fluidPriorities`. `gradlew build` green.
- [x] 2.3 GameTests in `logistics/gametest/ServusLogisticsGameTests`: water and lava sources reach a lava-only sink as lava only; a block with two fluids gives only the listed one; priority 5 tank fills before priority 0; same priority shares; pushed water refused by a lava-only side; filters and priorities survive save and load. GameTests green.

## 3. Menu and payload

- [x] 3.1 Add `logistics/menu/SetDuctFluidFilterPayload` (register beside `SetDuctFilterPayload`) and `UtilityDuctMenu.setFluidFilterById` with the checks from design D2; fluid priority data slot and priority buttons for the fluid line. `gradlew build` green.

## 4. Screen, picker and JEI

- [x] 4.1 `client/ItemPicker` fluid entry kind drawn with `client/FluidSprites`; `logistics/client/FilterPickerEntries.fluids()` (source fluids, not empty, named by fluid type, sorted, "none" first). `gradlew build` green.
- [x] 4.2 `logistics/client/UtilityDuctScreen`: Fluid Pipe page with nine fluid slots (sprite, tooltip name), click with carried fluid item, right click clear, empty cursor opens the fluid picker, shift click of an inventory item sets the first free slot, fluid priority with minus and plus; item slots hidden there. `gradlew build` green.
- [x] 4.3 `integration/jei/DuctFilterGhostHandler`: fluid targets for `NeoForgeTypes.FLUID_STACK` and for filled containers on the Fluid Pipe page. `gradlew build` green.
- [x] 4.4 Lang keys in `logistics/datagen/LogisticsDataParts` (fluid filter label, fluid picker title, fluid priority). `gradlew :foundation:runData` and `gradlew build` green.

## 5. Data Potato

- [x] 5.1 `logistics/duct/DuctReport`: fluid priority and filter count on the fluid side line, filter count on the item side line; lang keys. Extend `DataPotatoGameTests` with a fluid side report. `gradlew :foundation:runData`, `gradlew build` and GameTests green.

## 6. Play check and wrap up

- [ ] 6.1 Play check from main: water bucket, lava bucket, a filled Fluid Tank item and a modded container set slots without being emptied; shift click sets the first free slot; picker lists each fluid once; JEI fluid drag; lava only reaches the furnace-like sink; a priority 5 tank fills first; slots look right in both GUI scales.
- [ ] 6.2 Full gradle build, tests green, game launches with all three mods.

## Workflow follow-up

- Archive with `/opsx:archive` after `m5-duct-endpoint-gui`, add player lines under `## Unreleased` in `foundation/CHANGELOG.md` (Added: fluid filters with buckets and JEI, fluid priorities on the duct).
