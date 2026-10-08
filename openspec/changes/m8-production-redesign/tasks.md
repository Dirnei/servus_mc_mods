# Tasks

## 1. Pure helpers

- [x] 1.1 Add `storage/.../production/JobStatus` and `MachineOrder` (no Minecraft imports) and row geometry in `terminal/TerminalLayout.Production` (`HEADER`, job and machine row parts, `jobAreas()`, `machineAreas()`). JUnit: `JobStatusTest` (done, cancelled, waiting for CPU, missing, out of fuel, blocked reason, waiting for machine, getting ingredients, crafting 1 of 2, precedence), `MachineOrderTest` (problems first, then position), and `TerminalLayoutTest` cases for job and machine rows. Verify with `gradlew :storage:test`.

## 2. Server rows

- [ ] 2.1 Replace `OrderRow`, `StepRow`, `RecipeRow` with `JobRow` and the compact `MachineRow` and summary `CpuRow` in `production/ProductionRows`; rebuild `ProductionView` (jobs with progress, status from `JobStatus`, recipe verb by recipe type, machine states and `MachineOrder`), `ProductionPayload`, the menu signature and `ClientProduction` accessors. Update `ProductionTerminalGameTests` and `PatternGameTests` to the new rows and add GameTests: job row progress and "crafting" wording, waiting for CPU wording, machines problems first with out of fuel, makes count instead of a recipe list, cancel still cancels. Verify with `gradlew build` and `:storage:runGameTestServer`.

## 3. Screen

- [ ] 3.1 Rework `client/ProductionTerminalScreen`: tabs Crafting, Patterns, Machines; header with CPU tooltip; job rows with icon, title, progress bar, status, X button that alone cancels; fade of ended jobs by `doneAt`; machine rows with icon, name, state, upkeep bar and tooltip; texts cut to their rectangles. Lang keys in `datagen/ServusStorageLanguageProvider` (drop the old order, step and recipe keys), run `:storage:runData`. Verify with `gradlew build` and the Storage GameTests.

## 4. Docs

- [ ] 4.1 Update the Production Terminal section of `docs/src/content/docs/mods/storage.md`. Verify with `npx vitest run` in `docs/`.

## 5. Integration

- [ ] 5.1 Full `gradlew build` and `gradlew test` green, `:storage:runGameTestServer` green, every payload type registered.
