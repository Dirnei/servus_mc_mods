# Tasks

## 1. Pure rate logic

- [x] 1.1 Add `SinkRates` in `manufacturing/.../machine/sink/SinkRates.java` (no Minecraft imports) and `SinkRatesTest` in `manufacturing/src/test/java/.../machine/sink/`: height 0, last limited height, unlimited height, heights above it, lists of different length; verify `gradlew :manufacturing:test` is green
- [x] 1.2 Add `VoidBudget` in `machine/sink/VoidBudget.java` and `VoidBudgetTest`: idle bucket grants one second of rate, second take in the same tick gets only the remainder, steady requests settle at the rate, fractional item rate, unlimited grants everything, rate drop caps stored tokens, game time going backwards counts as zero elapsed; verify `gradlew :manufacturing:test` is green

## 2. Data

- [x] 2.1 Add `SinkStats` (codec with code defaults) and `SinkStatsManager` in `machine/sink/`, register the reload listener next to `MachineStatsManager` in `ServusManufacturingEvents`, add `src/main/resources/data/servus_manufacturing/sink/sink.json` with the example tables and `active_ticks` 40; verify `gradlew :manufacturing:build` and the server log line "Loaded sink stats"

## 3. Blocks and block entity

- [x] 3.1 Add `ChimneyBlock` (`TOP` property, `updateShape`, `onPlace`/`onRemove` walking down to a Sink) and `SinkBlock` (`FACING`, `ACTIVE`, `Wrenchable` via `SideConfigWrench`, scheduled `tick` clearing `ACTIVE`, `updateShape` recount on `UP`) in `machine/sink/`; register both blocks and items in `ServusManufacturingBlocks` and `ServusManufacturingItems`; verify `gradlew :manufacturing:build`
- [x] 3.2 Add `SinkBlockEntity` (side config Input/Off with front locked Off, three `VoidBudget`s, `recountChimney`, `markVoided` with scheduled tick, height in the update tag, NBT side config, `onLoad` recount) and register it in `ServusManufacturingBlockEntities`; verify `gradlew :manufacturing:build`
- [x] 3.3 Add the item, fluid and energy void handlers in `machine/sink/` and register `Capabilities.ItemHandler.BLOCK`, `FluidHandler.BLOCK` and `EnergyStorage.BLOCK` in `ServusManufacturingEvents`, returning null on Off sides; add GameTests in `ServusManufacturingGameTests`: hopper into a five chimney Sink empties, base rate accepts 4 of 64 items, five chimney blocks accept 10,000 FE and a full fluid fill in one tick, front and an Off side return no capability, nothing can be extracted; verify `gradlew :manufacturing:runGameTestServer` is green
- [x] 3.4 GameTests for the column and active state: three chimney blocks give height 3, breaking the second of four gives height 1, Chimney on stone does nothing, `ACTIVE` goes true on voiding and false after `active_ticks`, a sneak carrot dismantled Sink keeps an Off side; verify `gradlew :manufacturing:runGameTestServer` is green

## 4. GUI and buckets

- [x] 4.1 Add `SinkMenu` with a `TrashSlot` and `ContainerData` (height, three rates, side modes), register it in `ServusManufacturingMenus`; add bucket handling in `SinkBlock.useItemOn` via `FluidUtil.interactWithFluidHandler` bypassing the budget; GameTest: a lava bucket used by a mock player leaves an empty bucket; verify `gradlew :manufacturing:runGameTestServer` is green
- [x] 4.2 Add `SinkScreen` in `client/` with the trash slot, height and rate lines (Unlimited for `-1`) and `SideConfigPanel`, register it in `ServusManufacturingClient`; verify `gradlew :manufacturing:build`, then in the client: shift click a stack into the slot, it is gone, the lines match the chimney height

## 5. Look

