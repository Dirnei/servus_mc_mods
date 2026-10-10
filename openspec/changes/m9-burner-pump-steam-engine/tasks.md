# Tasks

## 1. Two block Burner Pump

- [x] 1.1 Read the decompiled 1.21.1 `DoorBlock` and `DoublePlantBlock` (placement, `updateShape`, `playerWillDestroy`, `preventDropFromBottomPart`) and note the exact signatures in a comment on `BurnerPumpBlock`. Verify: `gradlew :foundation:compileJava` passes.
- [x] 1.2 Add `DOUBLE_BLOCK_HALF` to `BurnerPumpBlock` (`foundation/.../machine/pump/BurnerPumpBlock.java`): placement needs a replaceable block above, `setPlacedBy` sets the upper half, `updateShape` removes a half without its partner and copies `FACING` and `LIT` to the upper half, creative break drops nothing, no block entity on the upper half, `PushReaction.BLOCK`, and `useWithoutItem` and `onWrench` on the upper half redirect to the lower half. Verify: `gradlew build`.
- [x] 1.3 Turn `PumpBlock.givesWater` into the overridable `opensFace(state, face)` used by the stub logic and the Electric Pump energy registration in `MachineEvents.java`. `BurnerPumpBlock` excludes `UP` and every face of the upper half. Lock `RelativeSide.TOP` to `DISABLED` for `BurnerPumpBlockEntity`, next to the `PumpLogic` bottom lock. Verify: `gradlew build`.
- [x] 1.4 Give each half its own shape in `BurnerPumpBlock` from the boxes in `BurnerPumpParts` (JUnit `BurnerPumpPartsTest`: rotation, rod clear of the side stub): lower = base plate, boiler and stubs; upper = chimney, cylinder and pivot post (pixel boxes from design D3). Verify: `gradlew build`.
- [x] 1.5 Loot: in `MachineBlockLoot.java` and `MachineDataParts.selfDropping()`, take the Burner Pump out of `dropSelf` and give it `createSinglePropConditionTable(..., DOUBLE_BLOCK_HALF, LOWER)`. Verify: `gradlew runData` regenerates `loot_table/blocks/burner_pump.json` with the half condition, then `gradlew build`.
- [x] 1.6 GameTests in `PumpGameTests.java`: the `burnerPump` helper places both halves; `burnerPumpFillsAdjacentCauldron` reads the buffer from a side instead of `UP`; `MachineBaseGameTests.burnerMachinesExposeUpkeep` places the upper half for the Burner Pump. Add the tests `burnerPumpPlacesTwoHalves` (item use by a mock player on stone with air above), `burnerPumpNeedsRoomAbove`, `breakingEitherHalfDropsOnePump` (each half with `destroyBlock(pos, true)`, one item entity and the coal), `burnerPumpUpperHalfHasNoCapabilities` and `burnerPumpTopFaceClosed` (no fluid handler on `UP`, the item handler there refuses coal, `up` stub 0 with a Fluid Pipe duct beside the upper half). Verify: `gradlew test` and `gradlew runGameTestServer` green.

## 2. Look: models, beam, smoke

- [ ] 2.1 Pure `BeamStroke` in `foundation/.../machine/pump/BeamStroke.java` (no Minecraft imports): angle from phase, amplitude and period, and rod offset from angle and lever arm. JUnit `BeamStrokeTest` in `foundation/src/test/.../machine/pump/`: angle is 0 at phase 0, never exceeds the amplitude, repeats after one period, and rod offset is 0 at angle 0, has the same sign as the angle and equals `arm * sin(angle)`. Verify: `gradlew test`.
- [ ] 2.2 Datagen in `MachineDataParts.java`: the Burner Pump gets its own multipart. The lower body per facing and lit state (base plate, boiler, front texture), the upper body per facing (chimney, cylinder, pivot post), the existing stub parts on the lower half only, and standalone models `burner_pump_beam`, `burner_pump_rod` and `burner_pump_piston` from `BurnerPumpParts`. The Electric Pump keeps `pump(...)` unchanged. Add an item model with both halves stacked and scaled display transforms. Verify: `gradlew runData`, then `gradlew build`.
- [ ] 2.3 `BurnerPumpRenderer` in `foundation/.../client/`: draw beam and rod with `BeamStroke`, a client side phase field on `BurnerPumpBlockEntity` that advances only while `LIT`, and a render box over both halves. Register it instead of `SideMarkerRenderer` and register the three standalone models in `MachineClient.java`. Verify: `gradlew build`.
- [ ] 2.4 Smoke: `BurnerPumpBlock.animateTick` on a lit lower half spawns `CAMPFIRE_COSY_SMOKE` at the chimney top, rotated by facing. Verify: `gradlew build`, then the user launches the client from main and checks on a pond: two blocks placed, beam rocks and smoke rises while burning, both stop when unlit, stubs on the back and sides, no stub on top, the item icon reads as an engine.

## 3. Plan and docs

- [ ] 3.1 `PLAN.md`: add M9 item 1b `m9-burner-pump-steam-engine` and describe the Burner Pump as a two block beam engine in the pumps paragraph of section 2. `docs/src/content/docs/mods/foundation.md`: the Burner Pump line says it is two blocks tall, smokes and rocks its beam while burning, and gives water through its back and sides. Verify: `npm run build` in `docs/` passes.

## 4. Integration

- [ ] 4.1 Full gradle build, tests green, game launches with all three mods.
