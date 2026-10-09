# Tasks

## 1. Pure layer model

- [x] 1.1 Add `SideLayer` and `FluidSides` to `foundation/.../side` (Off, All, tank index; default All; fixed sides Off; cycle Off, All, tanks, Off; `toBytes`/`fromBytes`). JUnit `FluidSidesTest`: default is All, cycle order with three tanks, fixed side ignores cycle and set, encoding round trip, decoding with a different tank count falls back to All. Verify: `gradlew :foundation:test` green.

## 2. Block entity, sync and carrot

- [x] 2.1 `SidedBlockEntity`: `fluidSides()`, `sideTanks()`, `cycleFluidSide`; `SideTank` record; `MachineBlockEntity` saves and loads the Fluids layer, keeps it on dismantle, reserves `DATA_FLUID_SIDE_FIRST` (six values) and shifts `DATA_COUNT`; `SideConfigMenus` and `MachineMenu` route buttons `1006..1011`. Verify: `gradlew build`, all GameTests green (nothing moved).
- [x] 2.2 Carrot layer: data component `servus_foundation:carrot_layer`, `use` on the air switches the layer with an action bar message, tooltip line; `SideConfigWrench` cycles the Fluids side for a carrot set to Fluids on blocks that have one; lang. GameTest: a carrot set to Fluids cycles a Calciner's Fluids side and leaves its Items side unchanged. Verify: Foundation and Manufacturing GameTests green.

## 3. Calciner and Coagulator

- [x] 3.1 Per side tank views in `PrincipleTanks`; Calciner and Coagulator declare their tanks, the fluid capability follows the Fluids layer, `AlchemyMachines.push` pushes on tank and All sides. GameTests: two pipes from a Calciner set to Salt and Mercury fill two tanks with one principle each; a Coagulator side set to Mercury refuses Salt; an unconfigured Calciner still fills a touching tank. Verify: Manufacturing GameTests green.

## 4. Client

- [ ] 4.1 `SideConfigPanel` tabs (chest and bucket icons), fluid cell colours and letters, tooltips "Left: Salt"; screens keep the selected tab; `SideMarkerRenderer` draws the carrot's layer with tank colours. Verify: `gradlew build`, client check owed to the play check from main.

## 5. Plan and docs

- [ ] 5.1 PLAN.md: add `m9-side-config-tabs` to the M9 list and one sentence in section 2.1; docs `mods/foundation.md` side configuration section describes the Fluids layer, tabs and the carrot layer; `mods/manufacturing.md` mentions one principle per side. Verify: docs tests and docs build pass.

## 6. Integration

- [ ] 6.1 Full `gradlew build`, `gradlew test`, GameTests of all three mods green, and the client launches from main with all three mods: set a Calciner's sides to Salt and Mercury in the Fluids tab and with the carrot, pipe each to its own tank on its own channel, and see the fluid markers.
