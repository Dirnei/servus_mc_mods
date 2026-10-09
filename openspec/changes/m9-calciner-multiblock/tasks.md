# Tasks

## 1. Layout

- [x] 1.1 Add pure `CalcinerLayout` (cells, `Role`, `Facing`, `worldOffset`, `controllerOffset`, `roleAt`) in `manufacturing/.../alchemy/calciner/` with JUnit `CalcinerLayoutTest` (cell count and roles, round trip for every cell and facing, Receiver right of the Alembic per facing, free corners are no cells); `gradlew :manufacturing:test` green

## 2. Blocks and forming

- [x] 2.1 Add `CalcinerPartBlock` with `Role`, `FORMED`, `FACING` (Firebox also `LIT`) and register Firebox, Ash Pan, Alembic, Receiver, Sulfur Hood blocks and items; add `calciner_shell` (no item, drops Bricks, bricks hardness) in `ServusManufacturingBlocks`/`Items`; datagen unformed models, blockstates, loot, lang and the part recipes from the design in `ServusManufacturingBlockStateProvider`, `ServusManufacturingBlockLoot`, `ServusManufacturingRecipeProvider`, `ServusManufacturingLanguageProvider`; change the Calciner recipe to bricks, iron bars and a cauldron; `gradlew :manufacturing:runData` and `gradlew build` green
- [x] 2.2 Add `CalcinerStructure` (form: check four facings with `CalcinerLayout`, best facing for the report, swap Bricks to shells, set `FORMED`; unform: shells back to Bricks, `FORMED=false`, skip the broken position); `FORMED` on `CalcinerBlock`; `formed` and `facing` read from the Calciner block state; `onRemove` on Calciner, parts and shell calls unform; `PushReaction.BLOCK` while formed; `gradlew build` green
- [ ] 2.3 `CalcinerBlock.useItemOn`: the Journal forms or reports the first wrong block (part name and relative position, pointing to the Journal page) and does not open the book, the forming player becomes owner; the carrot fails on every Calciner block; calcinable items go into the input; sneaking with an empty hand returns the input; no menu. `FireboxBlock` takes fuel by hand. GameTests in `AlchemyGameTests`: forms in all four facings, missing Receiver reported and not formed, Journal on the Firebox forms nothing, carrot changes nothing, breaking a shell unforms and drops Bricks, breaking the Calciner drops input and fuel; `gradlew :manufacturing:runGameTestServer` green

## 3. Work and ports

- [ ] 3.1 `CalcinerBlockEntity`: `canWork()` needs `formed`; journal owner is the forming player; remove `sideTanks()` use, lock item sides Off, delete `createMenu`, `CalcinerMenu`, `client/CalcinerScreen`, the menu type and the screen registration; `gradlew build` green
- [ ] 3.2 Replace the Calciner block entity capability registrations in `ServusManufacturingEvents` with `registerBlock` providers by position: insert only input on the Calciner block, insert only fuel and upkeep on the Firebox, drain only Salt on the Ash Pan, Mercury on the Receiver, Sulfur on the Hood, null while unformed; push each principle from its port block every 10 ticks, skipping structure blocks; empty containers fill at the ports with `FluidUtil`. GameTests: a formed Calciner calcines cobblestone with coal from a hopper into the mouth, unformed does nothing, a tank at the Ash Pan gets only Salt, a tank on the Hood only Sulfur, a bucket on the Receiver fills with Mercury, no energy capability on any block, upkeep reaches the fuel slot through the Firebox; `gradlew :manufacturing:runGameTestServer` green

## 4. Formed look

- [ ] 4.1 Formed model pieces per cell under `models/block/calciner/formed/` with `formed` blockstate variants rotated by `FACING` for Calciner, parts and shell; Firebox `LIT` light level and emissive coals; `animateTick` flames at the Firebox and smoke at the Hood; `gradlew :manufacturing:runData` and `gradlew build` green
- [ ] 4.2 `CalcinerRenderer` BER with `getRenderBoundingBox` over the 3x5 box: Salt heap in the Ash Pan, Mercury in the Receiver, Sulfur crust in the Hood by fill; block entity update only when a drawn level changes by a sixteenth; register in `ServusManufacturingClient`; `gradlew build` green, then launch the client and check forming, fire, smoke and fill levels in game

## 5. Journal and Jade

- [ ] 5.1 Add `ServusManufacturingMultiblockProvider` generating `data/servus_manufacturing/modonomicon/multiblocks/calciner.json` from `CalcinerLayout` (`0` the Calciner, `_` free corners) and register it in the data generators; extend `WorkshopCategory.calciner()` with the multiblock page with Visualize, crafting pages for all six parts and new text on forming with the Journal, ports and buckets (no gauges or GUI); `gradlew :manufacturing:runData` and `gradlew build` green, then check the page and Visualize in game
- [ ] 5.2 Add Jade `compileOnly` to `manufacturing/build.gradle` and `integration/jade/CalcinerJadePlugin` showing input, fuel, progress and the three principles for any block of a formed Calciner; `gradlew build` green, then check the tooltip on the Receiver in game

## 6. Plan and integration

- [ ] 6.1 Update `PLAN.md` M9: items 6b `m9-calciner-multiblock`, 6c `m9-coagulator-multiblock`, 6d `m9-athanor-multiblock`, the Tria Prima paragraph (Calciner is a structure with ports, no side configuration) and the Calciner row of the machine table; check no em dashes were added
- [ ] 6.2 Full `gradlew build`, `gradlew test` and `:manufacturing:runGameTestServer` green, the game launches with all three mods, and the user play checks the build from the Journal schematic to a working Calciner
