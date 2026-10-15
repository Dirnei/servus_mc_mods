# Tasks

## 1. Speed as pure logic and data

- [x] 1.1 Add `alchemy/.../calciner/EternalFireSpeed.java` with `cycleTicks(int recipeTicks, int speed)` (`max(1, recipeTicks / max(1, speed))`) and `alchemy/src/test/java/.../calciner/EternalFireSpeedTest.java` with tests `halvesTheTime` (80, 2 gives 40), `roundsDown` (81, 2 gives 40), `neverBelowOneTick` (1, 4 gives 1), `speedOneKeepsTheTime` (80, 1 gives 80), `speedBelowOneCountsAsOne` (80, 0 gives 80). Verify: `gradlew :alchemy:test` green.
- [x] 1.2 Add `alchemy/.../calciner/EternalFireData.java` (reload listener for `servus_alchemy/eternal_fire`, file `calciner`, field `speed` 1 to 64, default 1), register it in `AlchemyEvents.java`, and ship `alchemy/src/main/resources/data/servus_alchemy/servus_alchemy/eternal_fire/calciner.json` with `{ "speed": 2 }`. Verify: `gradlew build` green.

## 2. Fire that never goes out

- [x] 2.1 `foundation/.../machine/BurnerMachineBlockEntity.java`: add `protected boolean eternalFire()` (default false); while true `tick()` burns without `consumeFuel()` and without counting `burnTime` down, `upkeep()` returns null and `getData` reports a full burn. Verify: `gradlew build` and `gradlew test` green, existing burner GameTests unchanged.
- [ ] 2.2 `alchemy/.../CalcinerBlockEntity.java`: third slot `SLOT_IGNIS`, `eternalFire()` from it, `workTicks()` through `EternalFireSpeed` with `EternalFireData`, `burns()` false while the Ignis is set, `setIgnis(Player, InteractionHand)` (one item, fuel slot back to the player), `takeIgnis(Player)`, `removeIgnis()` for drops, `Ignis` in `getUpdateTag` and a block update plus `invalidateCapabilities` on the Firebox position on every change. Verify: `gradlew build` green.

## 3. The Firebox

- [ ] 3.1 `alchemy/.../calciner/CalcinerPartBlock.java`: Ignis in hand on a formed Firebox sets it (before the fuel branch), sneak with an empty hand takes it back, and `affectNeighborsAfterRemoval` on the Firebox pops the Ignis before the structure unforms. Verify: `gradlew build` green.
- [ ] 3.2 GameTests in `alchemy/.../gametest/CalcinerGameTests.java`: `ignisSetByHand`, `ignisReturnsFuel`, `ignisOnlyOne`, `ignisNotAtTheMouth`, `ignisNotByPipe`, `ignisBurnsWithoutFuel`, `ignisRefusesCoal` (hand and port), `ignisNoUpkeep`, `ignisTwiceAsFast` (40 against 80 ticks with the shipped data), `ignisTakenBack`, `ignisKeptWhileUnformed`, `fireboxBrokenDropsIgnis`, `calcinerBrokenDropsIgnis`; `SalamanderGameTests.ignisIsNotAFuel` still passes. Verify: `gradlew build` and the GameTest run green.

## 4. Show and tell

- [ ] 4.1 `alchemy/.../client/CalcinerRenderer.java`: draw the Ignis Perpetuus item in the Firebox behind the grate at full brightness while the synced `Ignis` is true. List in the task notes, for the item quad against the grate bars and the Firebox front face, that no faces share a plane (2 px behind the front, 0.05 px inset). Verify: `gradlew build` green.
- [ ] 4.2 Jade: `alchemy/.../integration/jade/ServusAlchemyJadePlugin.java` writes `Ignis` and shows `jade.servus_alchemy.calciner.eternal_fire` instead of the fuel line; add the key in `ServusAlchemyLanguageProvider.java`. Verify: `gradlew runData` and `gradlew build` green.
- [ ] 4.3 Journal: `alchemy/.../datagen/book/WorkshopCategory.java` Ignis Perpetuus spotlight and the Calciner fire text say it burns a Calciner forever and faster when set into the Firebox, and a sneak with an empty hand takes it back. Verify: `gradlew runData` regenerates the pages, `gradlew build` green.

## 5. Plans and docs

- [ ] 5.1 `alchemy_plan.md`: remove the answered open question "Is Ignis Perpetuus also an endless fuel" and note under Citrinitas that it fires the Calciner forever and faster; `docs/src/content/docs/mods/alchemy.md`: replace "It is not a fuel" for Ignis Perpetuus and add the eternal flame to the Calciner line. Verify: docs build (`npm run build` in `docs/`) green.

## 6. Verification

- [ ] 6.1 Full `gradlew build` and `gradlew test` green, the game launches with all three mods; play check by the user from the main checkout: set the Ignis by hand, coal comes back, it calcines twice as fast without fuel, the glow in the grate seen from the front and both sides idle and working without flicker, Jade line, take back, break the Firebox and the Calciner.