- [x] 5.1 Textures in `src/main/resources/assets/servus_manufacturing/textures/block/`: `sink_front` (grate over a dark recess), `sink_fan`, `chimney_top` (cap with open flue); models `block/sink_fan.json` (blades as thin elements); datagen in `ServusManufacturingBlockStateProvider` for the Sink (facing variants) and the Chimney (`TOP` true and false, vanilla `minecraft:block/bricks` sides), loot tables (Sink copies block entity data), recipes (Sink from bricks, iron bars, iron ingot; 2 Chimney from 6 brick items), lang entries for blocks, GUI lines and Unlimited; verify `gradlew :manufacturing:runData` then `gradlew :manufacturing:build`
- [x] 5.2 Add `SinkRenderer` (fan model via `ModelEvent.RegisterAdditional` and `ModelResourceLocation.standalone`, rotation about the facing axis while `ACTIVE`, speed from the synced height, then side markers) and register it in `ServusManufacturingClient`; add `SinkBlock.animateTick` smoke at the column top; verify in the client: fan spins only while voiding and faster with a taller chimney, smoke rises from the top Chimney block, cap shows only on the top block

## 6. Plan and integration

- [x] 6.1 Update `PLAN.md`: section 2.1 gets a line on the Sink and Chimney, M3 gets the item "Sink and Chimney"; verify the text has no em dashes
- [x] 6.2 Full `gradlew build` and `gradlew test` green, all GameTests green, the game launches with all four mods, and in game a Sink fed by a Utility Duct item line, power line and a bucket voids all three

## 7. 2x2x2 multiblock rework (after the first play test)

- [x] 7.1 Add pure `SinkLayout` in `machine/sink/SinkLayout.java` and `SinkLayoutTest`: offsets for all four facings, `controllerOf` inverts `offset` for every part and facing, `candidates` for each front part position, the cube lies behind the clicked face, top and front part sets; verify `gradlew :manufacturing:test` is green
- [x] 7.2 Rework `SinkBlock` and `SinkBlockEntity` in `machine/sink/`: add `FORMED` and `PART`, remove `SidedBlockEntity`, side config, its NBT and menu data; carrot forming over the four candidates with the action bar message, sneak carrot dismantles one block, `onRemove` unforms the rest, piston push reaction blocked while formed, unformed right click hint; GameTests in `ServusManufacturingGameTests`: carrot forms from each front part, seven blocks do not form, forming ignores a formed neighbour, breaking a block unforms the rest and drops a plain Sink block, re-forming after repair; verify `gradlew :manufacturing:runGameTestServer` is green
- [x] 7.3 Route capabilities to the controller in `SinkHandlers` and `ServusManufacturingEvents`: formed parts on all faces except the front of front parts, unformed blocks none; move `markVoided` and `ACTIVE` to the controller; rework the earlier Sink GameTests for the cube (hopper into the side, front refuses, base rate 4 of 64, FE and fluid on different parts share the Sink, nothing to extract, active and idle timing, unformed exposes nothing); verify `gradlew :manufacturing:runGameTestServer` is green
- [x] 7.4 Chimney on a corner: `ChimneyBlock` walks down to any formed Sink part and recounts on its controller, `recountChimney` takes the maximum over the four top parts and syncs height and column position; GameTests: five on a corner gives unlimited, the tallest of two columns counts, breaking a middle chimney block lowers the height, Chimney on an unformed Sink block does nothing; verify `gradlew :manufacturing:runGameTestServer` is green
- [x] 7.5 GUI: `SinkMenu` and `SinkScreen` without side panel and side data, opened from any formed part through the controller; buckets on any formed part; keep the lava bucket and shift click GameTests passing on a formed cube; verify `gradlew :manufacturing:runGameTestServer` is green
- [x] 7.6 Look: textures `sink_casing` and `sink_front_0` to `sink_front_3` (one big grate over a dark recess in quarters), a 2x2 sized `sink_fan`; datagen in `ServusManufacturingBlockStateProvider` for the casing and one model per `PART` rotated by `FACING`; `SinkRenderer` draws one big fan for the controller centred on the 2x2 front with a cube sized render box, no side markers; `animateTick` smoke from the tallest column or the cube top; recipe 8 Sink blocks from 8 bricks around iron bars; lang for the action bar messages; verify `gradlew :manufacturing:runData` then `gradlew :manufacturing:build`
- [ ] 7.7 Update the Sink line in `PLAN.md` section 2.1 to the 2x2x2 multiblock formed with the carrot (no em dashes); full `gradlew build` and `gradlew test` green, all GameTests green, the game launches with all four mods; the user checks in game: forming with the carrot, big fan spinning for all four facings, smoke from the tallest column, unforming on break
