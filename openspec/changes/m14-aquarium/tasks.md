# Tasks

## 1. Pure logic

- [x] 1.1 Add `delicacies/.../aquarium/AquariumStats` (record with defaults from design) and `AquariumCycle` (input and output records, the eight steps from design). JUnit `AquariumCycleTest`: growth toward capacity (20 of 100 at 100% gives 21.92), single fish does not breed, unfed stops growth, starving after `starve_after` loses `starve_loss`, dirt per fish and feed, quality formula, dying below 20%, capacity clamp after draining, empty water kills all, filter cleans up to rate and reports wear, powered share 0 stops breeding and cleaning but not feeding or dirt, catch above keep limited by room, auto catch off. Verify: `gradlew :delicacies:test` green.
- [x] 1.2 Add `aquarium/AquariumWater` (dirt taken by draining x of y buckets), `AquariumPower` (cost per tick from tank blocks and lid slabs) and `AquariumLook` (drawn fish from stock, capacity, cap; at least one per fish up to two). JUnit `AquariumWaterTest`, `AquariumPowerTest` (4 x 2 x 2 with 8 lids costs 40), `AquariumLookTest` (50% of cap 16 draws 8, 1 fish draws 1, 0 draws 0). Verify: `gradlew :delicacies:test` green.
- [x] 1.3 Add `aquarium/FishPath` (seeded smooth path inside a box below a surface height, heading and pitch from the derivative, speed factor). JUnit `FishPathTest`: positions stay inside the box minus margin for 10,000 samples, same seed gives the same path, surface lower than the box keeps fish below it. Verify: `gradlew :delicacies:test` green.

## 2. Data, fluid and registration

- [x] 2.1 Add `aquarium/AquariumStatsManager` (reload listener like `kitchen/KitchenStatsManager`) and `data/servus_delicacies/aquarium/aquarium.json`; `KitchenTags.FISH_FEED` (`c:fish_feed`) filled with `#c:seeds` in `datagen/ServusDelicaciesItemTagsProvider`. Verify: `gradlew :delicacies:runData`, `gradlew build`, GameTest that the stats load with the example values and wheat seeds are in `c:fish_feed`.
- [x] 2.2 Add Murky Water in `registry/DelicaciesFluids` (type, source, flowing, liquid block, bucket, following `BreweryFluids`), client extension with tinted water textures in `client/ServusDelicaciesClient`, blockstate, bucket model and lang via datagen. Verify: `gradlew build`, in game the Murky Water bucket places a brown green fluid.
  - Note: the in game look of Murky Water is part of the 7.2 play check.

## 3. Parts and forming

- [x] 3.1 Add `aquarium/AquariumFloorBlock`, `AquariumLidBlock` (slab shape 0 to 4 px, `LIT` property, light 12 when lit, occluding), their block entities (`AquariumPartBlockEntity` storing the master position, `AquariumMasterState` held by the master floor), registrations in `registry/ServusDelicaciesBlocks`, `ServusDelicaciesItems`, `ServusDelicaciesBlockEntities`, creative tab, textures (`textures/block/aquarium_floor_side.png`, `aquarium_floor_top.png`, `aquarium_lid_top.png`, `aquarium_lid_side.png`, `aquarium_lid_light.png`, `aquarium_lid_light_off.png`), models, blockstates, loot, recipes and lang via datagen. Task note lists the z-fighting pairs from design (floor top against the tank plate, lid underside against the tank glass top). Verify: `gradlew :delicacies:runData`, `gradlew build`, in game both parts place and drop.
  - Note: one block entity type `aquarium_part` serves both blocks. Z-fighting pairs: Floor top (y 16) against the tank's floor plate down face (y 0): the plate face is culled by the full Floor, and the plate the tank renderer draws starts 0.001 above it. Lid down face (y 0) against the tank's glass top (y 16 below): the glass top is culled by the lid's full bottom (the Lid keeps occlusion). Lid body and panel elements share the box but no face (body: top and sides, panel: down only). In game placing and dropping is part of the 7.2 play check.
- [x] 3.2 Forming and unforming in `aquarium/AquariumForming` with `Wrenchable.onWrench` on both parts: tank lookup through `FluidTankBlockEntity.network()` and `TankNetwork.boxAt`, footprint checks, water or empty, messages; unform on part or tank break, sneaking carrot dismantle and box change (master checks `boxAt` every 20 ticks); drops of slots and raw fish. Verify: `gradlew build`, GameTests in `gametest/AquariumGameTests`: forms 3 x 2 x 2, missing lid refused, lava refused, breaking a floor drops fish and slots, adding a tank layer unforms.
- [x] 3.3 `aquarium/AquariumLidItem`: whole lid layer on a tank top. Verify: `gradlew build`, GameTests: 16 items over a 3 x 2 tank place six and leave 10, 4 items place one, sneaking places one.

## 4. Stock, cycle, handlers and power

