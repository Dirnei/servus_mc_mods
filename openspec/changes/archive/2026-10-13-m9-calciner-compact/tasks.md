# Tasks

## 1. Layout

- [x] 1.1 Rewrite `CalcinerLayout` (manufacturing `alchemy/calciner`) to the 14 cells of the design.
  - Roles `STONE_BRICKS` (`S`) and `DEEPSLATE_TILES` (`E`); drop `BRICK` and `AIR`.
  - `FIRST_DUCT = 12`, `SULFUR_OUTLET = 13`, new `SULFUR_TANK = 10` and `MERCURY_TANK = 11`.
  - Bounds -1 to 1 wide and -2 to 2 tall. Update the class Javadoc picture.
  - Update `CalcinerLayoutTest`:
    - `cellsOfEveryRole`: 14 cells, 2 stone, 2 deepslate, 2 tanks, 2 ducts.
    - `patternIsTheFrontView`: `DH_`, `DLR`, `S0S`, `TFT`, `EAE`.
    - `roundTripForEveryCellAndFacing`: unchanged.
    - `receiverIsOnThePlayersRight`: the Receiver directly right of the Alembic, offset (-1,1,0) facing north.
    - `freeCornersAreNoCells`: only (1,2).
    - `placementNamesTheNeighbouringPart`: the new cells.
    - New `tanksAreBesideTheFirebox`: Sulfur tank left, Mercury tank right.
  - Verify: `gradlew :manufacturing:test` green.

## 2. Structure, shell blocks and ports

- [x] 2.1 Register two shell blocks in `ServusManufacturingBlocks`, replacing `CALCINER_SHELL`:
  - `CALCINER_STONE_BRICKS` (`calciner_stone_bricks`) and `CALCINER_DEEPSLATE_TILES` (`calciner_deepslate_tiles`).
  - Both use `CalcinerShellBlock`, with property helpers like Stone Bricks and Deepslate Tiles.
  - Set the `CELL` range to 1 to 4 and update the shell Javadoc.
  - Fix every user: `CalcinerStructure.controllerOf` and `facingOf` match both blocks, plus the datagen loot (`dropOther` Stone Bricks / Deepslate Tiles), block tags (pickaxe) and lang ("Calciner Stone Bricks", "Calciner Deepslate Tiles").
  - Verify: `gradlew :manufacturing:compileJava`.
- [x] 2.2 Update `CalcinerStructure`:
  - `expected` maps `STONE_BRICKS` to `Blocks.STONE_BRICKS` and `DEEPSLATE_TILES` to `Blocks.DEEPSLATE_TILES`.
  - `form` puts the matching shell block in place, with its cell.
  - `unform` puts the vanilla block back.
  - `matches` and `neighbourChanged` drop `AIR` and check only the two tank cells.
  - Update the Javadoc mentioning seven cells.
  - Verify: compile.
- [x] 2.3 Renumber the Copper Duct pieces in `CopperDuctBlock` (manufacturing `alchemy/structure`): `PIECE` 0 to 3, `ATHANOR_NEAR = 2`, `ATHANOR_OVER = 3`. Update its Javadoc for elbow and drop.
  - Verify: compile, and `AlchemyGameTests` still references only the constants.
- [x] 2.4 Make `CalcinerPorts.push` fill the Fluid Tank at `SULFUR_TANK` with Sulfur and at `MERCURY_TANK` with Mercury through `Capabilities.Fluid.BLOCK`, instead of pushing from the ports into touching handlers. Keep `fluid()` and the bucket use. Add `Role.RECEIVER` to `CalcinerStructure.light`.
  - Verify: compile.
- [x] 2.5 Update `CalcinerGameTests`:
  - `build` and `assertFormed` use the new roles.
  - `breakingABrickUnforms` breaks the Stone Bricks and expects a Stone Bricks drop.
  - New `breakingDeepslateDropsDeepslate`.
  - Remove `blockUnderTheSpoutUnforms`.
  - New `oldBricksDoNotForm`: vanilla Bricks right of the Calciner, and the missing message names Stone Bricks.
  - `eachPortGivesItsPrinciple` becomes `tanksFillFromTheCalciner`: Sulfur and Mercury reach their tanks.
  - New `fullTankKeepsMercuryInTheCalciner`.
  - `breakingATankUnforms` uses the single Mercury tank.
  - `schematicMatchesTheBuild` follows the new layout via `ServusManufacturingMultiblockProvider`. Drop the `AIR` skip there.
  - Verify: `gradlew :manufacturing:runGameTestServer` green.

## 3. Formed look

- [x] 3.1 In `AlchemyModels`:
  - `brickFrame` takes a texture. The Ash Pan uses `block/deepslate_tiles`, the Firebox `block/polished_deepslate`, and the mouth `block/stone_bricks` at full height.
  - Delete `trim`, `CORNICE`, `PLINTH` and the `BRICKS` constant if unused.
  - Shell models:
    - Cell 1: stone collar model.
    - Cell 2: stone basin model with the two level grate.
    - Cells 3 and 4: the deepslate tiles cube.
  - Duct pieces 0 (elbow with the across arm 5 px into the Hood) and 1 (drop to y -3).
  - Receiver formed on legs with the tap. Alembic beak from x -4 to 4.
  - Before checking off, list each overlapping box pair of every new or changed model and confirm that no faces share a plane.
  - Verify: `gradlew :manufacturing:runData`, then `gradlew build`.
- [x] 3.2 Update `CalcinerRenderer`:
  - Move the flask fill to the new flask.
  - Draw the Mercury drop (falling 1 px box by game time) from the tap into the basin while the Receiver is `LIT`.
  - Draw the puddle on the basin floor while the Fluid Tank at `MERCURY_TANK` has a non-empty `rawShare()`.
  - Bounding box 3 by 5.
  - Verify: `gradlew build`.

## 4. Docs and journal

- [x] 4.1 Update the PLAN.md M9 Calciner row and the "Machines are built, not placed" paragraph to the compact build (stone and deepslate furnace, one tank per side). Check the Journal Calciner entry text in `JournalEntries` for layout words and fix them.
  - Verify: `gradlew :manufacturing:runData` and `gradlew build`.

## 5. Verification

- [x] 5.1 Full `gradlew build`, `gradlew test` and `gradlew :manufacturing:runGameTestServer` green.
- [x] 5.2 User play check from the main checkout, with all three mods:
  - Build and form the Calciner in all four facings.
  - Check the models from several angles for flicker.
  - Watch the drip and the puddle.
  - Check that the tanks fill.
  - Break a stone and a deepslate block and check the drops.
