# Tasks

## 1. Foundation hook

- [x] 1.1 Add `PotatoInspectable` in `foundation/item`; make `UtilityDuctBlock` (via `DuctReport.at`) and `FluidTankBlock` (via `TankReport.of`) implement it; change `DataPotatoItem.useOn` to ask the clicked block and pass when it gives no report; update the potato tooltip in `ServusFoundationLanguageProvider` and run `:foundation:runData`. Verify `:foundation:build` and the existing `DataPotatoGameTests` pass in `:foundation:runGameTestServer`.

## 2. Rack section report

- [x] 2.1 Add pure `rack/SectionFill` (the `SlotFill.Slot` states in order, used via `SlotFill.inUse`, total, full, top items via `TopCounts.pick`) with JUnit `SectionFillTest`: totals over several slots of one item, largest first, ties in slot order, the limit, empty slots, a locked empty slot counting as used, used and full counts. Verify `:shuttles:test`.
- [x] 2.2 Add `potato_top_items` (example 5) to `rack.json` and `ShuttlesStatsManager`; add `rack/RackReport` and make `HeavyRackBlock` implement `PotatoInspectable` and pass the potato in `useItemOn`; add the `message.servus_shuttles.potato.*` rack keys to `ServusShuttlesLanguageProvider` and run `:shuttles:runData`. Add GameTests in `gametest/PotatoStorageGameTests` (section of three with zone "Ores" and a locked empty slot, full single bay, empty unzoned bay, front click opens no GUI and changes nothing). Verify `:shuttles:build` and the new GameTests pass.

## 3. Gantry report

- [x] 3.1 Add `gantry/GantryReport` (mode line, per tank side, `FluidTankBlock.describe`, lock) and make `FluidGantryBlock` implement `PotatoInspectable` and pass the potato in `useItemOn`; add the gantry lang keys and run `:shuttles:runData`. Add GameTests in `gametest/PotatoStorageGameTests` (Storage gantry with one upgrade, water locked left, empty Any right, click on the beam and on a leg, no GUI, nothing changed). Verify `:shuttles:build` and the GameTests pass.

## 4. Integration

- [x] 4.1 Full `gradlew build`, `gradlew test` and `runGameTestServer` for foundation and shuttles green.
- [x] 4.2 Play check in game (game launches with all mods): potato on a rack section, a full bay, an empty bay and on a gantry beam and leg prints the reports and opens nothing.
