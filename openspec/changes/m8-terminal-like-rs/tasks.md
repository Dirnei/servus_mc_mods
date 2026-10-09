# Tasks

## 1. Pure helpers

- [ ] 1.1 Add `storage/.../terminal/AmountSteps.java`, `TerminalSort.java` and `TerminalSettings.java` (no Minecraft imports) with JUnit tests `AmountStepsTest` (plus stack 1 -> 64, 64 -> 128, 70 -> 128, 0 -> 64; minus stack 70 -> 64, 128 -> 64, 64 -> 1, 10 -> 1; add clamps to 1..99999), `TerminalSortTest` (name, amount, mod, both directions, ties) and `TerminalSettingsTest` (round trip, unknown values fall back to defaults). Verify with `gradlew :storage:test`.

## 2. Layout

- [ ] 2.1 Rework `terminal/TerminalLayout.Counter`: base slot constants for the menu, `layout(rows)` returning all rectangles with the upward extension, `rowsFor(height setting, screen height)`; update `menu/CounterTerminalMenu` slot positions; extend `TerminalLayoutTest` for every height at 1080p GUI scales 2, 3 and 4 (no overlaps, inside the panel, panel on the screen, side column left of the panel). Verify with `gradlew :storage:test` and `gradlew build`.

## 3. Screen

- [ ] 3.1 Rewrite `client/CounterTerminalScreen` on the new layout: side column buttons with icons and tooltips, requests as first grid entries with tint, counts, state marks and cancel on click, sorting, view filter, height, dialogs covering the grid and crafting row at z 400 with the six step buttons; add `client/ClientTerminalSettings` saving to `config/servus_storage-terminal.properties`; button icons generated with the PNG helper; lang entries. Verify with `gradlew build` and `:storage:runGameTestServer`.
- [ ] 3.2 Add `client/JeiSearchBridge`, fill it in `integration/jei/ServusStorageJeiPlugin` (runtime available and unavailable) and register a gui container handler with the side column and upward extension as extra areas. Verify with `gradlew build` and `:storage:runGameTestServer`.

## 4. Docs

- [ ] 4.1 Update `docs/src/content/docs/mods/storage.md` (side buttons, requests in the grid, amount steps, JEI sync). Verify with `npx vitest run` in `docs/`.

## 5. Integration

- [ ] 5.1 Full `gradlew build` and `gradlew test` green, Storage GameTests green, `openspec validate m8-terminal-like-rs --strict`.
