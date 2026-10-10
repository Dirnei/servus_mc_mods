# Tasks

## 1. Shared geometry and layout

- [x] 1.1 Move `Facing` and `Offset` into `alchemy/structure` and add `Structures` (facing, direction, pos); switch `CalcinerLayout`, `CalcinerStructure`, `CalcinerPorts`, `CalcinerRenderer`, the Calciner GameTests and `CalcinerLayoutTest` to them; `gradlew build`, `gradlew test` and `:manufacturing:runGameTestServer` green
- [x] 1.2 Pure `alchemy/coagulator/CoagulatorLayout` with JUnit `CoagulatorLayoutTest` (cells, Vat principles, pattern, round trips, placement); `gradlew :manufacturing:test` green

## 2. Build

- [x] 2.1 `VatBlock` (item, recipe, loot, tag, lang, unformed and formed models), `FORMED` on `CoagulatorBlock`, `CoagulatorStructure` (check, form, unform, neighbour checks of the Stone Bricks, Journal message), the Coagulator's formed model; the carrot passes; `gradlew :manufacturing:runData` and `gradlew build` green
- [x] 2.2 Ports by block (Vat fluid views, Coagulator item port), block entity registrations removed, side tanks and item sides off, works only when formed, owner is the forming player, buckets on Vats, GUI opens only when formed and has no side panel; `CoagulatorGameTests` (forming, missing Vat, unform by brick, Vat principles, wrong bucket, items, unformed idle) and the existing Coagulator GameTests forming first; `:manufacturing:runGameTestServer` green

## 3. Look and journal

- [x] 3.1 Vat levels drawn by the Coagulator renderer with fill sync; schematic generated from `CoagulatorLayout`, Coagulator journal page with multiblock, Vat recipe and text; a GameTest that the schematic accepts the build; `gradlew :manufacturing:runData`, `gradlew build` and `:manufacturing:runGameTestServer` green and the client launches without model errors
- [x] 3.2 PLAN.md M9 item 6c text; no em dashes

## 4. Integration

- [x] 4.1 Full `gradlew build`, `gradlew test` and `:manufacturing:runGameTestServer` green, and the user play checks the Coagulator build
