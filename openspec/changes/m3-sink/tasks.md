# Tasks

## 1. Pure rate logic

- [x] 1.1 Add `SinkRates` in `manufacturing/.../machine/sink/SinkRates.java` (no Minecraft imports) and `SinkRatesTest` in `manufacturing/src/test/java/.../machine/sink/`: height 0, last limited height, unlimited height, heights above it, lists of different length; verify `gradlew :manufacturing:test` is green
- [x] 1.2 Add `VoidBudget` in `machine/sink/VoidBudget.java` and `VoidBudgetTest`: idle bucket grants one second of rate, second take in the same tick gets only the remainder, steady requests settle at the rate, fractional item rate, unlimited grants everything, rate drop caps stored tokens, game time going backwards counts as zero elapsed; verify `gradlew :manufacturing:test` is green

## 2. Data

- [ ] 2.1 Add `SinkStats` (codec with code defaults) and `SinkStatsManager` in `machine/sink/`, register the reload listener next to `MachineStatsManager` in `ServusManufacturingEvents`, add `src/main/resources/data/servus_manufacturing/sink/sink.json` with the example tables and `active_ticks` 40; verify `gradlew :manufacturing:build` and the server log line "Loaded sink stats"

## 3. Blocks and block entity

- [ ] 3.1 Add `ChimneyBlock` (`TOP` property, `updateShape`, `onPlace`/`onRemove` walking down to a Sink) and `SinkBlock` (`FACING`, `ACTIVE`, `Wrenchable` via `SideConfigWrench`, scheduled `tick` clearing `ACTIVE`, `updateShape` recount on `UP`) in `machine/sink/`; register both blocks and items in `ServusManufacturingBlocks` and `ServusManufacturingItems`; verify `gradlew :manufacturing:build`
- [ ] 3.2 Add `SinkBlockEntity` (side config Input/Off with front locked Off, three `VoidBudget`s, `recountChimney`, `markVoided` with scheduled tick, height in the update tag, NBT side config, `onLoad` recount) and register it in `ServusManufacturingBlockEntities`; verify `gradlew :manufacturing:build`
- [ ] 3.3 Add the item, fluid and energy void handlers in `machine/sink/` and register `Capabilities.ItemHandler.BLOCK`, `FluidHandler.BLOCK` and `EnergyStorage.BLOCK` in `ServusManufacturingEvents`, returning null on Off sides; add GameTests in `ServusManufacturingGameTests`: hopper into a five chimney Sink empties, base rate accepts 4 of 64 items, five chimney blocks accept 10,000 FE and a full fluid fill in one tick, front and an Off side return no capability, nothing can be extracted; verify `gradlew :manufacturing:runGameTestServer` is green
- [ ] 3.4 GameTests for the column and active state: three chimney blocks give height 3, breaking the second of four gives height 1, Chimney on stone does nothing, `ACTIVE` goes true on voiding and false after `active_ticks`, a sneak carrot dismantled Sink keeps an Off side; verify `gradlew :manufacturing:runGameTestServer` is green

## 4. GUI and buckets

- [ ] 4.1 Add `SinkMenu` with a `TrashSlot` and `ContainerData` (height, three rates, side modes), register it in `ServusManufacturingMenus`; add bucket handling in `SinkBlock.useItemOn` via `FluidUtil.interactWithFluidHandler` bypassing the budget; GameTest: a lava bucket used by a mock player leaves an empty bucket; verify `gradlew :manufacturing:runGameTestServer` is green
- [ ] 4.2 Add `SinkScreen` in `client/` with the trash slot, height and rate lines (Unlimited for `-1`) and `SideConfigPanel`, register it in `ServusManufacturingClient`; verify `gradlew :manufacturing:build`, then in the client: shift click a stack into the slot, it is gone, the lines match the chimney height

## 5. Look

- [ ] 5.1 Textures in `src/main/resources/assets/servus_manufacturing/textures/block/`: `sink_front` (grate over a dark recess), `sink_fan`, `chimney_top` (cap with open flue); models `block/sink_fan.json` (blades as thin elements); datagen in `ServusManufacturingBlockStateProvider` for the Sink (facing variants) and the Chimney (`TOP` true and false, vanilla `minecraft:block/bricks` sides), loot tables (Sink copies block entity data), recipes (Sink from bricks, iron bars, iron ingot; 2 Chimney from 6 brick items), lang entries for blocks, GUI lines and Unlimited; verify `gradlew :manufacturing:runData` then `gradlew :manufacturing:build`
- [ ] 5.2 Add `SinkRenderer` (fan model via `ModelEvent.RegisterAdditional` and `ModelResourceLocation.standalone`, rotation about the facing axis while `ACTIVE`, speed from the synced height, then side markers) and register it in `ServusManufacturingClient`; add `SinkBlock.animateTick` smoke at the column top; verify in the client: fan spins only while voiding and faster with a taller chimney, smoke rises from the top Chimney block, cap shows only on the top block

## 6. Plan and integration

- [ ] 6.1 Update `PLAN.md`: section 2.1 gets a line on the Sink and Chimney, M3 gets the item "Sink and Chimney"; verify the text has no em dashes
- [ ] 6.2 Full `gradlew build` and `gradlew test` green, all GameTests green, the game launches with all four mods, and in game a Sink fed by a Utility Duct item line, power line and a bucket voids all three