- [x] 4.1 Buckets on parts in `useItemOn`: pour, catch, species set and refusals, full and empty refusals; glass untouched. Verify: `gradlew build`, GameTests: pour cod, catch cod, salmon refused while cod present, salmon accepted after the last cod, water bucket on glass fills the tank.
- [x] 4.2 Master ticker: cycle timer running `AquariumCycle`, feed taken from the feed slot, filter through the `m14-carbon-filter` API with the spent item into a catch slot, catch into raw fish, sync of stock, quality and deaths to clients. Verify: `gradlew build`, GameTests with a forced cycle: growth, catch above keep, filter wear and spent filter, starving, dirty deaths.
- [x] 4.3 Item and fluid capabilities on both parts and energy on Floor parts (`RegisterCapabilitiesEvent` in `ServusDelicaciesEvents`): feed and filter insert only, catch extract only, water in through `TankNetwork.fill`, Murky Water out with dirt taken; FE buffer (`MachineEnergyStorage`), cost per tick, powered share, lid `LIT` flips. Verify: `gradlew build`, GameTests: seeds piped in land in feed, bread refused, extract gives only catch, draining 10 of 100 buckets removes 10% dirt and gives Murky Water, lava refused, lid takes no FE, 40 FE/t for 4 x 2 x 2 with 8 lids, unpowered cycle does not breed.

## 5. GUI

- [x] 5.1 `aquarium/AquariumMenu` (registered with `IMenuTypeExtension.create`), `client/AquariumScreen` (status lines, slots, auto catch toggle, keep level with the Foundation one line stepper), `AquariumSettingsPayload` registered in a Delicacies payload class; empty hand or non bucket on a formed part opens it, an unformed part tells the player to form it. Verify: `gradlew build`, GameTest that the settings payload handler changes keep and auto catch, in game the GUI opens from floor and lid.
  - Note: opening from floor and lid in game is part of the 7.2 play check.

## 6. Rendering

- [x] 6.1 `client/AquariumRenderer` on the master: fish with `ModelLayers.COD` / `SALMON` via `submitModel`, paths from `FishPath`, drawn count, slow tails in dirty water, floaters, flopping, gravel bed and seagrass custom geometry, `getRenderBoundingBox` over tank and lid; registered in `client/ServusDelicaciesClient`. Task note confirms the z-fighting list from design. Verify: `gradlew build`, in game from several angles: fish inside the water, no flicker at the bed, lid or floor, fish visible under the water's tint.
  - Note: z-fighting confirmed against the design list: gravel bed 1.05 to 1.95 px over the plate (top at 1.016 px) and under the frame bars' top (2 px), sides inset 0.05 px from the glass (the water's sides sit 0.16 px in); the water's bottom face (1.096 px) lies inside the bed volume, not on a bed face; neighbouring bed cells skip their shared sides; seagrass quads cross on a line and step 0.005 px per plant. The in game look is part of the 7.2 play check.

## 7. Integration and finish

- [x] 7.1 JEI ingredient info for Aquarium Floor, Lid and Murky Water in `integration/jei`. Verify: `gradlew build`, info pages show in JEI.
  - Note: the JEI pages in game are part of the 7.2 play check.
- [x] 7.3 Play check fixes: gravel bed drawn per column in its own 0 to 1 space so every column shows gravel (`client/AquariumRenderer`); fish bucket on the tank glass pours the fish (`aquarium/AquariumTankClicks`); client update tags no longer clear the slots, so the catch stays in the open GUI after a keep or auto catch change (`aquarium/AquariumMasterState`). Verify: `gradlew build`, GameTest: cod bucket on the glass adds one cod and leaves the water unchanged.
- [x] 7.4 Murky Water from the filter: stats `filter_buckets`, `murky_per_dirt`, `murky_capacity` (`AquariumStats`, `AquariumStatsManager`, `aquarium.json`); the cycle scales the filter limit with the tank and caps it by Murky room and water, returning the Murky made (`AquariumCycle`); the master drains that water from the tank into a saved Murky amount (`AquariumPartBlockEntity`, `AquariumMasterState`); parts drain only that amount (`AquariumFluidHandler`, `AquariumWater` removed); GUI gauge with tooltip and JEI hover (`AquariumMenu`, `AquariumScreen`, `ServusDelicaciesJeiPlugin`); lang. Verify: `gradlew build`, JUnit `AquariumCycleTest` (filter rate grows with the tank, cleaned dirt turns water murky, full Murky tank stops the filter, no filter lets the quality sink), GameTests: filter makes 300 mB Murky Water from 12 dirt, full Murky tank stops the filter, parts drain only the Murky Water.
- [ ] 7.2 Full gradle build, tests green, game launches with all mods; play check: forming with the carrot, lid layer placement, buckets, GUI, swimming fish and their count, floaters and flopping, dirty water look, lid light with and without power, pipes and a Shuttles Inbound and Outbound Station feeding and taking the catch, Murky Water into a Sink. Verify: `gradlew build` and `gradlew test` green, client starts from main.

## Workflow follow-up

- Apply after `m14-carbon-filter` and after the applied Delicacies m14 changes are archived.
- At archive: add `m14-aquarium` to PLAN.md's M14 list and groom it, and add an Added line to Delicacies `CHANGELOG.md` under `## Unreleased`.
