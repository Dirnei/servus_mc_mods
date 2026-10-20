# Tasks

## 1. Foundation item

- [x] 1.1 `foundation/.../upgrade/HeatingElementItem`; registration in `ServusFoundationItems`, Servus tab, recipe, lang and item model in the Foundation datagen providers; `UpgradeGameTests.heatingElementIsNoUpgrade` (D1). Verify: `:foundation:runData`, `gradlew build`, `:foundation:runGameTestServer` green.

## 2. Pure heat logic

- [x] 2.1 `delicacies/.../firebox/HeatingStats`, `EnergyDraw`, `ElectricHeat`, `FireboxHeat`, `GaugeSync`, `GaugeNeedle` (D2); JUnit `HeatingStatsTest`, `ElectricHeatTest`, `FireboxHeatTest`, `GaugeSyncTest`, `GaugeNeedleTest`. Verify: `gradlew :delicacies:test` and `gradlew build` green.

## 3. Firebox on FE

- [x] 3.1 `firebox/HeatingStatsManager`, `firebox/FireboxHeating`, `firebox/ElementUse`, `data/servus_delicacies/heating/tiled_stove.json`, reload listener in `ServusDelicaciesEvents` (D3). Verify: `gradlew build` green.
- [x] 3.2 Tiled Stove: `StoveTileBlock` (`HEATER`, `PORT`, light, hand use, particles), `StoveTileBlockEntity` (heating field, fuel rule, unform drop, tick sync, update tag, energy capability, status), energy capability registration, lang (D4). Verify: `gradlew build` green.
- [x] 3.3 Brewhouse: `BrewhouseBricksBlock` (`HEATER`, light, hand use, particles), `BrewhouseBlockEntity` (same as the stove, port on `KILN_BACK`), `data/servus_brewery/heating/brewhouse.json`, energy capability in `ServusBreweryEvents` (D5). Verify: `gradlew build` green.

## 4. Look

- [x] 4.1 `tools/heating-textures/gen.py`: element item, stove and Brewhouse coil doors (hot and cold), gauge dial, needle, port (D8). Verify: the script runs clean, look at an 8x preview.
- [x] 4.2 Datagen: stove tile models per door, lit, heater and port with the dial and port elements; the needle standalone model; `BrewhouseModel.HEATER_PARTS` and the Brewhouse brick models with `HEATER`; overlap check from the design table against the final boxes (D6). Verify: `:delicacies:runData`, `:brewery:runData`, generated files committed, `gradlew build` green.
- [x] 4.3 Client: `client/GaugeRenderer` with the needle key, registered for the stove tile in `ServusDelicaciesClient` and for the Brewhouse in `BreweryClient` (D7). Verify: `gradlew build` green.

## 5. GameTests

- [x] 5.1 `delicacies/.../gametest/HeatingElementGameTests` registered in `DelicaciesGameTests`; Brewhouse element tests in `BrewhouseGameTests` (D9). Verify: `:delicacies:runGameTestServer`, `:brewery:runGameTestServer`, `gradlew build test` green.

## 6. Play check

- [ ] 6.1 Play check by the user:
  - Craft a Heating Element; it does not go into a Speed Upgrade slot or the Sink's capacity slots.
  - Right click a formed Tiled Stove with it: the door shows dull coils, a brass thermometer appears above the door and a small iron port on the right end; a second element is refused; Coal is refused while it is in.
  - Run a power cable to the port: with a Pot cooking the needle climbs steadily, the coils turn warm orange, the Pot cooks without fuel; cut the power: the needle sinks slowly and the Pot stops once it is cold; no FE goes in on any other face.
  - Sneak with an empty hand on a tile: the element comes out, gauge, port and coils disappear, the wood fire works as before. Break a tile with the element in: it drops.
  - The same on the Brewhouse: port on the kiln side's right end, gauge between the doors, coils on both doors, the kettle brews and the kiln dries on FE.
  - Look at the gauge, needle and port from several angles and while the needle moves: no flicker, the dial reads round, the port reads as a solid iron plate.
