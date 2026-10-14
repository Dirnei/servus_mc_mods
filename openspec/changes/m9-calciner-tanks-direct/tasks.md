# Tasks

## 1. Ash room

- [x] 1.1 Add the optional `item_capacity` field to `foundation/.../machine/MachineStats.java` (record, `Partial` codec, `over`, factories pass 0) and extend `MachineDefaultsTest` with a case that reads it and one where it is missing (0). Verify: `gradlew :foundation:test` green.
- [ ] 1.2 Add pure `AshCount.fits(salt, extra, ashRoom, saltPerAsh)` and `AshCount.room(ashRoom, saltPerAsh)` (a room below 1 counts as 64) in `manufacturing/.../alchemy/AshCount.java`, with JUnit cases in `AshCountTest`: fits up to exactly 64 Ash, 1 mB over does not fit, a zero room counts as 64, uneven Salt per Ash. Verify: `gradlew :manufacturing:test` green.

## 2. Calciner uses its tanks

- [ ] 2.1 In `CalcinerBlockEntity`, replace `PrincipleTanks` with `int salt`. `canWork` checks the Ash room and simulated fills of the Sulfur and Mercury tanks; `finishWork` fills both tanks and adds the Salt. Drop the push timer. Keep `ashCount`, `takeAsh` and save/load (`Salt`). In `CalcinerPorts`, add `fits`/`fill` helpers for the two structure tanks and remove `push`, `fluid` and `principle`. Remove the `Capabilities.Fluid.BLOCK` registration in `ServusManufacturingEvents` and the fluid container branch in `CalcinerPartBlock.useItemOn`. Set `"item_capacity": 64` and drop `fluid_capacity` in `machine/calciner.json`. Update the Javadoc of the touched classes. Verify: `gradlew build` green.
- [ ] 2.2 Sync and draw: `getUpdateTag` sends `Salt` and `AshRoom`; `DrawnFill` gets an overload for one amount and room; `CalcinerRenderer` drops the Receiver fill and draws the ash heap from Salt and room. In `ServusManufacturingJadePlugin`, show Ash as "%s / %s Ash" with a new lang key in `ServusManufacturingLanguageProvider`, and drop the principle lines. `GameDataExportProvider` exports `item_capacity` when set. Verify: `gradlew :manufacturing:runData` and `gradlew build` green.
- [ ] 2.3 Rewrite the GameTests in `CalcinerGameTests`:
  - `tanksFillFromTheCalciner`: a finished cycle lands in the tanks and the Ash Pan
  - `fullTankKeepsMercuryInTheCalciner` becomes `fullMercuryTankStopsTheCalciner`: a full Mercury tank keeps the input and makes nothing
  - `bucketFillsAtTheReceiver` becomes `bucketDoesNothingAtTheReceiver`
  - add `ashPanHoldsAStack`: it stops at 64 Ash
  - add `noFluidPortOnTheReceiverOrDuct`
  - adjust `hopperFeedsAFormedCalciner`, `unformedCalcinerDoesNothing`, `hopperTakesAshFromTheAshPan` and `handTakesAshFromTheAshPan` to the Salt amount

  Verify: `gradlew :manufacturing:runGameTestServer` green.

## 3. Copper condenser

- [ ] 3.1 In `manufacturing/.../datagen/AlchemyModels.java`, replace the flask of `receiver` (formed and unformed) with the copper condenser from design.md, shorten the formed Alembic beak to x -5, and update both Javadocs. List the face check from design.md against the generated JSON. Verify: `gradlew :manufacturing:runData` writes `receiver.json`, `receiver_formed.json` and `alembic_formed.json` without a glass texture, and `gradlew build` is green.

## 4. Texts

- [ ] 4.1 Update the Calciner entry in `manufacturing/.../datagen/book/WorkshopCategory.java` (condenser and drip, tanks filled directly, Ash Pan holds a stack, waits when the Ash Pan or a tank is full) and the Calciner line in `docs/src/content/docs/mods/manufacturing.md`. Add this change to the M9 list in `PLAN.md`. Verify: `gradlew :manufacturing:runData` green and docs tests green.

## 5. Integration

- [ ] 5.1 Full `gradlew build`, unit tests and the manufacturing GameTests green. The game launches with all three mods.
- [ ] 5.2 Play check in game by the user from main:
  - The Receiver is a copper drum on legs in the basin, the beak enters its side, and the tap drips while working.
  - Nothing flickers, floats or is see-through.
  - The tanks fill directly.
  - The Calciner stops when the Mercury tank or the Ash Pan is full, and the Ash Pan gives 64 Ash.
  - Jade shows "x / 64 Ash".
  - A bucket on the Receiver does nothing.

  Verify: the user confirms in game.
