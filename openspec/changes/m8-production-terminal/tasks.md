# Tasks

## 1. Block on the data line

- [x] 1.1 Add `storage/production/ProductionTerminalBlock` and `ProductionTerminalBlockEntity` (facing, online lamp refreshed every 20 ticks from CPUs and warehouses on the line, `DataLink`), register block, item, block entity, the `DATA_LINK` capability in `ServusStorageEvents` and the creative tab order. Verify with `gradlew build`.
- [x] 1.2 Datagen: oriented block state, item model, loot (drop self), pickaxe tag, recipe, lang; textures `production_terminal_front`, `production_terminal_front_online`, `production_terminal_side`, `production_terminal_top` via the PNG helper; copy the GUI panel to `textures/gui/production_terminal.png`. Run `:storage:runData`, verify the generated files, then `gradlew build`.

## 2. View model, menu and payloads

- [ ] 2.1 Add the row records (`MachineRow`, `RecipeRow`, `CpuRow`, `OrderRow`, `StepRow`), `ProductionPayload`, `CancelProductionOrderPayload`, `ProductionView` (server snapshot) and `ProductionTerminalMenu` (sync every 10 ticks on signature change, cancel path); register the menu type and both payloads in `ServusStoragePayloads`, and grep that every payload `TYPE` in the storage sources is registered. Verify with `gradlew build`.
- [ ] 2.2 GameTests `ProductionTerminalGameTests`: `productionTerminalJoinsLineAndLights`, `productionTerminalAloneIsDark`, `productionTerminalListsIdleStation`, `productionTerminalListsCpuOrder`, `productionTerminalCancelsOrder`, `productionTerminalNoCpuRows`. Verify with `:storage:runGameTestServer`, all green.

## 3. Screen

- [ ] 3.1 Add `ClientProduction` and `ProductionTerminalScreen` with the tab bar (ordered tab list, Orders selected first), Machines and Orders tabs, per tab scroll, tooltips and click to cancel; register the screen in `ServusStorageClient` and the payload handler on the client. Verify with `gradlew build`.

## 4. Docs and integration

- [ ] 4.1 Describe the Production Terminal on `docs/src/content/docs/mods/storage.md`; regenerate `docs/src/data/gamedata.json` with `:manufacturing:runData`; run `npx vitest run` in `docs/`. Verify the tests pass.
- [ ] 4.2 Full `gradlew build` and `gradlew test`, GameTests of Foundation, Manufacturing and Storage green, `runClientAll` reaches the title screen without asset warnings.
